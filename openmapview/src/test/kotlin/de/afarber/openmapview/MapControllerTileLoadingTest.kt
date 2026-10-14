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
import kotlinx.coroutines.awaitCancellation
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
import java.time.Duration

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
    private var serverHangs = false
    private var requests = 0

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        disk = DiskTileCache(folder.newFolder(), DiskTileCache.DEFAULT_NAMESPACE, 1024 * 1024, MAX_AGE_MS, { now })
        cache = TileCache(disk)
        setField(controller, "tileCache", cache)
        setField(controller, "tileDownloader", mockDownloader())
    }

    // A TileDownloader whose HTTP client answers from the fields above
    private fun mockDownloader(): TileDownloader {
        val engine =
            MockEngine {
                requests++
                if (serverHangs) awaitCancellation()
                if (serverUp) {
                    respond(ByteReadChannel(createPngBytes()), HttpStatusCode.OK)
                } else {
                    respond(ByteReadChannel(ByteArray(0)), HttpStatusCode.ServiceUnavailable)
                }
            }
        val downloader = TileDownloader()
        setField(downloader, "client", HttpClient(engine))
        return downloader
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

    @Test
    fun testStaleTileNeedsDownloadAfterBackOff() {
        disk.put(tile, createPngBytes())
        now += MAX_AGE_MS + 1
        serverUp = false
        download()

        assertFalse("Still waiting for the retry delay", controller.needsDownload(tile, SystemClock.elapsedRealtime()))

        // Prefetch and visible tiles share needsDownload(), so both retry the stale tile
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(TileRetryTracker.INITIAL_DELAY_MS))
        assertTrue(controller.needsDownload(tile, SystemClock.elapsedRealtime()))
    }

    @Test
    fun testFreshTileInMemoryNeedsNoDownload() {
        assertTrue(controller.needsDownload(tile, SystemClock.elapsedRealtime()))

        download()

        assertFalse(controller.needsDownload(tile, SystemClock.elapsedRealtime()))
    }

    @Test
    fun testOnDestroy_CancelsRunningDownloadAndMapStillLoadsTilesAfterwards() {
        serverHangs = true
        val downloading = startDownload()
        val deadline = System.currentTimeMillis() + 5_000
        while (requests == 0) {
            check(System.currentTimeMillis() < deadline) { "Request was not sent" }
            Thread.sleep(10)
        }

        controller.onDestroy()

        // The cancelled download no longer blocks the tile and the HTTP client is closed
        assertFalse(downloadingTiles().contains(tile))
        assertTrue(downloading.contains(tile))
        val field = MapController::class.java.getDeclaredField("tileDownloader")
        field.isAccessible = true
        assertNull(field.get(controller))
        assertTrue(controller.needsDownload(tile, SystemClock.elapsedRealtime()))

        // Drawn again, e.g. after the view is attached again, the map downloads the tile
        serverHangs = false
        setField(controller, "tileDownloader", mockDownloader())
        download()

        assertNotNull(cache.getMemory(tile))
    }

    @Test
    fun testOnDestroy_CanBeCalledTwice() {
        controller.onDestroy()
        controller.onDestroy()

        setField(controller, "tileDownloader", mockDownloader())
        download()

        assertNotNull(cache.getMemory(tile))
    }

    @Suppress("UNCHECKED_CAST")
    private fun downloadingTiles(): MutableSet<TileCoordinate> = getField(controller, "downloadingTiles") as MutableSet<TileCoordinate>

    // Calls the private downloadTile() and returns the set it removes the tile from when done
    private fun startDownload(): MutableSet<TileCoordinate> {
        val downloading = downloadingTiles()
        downloading.add(tile)
        val method =
            MapController::class.java.getDeclaredMethod(
                "downloadTile",
                TileCoordinate::class.java,
                Boolean::class.javaPrimitiveType,
            )
        method.isAccessible = true
        method.invoke(controller, tile, false)
        return downloading
    }

    // Calls the private downloadTile() and waits until its result reached the main thread
    private fun download() {
        val downloading = startDownload()

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
