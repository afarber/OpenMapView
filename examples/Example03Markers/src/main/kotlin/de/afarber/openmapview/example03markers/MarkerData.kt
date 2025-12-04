/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example03markers

import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.LatLng

/**
 * Data class representing a marker location with its display properties.
 *
 * @param position Geographic coordinates of the marker.
 * @param title Display title shown in the info window.
 * @param snippet Additional text shown in the info window.
 * @param hue Color hue for the marker icon.
 */
data class MarkerData(
    val position: LatLng,
    val title: String,
    val snippet: String,
    val hue: Float,
)

/** Initial markers at notable Bochum locations. */
val initialMarkerData = listOf(
    MarkerData(
        position = LatLng(51.4783, 7.2231),
        title = "Bochum Hauptbahnhof",
        snippet = "Main railway station",
        hue = BitmapDescriptorFactory.HUE_RED,
    ),
    MarkerData(
        position = LatLng(51.4452, 7.2622),
        title = "Ruhr University",
        snippet = "Ruhr-Universität Bochum",
        hue = BitmapDescriptorFactory.HUE_BLUE,
    ),
    MarkerData(
        position = LatLng(51.4816, 7.2166),
        title = "Bochum Rathaus",
        snippet = "City Hall",
        hue = BitmapDescriptorFactory.HUE_GREEN,
    ),
    MarkerData(
        position = LatLng(51.4807, 7.2222),
        title = "Bermuda3eck",
        snippet = "Entertainment district",
        hue = BitmapDescriptorFactory.HUE_ORANGE,
    ),
    MarkerData(
        position = LatLng(51.4892, 7.2174),
        title = "Bergbau-Museum",
        snippet = "German Mining Museum",
        hue = BitmapDescriptorFactory.HUE_MAGENTA,
    ),
    MarkerData(
        position = LatLng(51.4649, 7.2043),
        title = "Starlight Express",
        snippet = "Musical theater",
        hue = BitmapDescriptorFactory.HUE_CYAN,
    ),
)
