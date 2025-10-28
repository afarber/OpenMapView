/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Map type constants compatible with Google Maps Android API.
 *
 * OpenMapView supports a subset of Google's map types plus additional OSM-specific styles.
 *
 * **Supported Types:**
 * - [NONE]: No base map tiles
 * - [NORMAL]: Standard OpenStreetMap road map (default)
 * - [TERRAIN]: Topographic map with contour lines (OpenTopoMap)
 * - [HUMANITARIAN]: Humanitarian-focused OSM style
 * - [TOPO]: Alias for TERRAIN
 * - [CYCLE]: Cycling-focused map style (CyclOSM)
 *
 * **Unsupported Types (throw exception):**
 * - [SATELLITE]: Satellite imagery not available (requires paid API)
 * - [HYBRID]: Satellite + labels not available (requires paid API)
 *
 * @see OpenMapView.setMapType
 * @see OpenMapView.getMapType
 */
object MapType {
    /**
     * No base map tiles displayed.
     */
    const val NONE: Int = 0

    /**
     * Standard OpenStreetMap road map (default).
     *
     * Uses tiles from tile.openstreetmap.org.
     * Shows roads, buildings, land use, and labels.
     */
    const val NORMAL: Int = 1

    /**
     * Satellite imagery.
     *
     * **NOT SUPPORTED** - Throws [UnsupportedOperationException].
     * Satellite tiles require paid APIs (Mapbox, Google, Bing).
     */
    const val SATELLITE: Int = 2

    /**
     * Topographic terrain map with contour lines.
     *
     * Uses OpenTopoMap tiles showing elevation, hillshading, and contour lines.
     * Zoom levels: 0-17
     */
    const val TERRAIN: Int = 3

    /**
     * Satellite imagery with road and label overlays.
     *
     * **NOT SUPPORTED** - Throws [UnsupportedOperationException].
     * Hybrid maps require paid satellite APIs.
     */
    const val HYBRID: Int = 4

    /**
     * Humanitarian-focused OpenStreetMap style.
     *
     * Uses Humanitarian OSM tiles with red/orange color scheme.
     * Emphasizes hospitals, schools, water sources, and disaster response features.
     */
    const val HUMANITARIAN: Int = 5

    /**
     * Topographic map (same as [TERRAIN]).
     *
     * Alias for TERRAIN type providing OpenTopoMap tiles.
     */
    const val TOPO: Int = 6

    /**
     * Cycling-focused map style.
     *
     * Uses CyclOSM tiles emphasizing cycling infrastructure:
     * bike lanes, paths, parking, shops, and difficulty ratings.
     */
    const val CYCLE: Int = 7
}
