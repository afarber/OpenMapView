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
 * Each source provides a URL template for fetching map tiles and attribution text.
 *
 * @property urlTemplate The URL template with {x}, {y}, {z} placeholders
 * @property attribution Attribution text required by the tile provider
 */
enum class TileSource(
    private val urlTemplate: String,
    val attribution: String,
) {
    /**
     * Standard OpenStreetMap tile server.
     *
     * Uses the default OSM tile server at tile.openstreetmap.org.
     * Subject to OSM tile usage policy.
     */
    STANDARD(
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        "© OpenStreetMap contributors",
    ),

    /**
     * Humanitarian OpenStreetMap style.
     *
     * Red/orange color scheme emphasizing humanitarian features:
     * hospitals, schools, water sources, disaster response.
     */
    HUMANITARIAN(
        "https://tile-a.openstreetmap.fr/hot/{z}/{x}/{y}.png",
        "© OpenStreetMap contributors",
    ),

    /**
     * OpenTopoMap with topographic features.
     *
     * Includes elevation contour lines, hillshading, and terrain details.
     * Zoom levels: 0-17
     */
    TOPO(
        "https://a.tile.opentopomap.org/{z}/{x}/{y}.png",
        "© OpenStreetMap contributors, SRTM | © OpenTopoMap (CC-BY-SA)",
    ),

    /**
     * CyclOSM cycling-focused map.
     *
     * Emphasizes cycling infrastructure: bike lanes, paths, parking,
     * shops, and difficulty ratings.
     */
    CYCLE(
        "https://a.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png",
        "© OpenStreetMap contributors | © CyclOSM",
    ),
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
