/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

data class CameraPosition(
    val target: LatLng,
    val zoom: Double,
) {
    init {
        require(zoom >= MIN_ZOOM) { "Zoom level must be at least $MIN_ZOOM" }
        require(zoom <= MAX_ZOOM) { "Zoom level must be at most $MAX_ZOOM" }
    }

    companion object {
        private const val MIN_ZOOM = 2.0
        private const val MAX_ZOOM = 19.0
    }
}
