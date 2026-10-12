/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.jakewharton.disklrucache.DiskLruCache
import java.io.File
import java.io.IOException

/**
 * Persistent disk cache for map tiles using DiskLruCache.
 *
 * Stores up to 50MB of tile images in PNG format in the app's cache directory.
 * Tiles persist across app restarts, reducing network usage on subsequent launches.
 * Uses LRU eviction when cache size limit is reached.
 *
 * The cache stays usable after [clear], and a closed or failing cache only results
 * in cache misses. Methods are synchronized because tiles are read and written from
 * background threads while the main thread may clear or close the cache.
 */
class DiskTileCache(
    context: Context,
) {
    private val maxSize = 50L * 1024 * 1024
    private val cacheDir = File(context.cacheDir, "tiles")
    private var diskCache: DiskLruCache? = open()

    companion object {
        private val TAG = DiskTileCache::class.java.simpleName
        private const val DISK_CACHE_VERSION = 1
        private const val VALUE_COUNT = 1
    }

    @Synchronized
    fun get(tile: TileCoordinate): Bitmap? {
        val key = getTileKey(tile)
        return try {
            diskCache?.get(key)?.use { snapshot ->
                val inputStream = snapshot.getInputStream(0)
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to read tile $key from disk cache", e)
            null
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to read tile $key from disk cache", e)
            null
        }
    }

    @Synchronized
    fun put(
        tile: TileCoordinate,
        bitmap: Bitmap,
    ) {
        val key = getTileKey(tile)
        try {
            diskCache?.edit(key)?.let { editor ->
                try {
                    editor.newOutputStream(0).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    editor.commit()
                } catch (e: IOException) {
                    Log.w(TAG, "Failed to write tile $key to disk cache", e)
                    editor.abort()
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to edit tile $key in disk cache", e)
        } catch (e: IllegalStateException) {
            Log.w(TAG, "Failed to edit tile $key in disk cache", e)
        }
    }

    /**
     * Removes all cached tiles. DiskLruCache.delete() also closes the cache,
     * so it is reopened afterwards to keep the cache usable.
     */
    @Synchronized
    fun clear() {
        val cache = diskCache ?: return
        try {
            cache.delete()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to clear disk cache", e)
        }
        diskCache = open()
    }

    /**
     * Closes the cache. Later calls to [get] and [put] are treated as cache misses.
     */
    @Synchronized
    fun close() {
        try {
            diskCache?.close()
        } catch (e: IOException) {
            Log.w(TAG, "Failed to close disk cache", e)
        }
        diskCache = null
    }

    private fun open(): DiskLruCache? =
        try {
            DiskLruCache.open(cacheDir, DISK_CACHE_VERSION, VALUE_COUNT, maxSize)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to initialize disk cache at $cacheDir", e)
            null
        }

    // Generate unique key for tile coordinate (e.g., "10_512_384" for zoom 10, x=512, y=384)
    private fun getTileKey(tile: TileCoordinate): String = "${tile.zoom}_${tile.x}_${tile.y}"
}
