/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Utility object for geodesic (great-circle) calculations.
 *
 * Provides functions for calculating distances and interpolating points
 * along the shortest path on Earth's surface between two coordinates.
 */
internal object GeodesicUtils {
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
    ): Double = SphericalUtil.computeDistanceBetween(from, to)

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
        // If points are very close, no interpolation needed
        if (numPoints <= 0 || SphericalUtil.computeAngleBetween(from, to) < 1e-10) return emptyList()
        return (1..numPoints).map { SphericalUtil.interpolate(from, to, it.toDouble() / (numPoints + 1)) }
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
