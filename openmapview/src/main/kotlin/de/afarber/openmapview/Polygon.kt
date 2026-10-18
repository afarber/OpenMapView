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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposePathEffect
import java.util.concurrent.atomic.AtomicLong

/**
 * A polygon on the map: a closed shape with optional holes.
 *
 * The polygon is a live handle, as returned by [OpenMapView.addPolygon]: setting a property, e.g.
 * `polygon.strokeWidth = 16f`, redraws the map right away, and [remove] takes it off the map.
 * Java code uses the getters and setters, e.g. `polygon.setStrokeColor(0xFFFF0000)`.
 *
 * @param points the geographic coordinates of the outline, at least 3; the last point connects to the first
 * @param strokeColor the outline color
 * @param strokeWidth the outline width in pixels
 * @param strokePattern the stroke pattern, e.g. dashed; null for a solid line
 * @param pattern the stroke pattern as dots, dashes and gaps, used instead of [strokePattern] if not null
 * @param strokeCap the shape of line ends
 * @param strokeJoin the shape of the corners
 * @param fillColor the color of the interior
 * @param holes holes cut out of the interior, each with at least 3 points
 * @param geodesic whether segments are drawn as great-circle paths instead of straight lines on the Mercator map
 * @param visible whether the polygon is visible
 * @param clickable whether a tap on the polygon is reported to its click listener
 * @param zIndex draw order: polygons with a higher zIndex are drawn on top
 * @param tag optional user data
 */
class Polygon(
    points: List<LatLng>,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 10f,
    strokePattern: PathEffect? = null,
    pattern: List<PatternItem>? = null,
    strokeCap: StrokeCap = StrokeCap.Butt,
    strokeJoin: StrokeJoin = StrokeJoin.Round,
    fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128),
    holes: List<List<LatLng>> = emptyList(),
    geodesic: Boolean = false,
    visible: Boolean = true,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
) {
    // The map that shows the polygon, redrawn when a property changes
    internal var mapView: OpenMapView? = null

    private fun <T> property(
        initial: T,
        check: (T) -> Unit = {},
    ) = ShapeProperty(initial, check) { mapView?.invalidate() }

    /** The geographic coordinates of the outline, at least 3; the last point connects to the first */
    var points: List<LatLng> by property(points) { require(it.size >= 3) { "Polygon must have at least 3 points" } }

    /** The outline color */
    var strokeColor: Color by property(strokeColor)

    /** The outline width in pixels */
    var strokeWidth: Float by property(strokeWidth)

    /** The stroke pattern, e.g. dashed; null for a solid line */
    var strokePattern: PathEffect? by property(strokePattern)

    /** The stroke pattern as dots, dashes and gaps, used instead of [strokePattern] if not null */
    var pattern: List<PatternItem>? by property(pattern)

    /** The shape of line ends */
    var strokeCap: StrokeCap by property(strokeCap)

    /** The shape of the corners */
    var strokeJoin: StrokeJoin by property(strokeJoin)

    /** The color of the interior */
    var fillColor: Color by property(fillColor)

    /** Holes cut out of the interior, each with at least 3 points */
    var holes: List<List<LatLng>> by property(holes) { holes ->
        holes.forEach {
            require(it.size >= 3) { "Polygon hole must have at least 3 points" }
        }
    }

    /** Whether segments are drawn as great-circle paths instead of straight lines on the Mercator map */
    var geodesic: Boolean by property(geodesic)

    /** Whether the polygon is visible */
    var visible: Boolean by property(visible)

    /** Whether a tap on the polygon is reported to its click listener */
    var clickable: Boolean by property(clickable)

    /** Draw order: polygons with a higher zIndex are drawn on top */
    var zIndex: Float by property(zIndex)

    /** Optional user data */
    var tag: Any? by property(tag)

    /**
     * The unique id of this polygon, e.g. "pg0", which stays the same for its lifetime.
     * [copy] returns a polygon with a new id.
     */
    val id: String = "pg${nextId.getAndIncrement()}"

    /** [strokeColor] as an ARGB color int, e.g. for Java code. */
    var strokeColorArgb: Int
        get() = strokeColor.toArgb()
        set(value) {
            strokeColor = Color(value)
        }

    /** [fillColor] as an ARGB color int, e.g. for Java code. */
    var fillColorArgb: Int
        get() = fillColor.toArgb()
        set(value) {
            fillColor = Color(value)
        }

    /** The stroke color as an ARGB color int, like `getStrokeColor()` of the Maps SDK. */
    fun getStrokeColor(): Int = strokeColorArgb

    /** Sets the stroke color from an ARGB color int, like `setStrokeColor(int)` of the Maps SDK. */
    fun setStrokeColor(color: Int) {
        strokeColorArgb = color
    }

    /** The fill color as an ARGB color int, like `getFillColor()` of the Maps SDK. */
    fun getFillColor(): Int = fillColorArgb

    /** Sets the fill color from an ARGB color int, like `setFillColor(int)` of the Maps SDK. */
    fun setFillColor(color: Int) {
        fillColorArgb = color
    }

    /** The shape of line ends as a [Cap], e.g. for Java code. */
    fun getStrokeCap(): Cap = strokeCap.toCap()

    /** Sets the shape of line ends, e.g. `setStrokeCap(new RoundCap())` from Java. */
    fun setStrokeCap(cap: Cap) {
        strokeCap = cap.toStrokeCap()
    }

    /** The shape of the corners as a [JointType] constant, like `getStrokeJointType()` of the Maps SDK. */
    fun getStrokeJointType(): Int = strokeJoin.toJointType()

    /** Sets the shape of the corners from a [JointType] constant, like `setStrokeJointType(int)` of the Maps SDK. */
    fun setStrokeJointType(jointType: Int) {
        strokeJoin = jointTypeToStrokeJoin(jointType)
    }

    /** Sets [pattern] from dots, dashes and gaps, like `setStrokePattern(List)` of the Maps SDK. */
    fun setStrokePattern(pattern: List<PatternItem>) {
        this.pattern = pattern
    }

    /** Sets [strokePattern] from an android.graphics.PathEffect, e.g. a DashPathEffect from Java. */
    fun setStrokePattern(pattern: android.graphics.PathEffect) {
        strokePattern = pattern.toComposePathEffect()
    }

    /** Removes the polygon from the map that shows it. */
    fun remove() {
        mapView?.removePolygon(this)
    }

    /** Returns a new polygon with the given properties changed, not added to any map. */
    fun copy(
        points: List<LatLng> = this.points,
        strokeColor: Color = this.strokeColor,
        strokeWidth: Float = this.strokeWidth,
        strokePattern: PathEffect? = this.strokePattern,
        pattern: List<PatternItem>? = this.pattern,
        strokeCap: StrokeCap = this.strokeCap,
        strokeJoin: StrokeJoin = this.strokeJoin,
        fillColor: Color = this.fillColor,
        holes: List<List<LatLng>> = this.holes,
        geodesic: Boolean = this.geodesic,
        visible: Boolean = this.visible,
        clickable: Boolean = this.clickable,
        zIndex: Float = this.zIndex,
        tag: Any? = this.tag,
    ): Polygon =
        Polygon(
            points,
            strokeColor,
            strokeWidth,
            strokePattern,
            pattern,
            strokeCap,
            strokeJoin,
            fillColor,
            holes,
            geodesic,
            visible,
            clickable,
            zIndex,
            tag,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Polygon) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    private companion object {
        // Shared by all maps, so ids are unique in the app
        val nextId = AtomicLong()
    }
}
