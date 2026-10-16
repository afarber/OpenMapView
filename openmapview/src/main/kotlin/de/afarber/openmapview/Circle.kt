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

/**
 * A circle on the map with a center and a radius in meters.
 *
 * The circle is a live handle, as returned by [OpenMapView.addCircle]: setting a property, e.g.
 * `circle.strokeWidth = 16f`, redraws the map right away, and [remove] takes it off the map.
 * Java code uses the getters and setters, e.g. `circle.setRadius(500f)`.
 *
 * @param center the geographic coordinate of the center
 * @param radius the radius in meters, greater than 0
 * @param strokeColor the outline color
 * @param strokeWidth the outline width in pixels
 * @param strokePattern the stroke pattern, e.g. dashed; null for a solid line
 * @param strokeCap the shape of line ends
 * @param strokeJoin the shape of the corners
 * @param fillColor the color of the interior
 * @param visible whether the circle is visible
 * @param clickable whether a tap on the circle is reported to its click listener
 * @param zIndex draw order: circles with a higher zIndex are drawn on top
 * @param tag optional user data
 */
class Circle(
    center: LatLng,
    radius: Float,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 10f,
    strokePattern: PathEffect? = null,
    strokeCap: StrokeCap = StrokeCap.Round,
    strokeJoin: StrokeJoin = StrokeJoin.Round,
    fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128),
    visible: Boolean = true,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
) {
    // The map that shows the circle, redrawn when a property changes
    internal var mapView: OpenMapView? = null

    private fun <T> property(
        initial: T,
        check: (T) -> Unit = {},
    ) = ShapeProperty(initial, check) { mapView?.invalidate() }

    /** The geographic coordinate of the center */
    var center: LatLng by property(center)

    /** The radius in meters, greater than 0 */
    var radius: Float by property(radius) { require(it > 0) { "Circle radius must be greater than 0" } }

    /** The outline color */
    var strokeColor: Color by property(strokeColor)

    /** The outline width in pixels */
    var strokeWidth: Float by property(strokeWidth)

    /** The stroke pattern, e.g. dashed; null for a solid line */
    var strokePattern: PathEffect? by property(strokePattern)

    /** The shape of line ends */
    var strokeCap: StrokeCap by property(strokeCap)

    /** The shape of the corners */
    var strokeJoin: StrokeJoin by property(strokeJoin)

    /** The color of the interior */
    var fillColor: Color by property(fillColor)

    /** Whether the circle is visible */
    var visible: Boolean by property(visible)

    /** Whether a tap on the circle is reported to its click listener */
    var clickable: Boolean by property(clickable)

    /** Draw order: circles with a higher zIndex are drawn on top */
    var zIndex: Float by property(zIndex)

    /** Optional user data */
    var tag: Any? by property(tag)

    /**
     * Unique identifier for this circle instance.
     * Used internally for management and callbacks.
     */
    internal val id: String = "circle_${System.nanoTime()}_${System.identityHashCode(this)}"

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

    /** Removes the circle from the map that shows it. */
    fun remove() {
        mapView?.removeCircle(this)
    }

    /** Returns a new circle with the given properties changed, not added to any map. */
    fun copy(
        center: LatLng = this.center,
        radius: Float = this.radius,
        strokeColor: Color = this.strokeColor,
        strokeWidth: Float = this.strokeWidth,
        strokePattern: PathEffect? = this.strokePattern,
        strokeCap: StrokeCap = this.strokeCap,
        strokeJoin: StrokeJoin = this.strokeJoin,
        fillColor: Color = this.fillColor,
        visible: Boolean = this.visible,
        clickable: Boolean = this.clickable,
        zIndex: Float = this.zIndex,
        tag: Any? = this.tag,
    ): Circle =
        Circle(center, radius, strokeColor, strokeWidth, strokePattern, strokeCap, strokeJoin, fillColor, visible, clickable, zIndex, tag)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Circle) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
