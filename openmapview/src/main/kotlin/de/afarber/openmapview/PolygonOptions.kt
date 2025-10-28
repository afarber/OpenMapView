/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Color

/**
 * Builder class for creating Polygon instances with a fluent API.
 * Matches the Google Maps API pattern for polygon configuration.
 *
 * Example usage:
 * ```
 * val polygon = PolygonOptions()
 *     .add(LatLng(51.5, 0.0))
 *     .add(LatLng(51.6, 0.0))
 *     .add(LatLng(51.6, 0.1))
 *     .strokeColor(Color.RED)
 *     .fillColor(Color.argb(128, 255, 0, 0))
 *     .clickable(true)
 * ```
 */
class PolygonOptions {
    private val points = mutableListOf<LatLng>()
    private val holes = mutableListOf<List<LatLng>>()
    private var strokeColor: Int = Color.BLACK
    private var strokeWidth: Float = 10f
    private var fillColor: Int = Color.argb(128, 128, 128, 128)
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
     * @param color Color value (e.g., Color.RED)
     */
    fun strokeColor(color: Int): PolygonOptions {
        this.strokeColor = color
        return this
    }

    /**
     * Sets the stroke width of the polygon outline in pixels.
     * @param width Width in pixels (default: 10f)
     */
    fun strokeWidth(width: Float): PolygonOptions {
        this.strokeWidth = width
        return this
    }

    /**
     * Sets the fill color of the polygon interior.
     * @param color Color value (e.g., Color.argb(128, 255, 0, 0))
     */
    fun fillColor(color: Int): PolygonOptions {
        this.fillColor = color
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
            fillColor = fillColor,
            holes = holes.toList(),
            visible = visible,
            clickable = clickable,
            zIndex = zIndex,
            tag = tag,
        )
}
