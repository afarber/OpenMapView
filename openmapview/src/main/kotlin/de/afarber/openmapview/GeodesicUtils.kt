/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility object for geodesic (great-circle) calculations.
 *
 * Provides functions for calculating distances and interpolating points
 * along the shortest path on Earth's surface between two coordinates.
 */
internal object GeodesicUtils {
    /** Earth's mean radius in meters. */
    private const val EARTH_RADIUS_METERS = 6371000.0

    /** Minimum distance in meters to consider for interpolation. */
    private const val MIN_INTERPOLATION_DISTANCE = 1000.0

    /** Distance between interpolated points in meters. */
    private const val INTERPOLATION_STEP_METERS = 50000.0

    /**
     * Calculates the great-circle distance between two points using the Haversine formula.
     *
     * @param from Starting coordinate.
     * @param to Ending coordinate.
     * @return Distance in meters.
     */
    fun haversineDistance(
        from: LatLng,
        to: LatLng,
    ): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLat = Math.toRadians(to.latitude - from.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val a =
            sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Interpolates points along the great-circle path between two coordinates.
     *
     * Uses spherical linear interpolation (slerp) to generate intermediate points
     * that follow the shortest path on Earth's surface.
     *
     * @param from Starting coordinate.
     * @param to Ending coordinate.
     * @param numPoints Number of intermediate points to generate (not including from/to).
     * @return List of intermediate LatLng points. Empty if numPoints <= 0.
     */
    fun interpolateGreatCircle(
        from: LatLng,
        to: LatLng,
        numPoints: Int,
    ): List<LatLng> {
        if (numPoints <= 0) return emptyList()

        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)

        // Calculate angular distance
        val deltaLat = lat2 - lat1
        val deltaLon = lon2 - lon1
        val a =
            sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) * sin(deltaLon / 2) * sin(deltaLon / 2)
        val angularDistance = 2 * asin(sqrt(a))

        // If points are very close, no interpolation needed
        if (angularDistance < 1e-10) return emptyList()

        val points = mutableListOf<LatLng>()

        for (i in 1..numPoints) {
            val fraction = i.toDouble() / (numPoints + 1)

            // Spherical linear interpolation
            val factorA = sin((1 - fraction) * angularDistance) / sin(angularDistance)
            val factorB = sin(fraction * angularDistance) / sin(angularDistance)

            val x = factorA * cos(lat1) * cos(lon1) + factorB * cos(lat2) * cos(lon2)
            val y = factorA * cos(lat1) * sin(lon1) + factorB * cos(lat2) * sin(lon2)
            val z = factorA * sin(lat1) + factorB * sin(lat2)

            val lat = atan2(z, sqrt(x * x + y * y))
            val lon = atan2(y, x)

            points.add(LatLng(Math.toDegrees(lat), Math.toDegrees(lon)))
        }

        return points
    }

    /**
     * Expands a list of points with geodesic interpolation between each consecutive pair.
     *
     * Automatically determines the number of interpolation points based on distance,
     * using approximately one point per [INTERPOLATION_STEP_METERS] meters.
     *
     * @param points Original list of coordinates.
     * @return Expanded list with interpolated points inserted between original points.
     */
    fun expandWithGeodesicPoints(points: List<LatLng>): List<LatLng> {
        if (points.size < 2) return points

        val expanded = mutableListOf<LatLng>()

        for (i in 0 until points.size - 1) {
            expanded.add(points[i])

            val distance = haversineDistance(points[i], points[i + 1])
            if (distance > MIN_INTERPOLATION_DISTANCE) {
                val numPoints = (distance / INTERPOLATION_STEP_METERS).toInt().coerceAtLeast(1)
                val interpolated = interpolateGreatCircle(points[i], points[i + 1], numPoints)
                expanded.addAll(interpolated)
            }
        }

        expanded.add(points.last())
        return expanded
    }
}
