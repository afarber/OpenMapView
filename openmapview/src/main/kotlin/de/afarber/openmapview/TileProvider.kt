/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for providing tiles to a [TileOverlay].
 *
 * Implementations can provide tiles from various sources:
 * - URLs (via [UrlTileProvider])
 * - Local storage
 * - Dynamically generated content
 * - Custom tile rendering
 *
 * Tile coordinates use the Web Mercator projection (EPSG:3857) with the XYZ tile scheme:
 * - x: Tile column (0 to 2^zoom - 1, west to east)
 * - y: Tile row (0 to 2^zoom - 1, north to south)
 * - zoom: Zoom level (0 = world, 19 = building level)
 *
 * Example implementation:
 * ```kotlin
 * class CustomTileProvider : TileProvider {
 *     override suspend fun getTile(x: Int, y: Int, zoom: Int): Tile? {
 *         // Return tile data or null if tile is not available
 *         return Tile(256, 256, pngByteArray)
 *     }
 * }
 * ```
 *
 * @see UrlTileProvider
 * @see Tile
 * @see TileOverlay
 */
fun interface TileProvider {
    /**
     * Returns a [Tile] for the specified coordinates.
     *
     * This method is called asynchronously when tiles are needed for rendering.
     * Implementations should be thread-safe and non-blocking.
     *
     * @param x Tile column (0 to 2^zoom - 1)
     * @param y Tile row (0 to 2^zoom - 1)
     * @param zoom Zoom level (0-19)
     * @return [Tile] containing image data, or null if tile is not available
     */
    suspend fun getTile(
        x: Int,
        y: Int,
        zoom: Int,
    ): Tile?
}
