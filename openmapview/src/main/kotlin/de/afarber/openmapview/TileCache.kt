/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Two-level cache for map tiles: in-memory LRU cache backed by persistent disk cache.
 *
 * Memory cache holds up to 1/8 of available heap memory for fast access. The tile caches
 * of all maps with a disk cache share one memory cache, with the tiles of each namespace
 * kept apart. So a map shown again, e.g. in a tab or a list, draws its tiles from the first
 * frame on, instead of waiting for them to be read from disk.
 * [put] also writes the tile to the disk cache, so call it from a background thread.
 * Disk cache stores up to 50MB of tiles to reduce network requests across app restarts.
 *
 * A tile older than the disk cache's maximum age is only returned by [getStale], as a
 * fallback when a fresh download failed. It is then marked as stale (see [isStale]) until
 * a fresh tile is put.
 *
 * @param context Context for the disk cache, or null for a memory-only cache of its own
 * @param namespace Disk cache namespace, e.g. the tile source, so tiles of different
 *   sources are stored separately
 */
class TileCache private constructor(
    private var diskCache: DiskTileCache?,
    private val memory: MemoryTiles,
    private val namespace: String,
) {
    constructor(
        context: Context? = null,
        namespace: String = DiskTileCache.DEFAULT_NAMESPACE,
    ) : this(
        context?.let { DiskTileCache(it, namespace) },
        if (context != null) sharedMemory else MemoryTiles(DEFAULT_MEMORY_KB),
        namespace,
    )

    // With a memory cache of its own, e.g. for tests
    internal constructor(
        diskCache: DiskTileCache?,
        cacheSize: Int = DEFAULT_MEMORY_KB,
    ) : this(diskCache, MemoryTiles(cacheSize), DiskTileCache.DEFAULT_NAMESPACE)

    companion object {
        private val TAG = TileCache::class.java.simpleName

        // Use 1/8 of available heap memory for tile cache
        private val DEFAULT_MEMORY_KB = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

        private val sharedMemory = MemoryTiles(DEFAULT_MEMORY_KB)

        // Empties the memory cache shared by all maps, so tests start without tiles
        internal fun clearSharedMemory() = sharedMemory.evict { true }
    }

    private data class Key(
        val namespace: String,
        val tile: TileCoordinate,
    )

    // An LRU cache of bitmaps with a size in KB, and the stale marks of its tiles
    private class MemoryTiles(
        cacheSize: Int,
    ) {
        // Tiles in the memory cache that came from an expired disk entry
        val staleTiles: MutableSet<Key> = Collections.newSetFromMap(ConcurrentHashMap())

        val bitmaps =
            object : LruCache<Key, Bitmap>(cacheSize) {
                override fun sizeOf(
                    key: Key,
                    bitmap: Bitmap,
                ): Int = bitmap.byteCount / 1024

                // A tile that leaves the memory cache is no longer shown, so forget its stale mark
                override fun entryRemoved(
                    evicted: Boolean,
                    key: Key,
                    oldValue: Bitmap,
                    newValue: Bitmap?,
                ) {
                    staleTiles.remove(key)
                }
            }

        fun evict(predicate: (Key) -> Boolean) {
            bitmaps
                .snapshot()
                .keys
                .filter(predicate)
                .forEach { bitmaps.remove(it) }
        }
    }

    private fun key(tile: TileCoordinate) = Key(namespace, tile)

    /**
     * Returns the tile from memory (which may be stale, see [isStale]), or a fresh tile from disk.
     */
    fun get(tile: TileCoordinate): Bitmap? {
        // Check memory cache first for fastest access
        val memoryBitmap = getMemory(tile)
        if (memoryBitmap != null) {
            return memoryBitmap
        }

        // Fall back to disk cache if not in memory
        val diskBitmap = diskCache?.get(tile)
        if (diskBitmap != null) {
            // Promote disk-cached tile back to memory for faster subsequent access
            memory.bitmaps.put(key(tile), diskBitmap)
            return diskBitmap
        }

        return null
    }

    fun getMemory(tile: TileCoordinate): Bitmap? = memory.bitmaps.get(key(tile))

    /**
     * Returns true if the tile in the memory cache is an expired one from [getStale],
     * which should be replaced by a fresh download.
     */
    fun isStale(tile: TileCoordinate): Boolean = key(tile) in memory.staleTiles

    /**
     * Returns the tile even if its disk entry has expired, for use when downloading a fresh
     * tile failed (e.g. offline). An expired tile is promoted to memory and marked as stale.
     */
    fun getStale(tile: TileCoordinate): Bitmap? {
        get(tile)?.let { return it }
        val diskBitmap = diskCache?.get(tile, allowStale = true) ?: return null
        memory.bitmaps.put(key(tile), diskBitmap)
        memory.staleTiles.add(key(tile))
        return diskBitmap
    }

    /**
     * Puts the tile into the memory cache and the disk cache.
     *
     * @param bytes The tile as sent by the server, stored on disk as is. If null, the
     *   bitmap is encoded as PNG for the disk cache.
     */
    fun put(
        tile: TileCoordinate,
        bitmap: Bitmap,
        bytes: ByteArray? = null,
    ) {
        memory.bitmaps.put(key(tile), bitmap)
        memory.staleTiles.remove(key(tile))
        // Write to disk right away, so the tile survives the process even if it is never
        // evicted from memory
        try {
            if (bytes != null) {
                diskCache?.put(tile, bytes)
            } else {
                diskCache?.put(tile, bitmap)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write tile to disk cache", e)
        }
    }

    fun clear() {
        // Other caches may share the memory cache, so only remove the tiles of this one
        memory.evict { it.namespace == namespace }
        try {
            diskCache?.clear()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear disk cache", e)
        }
    }

    fun close() {
        try {
            diskCache?.close()
            diskCache = null
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close disk cache", e)
        }
    }
}
