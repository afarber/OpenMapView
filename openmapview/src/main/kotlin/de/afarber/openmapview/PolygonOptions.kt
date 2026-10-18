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
 * Builder class for creating Polygon instances with a fluent API.
 * A builder for polygon configuration.
 *
 * Example usage:
 * ```
 * val polygon = PolygonOptions()
 *     .add(LatLng(51.5, 0.0))
 *     .add(LatLng(51.6, 0.0))
 *     .add(LatLng(51.6, 0.1))
 *     .strokeColor(Color.Red)
 *     .fillColor(Color(red = 255, green = 0, blue = 0, alpha = 128))
 *     .clickable(true)
 * ```
 */
class PolygonOptions {
    private val points = mutableListOf<LatLng>()
    private val holes = mutableListOf<List<LatLng>>()
    private var strokeColor: Color = Color.Black
    private var strokeWidth: Float = 10f
    private var strokePattern: PathEffect? = null
    private var pattern: List<PatternItem>? = null
    private var strokeCap: StrokeCap = StrokeCap.Butt
    private var strokeJoin: StrokeJoin = StrokeJoin.Round
    private var fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128)
    private var geodesic: Boolean = false
    private var visible: Boolean = true
    private var clickable: Boolean = false
    private var zIndex: Float = 0f
    private var tag: Any? = null

    /**
     * Adds a vertex to the outline of the polygon.
     * @param point Geographic coordinate to add
     */
    fun add(point: LatLng): PolygonOptions {
        points.add(point)
        return this
    }

    /**
     * Adds several vertices in order, e.g. `add(a, b, c)` from Java.
     * @param points Geographic coordinates to add
     */
    fun add(vararg points: LatLng): PolygonOptions {
        this.points.addAll(points)
        return this
    }

    /**
     * Adds vertices to the outline of the polygon.
     * @param points Geographic coordinates to add
     */
    fun addAll(points: Iterable<LatLng>): PolygonOptions {
        this.points.addAll(points)
        return this
    }

    /**
     * Adds a hole to the polygon.
     * @param hole List of coordinates defining the hole (must have at least 3 points)
     */
    fun addHole(hole: Iterable<LatLng>): PolygonOptions {
        holes.add(hole.toList())
        return this
    }

    /**
     * Sets the stroke color of the polygon outline.
     * @param color Color value (e.g., Color.Red)
     */
    fun strokeColor(color: Color): PolygonOptions {
        this.strokeColor = color
        return this
    }

    /**
     * Sets the stroke color of the polygon outline as an ARGB color int.
     * Use this overload from Java, e.g. with android.graphics.Color.RED or 0x800000FF.
     * @param color ARGB color int
     */
    fun strokeColor(color: Int): PolygonOptions = strokeColor(Color(color))

    /**
     * Sets the stroke width of the polygon outline in pixels.
     * @param width Width in pixels (default: 10f)
     */
    fun strokeWidth(width: Float): PolygonOptions {
        this.strokeWidth = width
        return this
    }

    /**
     * Sets the stroke pattern of the polygon outline (dashed, dotted, etc.).
     * @param pattern PathEffect defining the pattern, or null for solid line
     */
    fun strokePattern(pattern: PathEffect?): PolygonOptions {
        this.strokePattern = pattern
        return this
    }

    /**
     * Sets the stroke pattern from an android.graphics.PathEffect, e.g. a DashPathEffect from Java.
     * @param pattern PathEffect defining the pattern
     */
    fun strokePattern(pattern: android.graphics.PathEffect): PolygonOptions = strokePattern(pattern.toComposePathEffect())

    /**
     * Sets the stroke pattern as dots, dashes and gaps, like `strokePattern(List)` of the Maps SDK.
     * @param pattern The repeating pattern
     */
    fun strokePattern(pattern: List<PatternItem>): PolygonOptions = pattern(pattern)

    /**
     * Sets the stroke pattern as dots, dashes and gaps, e.g. `listOf(Dot(), Gap(20f))`.
     * It is used instead of a [strokePattern] if not null.
     * @param pattern The repeating pattern, or null for none
     */
    fun pattern(pattern: List<PatternItem>?): PolygonOptions {
        this.pattern = pattern
        return this
    }

    /**
     * Sets the stroke cap style for line endpoints.
     * @param cap StrokeCap style (Butt, Round, or Square)
     */
    fun strokeCap(cap: StrokeCap): PolygonOptions {
        this.strokeCap = cap
        return this
    }

    /**
     * Sets the stroke join style for line corners.
     * @param join StrokeJoin style (Miter, Round, or Bevel)
     */
    fun strokeJoin(join: StrokeJoin): PolygonOptions {
        this.strokeJoin = join
        return this
    }

    /**
     * Sets the shape of line ends, e.g. `strokeCap(new RoundCap())` from Java.
     * @param cap ButtCap, RoundCap or SquareCap
     */
    fun strokeCap(cap: Cap): PolygonOptions = strokeCap(cap.toStrokeCap())

    /**
     * Sets the shape of the corners from a [JointType] constant, e.g. `strokeJointType(JointType.ROUND)`.
     * @param jointType JointType.DEFAULT, BEVEL or ROUND
     */
    fun strokeJointType(jointType: Int): PolygonOptions = strokeJoin(jointTypeToStrokeJoin(jointType))

    /**
     * Sets the fill color of the polygon interior.
     * @param color Color value (e.g., Color(red = 255, green = 0, blue = 0, alpha = 128))
     */
    fun fillColor(color: Color): PolygonOptions {
        this.fillColor = color
        return this
    }

    /**
     * Sets the fill color of the polygon interior as an ARGB color int.
     * Use this overload from Java, e.g. with android.graphics.Color.RED or 0x800000FF.
     * @param color ARGB color int
     */
    fun fillColor(color: Int): PolygonOptions = fillColor(Color(color))

    /**
     * Sets whether segments are drawn as geodesics (great-circle paths).
     * @param geodesic true for geodesic paths, false for straight Mercator lines
     */
    fun geodesic(geodesic: Boolean): PolygonOptions {
        this.geodesic = geodesic
        return this
    }

    /**
     * Sets whether the polygon is visible.
     * @param visible true to show the polygon, false to hide it
     */
    fun visible(visible: Boolean): PolygonOptions {
        this.visible = visible
        return this
    }

    /**
     * Sets whether the polygon is clickable.
     * @param clickable true to enable click detection, false otherwise
     */
    fun clickable(clickable: Boolean): PolygonOptions {
        this.clickable = clickable
        return this
    }

    /**
     * Sets the z-index for draw order.
     * @param zIndex Higher values are drawn on top (default: 0f)
     */
    fun zIndex(zIndex: Float): PolygonOptions {
        this.zIndex = zIndex
        return this
    }

    /**
     * Sets optional user data associated with the polygon.
     * @param tag Any object to attach to the polygon
     */
    fun tag(tag: Any?): PolygonOptions {
        this.tag = tag
        return this
    }

    /**
     * Builds and returns a Polygon instance.
     * @throws IllegalArgumentException if fewer than 3 points are provided
     */
    internal fun build(): Polygon =
        Polygon(
            points = points.toList(),
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            strokePattern = strokePattern,
            pattern = pattern,
            strokeCap = strokeCap,
            strokeJoin = strokeJoin,
            fillColor = fillColor,
            holes = holes.toList(),
            geodesic = geodesic,
            visible = visible,
            clickable = clickable,
            zIndex = zIndex,
            tag = tag,
        )
}
