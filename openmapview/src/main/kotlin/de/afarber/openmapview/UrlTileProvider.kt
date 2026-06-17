/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes

/**
 * Abstract base class for [TileProvider] implementations that fetch tiles from URLs.
 *
 * This class handles HTTP requests and tile downloading, while subclasses provide
 * the URL template and optional customization.
 *
 * URL templates use placeholders that are replaced with tile coordinates:
 * - `{z}` - Zoom level
 * - `{x}` - Tile column
 * - `{y}` - Tile row
 *
 * Example usage:
 * ```kotlin
 * class CustomTileProvider : UrlTileProvider(
 *     urlTemplate = "https://example.com/tiles/{z}/{x}/{y}.png"
 * ) {
 *     override fun getUserAgent(): String {
 *         return "MyApp/1.0"
 *     }
 * }
 * ```
 *
 * @property urlTemplate URL pattern with {z}, {x}, {y} placeholders
 *
 * @see TileProvider
 * @see Tile
 */
abstract class UrlTileProvider(
    private val urlTemplate: String,
) : TileProvider {
    companion object {
        private val TAG = UrlTileProvider::class.java.simpleName
    }

    private val client =
        HttpClient(Android) {
            engine {
                connectTimeout = 10_000
                socketTimeout = 10_000
            }
        }

    /**
     * Returns the User-Agent header to use for HTTP requests.
     * Subclasses can override to provide a custom User-Agent.
     *
     * @return User-Agent string
     */
    protected open fun getUserAgent(): String = "OpenMapView/0.13.2 (https://github.com/afarber/OpenMapView)"

    /**
     * Builds the URL for the specified tile coordinates by replacing placeholders
     * in the URL template.
     *
     * Subclasses can override to customize URL generation.
     *
     * @param x Tile column
     * @param y Tile row
     * @param zoom Zoom level
     * @return Complete URL for the tile
     */
    protected open fun getTileUrl(
        x: Int,
        y: Int,
        zoom: Int,
    ): String =
        urlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())

    /**
     * Fetches tile data from the URL for the specified coordinates.
     *
     * @param x Tile column
     * @param y Tile row
     * @param zoom Zoom level
     * @return [Tile] with image data, or null if download fails
     */
    override suspend fun getTile(
        x: Int,
        y: Int,
        zoom: Int,
    ): Tile? =
        try {
            val url = getTileUrl(x, y, zoom)
            val response =
                client.get(url) {
                    header("User-Agent", getUserAgent())
                }
            val bytes = response.readRawBytes()

            if (bytes.isEmpty()) {
                Log.w(TAG, "Empty tile data from $url")
                null
            } else {
                // Standard OSM tile size
                Tile(256, 256, bytes)
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to download tile ($x, $y, $zoom): ${e.javaClass.simpleName}: ${e.message}",
                e,
            )
            null
        }

    /**
     * Closes the HTTP client and releases resources.
     * Should be called when the tile provider is no longer needed.
     */
    fun close() {
        client.close()
    }
}
