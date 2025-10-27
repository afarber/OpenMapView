/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Defines available tile sources for map rendering.
 *
 * Each source provides a URL template for fetching map tiles.
 *
 * @property urlTemplate The URL template with {x}, {y}, {z} placeholders
 */
enum class TileSource(
    private val urlTemplate: String,
) {
    /**
     * Standard OpenStreetMap tile server.
     *
     * Uses the default OSM tile server at tile.openstreetmap.org.
     * Subject to OSM tile usage policy.
     */
    STANDARD("https://tile.openstreetmap.org/{z}/{x}/{y}.png"),
    ;

    /**
     * Generates the tile URL for the specified tile coordinate.
     *
     * @param tile The tile coordinate
     * @return The complete URL for downloading the tile
     */
    fun getTileUrl(tile: TileCoordinate): String =
        urlTemplate
            .replace("{z}", tile.zoom.toString())
            .replace("{x}", tile.x.toString())
            .replace("{y}", tile.y.toString())
}
