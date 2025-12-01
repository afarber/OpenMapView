/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example01pan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * OSM-inspired colors and shared dimensions for the Example01Pan app.
 */

/** Green color used by OpenStreetMap for parks and forests. */
val OsmParkGreen = Color(0xFFAAD3A2)

/** Pink color used by OpenStreetMap for highways and major roads. */
val OsmHighwayPink = Color(0xFFE892A2)

/** Blue color used by OpenStreetMap for water areas (lakes, rivers). */
val OsmWaterBlue = Color(0xFFAAD3DF)

/** Shared corner radius for all toolbar components. */
val ToolbarCornerRadius = 8.dp
