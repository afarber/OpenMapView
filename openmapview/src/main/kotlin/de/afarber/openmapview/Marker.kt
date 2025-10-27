/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Represents a marker on the map at a specific geographic location.
 *
 * @property position The geographic location of the marker (mutable for dragging)
 * @property title Optional title text displayed when marker is clicked
 * @property snippet Optional snippet text displayed below the title
 * @property icon Custom icon descriptor. If null, a default red marker icon will be used
 * @property anchor Anchor point for the marker icon. Default (0.5f, 1.0f) means
 *                  the marker is centered horizontally and anchored at the bottom
 * @property visible Whether the marker is visible. Default is true
 * @property alpha Opacity of the marker from 0.0 (transparent) to 1.0 (opaque). Default is 1.0
 * @property draggable Whether the marker can be dragged. Default is false
 * @property zIndex Draw order. Markers with higher zIndex are drawn on top. Default is 0.0
 * @property tag Optional user data associated with the marker
 */
data class Marker(
    var position: LatLng,
    val title: String? = null,
    val snippet: String? = null,
    val icon: BitmapDescriptor? = null,
    val anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    val visible: Boolean = true,
    val alpha: Float = 1.0f,
    val draggable: Boolean = false,
    val zIndex: Float = 0f,
    val tag: Any? = null,
) {
    init {
        require(alpha in 0.0f..1.0f) { "Alpha must be between 0.0 and 1.0" }
    }

    /**
     * Unique identifier for this marker instance.
     * Used internally for touch detection and callbacks.
     */
    internal val id: String = "marker_${System.nanoTime()}_${System.identityHashCode(this)}"

    /**
     * Whether the info window is currently shown for this marker.
     */
    internal var isInfoWindowShown: Boolean = false

    /**
     * Shows the info window for this marker.
     * The info window displays the marker's title and snippet text.
     */
    fun showInfoWindow() {
        isInfoWindowShown = true
    }

    /**
     * Hides the info window for this marker.
     */
    fun hideInfoWindow() {
        isInfoWindowShown = false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Marker) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
