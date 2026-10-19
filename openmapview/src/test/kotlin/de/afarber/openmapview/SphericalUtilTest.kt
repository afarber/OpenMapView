/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class SphericalUtilTest {
    private val london = LatLng(51.5074, -0.1278)
    private val paris = LatLng(48.8566, 2.3522)
    private val origin = LatLng(0.0, 0.0)

    private fun assertLatLng(
        expected: LatLng,
        actual: LatLng,
        delta: Double = 1e-9,
    ) {
        assertEquals(expected.latitude, actual.latitude, delta)
        assertEquals(expected.longitude, actual.longitude, delta)
    }

    @Test
    fun testComputeDistanceBetween_KnownDistances() {
        assertEquals(343_560.0, SphericalUtil.computeDistanceBetween(london, paris), 1_000.0)
        // A quarter of a meridian
        val quarter = SphericalUtil.EARTH_RADIUS * PI / 2
        assertEquals(quarter, SphericalUtil.computeDistanceBetween(origin, LatLng(90.0, 0.0)), 1e-6)
        assertEquals(0.0, SphericalUtil.computeDistanceBetween(london, london), 1e-9)
        assertEquals(
            SphericalUtil.computeDistanceBetween(london, paris),
            SphericalUtil.computeDistanceBetween(paris, london),
            1e-9,
        )
    }

    @Test
    fun testComputeHeading_CompassDirections() {
        assertEquals(0.0, SphericalUtil.computeHeading(origin, LatLng(1.0, 0.0)), 1e-9)
        assertEquals(90.0, SphericalUtil.computeHeading(origin, LatLng(0.0, 1.0)), 1e-9)
        // South is -180, as headings are between -180 and 180
        assertEquals(-180.0, SphericalUtil.computeHeading(origin, LatLng(-1.0, 0.0)), 1e-9)
        assertEquals(-90.0, SphericalUtil.computeHeading(origin, LatLng(0.0, -1.0)), 1e-9)
        // London to Paris is south-east
        assertEquals(148.1, SphericalUtil.computeHeading(london, paris), 0.1)
    }

    @Test
    fun testComputeOffset_AndOrigin() {
        val to = SphericalUtil.computeOffset(london, 100_000.0, 45.0)

        assertEquals(100_000.0, SphericalUtil.computeDistanceBetween(london, to), 1e-3)
        assertEquals(45.0, SphericalUtil.computeHeading(london, to), 1e-6)
        val from = SphericalUtil.computeOffsetOrigin(to, 100_000.0, 45.0)
        assertNotNull(from)
        assertLatLng(london, from!!, 1e-7)
    }

    @Test
    fun testComputeOffset_CrossesDateLine() {
        val to = SphericalUtil.computeOffset(LatLng(0.0, 179.5), 111_195.0, 90.0)

        assertEquals(0.0, to.latitude, 1e-6)
        assertEquals(-179.5, to.longitude, 1e-3)
    }

    @Test
    fun testComputeOffsetOrigin_WithoutSolution() {
        // Going east at the start, a great circle only turns away from the pole. No start
        // reaches 80 degrees north after 20 degrees of arc: it would have to be above 90.
        assertNull(SphericalUtil.computeOffsetOrigin(LatLng(80.0, 0.0), SphericalUtil.EARTH_RADIUS * Math.toRadians(20.0), 90.0))
    }

    @Test
    fun testInterpolate_EndsAndMiddle() {
        assertLatLng(london, SphericalUtil.interpolate(london, paris, 0.0))
        assertLatLng(paris, SphericalUtil.interpolate(london, paris, 1.0))

        val middle = SphericalUtil.interpolate(london, paris, 0.5)
        assertEquals(
            SphericalUtil.computeDistanceBetween(london, middle),
            SphericalUtil.computeDistanceBetween(middle, paris),
            1e-3,
        )
        assertEquals(
            SphericalUtil.computeDistanceBetween(london, paris) / 2,
            SphericalUtil.computeDistanceBetween(london, middle),
            1e-3,
        )
    }

    @Test
    fun testInterpolate_CrossesDateLineTheShortWay() {
        val middle = SphericalUtil.interpolate(LatLng(0.0, 179.0), LatLng(0.0, -179.0), 0.5)
        assertEquals(0.0, middle.latitude, 1e-9)
        assertEquals(180.0, abs(middle.longitude), 1e-9)

        // Also for points too close for the spherical formula
        val near = SphericalUtil.interpolate(LatLng(10.0, 179.99999), LatLng(10.0, -179.99999), 0.5)
        assertEquals(180.0, abs(near.longitude), 1e-6)
    }

    @Test
    fun testComputeLength_SumsTheSegments() {
        val berlin = LatLng(52.52, 13.405)
        val expected = SphericalUtil.computeDistanceBetween(london, paris) + SphericalUtil.computeDistanceBetween(paris, berlin)

        assertEquals(expected, SphericalUtil.computeLength(listOf(london, paris, berlin)), 1e-6)
        assertEquals(0.0, SphericalUtil.computeLength(listOf(london)), 0.0)
        assertEquals(0.0, SphericalUtil.computeLength(emptyList()), 0.0)
    }

    @Test
    fun testComputeArea_DegreeSquareAtEquator() {
        // Counter-clockwise: east, north, west
        val square = listOf(LatLng(0.0, 0.0), LatLng(0.0, 1.0), LatLng(1.0, 1.0), LatLng(1.0, 0.0))
        // About the area between two parallels over a longitude span, R^2 * dLng * (sin lat2 -
        // sin lat1), 1.2364e10 m^2. The north edge is a great circle, which bulges slightly
        // north of the parallel, so the area is a little larger.
        val r = SphericalUtil.EARTH_RADIUS
        val band = r * r * Math.toRadians(1.0) * sin(Math.toRadians(1.0))
        val area = SphericalUtil.computeSignedArea(square)

        assertEquals(band, area, band * 1e-4)
        assertTrue(area > band)
        assertEquals(-area, SphericalUtil.computeSignedArea(square.reversed()), area * 1e-12)
        assertEquals(area, SphericalUtil.computeArea(square.reversed()), area * 1e-12)
        assertEquals(0.0, SphericalUtil.computeArea(square.take(2)), 0.0)
    }
}
