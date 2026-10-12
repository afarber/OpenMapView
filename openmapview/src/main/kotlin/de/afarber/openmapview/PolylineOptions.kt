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
 * Builder class for creating Polyline instances with a fluent API.
 * Matches the Google Maps API pattern for polyline configuration.
 *
 * Example usage:
 * ```
 * val polyline = PolylineOptions()
 *     .add(LatLng(51.5, 0.0))
 *     .add(LatLng(51.6, 0.1))
 *     .color(Color.Blue)
 *     .width(5f)
 *     .strokePattern(PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f))
 *     .clickable(true)
 * ```
 */
class PolylineOptions {
    private val points = mutableListOf<LatLng>()
    private var strokeColor: Color = Color.Black
    private var strokeWidth: Float = 10f
    private var strokePattern: PathEffect? = null
    private var startCap: StrokeCap = StrokeCap.Butt
    private var endCap: StrokeCap = StrokeCap.Butt
    private var strokeJoin: StrokeJoin = StrokeJoin.Round
    private var geodesic: Boolean = false
    private var spans: List<StyleSpan> = emptyList()
    private var visible: Boolean = true
    private var clickable: Boolean = false
    private var zIndex: Float = 0f
    private var tag: Any? = null

    /**
     * Adds a vertex to the end of the polyline.
     * @param point Geographic coordinate to add
     */
    fun add(point: LatLng): PolylineOptions {
        points.add(point)
        return this
    }

    /**
     * Adds vertices to the end of the polyline.
     * @param points Geographic coordinates to add
     */
    fun addAll(points: Iterable<LatLng>): PolylineOptions {
        this.points.addAll(points)
        return this
    }

    /**
     * Sets the stroke color of the polyline.
     * @param color Color value (e.g., Color.Red)
     */
    fun color(color: Color): PolylineOptions {
        this.strokeColor = color
        return this
    }

    /**
     * Sets the stroke width of the polyline in pixels.
     * @param width Width in pixels (default: 10f)
     */
    fun width(width: Float): PolylineOptions {
        this.strokeWidth = width
        return this
    }

    /**
     * Sets the stroke pattern of the polyline (dashed, dotted, etc.).
     * @param pattern PathEffect defining the pattern, or null for solid line
     */
    fun strokePattern(pattern: PathEffect?): PolylineOptions {
        this.strokePattern = pattern
        return this
    }

    /**
     * Sets the stroke cap style for the start of the polyline.
     * @param cap StrokeCap style (Butt, Round, or Square)
     */
    fun startCap(cap: StrokeCap): PolylineOptions {
        this.startCap = cap
        return this
    }

    /**
     * Sets the stroke cap style for the end of the polyline.
     * @param cap StrokeCap style (Butt, Round, or Square)
     */
    fun endCap(cap: StrokeCap): PolylineOptions {
        this.endCap = cap
        return this
    }

    /**
     * Sets whether segments are drawn as geodesics (great-circle paths).
     * @param geodesic true for geodesic paths, false for straight Mercator lines
     */
    fun geodesic(geodesic: Boolean): PolylineOptions {
        this.geodesic = geodesic
        return this
    }

    /**
     * Sets style spans for multi-colored polylines.
     * @param spans List of StyleSpan defining colors and segment counts
     */
    fun spans(spans: List<StyleSpan>): PolylineOptions {
        this.spans = spans
        return this
    }

    /**
     * Sets the stroke join style for line corners.
     * @param join StrokeJoin style (Miter, Round, or Bevel)
     */
    fun strokeJoin(join: StrokeJoin): PolylineOptions {
        this.strokeJoin = join
        return this
    }

    /**
     * Sets whether the polyline is visible.
     * @param visible true to show the polyline, false to hide it
     */
    fun visible(visible: Boolean): PolylineOptions {
        this.visible = visible
        return this
    }

    /**
     * Sets whether the polyline is clickable.
     * @param clickable true to enable click detection, false otherwise
     */
    fun clickable(clickable: Boolean): PolylineOptions {
        this.clickable = clickable
        return this
    }

    /**
     * Sets the z-index for draw order.
     * @param zIndex Higher values are drawn on top (default: 0f)
     */
    fun zIndex(zIndex: Float): PolylineOptions {
        this.zIndex = zIndex
        return this
    }

    /**
     * Sets optional user data associated with the polyline.
     * @param tag Any object to attach to the polyline
     */
    fun tag(tag: Any?): PolylineOptions {
        this.tag = tag
        return this
    }

    /**
     * Builds and returns a Polyline instance.
     * @throws IllegalArgumentException if fewer than 2 points are provided
     */
    internal fun build(): Polyline =
        Polyline(
            points = points.toList(),
            strokeColor = strokeColor,
            strokeWidth = strokeWidth,
            strokePattern = strokePattern,
            startCap = startCap,
            endCap = endCap,
            strokeJoin = strokeJoin,
            geodesic = geodesic,
            spans = spans,
            visible = visible,
            clickable = clickable,
            zIndex = zIndex,
            tag = tag,
        )
}
