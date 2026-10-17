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
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking

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
 * All instances share one HTTP client, so creating many providers (for example when
 * overlays are switched on and off) does not create more connection pools.
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

        // Shared by all providers and never closed, it lives as long as the process
        private val sharedClient by lazy {
            HttpClient(Android) {
                engine {
                    connectTimeout = 10_000
                    socketTimeout = 10_000
                }
            }
        }
    }

    private val client: HttpClient = sharedClient

    /**
     * Returns the User-Agent header to use for HTTP requests.
     *
     * The default identifies both OpenMapView and the app, e.g.
     * `OpenMapView/0.17.1 (com.example.app; https://github.com/afarber/OpenMapView)`.
     * Subclasses can override to provide a custom User-Agent.
     *
     * @return User-Agent string
     */
    protected open fun getUserAgent(): String = UserAgent.value

    /**
     * Builds the URL for the specified tile coordinates by replacing placeholders
     * in the URL template.
     *
     * Subclasses can override to customize URL generation, or return null where the
     * source has no tiles, e.g. outside its area or zoom range.
     *
     * @param x Tile column
     * @param y Tile row
     * @param zoom Zoom level
     * @return Complete URL for the tile, or null for [TileProvider.NO_TILE]
     */
    protected open fun getTileUrl(
        x: Int,
        y: Int,
        zoom: Int,
    ): String? =
        urlTemplate
            .replace("{z}", zoom.toString())
            .replace("{x}", x.toString())
            .replace("{y}", y.toString())

    /**
     * Downloads the tile for the specified coordinates, blocking the calling thread.
     *
     * @param x Tile column
     * @param y Tile row
     * @param zoom Zoom level
     * @return [Tile] with image data, [TileProvider.NO_TILE] if there is no URL for it or
     * the server has no tile (HTTP 404), or null if the download failed
     */
    override fun getTile(
        x: Int,
        y: Int,
        zoom: Int,
    ): Tile? = runBlocking { download(x, y, zoom) }

    private suspend fun download(
        x: Int,
        y: Int,
        zoom: Int,
    ): Tile? {
        val url = getTileUrl(x, y, zoom) ?: return TileProvider.NO_TILE
        return try {
            val response =
                client.get(url) {
                    header("User-Agent", getUserAgent())
                }
            if (response.status == HttpStatusCode.NotFound) {
                // Sparse overlays commonly answer 404 for areas without data
                Log.d(TAG, "No tile ($x, $y, $zoom) from $url")
                return TileProvider.NO_TILE
            }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Failed to download tile ($x, $y, $zoom) from $url: HTTP ${response.status.value}")
                return null
            }
            val bytes = response.readRawBytes()

            if (bytes.isEmpty()) {
                Log.w(TAG, "Empty tile data from $url")
                null
            } else {
                // Standard OSM tile size
                Tile(256, 256, bytes)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to download tile ($x, $y, $zoom): ${e.javaClass.simpleName}: ${e.message}",
                e,
            )
            null
        }
    }

    /**
     * Does nothing. All providers share one HTTP client, which must stay open
     * for the other providers.
     */
    @Deprecated("UrlTileProvider instances share one HTTP client now, there is nothing to close")
    fun close() {
        // Nothing to release per instance
    }
}
