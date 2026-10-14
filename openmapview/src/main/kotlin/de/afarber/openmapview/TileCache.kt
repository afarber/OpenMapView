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
 * Memory cache holds up to 1/8 of available heap memory for fast access.
 * [put] also writes the tile to the disk cache, so call it from a background thread.
 * Disk cache stores up to 50MB of tiles to reduce network requests across app restarts.
 *
 * A tile older than the disk cache's maximum age is only returned by [getStale], as a
 * fallback when a fresh download failed. It is then marked as stale (see [isStale]) until
 * a fresh tile is put.
 *
 * @param context Context for the disk cache, or null for a memory-only cache
 * @param namespace Disk cache namespace, e.g. the tile source, so tiles of different
 *   sources are stored separately
 */
class TileCache internal constructor(
    private var diskCache: DiskTileCache?,
    // Use 1/8 of available heap memory for tile cache
    cacheSize: Int = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt(),
) {
    constructor(
        context: Context? = null,
        namespace: String = DiskTileCache.DEFAULT_NAMESPACE,
    ) : this(context?.let { DiskTileCache(it, namespace) })

    companion object {
        private val TAG = TileCache::class.java.simpleName
    }

    // Tiles in the memory cache that came from an expired disk entry
    private val staleTiles: MutableSet<TileCoordinate> = Collections.newSetFromMap(ConcurrentHashMap())

    private val memoryCache =
        object : LruCache<TileCoordinate, Bitmap>(cacheSize) {
            override fun sizeOf(
                key: TileCoordinate,
                bitmap: Bitmap,
            ): Int = bitmap.byteCount / 1024

            // A tile that leaves the memory cache is no longer shown, so forget its stale mark
            override fun entryRemoved(
                evicted: Boolean,
                key: TileCoordinate,
                oldValue: Bitmap,
                newValue: Bitmap?,
            ) {
                staleTiles.remove(key)
            }
        }

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
            memoryCache.put(tile, diskBitmap)
            return diskBitmap
        }

        return null
    }

    fun getMemory(tile: TileCoordinate): Bitmap? = memoryCache.get(tile)

    /**
     * Returns true if the tile in the memory cache is an expired one from [getStale],
     * which should be replaced by a fresh download.
     */
    fun isStale(tile: TileCoordinate): Boolean = tile in staleTiles

    /**
     * Returns the tile even if its disk entry has expired, for use when downloading a fresh
     * tile failed (e.g. offline). An expired tile is promoted to memory and marked as stale.
     */
    fun getStale(tile: TileCoordinate): Bitmap? {
        get(tile)?.let { return it }
        val diskBitmap = diskCache?.get(tile, allowStale = true) ?: return null
        memoryCache.put(tile, diskBitmap)
        staleTiles.add(tile)
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
        memoryCache.put(tile, bitmap)
        staleTiles.remove(tile)
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
        memoryCache.evictAll()
        staleTiles.clear()
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
