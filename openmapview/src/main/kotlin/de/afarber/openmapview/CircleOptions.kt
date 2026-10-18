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
import androidx.compose.ui.graphics.toComposePathEffect

/**
 * Builder class for creating Circle instances with a fluent API.
 * A builder for circle configuration.
 *
 * Example usage:
 * ```
 * val circle = CircleOptions()
 *     .center(LatLng(40.7128, -74.0060))
 *     .radius(1000f)
 *     .strokeColor(Color.Red)
 *     .fillColor(Color(red = 255, green = 0, blue = 0, alpha = 128))
 *     .clickable(true)
 * ```
 */
class CircleOptions {
    private var center: LatLng? = null
    private var radius: Float? = null
    private var strokeColor: Color = Color.Black
    private var strokeWidth: Float = 10f
    private var strokePattern: PathEffect? = null
    private var pattern: List<PatternItem>? = null
    private var strokeCap: StrokeCap = StrokeCap.Round
    private var strokeJoin: StrokeJoin = StrokeJoin.Round
    private var fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128)
    private var visible: Boolean = true
    private var clickable: Boolean = false
    private var zIndex: Float = 0f
    private var tag: Any? = null

    /**
     * Sets the center point of the circle.
     * @param center Geographic coordinate of the circle center
     */
    fun center(center: LatLng): CircleOptions {
        this.center = center
        return this
    }

    /**
     * Sets the radius of the circle in meters.
     * @param radius Radius in meters (must be > 0)
     */
    fun radius(radius: Float): CircleOptions {
        this.radius = radius
        return this
    }

    /**
     * Sets the stroke color of the circle outline.
     * @param color Color value (e.g., Color.Red)
     */
    fun strokeColor(color: Color): CircleOptions {
        this.strokeColor = color
        return this
    }

    /**
     * Sets the stroke color of the circle outline as an ARGB color int.
     * Use this overload from Java, e.g. with android.graphics.Color.RED or 0x800000FF.
     * @param color ARGB color int
     */
    fun strokeColor(color: Int): CircleOptions = strokeColor(Color(color))

    /**
     * Sets the stroke width of the circle outline in pixels.
     * @param width Width in pixels (default: 10f)
     */
    fun strokeWidth(width: Float): CircleOptions {
        this.strokeWidth = width
        return this
    }

    /**
     * Sets the stroke pattern of the circle outline (dashed, dotted, etc.).
     * @param pattern PathEffect defining the pattern, or null for solid line
     */
    fun strokePattern(pattern: PathEffect?): CircleOptions {
        this.strokePattern = pattern
        return this
    }

    /**
     * Sets the stroke pattern from an android.graphics.PathEffect, e.g. a DashPathEffect from Java.
     * @param pattern PathEffect defining the pattern
     */
    fun strokePattern(pattern: android.graphics.PathEffect): CircleOptions = strokePattern(pattern.toComposePathEffect())

    /**
     * Sets the stroke pattern as dots, dashes and gaps, like `strokePattern(List)` of the Maps SDK.
     * @param pattern The repeating pattern
     */
    fun strokePattern(pattern: List<PatternItem>): CircleOptions = pattern(pattern)

    /**
     * Sets the stroke pattern as dots, dashes and gaps, e.g. `listOf(Dot(), Gap(20f))`.
     * It is used instead of a [strokePattern] if not null.
     * @param pattern The repeating pattern, or null for none
     */
    fun pattern(pattern: List<PatternItem>?): CircleOptions {
        this.pattern = pattern
        return this
    }

    /**
     * Sets the stroke cap style for line endpoints.
     * @param cap StrokeCap style (Butt, Round, or Square)
     */
    fun strokeCap(cap: StrokeCap): CircleOptions {
        this.strokeCap = cap
        return this
    }

    /**
     * Sets the stroke join style for line corners.
     * @param join StrokeJoin style (Miter, Round, or Bevel)
     */
    fun strokeJoin(join: StrokeJoin): CircleOptions {
        this.strokeJoin = join
        return this
    }

    /**
     * Sets the shape of line ends, e.g. `strokeCap(new RoundCap())` from Java.
     * @param cap ButtCap, RoundCap or SquareCap
     */
    fun strokeCap(cap: Cap): CircleOptions = strokeCap(cap.toStrokeCap())

    /**
     * Sets the shape of the corners from a [JointType] constant, e.g. `strokeJointType(JointType.ROUND)`.
     * @param jointType JointType.DEFAULT, BEVEL or ROUND
     */
    fun strokeJointType(jointType: Int): CircleOptions = strokeJoin(jointTypeToStrokeJoin(jointType))

    /**
     * Sets the fill color of the circle interior.
     * @param color Color value (e.g., Color(red = 255, green = 0, blue = 0, alpha = 128))
     */
    fun fillColor(color: Color): CircleOptions {
        this.fillColor = color
        return this
    }

    /**
     * Sets the fill color of the circle interior as an ARGB color int.
     * Use this overload from Java, e.g. with android.graphics.Color.RED or 0x800000FF.
     * @param color ARGB color int
     */
    fun fillColor(color: Int): CircleOptions = fillColor(Color(color))

    /**
     * Sets whether the circle is visible.
     * @param visible true to show the circle, false to hide it
     */
    fun visible(visible: Boolean): CircleOptions {
        this.visible = visible
        return this
    }

    /**
     * Sets whether the circle is clickable.
     * @param clickable true to enable click detection, false otherwise
     */
    fun clickable(clickable: Boolean): CircleOptions {
        this.clickable = clickable
        return this
    }

    /**
     * Sets the z-index for draw order.
     * @param zIndex Higher values are drawn on top (default: 0f)
     */
    fun zIndex(zIndex: Float): CircleOptions {
        this.zIndex = zIndex
        return this
    }

    /**
     * Sets optional user data associated with the circle.
     * @param tag Any object to attach to the circle
     */
    fun tag(tag: Any?): CircleOptions {
        this.tag = tag
        return this
    }

    /**
     * Builds and returns a Circle instance.
     * @throws IllegalArgumentException if center or radius is not set
     */
    internal fun build(): Circle {
        val centerValue = center ?: throw IllegalArgumentException("Circle center must be set")
        val radiusValue = radius ?: throw IllegalArgumentException("Circle radius must be set")

        return Circle(
            center = centerValue,
            radius = radiusValue,
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            strokePattern = strokePattern,
            pattern = pattern,
            strokeCap = strokeCap,
            strokeJoin = strokeJoin,
            fillColor = fillColor,
            visible = visible,
            clickable = clickable,
            zIndex = zIndex,
            tag = tag,
        )
    }
}
