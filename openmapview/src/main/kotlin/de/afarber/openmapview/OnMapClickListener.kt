/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Listener interface for map click events.
 *
 * Called when the user taps on the map (not on a marker or other overlay).
 */
fun interface OnMapClickListener {
    /**
     * Called when the map is clicked.
     *
     * @param latLng The geographic location that was clicked
     */
    fun onMapClick(latLng: LatLng)
}
