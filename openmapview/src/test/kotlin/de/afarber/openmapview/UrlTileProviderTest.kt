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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    fun testGetTile_NotFoundReturnsNull() =
        runTest {
            val provider =
                createProvider(
                    MockEngine {
                        respond(ByteReadChannel("Not Found"), HttpStatusCode.NotFound)
                    },
                )

            assertNull(provider.getTile(1, 2, 3))
        }

    private fun createProvider(engine: MockEngine): UrlTileProvider {
        val provider = object : UrlTileProvider("https://example.com/{z}/{x}/{y}.png") {}
        val reflectionField = UrlTileProvider::class.java.getDeclaredField("client")
        reflectionField.isAccessible = true
        reflectionField.set(provider, HttpClient(engine))
        return provider
    }
}
