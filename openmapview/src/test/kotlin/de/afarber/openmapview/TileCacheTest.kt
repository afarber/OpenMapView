/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class TileCacheTest {
    private lateinit var cache: TileCache

    @Before
    fun setUp() {
        cache = TileCache()
    }

    @Test
    fun testPutAndGet() {
        val tile = TileCoordinate(1, 2, 3)
        val bitmap = createMockBitmap(256, 256)

        cache.put(tile, bitmap)
        val result = cache.get(tile)

        assertNotNull(result)
        assertEquals(bitmap, result)
    }

    @Test
    fun testGetNonExistent() {
        val tile = TileCoordinate(99, 99, 10)
        val result = cache.get(tile)
        assertNull(result)
    }

    @Test
    fun testClear() {
        val tile1 = TileCoordinate(1, 2, 3)
        val tile2 = TileCoordinate(4, 5, 6)
        val bitmap1 = createMockBitmap(256, 256)
        val bitmap2 = createMockBitmap(256, 256)

        cache.put(tile1, bitmap1)
        cache.put(tile2, bitmap2)

        cache.clear()

        assertNull(cache.get(tile1))
        assertNull(cache.get(tile2))
    }

    @Test
    fun testOverwriteExisting() {
        val tile = TileCoordinate(1, 2, 3)
        val bitmap1 = createMockBitmap(256, 256)
        val bitmap2 = createMockBitmap(512, 512)

        cache.put(tile, bitmap1)
        cache.put(tile, bitmap2)

        val result = cache.get(tile)
        assertEquals(bitmap2, result)
    }

    @Test
    fun testMultipleTiles() {
        val tiles =
            listOf(
                TileCoordinate(0, 0, 5),
                TileCoordinate(1, 0, 5),
                TileCoordinate(0, 1, 5),
                TileCoordinate(1, 1, 5),
            )

        val bitmaps = tiles.map { createMockBitmap(256, 256) }

        tiles.zip(bitmaps).forEach { (tile, bitmap) ->
            cache.put(tile, bitmap)
        }

        tiles.zip(bitmaps).forEach { (tile, bitmap) ->
            assertEquals(bitmap, cache.get(tile))
        }
    }

    @Test
    fun testDifferentZoomLevels() {
        val tile5 = TileCoordinate(10, 20, 5)
        val tile10 = TileCoordinate(10, 20, 10)
        val tile15 = TileCoordinate(10, 20, 15)

        val bitmap5 = createMockBitmap(256, 256)
        val bitmap10 = createMockBitmap(256, 256)
        val bitmap15 = createMockBitmap(256, 256)

        cache.put(tile5, bitmap5)
        cache.put(tile10, bitmap10)
        cache.put(tile15, bitmap15)

        // Same x,y but different zoom should be treated as different tiles
        assertEquals(bitmap5, cache.get(tile5))
        assertEquals(bitmap10, cache.get(tile10))
        assertEquals(bitmap15, cache.get(tile15))
    }

    private fun createMockBitmap(
        width: Int,
        height: Int,
    ): Bitmap {
        val bitmap = mockk<Bitmap>(relaxed = true)
        every { bitmap.byteCount } returns width * height * 4 // ARGB
        every { bitmap.width } returns width
        every { bitmap.height } returns height
        return bitmap
    }
}
