/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlTileProviderTest {
    @Test
    fun testGetTile_Success() =
        runTest {
            var requestedUrl: String? = null
            val provider =
                createProvider(
                    MockEngine { request ->
                        requestedUrl = request.url.toString()
                        respond(ByteReadChannel(byteArrayOf(1, 2, 3)), HttpStatusCode.OK)
                    },
                )

            val tile = provider.getTile(1, 2, 3)

            assertNotNull(tile)
            assertEquals(3, tile?.data?.size)
            assertEquals("https://example.com/3/1/2.png", requestedUrl)
        }

    @Test
    fun testGetTile_SendsAppUserAgent() =
        runTest {
            val context: Context = ApplicationProvider.getApplicationContext()
            UserAgent.init(context)
            var userAgent: String? = null
            val provider =
                createProvider(
                    MockEngine { request ->
                        userAgent = request.headers[HttpHeaders.UserAgent]
                        respond(ByteReadChannel(byteArrayOf(1, 2, 3)), HttpStatusCode.OK)
                    },
                )

            provider.getTile(1, 2, 3)

            assertEquals(UserAgent.build(context.packageName), userAgent)
        }

    @Test
    fun testGetTile_NotFoundReturnsNoTile() {
        val provider =
            createProvider(
                MockEngine {
                    respond(ByteReadChannel("Not Found"), HttpStatusCode.NotFound)
                },
            )

        assertSame(TileProvider.NO_TILE, provider.getTile(1, 2, 3))
    }

    @Test
    fun testGetTile_ServerErrorReturnsNull() {
        val provider =
            createProvider(
                MockEngine {
                    respond(ByteReadChannel("Unavailable"), HttpStatusCode.ServiceUnavailable)
                },
            )

        assertNull(provider.getTile(1, 2, 3))
    }

    @Test
    fun testGetTile_NoUrlReturnsNoTile() {
        var requests = 0
        val provider =
            object : UrlTileProvider("https://example.com/{z}/{x}/{y}.png") {
                // Only tiles from zoom 10 on
                override fun getTileUrl(
                    x: Int,
                    y: Int,
                    zoom: Int,
                ): String? = if (zoom < 10) null else super.getTileUrl(x, y, zoom)
            }
        val reflectionField = UrlTileProvider::class.java.getDeclaredField("client")
        reflectionField.isAccessible = true
        reflectionField.set(
            provider,
            HttpClient(
                MockEngine {
                    requests++
                    respond(ByteReadChannel(byteArrayOf(1)), HttpStatusCode.OK)
                },
            ),
        )

        assertSame(TileProvider.NO_TILE, provider.getTile(1, 2, 3))
        assertEquals(0, requests)
        assertNotNull(provider.getTile(1, 2, 12))
    }

    @Test
    fun testProvidersShareOneClient() {
        val provider1 = object : UrlTileProvider("https://example.com/a/{z}/{x}/{y}.png") {}
        val provider2 = PredefinedTileProviders.openSeaMap() as UrlTileProvider

        assertSame(clientOf(provider1), clientOf(provider2))
    }

    @Test
    @Suppress("DEPRECATION")
    fun testCloseKeepsSharedClientOpen() {
        val provider1 = object : UrlTileProvider("https://example.com/a/{z}/{x}/{y}.png") {}
        val provider2 = object : UrlTileProvider("https://example.com/b/{z}/{x}/{y}.png") {}

        provider1.close()

        assertTrue(clientOf(provider2).isActive)
    }

    private fun clientOf(provider: UrlTileProvider): HttpClient {
        val reflectionField = UrlTileProvider::class.java.getDeclaredField("client")
        reflectionField.isAccessible = true
        return reflectionField.get(provider) as HttpClient
    }

    private fun createProvider(engine: MockEngine): UrlTileProvider {
        val provider = object : UrlTileProvider("https://example.com/{z}/{x}/{y}.png") {}
        val reflectionField = UrlTileProvider::class.java.getDeclaredField("client")
        reflectionField.isAccessible = true
        reflectionField.set(provider, HttpClient(engine))
        return provider
    }
}
