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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class TileCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var cache: TileCache

    @Before
    fun setUp() {
        TileCache.clearSharedMemory()
        cache = TileCache(RuntimeEnvironment.getApplication())
    }

    @After
    fun tearDown() {
        cache.close()
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
        cache.close()

        assertNull(cache.get(tile1))
        assertNull(cache.get(tile2))
    }

    @Test
    fun testGetAfterClearDoesNotThrow() {
        val tile = TileCoordinate(1, 2, 3)
        cache.put(tile, createMockBitmap(256, 256))

        cache.clear()

        assertNull(cache.get(tile))
        cache.put(tile, createMockBitmap(256, 256))
        assertNotNull(cache.get(tile))
    }

    @Test
    fun testNamespacesDoNotShareDiskTiles() {
        val tile = TileCoordinate(1, 2, 3)
        val standard = TileCache(RuntimeEnvironment.getApplication(), "STANDARD")
        val cyclosm = TileCache(RuntimeEnvironment.getApplication(), "CYCLOSM")

        standard.put(tile, createMockBitmap(256, 256))

        assertNotNull(standard.get(tile))
        assertNull(cyclosm.get(tile))
    }

    @Test
    fun testPutWritesToDisk() {
        val tile = TileCoordinate(7, 8, 9)
        cache.put(tile, createMockBitmap(256, 256))

        // A cache with a memory cache of its own must read the tile from disk
        val other = TileCache(DiskTileCache(RuntimeEnvironment.getApplication(), DiskTileCache.DEFAULT_NAMESPACE))

        assertNull(other.getMemory(tile))
        assertNotNull(other.get(tile))
    }

    @Test
    fun testCachesOfOneNamespaceShareMemory() {
        val tile = TileCoordinate(7, 8, 9)
        val bitmap = createMockBitmap(256, 256)
        cache.put(tile, bitmap)

        // E.g. the map of a tab that is shown again
        val other = TileCache(RuntimeEnvironment.getApplication())

        assertEquals(bitmap, other.getMemory(tile))
    }

    @Test
    fun testNamespacesDoNotShareMemory() {
        val tile = TileCoordinate(1, 2, 3)
        val standard = TileCache(RuntimeEnvironment.getApplication(), "STANDARD")
        val cyclosm = TileCache(RuntimeEnvironment.getApplication(), "CYCLOSM")

        standard.put(tile, createMockBitmap(256, 256))

        assertNotNull(standard.getMemory(tile))
        assertNull(cyclosm.getMemory(tile))
    }

    @Test
    fun testClearKeepsTheMemoryOfOtherNamespaces() {
        val tile = TileCoordinate(1, 2, 3)
        val standard = TileCache(RuntimeEnvironment.getApplication(), "STANDARD")
        val cyclosm = TileCache(RuntimeEnvironment.getApplication(), "CYCLOSM")
        standard.put(tile, createMockBitmap(256, 256))
        cyclosm.put(tile, createMockBitmap(256, 256))

        standard.clear()

        assertNull(standard.getMemory(tile))
        assertNotNull(cyclosm.getMemory(tile))
    }

    @Test
    fun testMemoryOnlyCachesDoNotShareMemory() {
        val tile = TileCoordinate(1, 2, 3)
        // Like the caches of two tile overlays
        val first = TileCache()
        val second = TileCache()

        first.put(tile, createMockBitmap(256, 256))

        assertNotNull(first.getMemory(tile))
        assertNull(second.getMemory(tile))
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

    @Test
    fun testExpiredTileIsOnlyAvailableAsStale() {
        var now = 1_000_000_000_000L
        val disk = DiskTileCache(folder.newFolder(), DiskTileCache.DEFAULT_NAMESPACE, 1024 * 1024, 60_000, { now })
        val tile = TileCoordinate(1, 2, 3)
        TileCache(disk).put(tile, createMockBitmap(256, 256), createPngBytes())
        now += 60_001

        // A new cache has an empty memory cache, so the tile must come from disk
        val stale = TileCache(disk)
        assertNull(stale.get(tile))
        assertFalse(stale.isStale(tile))

        assertNotNull(stale.getStale(tile))
        assertTrue(stale.isStale(tile))
        assertNotNull("Stale tile is in memory now", stale.getMemory(tile))

        stale.put(tile, createMockBitmap(256, 256), createPngBytes())
        assertFalse("A fresh tile replaces the stale one", stale.isStale(tile))
        assertNotNull(TileCache(disk).get(tile))
    }

    @Test
    fun testGetStaleReturnsFreshTile() {
        val tile = TileCoordinate(1, 2, 3)
        cache.put(tile, createMockBitmap(256, 256), createPngBytes())

        assertNotNull(cache.getStale(tile))
        assertFalse(cache.isStale(tile))
    }

    @Test
    fun testClearForgetsStaleTiles() {
        var now = 1_000_000_000_000L
        val disk = DiskTileCache(folder.newFolder(), DiskTileCache.DEFAULT_NAMESPACE, 1024 * 1024, 60_000, { now })
        val tile = TileCoordinate(1, 2, 3)
        val stale = TileCache(disk)
        disk.put(tile, createPngBytes())
        now += 60_001
        stale.getStale(tile)

        stale.clear()

        assertFalse(stale.isStale(tile))
        assertNull(stale.getStale(tile))
    }

    @Test
    fun testEvictedTileLosesStaleMark() {
        var now = 1_000_000_000_000L
        val disk = DiskTileCache(folder.newFolder(), DiskTileCache.DEFAULT_NAMESPACE, 1024 * 1024, 60_000, { now })
        val a = TileCoordinate(1, 2, 3)
        disk.put(a, createPngBytes())
        now += 60_001
        // Room for one 256x256 RGB_565 tile (128 KB)
        val small = TileCache(disk, cacheSize = 200)
        small.getStale(a)
        assertTrue(small.isStale(a))

        small.put(TileCoordinate(4, 5, 6), Bitmap.createBitmap(256, 256, Bitmap.Config.RGB_565))

        assertNull("Evicted from memory", small.getMemory(a))
        assertFalse(small.isStale(a))
    }

    private fun createPngBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        createMockBitmap(256, 256).compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }

    private fun createMockBitmap(
        width: Int,
        height: Int,
    ): Bitmap {
        // Robolectric enables creation of real Bitmap instances in unit tests
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
}
