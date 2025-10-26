/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TileDownloaderInstrumentationTest {
    @Test
    fun testDownloadRealOsmTile() =
        runBlocking {
            val downloader = TileDownloader()

            val tileUrl = TileSource.STANDARD.getTileUrl(TileCoordinate(x = 0, y = 0, zoom = 0))

            // Retry up to 3 times with delay to handle emulator network initialization
            var result = downloader.downloadTile(tileUrl)
            var attempts = 1
            while (result == null && attempts < 3) {
                delay(2000) // Wait 2 seconds before retry
                result = downloader.downloadTile(tileUrl)
                attempts++
            }

            assertNotNull("Should successfully download a real OSM tile after $attempts attempt(s)", result)
            result?.let {
                assert(it.width > 0) { "Downloaded bitmap should have width > 0" }
                assert(it.height > 0) { "Downloaded bitmap should have height > 0" }
            }

            downloader.close()
        }

    @Test
    fun testDownloadInvalidUrl() =
        runBlocking {
            val downloader = TileDownloader()

            val result = downloader.downloadTile("https://tile.openstreetmap.org/99/99/99.png")

            // Invalid tile coordinates should return null or handle gracefully
            // OSM returns 404 for invalid tile coordinates
            downloader.close()
        }
}
