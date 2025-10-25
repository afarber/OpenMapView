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
import com.jakewharton.disklrucache.DiskLruCache
import java.io.File
import java.io.IOException

/**
 * Persistent disk cache for map tiles using DiskLruCache.
 *
 * Stores up to 50MB of tile images in PNG format in the app's cache directory.
 * Tiles persist across app restarts, reducing network usage on subsequent launches.
 * Uses LRU eviction when cache size limit is reached.
 */
class DiskTileCache(
    context: Context,
) {
    private val maxSize = 50L * 1024 * 1024
    private val diskCache: DiskLruCache?

    companion object {
        private const val DISK_CACHE_VERSION = 1
        private const val VALUE_COUNT = 1
    }

    init {
        val cacheDir = File(context.cacheDir, "tiles")
        diskCache =
            try {
                DiskLruCache.open(cacheDir, DISK_CACHE_VERSION, VALUE_COUNT, maxSize)
            } catch (e: IOException) {
                null
            }
    }

    fun get(tile: TileCoordinate): Bitmap? {
        val key = getTileKey(tile)
        return try {
            diskCache?.get(key)?.use { snapshot ->
                val inputStream = snapshot.getInputStream(0)
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            null
        }
    }

    fun put(
        tile: TileCoordinate,
        bitmap: Bitmap,
    ) {
        val key = getTileKey(tile)
        try {
            diskCache?.edit(key)?.let { editor ->
                try {
                    val outputStream = editor.newOutputStream(0)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    editor.commit()
                } catch (e: IOException) {
                    editor.abort()
                }
            }
        } catch (e: IOException) {
            // Ignore disk cache errors
        }
    }

    fun clear() {
        try {
            diskCache?.delete()
        } catch (e: IOException) {
            // Ignore
        }
    }

    fun close() {
        try {
            diskCache?.close()
        } catch (e: IOException) {
            // Ignore
        }
    }

    // Generate unique key for tile coordinate (e.g., "10_512_384" for zoom 10, x=512, y=384)
    private fun getTileKey(tile: TileCoordinate): String = "${tile.zoom}_${tile.x}_${tile.y}"
}
