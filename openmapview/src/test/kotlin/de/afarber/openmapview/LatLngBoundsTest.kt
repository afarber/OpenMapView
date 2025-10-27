/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LatLngBoundsTest {
    private val epsilon = 0.0001

    @Test
    fun testConstructor_AndFields() {
        val sw = LatLng(40.0, -75.0)
        val ne = LatLng(42.0, -73.0)
        val bounds = LatLngBounds(sw, ne)

        assertEquals(40.0, bounds.southwest.latitude, epsilon)
        assertEquals(-75.0, bounds.southwest.longitude, epsilon)
        assertEquals(42.0, bounds.northeast.latitude, epsilon)
        assertEquals(-73.0, bounds.northeast.longitude, epsilon)
    }

    @Test
    fun testContains_PointInside() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))
        val point = LatLng(41.0, -74.0)

        assertTrue(bounds.contains(point))
    }

    @Test
    fun testContains_PointOutside() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))
        val point = LatLng(43.0, -74.0)

        assertFalse(bounds.contains(point))
    }

    @Test
    fun testContains_PointOnEdge() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))
        val point = LatLng(40.0, -74.0)

        assertTrue(bounds.contains(point))
    }

    @Test
    fun testContains_Southwest() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))

        assertTrue(bounds.contains(bounds.southwest))
    }

    @Test
    fun testContains_Northeast() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))

        assertTrue(bounds.contains(bounds.northeast))
    }

    @Test
    fun testGetCenter() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))
        val center = bounds.getCenter()

        assertEquals(41.0, center.latitude, epsilon)
        assertEquals(-74.0, center.longitude, epsilon)
    }

    @Test
    fun testGetCenter_CrossingDateLine() {
        val bounds = LatLngBounds(LatLng(40.0, 170.0), LatLng(42.0, -170.0))
        val center = bounds.getCenter()

        assertEquals(41.0, center.latitude, epsilon)
        // Accept both 180.0 and -180.0 as they represent the same meridian
        assertTrue(
            kotlin.math.abs(center.longitude - 180.0) < epsilon ||
                kotlin.math.abs(center.longitude - (-180.0)) < epsilon,
        )
    }

    @Test
    fun testIncluding_ExpandsNorth() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))
        val point = LatLng(43.0, -74.0)
        val expanded = bounds.including(point)

        assertEquals(40.0, expanded.southwest.latitude, epsilon)
        assertEquals(-75.0, expanded.southwest.longitude, epsilon)
        assertEquals(43.0, expanded.northeast.latitude, epsilon)
        assertEquals(-73.0, expanded.northeast.longitude, epsilon)
    }

    @Test
    fun testIncluding_ExpandsSouth() {
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))
        val point = LatLng(39.0, -74.0)
        val expanded = bounds.including(point)

        assertEquals(39.0, expanded.southwest.latitude, epsilon)
        assertEquals(-75.0, expanded.southwest.longitude, epsilon)
        assertEquals(42.0, expanded.northeast.latitude, epsilon)
        assertEquals(-73.0, expanded.northeast.longitude, epsilon)
    }

    @Test
    fun testBuilder_SinglePoint() {
        val point = LatLng(41.0, -74.0)
        val bounds = LatLngBounds.builder().include(point).build()

        assertEquals(41.0, bounds.southwest.latitude, epsilon)
        assertEquals(-74.0, bounds.southwest.longitude, epsilon)
        assertEquals(41.0, bounds.northeast.latitude, epsilon)
        assertEquals(-74.0, bounds.northeast.longitude, epsilon)
    }

    @Test
    fun testBuilder_MultiplePoints() {
        val bounds =
            LatLngBounds
                .builder()
                .include(LatLng(40.0, -75.0))
                .include(LatLng(42.0, -73.0))
                .include(LatLng(41.0, -76.0))
                .build()

        assertEquals(40.0, bounds.southwest.latitude, epsilon)
        assertEquals(-76.0, bounds.southwest.longitude, epsilon)
        assertEquals(42.0, bounds.northeast.latitude, epsilon)
        assertEquals(-73.0, bounds.northeast.longitude, epsilon)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBuilder_EmptyThrowsException() {
        LatLngBounds.builder().build()
    }
}
