/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color

/**
 * Represents a circle on the map with a center point and radius in meters.
 *
 * @property center Geographic coordinate of the circle center
 * @property radius Radius of the circle in meters
 * @property strokeColor Color of the circle outline (default: black)
 * @property strokeWidth Width of the outline in pixels (default: 10f)
 * @property fillColor Fill color for the circle interior (default: semi-transparent gray)
 * @property visible Whether the circle is visible. Default is true
 * @property clickable Whether the circle is clickable. Default is false
 * @property zIndex Draw order. Circles with higher zIndex are drawn on top. Default is 0.0
 * @property tag Optional user data associated with the circle
 */
data class Circle(
    val center: LatLng,
    val radius: Float,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 10f,
    val fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128),
    val visible: Boolean = true,
    val clickable: Boolean = false,
    val zIndex: Float = 0f,
    val tag: Any? = null,
) {
    /**
     * Unique identifier for this circle instance.
     * Used internally for management and callbacks.
     */
    internal val id: String = "circle_${System.nanoTime()}_${System.identityHashCode(this)}"

    init {
        require(radius > 0) { "Circle radius must be greater than 0" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Circle) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
