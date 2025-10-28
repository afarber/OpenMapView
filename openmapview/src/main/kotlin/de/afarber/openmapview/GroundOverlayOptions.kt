/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

class GroundOverlayOptions {
    private var image: BitmapDescriptor? = null
    private var position: LatLng? = null
    private var width: Float? = null
    private var height: Float? = null
    private var bounds: LatLngBounds? = null
    private var anchor: Pair<Float, Float> = Pair(0.5f, 0.5f)
    private var bearing: Float = 0f
    private var transparency: Float = 0f
    private var visible: Boolean = true
    private var zIndex: Float = 0f
    private var clickable: Boolean = false
    private var tag: Any? = null

    fun image(image: BitmapDescriptor): GroundOverlayOptions {
        this.image = image
        return this
    }

    fun position(
        position: LatLng,
        width: Float,
    ): GroundOverlayOptions {
        this.position = position
        this.width = width
        this.bounds = null
        return this
    }

    fun position(
        position: LatLng,
        width: Float,
        height: Float,
    ): GroundOverlayOptions {
        this.position = position
        this.width = width
        this.height = height
        this.bounds = null
        return this
    }

    fun positionFromBounds(bounds: LatLngBounds): GroundOverlayOptions {
        this.bounds = bounds
        this.position = null
        this.width = null
        this.height = null
        return this
    }

    fun anchor(
        u: Float,
        v: Float,
    ): GroundOverlayOptions {
        this.anchor = Pair(u, v)
        return this
    }

    fun bearing(bearing: Float): GroundOverlayOptions {
        this.bearing = bearing
        return this
    }

    fun transparency(transparency: Float): GroundOverlayOptions {
        this.transparency = transparency
        return this
    }

    fun visible(visible: Boolean): GroundOverlayOptions {
        this.visible = visible
        return this
    }

    fun zIndex(zIndex: Float): GroundOverlayOptions {
        this.zIndex = zIndex
        return this
    }

    fun clickable(clickable: Boolean): GroundOverlayOptions {
        this.clickable = clickable
        return this
    }

    fun tag(tag: Any?): GroundOverlayOptions {
        this.tag = tag
        return this
    }

    internal fun build(): GroundOverlay {
        val imageValue = image ?: throw IllegalArgumentException("Ground overlay image must be set")

        return GroundOverlay(
            image = imageValue,
            position = position,
            width = width,
            height = height,
            bounds = bounds,
            anchor = anchor,
            bearing = bearing,
            transparency = transparency,
            visible = visible,
            zIndex = zIndex,
            clickable = clickable,
            tag = tag,
        )
    }
}
