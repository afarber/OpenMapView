/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.os.Looper
import android.os.SystemClock
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
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
import org.robolectric.Shadows.shadowOf
import java.io.ByteArrayOutputStream

/**
 * Drives MapController's base tile download through a mocked HTTP client and a disk
 * cache with a controlled clock, to check the order disk, network, stale fallback and
 * the retry back-off.
 */
@RunWith(RobolectricTestRunner::class)
class MapControllerTileLoadingTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var controller: MapController
    private lateinit var disk: DiskTileCache
    private lateinit var cache: TileCache
    private val tile = TileCoordinate(1, 2, 3)
    private var now = 1_000_000_000_000L
    private var serverUp = true
    private var requests = 0

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        disk = DiskTileCache(folder.newFolder(), DiskTileCache.DEFAULT_NAMESPACE, 1024 * 1024, MAX_AGE_MS, { now })
        cache = TileCache(disk)
        setField(controller, "tileCache", cache)

        val engine =
            MockEngine {
                requests++
                if (serverUp) {
                    respond(ByteReadChannel(createPngBytes()), HttpStatusCode.OK)
                } else {
                    respond(ByteReadChannel(ByteArray(0)), HttpStatusCode.ServiceUnavailable)
                }
            }
        setField(getField(controller, "tileDownloader"), "client", HttpClient(engine))
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    @Test
    fun testFreshDiskTileIsNotDownloaded() {
        disk.put(tile, createPngBytes())

        download()

        assertEquals(0, requests)
        assertNotNull(cache.getMemory(tile))
        assertTrue(retries().canRequest(tile, SystemClock.elapsedRealtime()))
    }

    @Test
    fun testMissingTileIsDownloadedAndStored() {
        download()

        assertEquals(1, requests)
        assertNotNull(cache.getMemory(tile))
        assertNotNull("Stored on disk", TileCache(disk).get(tile))
    }

    @Test
    fun testFailedDownloadAfterDiskMissBacksOff() {
        serverUp = false

        download()

        assertNull(cache.getMemory(tile))
        assertFalse("Retried only after a delay", retries().canRequest(tile, SystemClock.elapsedRealtime()))
    }

    @Test
    fun testExpiredTileIsDownloadedAgain() {
        disk.put(tile, createPngBytes())
        now += MAX_AGE_MS + 1

        download()

        assertEquals(1, requests)
        assertFalse(cache.isStale(tile))
        assertNotNull("Fresh again", TileCache(disk).get(tile))
    }

    @Test
    fun testStaleTileIsShownWhenDownloadFailsAndRetriedWithBackOff() {
        disk.put(tile, createPngBytes())
        now += MAX_AGE_MS + 1
        serverUp = false

        download()

        assertNotNull("Stale tile shown", cache.getMemory(tile))
        assertTrue(cache.isStale(tile))
        assertFalse("Retried only after a delay", retries().canRequest(tile, SystemClock.elapsedRealtime()))

        // Once the network is back, the next attempt replaces the stale tile
        serverUp = true
        download()

        assertEquals(2, requests)
        assertFalse(cache.isStale(tile))
        assertTrue(retries().canRequest(tile, SystemClock.elapsedRealtime()))
    }

    // Calls the private downloadTile() and waits until its result reached the main thread
    @Suppress("UNCHECKED_CAST")
    private fun download() {
        val downloading = getField(controller, "downloadingTiles") as MutableSet<TileCoordinate>
        downloading.add(tile)
        val method =
            MapController::class.java.getDeclaredMethod(
                "downloadTile",
                TileCoordinate::class.java,
                Boolean::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(controller, tile, false)

        val deadline = System.currentTimeMillis() + 5_000
        while (downloading.contains(tile)) {
            check(System.currentTimeMillis() < deadline) { "Download did not finish" }
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
    }

    private fun retries(): TileRetryTracker = getField(controller, "tileRetries") as TileRetryTracker

    private fun getField(
        target: Any,
        name: String,
    ): Any {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        return field.get(target)!!
    }

    private fun setField(
        target: Any,
        name: String,
        value: Any,
    ) {
        val field = target.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(target, value)
    }

    private fun createPngBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, output)
        return output.toByteArray()
    }

    private companion object {
        const val MAX_AGE_MS = 60_000L
    }
}
