/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

object CameraUpdateFactory {
    fun newLatLng(target: LatLng): CameraUpdate = CameraUpdate.NewLatLng(target)

    fun newLatLngZoom(
        target: LatLng,
        zoom: Double,
    ): CameraUpdate = CameraUpdate.NewLatLngZoom(target, zoom)

    fun newCameraPosition(position: CameraPosition): CameraUpdate = CameraUpdate.NewCameraPosition(position)

    fun zoomIn(): CameraUpdate = CameraUpdate.ZoomIn

    fun zoomOut(): CameraUpdate = CameraUpdate.ZoomOut

    fun zoomTo(zoom: Double): CameraUpdate = CameraUpdate.ZoomTo(zoom)

    fun zoomBy(amount: Double): CameraUpdate = CameraUpdate.ZoomBy(amount)
}
