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
 * Represents a polyline on the map, consisting of connected line segments.
 *
 * @property points List of geographic coordinates that define the polyline path
 * @property strokeColor Color of the line stroke (default: black). Used when spans is empty.
 * @property strokeWidth Width of the line in pixels (default: 10f)
 * @property strokePattern Pattern for the stroke (dashed, dotted, etc.). Null means solid line (default: null)
 * @property startCap Shape of the start endpoint (default: Butt)
 * @property endCap Shape of the end endpoint (default: Butt)
 * @property strokeJoin Shape of line corners (default: Round)
 * @property geodesic Whether segments are drawn as geodesics (great-circle paths) instead of straight
 *                    lines on the Mercator projection. Default is false.
 * @property spans List of style spans for multi-colored polylines. When provided, overrides strokeColor
 *                 for the specified segments. Default is empty (use strokeColor for all segments).
 * @property visible Whether the polyline is visible. Default is true
 * @property clickable Whether the polyline is clickable. Default is false
 * @property zIndex Draw order. Polylines with higher zIndex are drawn on top. Default is 0.0
 * @property tag Optional user data associated with the polyline
 */
data class Polyline(
    val points: List<LatLng>,
    val strokeColor: Color = Color.Black,
    val strokeWidth: Float = 10f,
    val strokePattern: PathEffect? = null,
    val startCap: StrokeCap = StrokeCap.Butt,
    val endCap: StrokeCap = StrokeCap.Butt,
    val strokeJoin: StrokeJoin = StrokeJoin.Round,
    val geodesic: Boolean = false,
    val spans: List<StyleSpan> = emptyList(),
    val visible: Boolean = true,
    val clickable: Boolean = false,
    val zIndex: Float = 0f,
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
