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
 * The map calls [getTile] on a background thread, so it may block, e.g. to read a file or
 * render the tile. Implement it from Kotlin or Java:
 * ```kotlin
 * val provider = TileProvider { x, y, zoom ->
 *     if (zoom < 10) TileProvider.NO_TILE else Tile(256, 256, renderTile(x, y, zoom))
 * }
 * ```
 * ```java
 * TileProvider provider = (x, y, zoom) ->
 *     zoom < 10 ? TileProvider.NO_TILE : new Tile(256, 256, renderTile(x, y, zoom));
 * ```
 *
 * @see UrlTileProvider
 * @see Tile
 * @see TileOverlay
 */
fun interface TileProvider {
    /**
     * Returns the [Tile] for the specified coordinates.
     *
     * Called on a background thread; several calls may run at the same time, so
     * implementations must be thread-safe.
     *
     * @param x Tile column (0 to 2^zoom - 1)
     * @param y Tile row (0 to 2^zoom - 1)
     * @param zoom Zoom level (0-19)
     * @return The tile, [NO_TILE] if there is no tile at these coordinates (it is not
     * requested again), or null if it cannot be loaded right now (it is retried later)
     */
    fun getTile(
        x: Int,
        y: Int,
        zoom: Int,
    ): Tile?

    companion object {
        /**
         * Returned by [getTile] where there is no tile, e.g. outside the area a provider
         * covers. The map then draws nothing there and does not request the tile again.
         * Compared by reference: only this instance has that meaning.
         */
        @JvmField
        val NO_TILE: Tile = Tile(1, 1, byteArrayOf(0))
    }
}
