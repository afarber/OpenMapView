/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Color

/**
 * Builder class for creating Circle instances with a fluent API.
 * Matches the Google Maps API pattern for circle configuration.
 *
 * Example usage:
 * ```
 * val circle = CircleOptions()
 *     .center(LatLng(40.7128, -74.0060))
 *     .radius(1000f)
 *     .strokeColor(Color.RED)
 *     .fillColor(Color.argb(128, 255, 0, 0))
 *     .clickable(true)
 * ```
 */
class CircleOptions {
    private var center: LatLng? = null
    private var radius: Float? = null
    private var strokeColor: Int = Color.BLACK
    private var strokeWidth: Float = 10f
    private var fillColor: Int = Color.argb(128, 128, 128, 128)
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
     * @param color Color value (e.g., Color.RED)
     */
    fun strokeColor(color: Int): CircleOptions {
        this.strokeColor = color
        return this
    }

    /**
     * Sets the stroke width of the circle outline in pixels.
     * @param width Width in pixels (default: 10f)
     */
    fun strokeWidth(width: Float): CircleOptions {
        this.strokeWidth = width
        return this
    }

    /**
     * Sets the fill color of the circle interior.
     * @param color Color value (e.g., Color.argb(128, 255, 0, 0))
     */
    fun fillColor(color: Int): CircleOptions {
        this.fillColor = color
        return this
    }

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
            fillColor = fillColor,
            visible = visible,
            clickable = clickable,
            zIndex = zIndex,
            tag = tag,
        )
    }
}
