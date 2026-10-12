/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Represents the camera position for a map.
 *
 * Defines where the camera is positioned (target location) and how close
 * the camera is to the earth's surface (zoom level).
 *
 * @property target The location that the camera is pointing at
 * @property zoom The zoom level, ranging from 2.0 (world view) to 19.0 (street level)
 * @throws IllegalArgumentException if zoom is outside the valid range
 */
data class CameraPosition(
    val target: LatLng,
    val zoom: Float,
) {
    init {
        require(zoom >= MIN_ZOOM) { "Zoom level must be at least $MIN_ZOOM" }
        require(zoom <= MAX_ZOOM) { "Zoom level must be at most $MAX_ZOOM" }
    }

    companion object {
        private const val MIN_ZOOM = 2.0f
        private const val MAX_ZOOM = 19.0f
    }
}
