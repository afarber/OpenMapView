/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Factory for creating [CameraUpdate] objects to transform the camera on a map.
 *
 * Use these methods to create camera updates for [OpenMapView.moveCamera]
 * and [OpenMapView.animateCamera].
 */
object CameraUpdateFactory {
    /**
     * Returns a CameraUpdate that moves the camera to the specified location,
     * preserving the current zoom level.
     *
     * @param target The location to move the camera to
     * @return A CameraUpdate for the new location
     */
    fun newLatLng(target: LatLng): CameraUpdate = CameraUpdate.NewLatLng(target)

    /**
     * Returns a CameraUpdate that moves the camera to the specified location
     * and sets the zoom level.
     *
     * @param target The location to move the camera to
     * @param zoom The desired zoom level (2.0 to 19.0)
     * @return A CameraUpdate for the new location and zoom
     */
    fun newLatLngZoom(
        target: LatLng,
        zoom: Double,
    ): CameraUpdate = CameraUpdate.NewLatLngZoom(target, zoom)

    /**
     * Returns a CameraUpdate that moves the camera to the specified position.
     *
     * @param position The target camera position
     * @return A CameraUpdate for the new position
     */
    fun newCameraPosition(position: CameraPosition): CameraUpdate = CameraUpdate.NewCameraPosition(position)

    /**
     * Returns a CameraUpdate that increases the zoom level by 1.
     *
     * @return A CameraUpdate that zooms in
     */
    fun zoomIn(): CameraUpdate = CameraUpdate.ZoomIn

    /**
     * Returns a CameraUpdate that decreases the zoom level by 1.
     *
     * @return A CameraUpdate that zooms out
     */
    fun zoomOut(): CameraUpdate = CameraUpdate.ZoomOut

    /**
     * Returns a CameraUpdate that sets the zoom level to the specified value.
     *
     * @param zoom The target zoom level (2.0 to 19.0)
     * @return A CameraUpdate for the specified zoom level
     */
    fun zoomTo(zoom: Double): CameraUpdate = CameraUpdate.ZoomTo(zoom)

    /**
     * Returns a CameraUpdate that adjusts the zoom level by the specified amount.
     *
     * Positive values zoom in, negative values zoom out.
     *
     * @param amount The amount to adjust the zoom level (e.g., 1.5 or -2.0)
     * @return A CameraUpdate for the relative zoom adjustment
     */
    fun zoomBy(amount: Double): CameraUpdate = CameraUpdate.ZoomBy(amount)
}
