/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.util.LruCache

class TileCache {
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Use 1/8 of available memory

    private val cache =
        object : LruCache<TileCoordinate, Bitmap>(cacheSize) {
            override fun sizeOf(
                key: TileCoordinate,
                bitmap: Bitmap,
            ): Int {
                return bitmap.byteCount / 1024 // Size in KB
            }
        }

    fun get(tile: TileCoordinate): Bitmap? = cache.get(tile)

    fun put(
        tile: TileCoordinate,
        bitmap: Bitmap,
    ) {
        cache.put(tile, bitmap)
    }

    fun clear() {
        cache.evictAll()
    }
}
