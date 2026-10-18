/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving info window long click events.
 * Called when the user presses and holds the info window of a marker.
 */
fun interface OnInfoWindowLongClickListener {
    /**
     * Called when an info window is long clicked.
     *
     * @param marker The marker whose info window was long clicked
     */
    fun onInfoWindowLongClick(marker: Marker)
}
