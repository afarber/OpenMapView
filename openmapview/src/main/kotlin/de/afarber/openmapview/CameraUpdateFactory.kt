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

    /**
     * Returns a CameraUpdate that scrolls the map by the specified pixel amounts.
     *
     * Positive xPixels moves the viewport right (map content moves left).
     * Positive yPixels moves the viewport down (map content moves up).
     * The current zoom level is preserved.
     *
     * @param xPixels The horizontal scroll amount in pixels
     * @param yPixels The vertical scroll amount in pixels
     * @return A CameraUpdate for the pixel scroll
     */
    fun scrollBy(
        xPixels: Float,
        yPixels: Float,
    ): CameraUpdate = CameraUpdate.ScrollBy(xPixels, yPixels)

    /**
     * Returns a CameraUpdate that moves the camera to show the entire bounds.
     *
     * The camera will be positioned at the center of the bounds, and the zoom level
     * will be calculated to fit the entire bounds within the viewport with the specified padding.
     *
     * @param bounds The geographic bounds to display
     * @param padding Padding in pixels to apply uniformly on all sides
     * @return A CameraUpdate to display the bounds
     */
    fun newLatLngBounds(
        bounds: LatLngBounds,
        padding: Int,
    ): CameraUpdate = CameraUpdate.NewLatLngBounds(bounds, padding)

    /**
     * Returns a CameraUpdate that moves the camera to show the entire bounds
     * in a viewport of the specified dimensions.
     *
     * This overload is useful when the map view hasn't been laid out yet and you need
     * to calculate the camera position for specific viewport dimensions.
     *
     * @param bounds The geographic bounds to display
     * @param width The viewport width in pixels
     * @param height The viewport height in pixels
     * @param padding Padding in pixels to apply uniformly on all sides
     * @return A CameraUpdate to display the bounds
     */
    fun newLatLngBounds(
        bounds: LatLngBounds,
        width: Int,
        height: Int,
        padding: Int,
    ): CameraUpdate = CameraUpdate.NewLatLngBoundsWithSize(bounds, width, height, padding)
}
