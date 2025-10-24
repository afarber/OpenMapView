/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap

/**
 * Represents a marker on the map at a specific geographic location.
 *
 * @property position The geographic location of the marker
 * @property title Optional title text displayed when marker is clicked
 * @property snippet Optional snippet text displayed below the title
 * @property icon Custom icon bitmap. If null, a default marker icon will be used
 * @property anchor Anchor point for the marker icon. Default (0.5f, 1.0f) means
 *                  the marker is centered horizontally and anchored at the bottom
 * @property tag Optional user data associated with the marker
 */
data class Marker(
    val position: LatLng,
    val title: String? = null,
    val snippet: String? = null,
    val icon: Bitmap? = null,
    val anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    val tag: Any? = null,
) {
    /**
     * Unique identifier for this marker instance.
     * Used internally for touch detection and callbacks.
     */
    internal val id: String = "marker_${System.nanoTime()}_${hashCode()}"
}
