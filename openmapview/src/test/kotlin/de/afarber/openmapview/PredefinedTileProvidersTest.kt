/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PredefinedTileProvidersTest {
    @Test
    fun testProvidersRequestExpectedUrls() =
        runTest {
            val expected =
                mapOf(
                    PredefinedTileProviders.openSeaMap() to "https://tiles.openseamap.org/seamark/3/1/2.png",
                    PredefinedTileProviders.openRailwayMap() to "https://a.tiles.openrailwaymap.org/standard/3/1/2.png",
                    PredefinedTileProviders.openSnowMap() to "https://tiles.opensnowmap.org/pistes/3/1/2.png",
                    PredefinedTileProviders.waymarkedTrailsHiking() to "https://tile.waymarkedtrails.org/hiking/3/1/2.png",
                    PredefinedTileProviders.waymarkedTrailsCycling() to "https://tile.waymarkedtrails.org/cycling/3/1/2.png",
                    PredefinedTileProviders.waymarkedTrailsMtb() to "https://tile.waymarkedtrails.org/mtb/3/1/2.png",
                    PredefinedTileProviders.waymarkedTrailsSkating() to "https://tile.waymarkedtrails.org/skating/3/1/2.png",
                    PredefinedTileProviders.stamenTonerHybrid() to
                        "https://stamen-tiles.a.ssl.fastly.net/toner-hybrid/3/1/2.png",
                    PredefinedTileProviders.stamenTerrainLines() to
                        "https://stamen-tiles.a.ssl.fastly.net/terrain-lines/3/1/2.png",
                    PredefinedTileProviders.custom("https://example.com/{z}/{x}/{y}.png") to "https://example.com/3/1/2.png",
                )

            for ((provider, url) in expected) {
                assertEquals(url, requestedUrl(provider as UrlTileProvider))
            }
        }

    // Swaps in a mock HTTP client, so no real tile server is contacted
    private suspend fun requestedUrl(provider: UrlTileProvider): String? {
        var requested: String? = null
        val engine =
            MockEngine { request ->
                requested = request.url.toString()
                respond(ByteReadChannel(byteArrayOf(1)), HttpStatusCode.OK)
            }
        val reflectionField = UrlTileProvider::class.java.getDeclaredField("client")
        reflectionField.isAccessible = true
        reflectionField.set(provider, HttpClient(engine))

        provider.getTile(1, 2, 3)
        provider.close()
        return requested
    }
}
