/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeodesicUtilsTest {
    // Bochum, Germany
    private val bochum = LatLng(51.4818, 7.2162)

    // Berlin, Germany
    private val berlin = LatLng(52.5200, 13.4050)

    // New York, USA
    private val newYork = LatLng(40.7128, -74.0060)

    // London, UK
    private val london = LatLng(51.5074, -0.1278)

    @Test
    fun `haversineDistance returns correct distance between Bochum and Berlin`() {
        // Bochum to Berlin is approximately 430km
        val distance = GeodesicUtils.haversineDistance(bochum, berlin)
        assertEquals(430000.0, distance, 10000.0) // 430km +/- 10km
    }

    @Test
    fun `haversineDistance returns correct distance between New York and London`() {
        // New York to London is approximately 5570km
        val distance = GeodesicUtils.haversineDistance(newYork, london)
        assertEquals(5570000.0, distance, 50000.0) // 5570km +/- 50km
    }

    @Test
    fun `haversineDistance returns zero for same point`() {
        val distance = GeodesicUtils.haversineDistance(bochum, bochum)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `haversineDistance is symmetric`() {
        val distanceAB = GeodesicUtils.haversineDistance(bochum, berlin)
        val distanceBA = GeodesicUtils.haversineDistance(berlin, bochum)
        assertEquals(distanceAB, distanceBA, 0.001)
    }

    @Test
    fun `interpolateGreatCircle returns empty list for zero points`() {
        val points = GeodesicUtils.interpolateGreatCircle(bochum, berlin, 0)
        assertTrue(points.isEmpty())
    }

    @Test
    fun `interpolateGreatCircle returns empty list for negative points`() {
        val points = GeodesicUtils.interpolateGreatCircle(bochum, berlin, -5)
        assertTrue(points.isEmpty())
    }

    @Test
    fun `interpolateGreatCircle returns correct number of points`() {
        val points = GeodesicUtils.interpolateGreatCircle(bochum, berlin, 5)
        assertEquals(5, points.size)
    }

    @Test
    fun `interpolateGreatCircle returns midpoint correctly`() {
        val points = GeodesicUtils.interpolateGreatCircle(bochum, berlin, 1)
        assertEquals(1, points.size)

        // Midpoint should be roughly between Bochum and Berlin
        val midpoint = points[0]
        assertTrue(midpoint.latitude > bochum.latitude)
        assertTrue(midpoint.latitude < berlin.latitude)
        assertTrue(midpoint.longitude > bochum.longitude)
        assertTrue(midpoint.longitude < berlin.longitude)
    }

    @Test
    fun `interpolateGreatCircle points are evenly spaced`() {
        val points = GeodesicUtils.interpolateGreatCircle(newYork, london, 4)
        assertEquals(4, points.size)

        // Calculate distances between consecutive points
        val distances = mutableListOf<Double>()
        distances.add(GeodesicUtils.haversineDistance(newYork, points[0]))
        for (i in 0 until points.size - 1) {
            distances.add(GeodesicUtils.haversineDistance(points[i], points[i + 1]))
        }
        distances.add(GeodesicUtils.haversineDistance(points.last(), london))

        // All segment distances should be roughly equal
        val avgDistance = distances.average()
        for (distance in distances) {
            assertEquals(avgDistance, distance, avgDistance * 0.05) // 5% tolerance
        }
    }

    @Test
    fun `interpolateGreatCircle returns empty for same point`() {
        // Exact same point should return empty (angular distance is zero)
        val points = GeodesicUtils.interpolateGreatCircle(bochum, bochum, 5)
        assertTrue(points.isEmpty())
    }

    @Test
    fun `expandWithGeodesicPoints returns original for short distances`() {
        // Points very close together (less than MIN_INTERPOLATION_DISTANCE)
        val closePoints =
            listOf(
                bochum,
                LatLng(bochum.latitude + 0.001, bochum.longitude + 0.001),
            )
        val expanded = GeodesicUtils.expandWithGeodesicPoints(closePoints)

        // Should return original points without interpolation
        assertEquals(2, expanded.size)
        assertEquals(bochum, expanded[0])
    }

    @Test
    fun `expandWithGeodesicPoints adds interpolation for long distances`() {
        val points = listOf(bochum, berlin)
        val expanded = GeodesicUtils.expandWithGeodesicPoints(points)

        // Bochum to Berlin is ~430km, with 50km step we expect 8-9 interpolated points
        assertTrue(expanded.size > points.size)
        assertEquals(bochum, expanded.first())
        assertEquals(berlin, expanded.last())
    }

    @Test
    fun `expandWithGeodesicPoints handles multiple segments`() {
        val points = listOf(bochum, berlin, london)
        val expanded = GeodesicUtils.expandWithGeodesicPoints(points)

        // Should have more points due to interpolation
        assertTrue(expanded.size > points.size)
        assertEquals(bochum, expanded.first())
        assertEquals(london, expanded.last())
    }

    @Test
    fun `expandWithGeodesicPoints returns original for single point`() {
        val points = listOf(bochum)
        val expanded = GeodesicUtils.expandWithGeodesicPoints(points)
        assertEquals(1, expanded.size)
        assertEquals(bochum, expanded[0])
    }

    @Test
    fun `expandWithGeodesicPoints returns empty for empty input`() {
        val points = emptyList<LatLng>()
        val expanded = GeodesicUtils.expandWithGeodesicPoints(points)
        assertTrue(expanded.isEmpty())
    }

    @Test
    fun `haversineDistance handles equator crossing`() {
        val north = LatLng(10.0, 0.0)
        val south = LatLng(-10.0, 0.0)
        val distance = GeodesicUtils.haversineDistance(north, south)

        // 20 degrees of latitude is approximately 2222km
        assertEquals(2222000.0, distance, 50000.0)
    }

    @Test
    fun `haversineDistance handles antimeridian crossing`() {
        val west = LatLng(0.0, 170.0)
        val east = LatLng(0.0, -170.0)
        val distance = GeodesicUtils.haversineDistance(west, east)

        // 20 degrees of longitude at equator is approximately 2222km
        assertEquals(2222000.0, distance, 50000.0)
    }
}
