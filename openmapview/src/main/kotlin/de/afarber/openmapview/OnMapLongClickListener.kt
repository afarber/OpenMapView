/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Listener interface for map long-click events.
 *
 * Called when the user long-presses on the map (not on a marker or other overlay).
 */
fun interface OnMapLongClickListener {
    /**
     * Called when the map is long-clicked.
     *
     * @param latLng The geographic location that was long-clicked
     */
    fun onMapLongClick(latLng: LatLng)
}
