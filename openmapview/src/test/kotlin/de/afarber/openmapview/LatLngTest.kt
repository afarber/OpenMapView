/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class LatLngTest {
    companion object {
        private const val DELTA = 1e-9
    }

    @Test
    fun testInRangeValues_Unchanged() {
        val latLng = LatLng(51.4661, 7.2491)
        assertEquals(51.4661, latLng.latitude, DELTA)
        assertEquals(7.2491, latLng.longitude, DELTA)
    }

    @Test
    fun testLatitude_ClampedToPoles() {
        assertEquals(90.0, LatLng(95.0, 10.0).latitude, DELTA)
        assertEquals(-90.0, LatLng(-120.0, 10.0).latitude, DELTA)
    }

    @Test
    fun testLatitude_PolesUnchanged() {
        assertEquals(90.0, LatLng(90.0, 0.0).latitude, DELTA)
        assertEquals(-90.0, LatLng(-90.0, 0.0).latitude, DELTA)
    }

    @Test
    fun testLongitude_Wrapped() {
        assertEquals(-170.0, LatLng(10.0, 190.0).longitude, DELTA)
        assertEquals(170.0, LatLng(10.0, -190.0).longitude, DELTA)
        assertEquals(-180.0, LatLng(10.0, 540.0).longitude, DELTA)
        assertEquals(10.0, LatLng(10.0, 730.0).longitude, DELTA)
    }

    @Test
    fun testLongitude_AntimeridianUnchanged() {
        assertEquals(180.0, LatLng(0.0, 180.0).longitude, DELTA)
        assertEquals(-180.0, LatLng(0.0, -180.0).longitude, DELTA)
    }

    @Test
    fun testNonFiniteLatitude_Throws() {
        assertThrows(IllegalArgumentException::class.java) { LatLng(Double.NaN, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { LatLng(Double.POSITIVE_INFINITY, 0.0) }
        assertThrows(IllegalArgumentException::class.java) { LatLng(Double.NEGATIVE_INFINITY, 0.0) }
    }

    @Test
    fun testNonFiniteLongitude_Throws() {
        assertThrows(IllegalArgumentException::class.java) { LatLng(0.0, Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { LatLng(0.0, Double.POSITIVE_INFINITY) }
        assertThrows(IllegalArgumentException::class.java) { LatLng(0.0, Double.NEGATIVE_INFINITY) }
    }

    @Test
    fun testEquality_UsesNormalizedValues() {
        assertEquals(LatLng(90.0, 10.0), LatLng(95.0, 10.0))
        assertEquals(LatLng(10.0, -170.0), LatLng(10.0, 190.0))
        assertEquals(LatLng(10.0, -170.0).hashCode(), LatLng(10.0, 190.0).hashCode())
        assertNotEquals(LatLng(10.0, 20.0), LatLng(20.0, 10.0))
    }

    @Test
    fun testToString_Format() {
        assertEquals("LatLng(latitude=51.5, longitude=-0.1)", LatLng(51.5, -0.1).toString())
    }

    @Test
    fun testCopy_IsNormalized() {
        val copy = LatLng(51.5, -0.1).copy(longitude = 200.0)
        assertEquals(51.5, copy.latitude, DELTA)
        assertEquals(-160.0, copy.longitude, DELTA)
    }

    @Test
    fun testDestructuring() {
        val (latitude, longitude) = LatLng(51.5, -0.1)
        assertEquals(51.5, latitude, DELTA)
        assertEquals(-0.1, longitude, DELTA)
    }
}
