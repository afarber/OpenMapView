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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
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
    fun testClose() {
        val downloader = TileDownloader()
        downloader.close()
    }

    private fun createMockPngBytes(): ByteArray {
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
