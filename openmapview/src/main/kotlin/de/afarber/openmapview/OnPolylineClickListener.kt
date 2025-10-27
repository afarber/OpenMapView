/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving polyline click events.
 *
 * Implement this interface and set it using [OpenMapView.setOnPolylineClickListener]
 * to receive callbacks when a polyline is clicked on the map.
 */
fun interface OnPolylineClickListener {
    /**
     * Called when a polyline has been clicked.
     *
     * @param polyline The polyline that was clicked
     */
    fun onPolylineClick(polyline: Polyline)
}
