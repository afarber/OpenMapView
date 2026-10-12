/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving info window click events.
 * Called when the info window of a marker is clicked.
 */
fun interface OnInfoWindowClickListener {
    /**
     * Called when an info window is clicked.
     *
     * @param marker The marker whose info window was clicked
     */
    fun onInfoWindowClick(marker: Marker)
}
