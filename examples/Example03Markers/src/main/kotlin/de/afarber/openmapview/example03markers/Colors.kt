/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example03markers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * OSM-inspired colors and shared dimensions for the Example03Markers app.
 */

/** Green color used by OpenStreetMap for parks and forests. */
val OsmParkGreen = Color(0xFFAAD3A2)

/** Pink color used by OpenStreetMap for highways and major roads. */
val OsmHighwayPink = Color(0xFFE892A2)

/** Blue color used by OpenStreetMap for water areas (lakes, rivers). */
val OsmWaterBlue = Color(0xFFAAD3DF)

/** Shared corner radius for all toolbar components (matches Material3 FAB). */
val ToolbarCornerRadius = 16.dp

/** Duration after which info windows are automatically dismissed. */
val InfoWindowAutoDismissDuration: Duration = 5.seconds
