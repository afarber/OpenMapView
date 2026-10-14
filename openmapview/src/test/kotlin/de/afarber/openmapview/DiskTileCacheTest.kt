/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import org.junit.After
import org.junit.Assert.assertArrayEquals
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
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DiskTileCacheTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var diskCache: DiskTileCache

    // Controlled clock for expiry and LRU tests, in steps far above file time granularity
    private var now = 1_000_000_000_000L

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

    @Test
    fun testStoresServerBytesUnchanged() {
        val cache = testCache()
        val tile = TileCoordinate(1, 2, 3)
        val bytes = createPngBytes()

        cache.put(tile, bytes)
        val stored = cache.getBytes(tile)!!

        assertArrayEquals(bytes, stored.copyOfRange(stored.size - bytes.size, stored.size))
    }

    @Test
    fun testDiskHitIsDecodedAsRgb565() {
        val cache = testCache()
        val tile = TileCoordinate(1, 2, 3)

        cache.put(tile, createPngBytes())

        assertEquals(Bitmap.Config.RGB_565, cache.get(tile)?.config)
    }

    @Test
    fun testExpiredTileIsOnlyReturnedAsStale() {
        val cache = testCache(maxAgeMs = 60_000)
        val tile = TileCoordinate(1, 2, 3)
        cache.put(tile, createPngBytes())

        now += 60_000
        assertNotNull("Not expired yet", cache.get(tile))

        now += 1
        assertNull(cache.get(tile))
        assertNotNull(cache.get(tile, allowStale = true))
    }

    @Test
    fun testReadingDoesNotResetTheAge() {
        val cache = testCache(maxAgeMs = 60_000)
        val tile = TileCoordinate(1, 2, 3)
        cache.put(tile, createPngBytes())

        now += 50_000
        assertNotNull(cache.get(tile))
        now += 50_000

        assertNull(cache.get(tile))
    }

    @Test
    fun testTrimDeletesLeastRecentlyUsedTiles() {
        val bytes = ByteArray(1_000) { it.toByte() }
        // Room for three tiles, a fourth one triggers trimming to 90%
        val cache = testCache(maxSize = 3_500)
        val (a, b, c, d) = (1..4).map { TileCoordinate(it, 0, 5) }

        for (tile in listOf(a, b, c)) {
            now += 10_000
            cache.put(tile, bytes)
        }
        now += 10_000
        assertNotNull("Reading a makes b the least recently used", cache.getBytes(a))
        now += 10_000
        cache.put(d, bytes)

        assertNull(cache.getBytes(b))
        assertNotNull(cache.getBytes(a))
        assertNotNull(cache.getBytes(c))
        assertNotNull(cache.getBytes(d))
    }

    @Test
    fun testSizeIsCountedFromExistingFiles() {
        val bytes = ByteArray(1_000)
        val directory = folder.newFolder("shared")
        // Fill the directory with tiles written by an earlier process
        val first = testCache(directory = directory, maxSize = 3_500)
        for (x in 1..3) {
            now += 10_000
            first.put(TileCoordinate(x, 0, 5), bytes)
        }
        forgetStore(directory)

        now += 10_000
        val second = testCache(directory = directory, maxSize = 3_500)
        second.put(TileCoordinate(4, 0, 5), bytes)

        assertNull("The oldest tile was trimmed", second.getBytes(TileCoordinate(1, 0, 5)))
        assertNotNull(second.getBytes(TileCoordinate(4, 0, 5)))
    }

    @Test
    fun testNoTempFilesAreLeft() {
        val directory = folder.newFolder("tiles")
        val leftover = File(directory, "default/1_2_3.tile.tmp")
        leftover.parentFile!!.mkdirs()
        leftover.writeBytes(ByteArray(10))
        val cache = testCache(directory = directory)

        cache.put(TileCoordinate(1, 2, 3), createPngBytes())
        cache.put(TileCoordinate(4, 5, 6), createPngBytes())

        val files =
            directory
                .walkTopDown()
                .filter { it.isFile }
                .map { it.name }
                .toList()
        assertEquals(listOf("3_1_2.tile", "6_4_5.tile").sorted(), files.sorted())
    }

    @Test
    fun testCorruptFileIsAMiss() {
        val directory = folder.newFolder("tiles")
        val cache = testCache(directory = directory)
        val tile = TileCoordinate(1, 2, 3)
        cache.put(tile, createPngBytes())
        val file = File(directory, "default/3_1_2.tile")
        file.writeBytes(byteArrayOf(42))

        assertNull(cache.get(tile))
        assertFalse("Corrupt file is deleted", file.exists())
    }

    @Test
    fun testLegacyDirectoryIsDeleted() {
        val legacy = folder.newFolder("legacy")
        File(legacy, "journal").writeText("libcore.io.DiskLruCache")
        val cache = testCache(legacyDirectory = legacy)

        cache.put(TileCoordinate(1, 2, 3), createPngBytes())

        assertFalse(legacy.exists())
    }

    @Test
    fun testPutAfterDirectoryWasRemoved() {
        val directory = folder.newFolder("tiles")
        val cache = testCache(directory = directory)
        val tile = TileCoordinate(1, 2, 3)
        cache.put(tile, createPngBytes())

        // E.g. the user cleared the app cache in the system settings
        assertTrue(directory.deleteRecursively())
        cache.put(tile, createPngBytes())

        assertNotNull(cache.get(tile))
    }

    private fun testCache(
        directory: File = folder.newFolder(),
        maxSize: Long = 1024 * 1024,
        maxAgeMs: Long = 24 * 60 * 60 * 1000,
        legacyDirectory: File? = null,
    ) = DiskTileCache(directory, DiskTileCache.DEFAULT_NAMESPACE, maxSize, maxAgeMs, { now }, legacyDirectory)

    // Simulates a new process, which has to scan the directory again
    @Suppress("UNCHECKED_CAST")
    private fun forgetStore(directory: File) {
        val field = DiskTileCache::class.java.getDeclaredField("stores")
        field.isAccessible = true
        val stores = field.get(null) as HashMap<String, Any>
        synchronized(stores) { stores.remove(directory.absolutePath) }
    }

    private fun createPngBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        createMockBitmap(256, 256).compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }

    private fun createMockBitmap(
        width: Int,
        height: Int,
    ): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
}
