/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example04polylines

import androidx.compose.ui.graphics.Color
import de.afarber.openmapview.LatLng

/**
 * Common interface for overlay data classes.
 */
interface OverlayData {
    val title: String
    val snippet: String
    val points: List<LatLng>
}

/**
 * Data class representing a polyline with its display properties.
 *
 * @param title Display title for the polyline.
 * @param snippet Additional description text.
 * @param points List of coordinates defining the polyline path.
 * @param color Stroke color for the polyline.
 * @param width Stroke width in pixels.
 * @param geodesic Whether segments are drawn as geodesics (great-circle paths).
 */
data class PolylineData(
    override val title: String,
    override val snippet: String,
    override val points: List<LatLng>,
    val color: Color,
    val width: Float,
    val geodesic: Boolean = false,
) : OverlayData

/**
 * Data class representing a polygon with its display properties.
 *
 * @param title Display title for the polygon.
 * @param snippet Additional description text.
 * @param points List of coordinates defining the polygon outline.
 * @param holes List of hole definitions (each hole is a list of coordinates).
 * @param strokeColor Stroke color for the polygon outline.
 * @param fillColor Fill color for the polygon interior.
 */
data class PolygonData(
    override val title: String,
    override val snippet: String,
    override val points: List<LatLng>,
    val holes: List<List<LatLng>> = emptyList(),
    val strokeColor: Color,
    val fillColor: Color,
) : OverlayData

/**
 * Predefined overlays at notable Bochum locations.
 *
 * Includes real routes and areas in Bochum, Germany:
 * - Springorum cycling path (former railway line)
 * - Ruhr riverside walk
 * - Hauptbahnhof to Bermuda3eck route
 * - Stadtpark (city park)
 * - Westpark
 * - Ruhr University campus
 */
val poiOverlays: List<OverlayData> = listOf(
    // Ruhr University Campus - first in the list (polygon with hole)
    PolygonData(
        title = "Ruhr University Campus",
        snippet = "University grounds with lake",
        points = listOf(
            LatLng(51.4410, 7.2550),
            LatLng(51.4480, 7.2540),
            LatLng(51.4500, 7.2620),
            LatLng(51.4490, 7.2720),
            LatLng(51.4420, 7.2730),
            LatLng(51.4400, 7.2650),
        ),
        holes = listOf(
            // Kemnader See (lake) as a hole
            listOf(
                LatLng(51.4440, 7.2620),
                LatLng(51.4460, 7.2615),
                LatLng(51.4465, 7.2660),
                LatLng(51.4445, 7.2665),
            ),
        ),
        strokeColor = Color(0xFF3F51B5), // Indigo
        fillColor = Color(0x663F51B5), // Semi-transparent indigo
    ),
    // Polylines - Real Bochum routes
    PolylineData(
        title = "Springorum Radweg",
        snippet = "Cycling path on old railway",
        points = listOf(
            LatLng(51.4565, 7.2145),
            LatLng(51.4590, 7.2210),
            LatLng(51.4620, 7.2280),
            LatLng(51.4655, 7.2350),
            LatLng(51.4690, 7.2420),
        ),
        color = Color(0xFF2196F3), // Blue
        width = 8f,
    ),
    PolylineData(
        title = "Ruhr Riverside Walk",
        snippet = "Scenic walk along the Ruhr",
        points = listOf(
            LatLng(51.4380, 7.2350),
            LatLng(51.4375, 7.2420),
            LatLng(51.4365, 7.2490),
            LatLng(51.4360, 7.2560),
            LatLng(51.4355, 7.2630),
            LatLng(51.4350, 7.2700),
        ),
        color = Color(0xFF00BCD4), // Cyan
        width = 6f,
    ),
    PolylineData(
        title = "Hbf to Bermuda3eck",
        snippet = "City center walking route",
        points = listOf(
            LatLng(51.4783, 7.2231), // Hauptbahnhof
            LatLng(51.4778, 7.2210),
            LatLng(51.4772, 7.2190),
            LatLng(51.4765, 7.2175),
            LatLng(51.4761, 7.2161), // Bermuda3eck
        ),
        color = Color(0xFFFF5722), // Deep Orange
        width = 7f,
    ),
    // Geodesic polyline - Long distance route (demonstrates curved great-circle path)
    PolylineData(
        title = "Bochum to Berlin",
        snippet = "Geodesic (great-circle) path",
        points = listOf(
            LatLng(51.4818, 7.2162), // Bochum
            LatLng(52.5200, 13.4050), // Berlin
        ),
        color = Color(0xFFE91E63), // Pink
        width = 5f,
        geodesic = true,
    ),
    // Polygons - Real Bochum areas
    PolygonData(
        title = "Stadtpark",
        snippet = "Central city park",
        points = listOf(
            LatLng(51.4820, 7.2260),
            LatLng(51.4850, 7.2260),
            LatLng(51.4870, 7.2310),
            LatLng(51.4860, 7.2360),
            LatLng(51.4830, 7.2370),
            LatLng(51.4810, 7.2330),
        ),
        strokeColor = Color(0xFF4CAF50), // Green
        fillColor = Color(0x664CAF50), // Semi-transparent green
    ),
    PolygonData(
        title = "Westpark",
        snippet = "Western recreational park",
        points = listOf(
            LatLng(51.4750, 7.1980),
            LatLng(51.4780, 7.1990),
            LatLng(51.4790, 7.2050),
            LatLng(51.4770, 7.2080),
            LatLng(51.4740, 7.2060),
            LatLng(51.4735, 7.2010),
        ),
        strokeColor = Color(0xFF8BC34A), // Light Green
        fillColor = Color(0x668BC34A), // Semi-transparent light green
    ),
)
