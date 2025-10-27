/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving polygon click events.
 *
 * Implement this interface and set it using [OpenMapView.setOnPolygonClickListener]
 * to receive callbacks when a polygon is clicked on the map.
 */
fun interface OnPolygonClickListener {
    /**
     * Called when a polygon has been clicked.
     *
     * @param polygon The polygon that was clicked
     */
    fun onPolygonClick(polygon: Polygon)
}
