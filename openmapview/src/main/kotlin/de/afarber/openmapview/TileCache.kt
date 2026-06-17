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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Two-level cache for map tiles: in-memory LRU cache backed by persistent disk cache.
 *
 * Memory cache holds up to 1/8 of available heap memory for fast access.
 * When tiles are evicted from memory, they are automatically written to disk cache.
 * Disk cache stores up to 50MB of tiles to reduce network requests across app restarts.
 */
class TileCache(
    context: Context? = null,
) {
    companion object {
        private val TAG = TileCache::class.java.simpleName
    }

    // Use 1/8 of available heap memory for tile cache
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private var diskCache: DiskTileCache? =
        context?.let { DiskTileCache(it) }

    private val memoryCache =
        object : LruCache<TileCoordinate, Bitmap>(cacheSize) {
            override fun sizeOf(
                key: TileCoordinate,
                bitmap: Bitmap,
            ): Int = bitmap.byteCount / 1024

            // When a tile is evicted from memory, save it to disk for later reuse
            override fun entryRemoved(
                evicted: Boolean,
                key: TileCoordinate,
                oldValue: Bitmap,
                newValue: Bitmap?,
            ) {
                if (evicted && diskCache != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            diskCache?.put(key, oldValue)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to write evicted tile to disk cache", e)
                        }
                    }
                }
            }
        }

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

    fun put(
        tile: TileCoordinate,
        bitmap: Bitmap,
    ) {
        memoryCache.put(tile, bitmap)
    }

    fun clear() {
        memoryCache.evictAll()
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
