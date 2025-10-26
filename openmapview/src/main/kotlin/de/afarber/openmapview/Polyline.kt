/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Color

/**
 * Represents a polyline on the map, consisting of connected line segments.
 *
 * @property points List of geographic coordinates that define the polyline path
 * @property strokeColor Color of the line stroke (default: black)
 * @property strokeWidth Width of the line in pixels (default: 10f)
 * @property tag Optional user data associated with the polyline
 */
data class Polyline(
    val points: List<LatLng>,
    val strokeColor: Int = Color.BLACK,
    val strokeWidth: Float = 10f,
    val tag: Any? = null,
) {
    /**
     * Unique identifier for this polyline instance.
     * Used internally for management and callbacks.
     */
    internal val id: String = "polyline_${System.nanoTime()}_${System.identityHashCode(this)}"

    init {
        require(points.size >= 2) { "Polyline must have at least 2 points" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Polyline) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
