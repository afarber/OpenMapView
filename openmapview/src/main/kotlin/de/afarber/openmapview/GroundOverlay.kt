/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * An image placed on the map, e.g. a historic map or a floor plan, that scales with the map.
 *
 * It is placed either at a [position] with a [width] (and optional [height]) in meters, or
 * stretched over [bounds].
 *
 * The overlay is a live handle, as returned by [OpenMapView.addGroundOverlay]: setting a
 * property, e.g. `overlay.bearing = 45f`, redraws the map right away, [setPosition],
 * [setDimensions] and [setPositionFromBounds] move it, and [remove] takes it off the map.
 * Java code uses the getters and setters, e.g. `overlay.setTransparency(0.5f)`.
 *
 * @param image The image to draw
 * @param position The location of the [anchor], used with [width]; null when placed by [bounds]
 * @param width The width in meters, used with [position]
 * @param height The height in meters; null keeps the image's aspect ratio
 * @param bounds The area the image is stretched over; null when placed by [position]
 * @param anchor The point of the image at [position], from (0, 0) top left to (1, 1) bottom right
 * @param bearing The clockwise rotation in degrees around the anchor
 * @param transparency From 0 (opaque) to 1 (invisible)
 * @param visible Whether the overlay is drawn
 * @param zIndex Draw order: overlays with a higher zIndex are drawn on top
 * @param clickable Whether a tap on the overlay is reported to its click listener
 * @param tag Optional user data
 */
class GroundOverlay(
    image: BitmapDescriptor,
    position: LatLng? = null,
    width: Float? = null,
    height: Float? = null,
    bounds: LatLngBounds? = null,
    /** The point of the image at [position], from (0, 0) top left to (1, 1) bottom right */
    val anchor: Pair<Float, Float> = Pair(0.5f, 0.5f),
    bearing: Float = 0f,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    clickable: Boolean = false,
    tag: Any? = null,
) {
    // The map that shows the overlay, redrawn when a property changes
    internal var mapView: OpenMapView? = null

    private fun <T> property(
        initial: T,
        check: (T) -> Unit = {},
    ) = ShapeProperty(initial, check) { mapView?.invalidate() }

    /** The image to draw */
    var image: BitmapDescriptor by property(image)

    /** The clockwise rotation in degrees around the anchor */
    var bearing: Float by property(bearing)

    /** From 0 (opaque) to 1 (invisible) */
    var transparency: Float by property(transparency) {
        require(it in 0f..1f) { "Transparency must be between 0.0 and 1.0" }
    }

    /** Whether the overlay is drawn */
    var visible: Boolean by property(visible)

    /** Draw order: overlays with a higher zIndex are drawn on top */
    var zIndex: Float by property(zIndex)

    /** Whether a tap on the overlay is reported to its click listener */
    var clickable: Boolean by property(clickable)

    /** Optional user data */
    var tag: Any? by property(tag)

    /** The location of the [anchor], or null when the overlay is placed by [bounds] */
    var position: LatLng? = position
        private set

    /** The width in meters, or null when the overlay is placed by [bounds] */
    var width: Float? = width
        private set

    /** The height in meters, or null to keep the image's aspect ratio */
    var height: Float? = height
        private set

    /** The area the image is stretched over, or null when it is placed by [position] */
    var bounds: LatLngBounds? = bounds
        private set

    internal val id: String = "ground_overlay_${System.nanoTime()}_${System.identityHashCode(this)}"

    init {
        require(anchor.first in 0f..1f && anchor.second in 0f..1f) {
            "Anchor coordinates must be between 0.0 and 1.0"
        }
        checkPlacement(position, width, height, bounds)
    }

    /**
     * Moves the overlay, so its [anchor] is at [position]. An overlay placed by [bounds] keeps
     * the width of those bounds, measured at their center.
     */
    fun setPosition(position: LatLng) {
        val bounds = bounds
        if (bounds != null) {
            place(position, widthInMeters(bounds), heightInMeters(bounds), null)
        } else {
            place(position, width, height, null)
        }
    }

    /** Sets the width in meters and keeps the image's aspect ratio. */
    fun setDimensions(width: Float) = setDimensions(width, null)

    /**
     * Sets the size in meters, or keeps the image's aspect ratio for a null [height]. An
     * overlay placed by [bounds] is placed at their center instead.
     */
    fun setDimensions(
        width: Float,
        height: Float?,
    ) {
        place(position ?: bounds?.getCenter(), width, height, null)
    }

    /** Stretches the image over [bounds], replacing the position and size. */
    fun setPositionFromBounds(bounds: LatLngBounds) {
        place(null, null, null, bounds)
    }

    /** Removes the overlay from the map that shows it. */
    fun remove() {
        mapView?.removeGroundOverlay(this)
    }

    /** Returns a new overlay with the given properties changed, not added to any map. */
    fun copy(
        image: BitmapDescriptor = this.image,
        position: LatLng? = this.position,
        width: Float? = this.width,
        height: Float? = this.height,
        bounds: LatLngBounds? = this.bounds,
        anchor: Pair<Float, Float> = this.anchor,
        bearing: Float = this.bearing,
        transparency: Float = this.transparency,
        visible: Boolean = this.visible,
        zIndex: Float = this.zIndex,
        clickable: Boolean = this.clickable,
        tag: Any? = this.tag,
    ): GroundOverlay = GroundOverlay(image, position, width, height, bounds, anchor, bearing, transparency, visible, zIndex, clickable, tag)

    // Checks and applies a new placement, keeping the old one if it is invalid
    private fun place(
        position: LatLng?,
        width: Float?,
        height: Float?,
        bounds: LatLngBounds?,
    ) {
        checkPlacement(position, width, height, bounds)
        this.position = position
        this.width = width
        this.height = height
        this.bounds = bounds
        mapView?.invalidate()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GroundOverlay) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    private companion object {
        fun checkPlacement(
            position: LatLng?,
            width: Float?,
            height: Float?,
            bounds: LatLngBounds?,
        ) {
            val hasPosition = position != null && width != null
            val hasBounds = bounds != null
            require(hasPosition || hasBounds) {
                "Ground overlay must have either position with width or bounds"
            }
            require(!(hasPosition && hasBounds)) {
                "Ground overlay cannot have both position and bounds"
            }
            if (width != null) {
                require(width > 0) { "Width must be greater than 0" }
            }
            if (height != null) {
                require(height > 0) { "Height must be greater than 0" }
            }
        }

        fun widthInMeters(bounds: LatLngBounds): Float {
            val latitude = bounds.getCenter().latitude
            val west = LatLng(latitude, bounds.southwest.longitude)
            val east = LatLng(latitude, bounds.northeast.longitude)
            return GeodesicUtils.haversineDistance(west, east).toFloat()
        }

        fun heightInMeters(bounds: LatLngBounds): Float {
            val longitude = bounds.getCenter().longitude
            val south = LatLng(bounds.southwest.latitude, longitude)
            val north = LatLng(bounds.northeast.latitude, longitude)
            return GeodesicUtils.haversineDistance(south, north).toFloat()
        }
    }
}
