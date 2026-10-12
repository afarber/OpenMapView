/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class DiskTileCacheTest {
    private lateinit var diskCache: DiskTileCache

    @Before
    fun setUp() {
        diskCache = DiskTileCache(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        diskCache.clear()
        diskCache.close()
    }

    @Test
    fun testPutAndGet() {
        val tile = TileCoordinate(1, 2, 3)
        val bitmap = createMockBitmap(256, 256)

        diskCache.put(tile, bitmap)
        val result = diskCache.get(tile)

        assertNotNull(result)
        assertEquals(256, result?.width)
        assertEquals(256, result?.height)
    }

    @Test
    fun testGetNonExistent() {
        val tile = TileCoordinate(99, 99, 10)
        val result = diskCache.get(tile)
        assertNull(result)
    }

    @Test
    fun testClear() {
        val tile1 = TileCoordinate(1, 2, 3)
        val tile2 = TileCoordinate(4, 5, 6)
        val bitmap1 = createMockBitmap(256, 256)
        val bitmap2 = createMockBitmap(256, 256)

        diskCache.put(tile1, bitmap1)
        diskCache.put(tile2, bitmap2)

        diskCache.clear()

        assertNull(diskCache.get(tile1))
        assertNull(diskCache.get(tile2))
    }

    @Test
    fun testPutAndGetAfterClear() {
        val tile = TileCoordinate(1, 2, 3)
        diskCache.put(tile, createMockBitmap(256, 256))

        diskCache.clear()
        diskCache.put(tile, createMockBitmap(256, 256))

        assertNotNull(diskCache.get(tile))
    }

    @Test
    fun testGetAndPutAfterCloseDoNotThrow() {
        val tile = TileCoordinate(1, 2, 3)

        diskCache.close()
        diskCache.put(tile, createMockBitmap(256, 256))

        assertNull(diskCache.get(tile))
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
            diskCache.put(tile, bitmap)
        }

        tiles.forEach { tile ->
            val result = diskCache.get(tile)
            assertNotNull(result)
            assertEquals(256, result?.width)
            assertEquals(256, result?.height)
        }
    }

    @Test
    fun testNamespacesAreSeparate() {
        val tile = TileCoordinate(1, 2, 3)
        val standard = DiskTileCache(RuntimeEnvironment.getApplication(), "STANDARD")
        val cyclosm = DiskTileCache(RuntimeEnvironment.getApplication(), "CYCLOSM")

        standard.put(tile, createMockBitmap(256, 256))

        assertNotNull(standard.get(tile))
        assertNull(cyclosm.get(tile))
        assertNull(diskCache.get(tile))
    }

    @Test
    fun testInstancesShareTheDirectory() {
        val tile = TileCoordinate(1, 2, 3)
        val other = DiskTileCache(RuntimeEnvironment.getApplication())

        diskCache.put(tile, createMockBitmap(256, 256))
        assertNotNull(other.get(tile))

        other.close()
        assertNull(other.get(tile))
        assertNotNull(diskCache.get(tile))
    }

    private fun createMockBitmap(
        width: Int,
        height: Int,
    ): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
}
