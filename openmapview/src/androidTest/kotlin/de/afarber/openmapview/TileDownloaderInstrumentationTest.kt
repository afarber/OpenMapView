/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TileDownloaderInstrumentationTest {
    @Test
    fun testDownloadRealOsmTile() =
        runTest {
            val downloader = TileDownloader()

            val tileUrl = TileSource.STANDARD.getTileUrl(TileCoordinate(x = 0, y = 0, zoom = 0))
            val result = downloader.downloadTile(tileUrl)

            assertNotNull("Should successfully download a real OSM tile", result)
            result?.let {
                assert(it.width > 0) { "Downloaded bitmap should have width > 0" }
                assert(it.height > 0) { "Downloaded bitmap should have height > 0" }
            }

            downloader.close()
        }

    @Test
    fun testDownloadInvalidUrl() =
        runTest {
            val downloader = TileDownloader()

            val result = downloader.downloadTile("https://tile.openstreetmap.org/99/99/99.png")

            // Invalid tile coordinates should return null or handle gracefully
            // OSM returns 404 for invalid tile coordinates
            downloader.close()
        }
}
