/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleRegionTest {
    private val epsilon = 0.0001

    @Test
    fun testConstructor_AndFields() {
        val nearLeft = LatLng(40.0, -75.0)
        val nearRight = LatLng(40.0, -73.0)
        val farLeft = LatLng(42.0, -75.0)
        val farRight = LatLng(42.0, -73.0)
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))

        val region = VisibleRegion(nearLeft, nearRight, farLeft, farRight, bounds)

        assertEquals(40.0, region.nearLeft.latitude, epsilon)
        assertEquals(-75.0, region.nearLeft.longitude, epsilon)
        assertEquals(40.0, region.nearRight.latitude, epsilon)
        assertEquals(-73.0, region.nearRight.longitude, epsilon)
        assertEquals(42.0, region.farLeft.latitude, epsilon)
        assertEquals(-75.0, region.farLeft.longitude, epsilon)
        assertEquals(42.0, region.farRight.latitude, epsilon)
        assertEquals(-73.0, region.farRight.longitude, epsilon)
        assertEquals(bounds, region.latLngBounds)
    }

    @Test
    fun testBoundsMatchesCorners() {
        val nearLeft = LatLng(40.0, -75.0)
        val nearRight = LatLng(40.0, -73.0)
        val farLeft = LatLng(42.0, -75.0)
        val farRight = LatLng(42.0, -73.0)
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))

        val region = VisibleRegion(nearLeft, nearRight, farLeft, farRight, bounds)

        assertEquals(40.0, region.latLngBounds.southwest.latitude, epsilon)
        assertEquals(-75.0, region.latLngBounds.southwest.longitude, epsilon)
        assertEquals(42.0, region.latLngBounds.northeast.latitude, epsilon)
        assertEquals(-73.0, region.latLngBounds.northeast.longitude, epsilon)
    }

    @Test
    fun testDataClass_Equality() {
        val nearLeft = LatLng(40.0, -75.0)
        val nearRight = LatLng(40.0, -73.0)
        val farLeft = LatLng(42.0, -75.0)
        val farRight = LatLng(42.0, -73.0)
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))

        val region1 = VisibleRegion(nearLeft, nearRight, farLeft, farRight, bounds)
        val region2 = VisibleRegion(nearLeft, nearRight, farLeft, farRight, bounds)

        assertEquals(region1, region2)
    }

    @Test
    fun testDataClass_HashCode() {
        val nearLeft = LatLng(40.0, -75.0)
        val nearRight = LatLng(40.0, -73.0)
        val farLeft = LatLng(42.0, -75.0)
        val farRight = LatLng(42.0, -73.0)
        val bounds = LatLngBounds(LatLng(40.0, -75.0), LatLng(42.0, -73.0))

        val region1 = VisibleRegion(nearLeft, nearRight, farLeft, farRight, bounds)
        val region2 = VisibleRegion(nearLeft, nearRight, farLeft, farRight, bounds)

        assertEquals(region1.hashCode(), region2.hashCode())
    }
}
