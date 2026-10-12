/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

data class GroundOverlay(
    val image: BitmapDescriptor,
    val position: LatLng? = null,
    val width: Float? = null,
    val height: Float? = null,
    val bounds: LatLngBounds? = null,
    val anchor: Pair<Float, Float> = Pair(0.5f, 0.5f),
    val bearing: Float = 0f,
    val transparency: Float = 0f,
    val visible: Boolean = true,
    val zIndex: Float = 0f,
    val clickable: Boolean = false,
    val tag: Any? = null,
) {
    internal val id: String = "ground_overlay_${System.nanoTime()}_${System.identityHashCode(this)}"

    init {
        require(transparency in 0f..1f) { "Transparency must be between 0.0 and 1.0" }
        require(anchor.first in 0f..1f && anchor.second in 0f..1f) {
            "Anchor coordinates must be between 0.0 and 1.0"
        }

        val hasPosition = position != null && width != null
        val hasBounds = bounds != null

        require(hasPosition || hasBounds) {
            "Ground overlay must have either position with width or bounds"
        }

        require(!(hasPosition && hasBounds)) {
            "Ground overlay cannot have both position and bounds"
        }

        if (width != null) {
            require(width > 0) { "Width must be greater than 0" }
        }

        if (height != null) {
            require(height > 0) { "Height must be greater than 0" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroundOverlay) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
