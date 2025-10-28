/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Map type constants for OpenMapView.
 *
 * Defines available map tile styles using free OpenStreetMap-based tile sources.
 *
 * **Available Types:**
 * - [NONE]: No base map tiles
 * - [NORMAL]: Standard OpenStreetMap road map (default)
 * - [TERRAIN]: Topographic map with contour lines (OpenTopoMap)
 * - [HUMANITARIAN]: Humanitarian-focused OSM style
 * - [CYCLE]: Cycling-focused map style (CyclOSM)
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
     * Topographic terrain map with contour lines.
     *
     * Uses OpenTopoMap tiles showing elevation, hillshading, and contour lines.
     * Zoom levels: 0-17
     */
    const val TERRAIN: Int = 2

    /**
     * Humanitarian-focused OpenStreetMap style.
     *
     * Uses Humanitarian OSM tiles with red/orange color scheme.
     * Emphasizes hospitals, schools, water sources, and disaster response features.
     */
    const val HUMANITARIAN: Int = 3

    /**
     * Cycling-focused map style.
     *
     * Uses CyclOSM tiles emphasizing cycling infrastructure:
     * bike lanes, paths, parking, shops, and difficulty ratings.
     */
    const val CYCLE: Int = 4
}
