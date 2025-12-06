/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving info window close events.
 * Called when the info window of a marker is closed, either manually or via auto-dismiss.
 */
fun interface OnInfoWindowCloseListener {
    /**
     * Called when an info window is closed.
     *
     * @param marker The marker whose info window was closed
     */
    fun onInfoWindowClose(marker: Marker)
}
