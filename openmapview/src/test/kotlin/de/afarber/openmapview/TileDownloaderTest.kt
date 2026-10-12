/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
class TileDownloaderTest {
    @Test
    fun testDownloadTile_Success() =
        runTest {
            val mockBitmapBytes = createMockPngBytes()
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = ByteReadChannel(mockBitmapBytes),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "image/png"),
                    )
                }

            val client = HttpClient(mockEngine)
            val downloader = TileDownloader()
            val reflectionField = TileDownloader::class.java.getDeclaredField("client")
            reflectionField.isAccessible = true
            reflectionField.set(downloader, client)

            val result = downloader.downloadTile("https://example.com/tile.png")

            assertNotNull("Should successfully decode valid PNG bytes", result)
            result?.let {
                assert(it.width == 256) { "Bitmap width should be 256" }
                assert(it.height == 256) { "Bitmap height should be 256" }
            }
            downloader.close()
        }

    @Test
    fun testDownloadTile_SendsVersionedUserAgent() =
        runTest {
            var userAgent: String? = null
            val mockEngine =
                MockEngine { request ->
                    userAgent = request.headers[HttpHeaders.UserAgent]
                    respond(
                        content = ByteReadChannel(createMockPngBytes()),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "image/png"),
                    )
                }

            val downloader = TileDownloader()
            val reflectionField = TileDownloader::class.java.getDeclaredField("client")
            reflectionField.isAccessible = true
            reflectionField.set(downloader, HttpClient(mockEngine))

            downloader.downloadTile("https://example.com/tile.png")

            assertTrue("Library version should not be blank", LIBRARY_VERSION.isNotBlank())
            assertEquals("OpenMapView/$LIBRARY_VERSION (https://github.com/afarber/OpenMapView)", userAgent)
            downloader.close()
        }

    @Test
    fun testDownloadTile_HttpErrorReturnsNull() =
        runTest {
            val mockEngine =
                MockEngine {
                    respond(
                        content = ByteReadChannel(createMockPngBytes()),
                        status = HttpStatusCode.TooManyRequests,
                        headers = headersOf(HttpHeaders.ContentType, "image/png"),
                    )
                }

            val downloader = createDownloader(HttpClient(mockEngine))

            assertNull(downloader.downloadTile("https://example.com/tile.png"))
            downloader.close()
        }

    @Test
    fun testDownloadTile_RethrowsCancellation() =
        runTest {
            val mockEngine = MockEngine { throw CancellationException("cancelled") }
            val downloader = createDownloader(HttpClient(mockEngine))

            var cancelled = false
            try {
                downloader.downloadTile("https://example.com/tile.png")
            } catch (e: CancellationException) {
                cancelled = true
            }

            assertTrue("Cancellation should not be swallowed", cancelled)
            downloader.close()
        }

    @Test
    fun testClose() {
        val downloader = TileDownloader()
        downloader.close()
    }

    private fun createDownloader(client: HttpClient): TileDownloader {
        val downloader = TileDownloader()
        val reflectionField = TileDownloader::class.java.getDeclaredField("client")
        reflectionField.isAccessible = true
        reflectionField.set(downloader, client)
        return downloader
    }

    private fun createMockPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
