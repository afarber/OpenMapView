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

/**
 * A polyline on the map: connected line segments.
 *
 * The polyline is a live handle, as returned by [OpenMapView.addPolyline]: setting a property, e.g.
 * `polyline.strokeWidth = 16f`, redraws the map right away, and [remove] takes it off the map.
 * Java code uses the getters and setters, e.g. `polyline.setWidth(16f)`.
 *
 * @param points the geographic coordinates of the path, at least 2
 * @param strokeColor the line color, used where no [spans] apply
 * @param strokeWidth the line width in pixels
 * @param strokePattern the stroke pattern, e.g. dashed; null for a solid line
 * @param pattern the stroke pattern as dots, dashes and gaps, used instead of [strokePattern] if not null
 * @param startCap the shape of the start
 * @param endCap the shape of the end
 * @param strokeJoin the shape of the corners
 * @param geodesic whether segments are drawn as great-circle paths instead of straight lines on the Mercator map
 * @param spans style spans for a multi-colored line, overriding [strokeColor] for their segments
 * @param visible whether the polyline is visible
 * @param clickable whether a tap on the polyline is reported to its click listener
 * @param zIndex draw order: polylines with a higher zIndex are drawn on top
 * @param tag optional user data
 */
class Polyline(
    points: List<LatLng>,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 10f,
    strokePattern: PathEffect? = null,
    pattern: List<PatternItem>? = null,
    startCap: StrokeCap = StrokeCap.Butt,
    endCap: StrokeCap = StrokeCap.Butt,
    strokeJoin: StrokeJoin = StrokeJoin.Round,
    geodesic: Boolean = false,
    spans: List<StyleSpan> = emptyList(),
    visible: Boolean = true,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
) {
    // The map that shows the polyline, redrawn when a property changes
    internal var mapView: OpenMapView? = null

    private fun <T> property(
        initial: T,
        check: (T) -> Unit = {},
    ) = ShapeProperty(initial, check) { mapView?.invalidate() }

    /** The geographic coordinates of the path, at least 2 */
    var points: List<LatLng> by property(points) { require(it.size >= 2) { "Polyline must have at least 2 points" } }

    /** The line color, used where no [spans] apply */
    var strokeColor: Color by property(strokeColor)

    /** The line width in pixels */
    var strokeWidth: Float by property(strokeWidth)

    /** The stroke pattern, e.g. dashed; null for a solid line */
    var strokePattern: PathEffect? by property(strokePattern)

    /** The stroke pattern as dots, dashes and gaps, used instead of [strokePattern] if not null */
    var pattern: List<PatternItem>? by property(pattern)

    /** The shape of the start */
    var startCap: StrokeCap by property(startCap)

    /** The shape of the end */
    var endCap: StrokeCap by property(endCap)

    /** The shape of the corners */
    var strokeJoin: StrokeJoin by property(strokeJoin)

    /** Whether segments are drawn as great-circle paths instead of straight lines on the Mercator map */
    var geodesic: Boolean by property(geodesic)

    /** Style spans for a multi-colored line, overriding [strokeColor] for their segments */
    var spans: List<StyleSpan> by property(spans)

    /** Whether the polyline is visible */
    var visible: Boolean by property(visible)

    /** Whether a tap on the polyline is reported to its click listener */
    var clickable: Boolean by property(clickable)

    /** Draw order: polylines with a higher zIndex are drawn on top */
    var zIndex: Float by property(zIndex)

    /** Optional user data */
    var tag: Any? by property(tag)

    /**
     * Unique identifier for this polyline instance.
     * Used internally for management and callbacks.
     */
    internal val id: String = "polyline_${System.nanoTime()}_${System.identityHashCode(this)}"

    /** [strokeColor] as an ARGB color int, e.g. for Java code. */
    var strokeColorArgb: Int
        get() = strokeColor.toArgb()
        set(value) {
            strokeColor = Color(value)
        }

    /** The stroke color as an ARGB color int, like `getColor()` of the Maps SDK. */
    fun getColor(): Int = strokeColorArgb

    /** Sets the stroke color from an ARGB color int, like `setColor(int)` of the Maps SDK. */
    fun setColor(color: Int) {
        strokeColorArgb = color
    }

    /** The stroke color as an ARGB color int, the same as [getColor]. */
    fun getStrokeColor(): Int = strokeColorArgb

    /** Sets the stroke color from an ARGB color int, the same as [setColor]. */
    fun setStrokeColor(color: Int) {
        strokeColorArgb = color
    }

    /** The shape of the start, like `getStartCap()` of the Maps SDK. */
    fun getStartCap(): Cap = startCap.toCap()

    /** Sets the shape of the start, like `setStartCap(Cap)` of the Maps SDK. */
    fun setStartCap(cap: Cap) {
        startCap = cap.toStrokeCap()
    }

    /** The shape of the end, like `getEndCap()` of the Maps SDK. */
    fun getEndCap(): Cap = endCap.toCap()

    /** Sets the shape of the end, like `setEndCap(Cap)` of the Maps SDK. */
    fun setEndCap(cap: Cap) {
        endCap = cap.toStrokeCap()
    }

    /** The shape of the corners as a [JointType] constant, like `getJointType()` of the Maps SDK. */
    fun getJointType(): Int = strokeJoin.toJointType()

    /** Sets the shape of the corners from a [JointType] constant, like `setJointType(int)` of the Maps SDK. */
    fun setJointType(jointType: Int) {
        strokeJoin = jointTypeToStrokeJoin(jointType)
    }

    /** Sets [strokePattern] from an android.graphics.PathEffect, e.g. a DashPathEffect from Java. */
    fun setStrokePattern(pattern: android.graphics.PathEffect) {
        strokePattern = pattern.toComposePathEffect()
    }

    /** The stroke width in pixels, like `getWidth()` of the Maps SDK. */
    fun getWidth(): Float = strokeWidth

    /** Sets the stroke width in pixels, like `setWidth(float)` of the Maps SDK. */
    fun setWidth(width: Float) {
        strokeWidth = width
    }

    /** Removes the polyline from the map that shows it. */
    fun remove() {
        mapView?.removePolyline(this)
    }

    /** Returns a new polyline with the given properties changed, not added to any map. */
    fun copy(
        points: List<LatLng> = this.points,
        strokeColor: Color = this.strokeColor,
        strokeWidth: Float = this.strokeWidth,
        strokePattern: PathEffect? = this.strokePattern,
        pattern: List<PatternItem>? = this.pattern,
        startCap: StrokeCap = this.startCap,
        endCap: StrokeCap = this.endCap,
        strokeJoin: StrokeJoin = this.strokeJoin,
        geodesic: Boolean = this.geodesic,
        spans: List<StyleSpan> = this.spans,
        visible: Boolean = this.visible,
        clickable: Boolean = this.clickable,
        zIndex: Float = this.zIndex,
        tag: Any? = this.tag,
    ): Polyline =
        Polyline(
            points,
            strokeColor,
            strokeWidth,
            strokePattern,
            pattern,
            startCap,
            endCap,
            strokeJoin,
            geodesic,
            spans,
            visible,
            clickable,
            zIndex,
            tag,
        )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Polyline) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
