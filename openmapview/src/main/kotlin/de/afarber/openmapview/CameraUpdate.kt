/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

sealed class CameraUpdate {
    internal data class NewLatLng(
        val target: LatLng,
    ) : CameraUpdate()

    internal data class NewLatLngZoom(
        val target: LatLng,
        val zoom: Double,
    ) : CameraUpdate()

    internal data class NewCameraPosition(
        val position: CameraPosition,
    ) : CameraUpdate()

    internal data object ZoomIn : CameraUpdate()

    internal data object ZoomOut : CameraUpdate()

    internal data class ZoomTo(
        val zoom: Double,
    ) : CameraUpdate()

    internal data class ZoomBy(
        val amount: Double,
    ) : CameraUpdate()
}
