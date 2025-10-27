/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Contains the four points defining the four-sided polygon visible in the map camera.
 *
 * Compatible with Google Maps API. This polygon can be a trapezoid instead of a rectangle
 * when the camera has tilt. For a flat map view (no tilt), it represents a rectangle.
 *
 * @property nearLeft The bottom-left corner of the camera (southwest)
 * @property nearRight The bottom-right corner of the camera (southeast)
 * @property farLeft The top-left corner of the camera (northwest)
 * @property farRight The top-right corner of the camera (northeast)
 * @property latLngBounds The smallest bounding box that includes the visible region
 */
data class VisibleRegion(
    val nearLeft: LatLng,
    val nearRight: LatLng,
    val farLeft: LatLng,
    val farRight: LatLng,
    val latLngBounds: LatLngBounds,
)
