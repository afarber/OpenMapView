/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving marker drag events.
 *
 * Compatible with Google Maps API. Implement this interface to receive
 * callbacks when a draggable marker is dragged across the map.
 */
interface OnMarkerDragListener {
    /**
     * Called when a marker starts being dragged.
     *
     * @param marker The marker being dragged
     */
    fun onMarkerDragStart(marker: Marker)

    /**
     * Called repeatedly while a marker is being dragged.
     *
     * @param marker The marker being dragged
     */
    fun onMarkerDrag(marker: Marker)

    /**
     * Called when a marker has finished being dragged.
     *
     * @param marker The marker that was dragged
     */
    fun onMarkerDragEnd(marker: Marker)
}
