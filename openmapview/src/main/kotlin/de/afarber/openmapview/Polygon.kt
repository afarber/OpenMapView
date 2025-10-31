/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

/**
 * Represents a polygon on the map, consisting of a closed shape with optional holes.
 *
 * @property points List of geographic coordinates that define the polygon outline.
 *                  The polygon is automatically closed between the last and first point.
 * @property strokeColor Color of the polygon outline (default: black)
 * @property strokeWidth Width of the outline in pixels (default: 10f)
 * @property strokePattern Pattern for the stroke (dashed, dotted, etc.). Null means solid line (default: null)
 * @property strokeCap Shape of line endpoints (default: Round)
 * @property strokeJoin Shape of line corners (default: Round)
 * @property fillColor Fill color for the polygon interior (default: semi-transparent gray)
 * @property holes List of hole definitions, where each hole is a list of LatLng points
 * @property visible Whether the polygon is visible. Default is true
 * @property clickable Whether the polygon is clickable. Default is false
 * @property zIndex Draw order. Polygons with higher zIndex are drawn on top. Default is 0.0
 * @property tag Optional user data associated with the polygon
 */
data class Polygon(
    val points: List<LatLng>,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 10f,
    val strokePattern: PathEffect? = null,
    val strokeCap: StrokeCap = StrokeCap.Round,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
    val fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128),
    val holes: List<List<LatLng>> = emptyList(),
    val visible: Boolean = true,
    val clickable: Boolean = false,
    val zIndex: Float = 0f,
    val tag: Any? = null,
) {
    /**
     * Unique identifier for this polygon instance.
     * Used internally for management and callbacks.
     */
    internal val id: String = "polygon_${System.nanoTime()}_${System.identityHashCode(this)}"

    init {
        require(points.size >= 3) { "Polygon must have at least 3 points" }
        holes.forEach { hole ->
            require(hole.size >= 3) { "Polygon hole must have at least 3 points" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Polygon) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
