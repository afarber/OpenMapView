/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Builder class for creating Marker instances with a fluent API.
 * A builder for marker configuration.
 *
 * Example usage:
 * ```
 * val marker = MarkerOptions()
 *     .position(LatLng(51.4818, 0.0))
 *     .title("Greenwich Observatory")
 *     .snippet("Prime Meridian")
 *     .draggable(true)
 *     .alpha(0.8f)
 * ```
 */
class MarkerOptions {
    private var position: LatLng? = null
    private var title: String? = null
    private var snippet: String? = null
    private var icon: BitmapDescriptor? = null
    private var anchor: Pair<Float, Float> = Pair(0.5f, 1.0f)
    private var infoWindowAnchor: Pair<Float, Float> = Pair(0.5f, 0.0f)
    private var rotation: Float = 0f
    private var flat: Boolean = false
    private var visible: Boolean = true
    private var alpha: Float = 1.0f
    private var draggable: Boolean = false
    private var zIndex: Float = 0f
    private var tag: Any? = null

    /**
     * Sets the position of the marker.
     * @param position Geographic location of the marker
     */
    fun position(position: LatLng): MarkerOptions {
        this.position = position
        return this
    }

    /**
     * Sets the title text for the marker's info window.
     * @param title Title text (optional)
     */
    fun title(title: String?): MarkerOptions {
        this.title = title
        return this
    }

    /**
     * Sets the snippet text for the marker's info window.
     * @param snippet Snippet text (optional)
     */
    fun snippet(snippet: String?): MarkerOptions {
        this.snippet = snippet
        return this
    }

    /**
     * Sets a custom icon for the marker.
     * @param icon BitmapDescriptor for the icon (null for default red marker)
     */
    fun icon(icon: BitmapDescriptor?): MarkerOptions {
        this.icon = icon
        return this
    }

    /**
     * Sets the anchor point for the marker icon.
     * @param anchorU Horizontal anchor (0.0 = left edge, 0.5 = center, 1.0 = right edge)
     * @param anchorV Vertical anchor (0.0 = top edge, 0.5 = center, 1.0 = bottom edge)
     */
    fun anchor(
        anchorU: Float,
        anchorV: Float,
    ): MarkerOptions {
        this.anchor = Pair(anchorU, anchorV)
        return this
    }

    /**
     * Sets the point of the icon the info window points at.
     * @param anchorU Horizontal position (0.0 = left edge, 0.5 = center, 1.0 = right edge)
     * @param anchorV Vertical position (0.0 = top edge, 0.5 = center, 1.0 = bottom edge), default 0.0
     */
    fun infoWindowAnchor(
        anchorU: Float,
        anchorV: Float,
    ): MarkerOptions {
        this.infoWindowAnchor = Pair(anchorU, anchorV)
        return this
    }

    /**
     * Sets the clockwise rotation of the icon around its anchor, e.g. a vehicle's heading.
     * @param rotation Rotation in degrees (default: 0f)
     */
    fun rotation(rotation: Float): MarkerOptions {
        this.rotation = rotation
        return this
    }

    /**
     * Sets whether the icon lies flat on the map instead of facing the screen.
     * It has no effect yet, as the map doesn't rotate or tilt.
     * @param flat true for a flat icon (default: false)
     */
    fun flat(flat: Boolean): MarkerOptions {
        this.flat = flat
        return this
    }

    /**
     * Sets whether the marker is visible.
     * @param visible true to show the marker, false to hide it
     */
    fun visible(visible: Boolean): MarkerOptions {
        this.visible = visible
        return this
    }

    /**
     * Sets the opacity of the marker.
     * @param alpha Opacity value between 0.0 (transparent) and 1.0 (opaque)
     */
    fun alpha(alpha: Float): MarkerOptions {
        this.alpha = alpha
        return this
    }

    /**
     * Sets whether the marker can be dragged.
     * @param draggable true to enable dragging, false otherwise
     */
    fun draggable(draggable: Boolean): MarkerOptions {
        this.draggable = draggable
        return this
    }

    /**
     * Sets the z-index for draw order.
     * @param zIndex Higher values are drawn on top (default: 0f)
     */
    fun zIndex(zIndex: Float): MarkerOptions {
        this.zIndex = zIndex
        return this
    }

    /**
     * Sets optional user data associated with the marker.
     * @param tag Any object to attach to the marker
     */
    fun tag(tag: Any?): MarkerOptions {
        this.tag = tag
        return this
    }

    /**
     * Builds and returns a Marker instance.
     * @throws IllegalArgumentException if position is not set
     */
    internal fun build(): Marker {
        val positionValue = position ?: throw IllegalArgumentException("Marker position must be set")

        return Marker(
            position = positionValue,
            title = title,
            snippet = snippet,
            icon = icon,
            anchor = anchor,
            infoWindowAnchor = infoWindowAnchor,
            rotation = rotation,
            isFlat = flat,
            visible = visible,
            alpha = alpha,
            draggable = draggable,
            zIndex = zIndex,
            tag = tag,
        )
    }
}
