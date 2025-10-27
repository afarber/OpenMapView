/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * An immutable class representing a pair of latitude and longitude coordinates.
 *
 * Latitude ranges from -90 to 90 degrees, and longitude ranges from -180 to 180 degrees.
 *
 * @property latitude The latitude in degrees
 * @property longitude The longitude in degrees
 */
data class LatLng(
    val latitude: Double,
    val longitude: Double,
)
