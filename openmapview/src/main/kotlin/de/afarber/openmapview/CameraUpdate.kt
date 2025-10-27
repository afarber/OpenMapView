/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Defines a camera update to be applied to the map.
 *
 * Camera updates are created using [CameraUpdateFactory] and applied via
 * [OpenMapView.moveCamera] or [OpenMapView.animateCamera].
 *
 * This is a sealed hierarchy with internal implementations to prevent external subclassing.
 */
sealed class CameraUpdate {
    /**
     * Updates the camera target to a new location, preserving current zoom.
     */
    internal data class NewLatLng(
        val target: LatLng,
    ) : CameraUpdate()

    /**
     * Updates the camera target and zoom level.
     */
    internal data class NewLatLngZoom(
        val target: LatLng,
        val zoom: Double,
    ) : CameraUpdate()

    /**
     * Updates the camera to a new position.
     */
    internal data class NewCameraPosition(
        val position: CameraPosition,
    ) : CameraUpdate()

    /**
     * Increases zoom level by 1.
     */
    internal data object ZoomIn : CameraUpdate()

    /**
     * Decreases zoom level by 1.
     */
    internal data object ZoomOut : CameraUpdate()

    /**
     * Sets zoom to a specific level.
     */
    internal data class ZoomTo(
        val zoom: Double,
    ) : CameraUpdate()

    /**
     * Adjusts zoom by a relative amount (positive to zoom in, negative to zoom out).
     */
    internal data class ZoomBy(
        val amount: Double,
    ) : CameraUpdate()
}
