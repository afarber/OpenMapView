/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Represents an edge of the map view for edge effect triggering.
 *
 * Used with [OpenMapView.triggerEdgeEffect] to specify which edges should display
 * a visual glow effect when the camera reaches its bounds.
 */
enum class Edge {
    /** Top edge of the map view (north direction) */
    TOP,

    /** Bottom edge of the map view (south direction) */
    BOTTOM,

    /** Left edge of the map view (west direction) */
    LEFT,

    /** Right edge of the map view (east direction) */
    RIGHT,
}
