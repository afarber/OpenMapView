/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Listener interface for marker click events.
 *
 * Called when the user taps on a marker.
 */
fun interface OnMarkerClickListener {
    /**
     * Called when a marker is clicked.
     *
     * @param marker The marker that was clicked
     * @return true to consume the event, false for the default behavior: the marker's info
     *         window opens and the camera moves to the marker
     */
    fun onMarkerClick(marker: Marker): Boolean
}
