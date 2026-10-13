/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Point
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for the public [Projection] class (screen <-> LatLng conversion).
 * ProjectionTest covers the underlying ProjectionUtils math.
 */
@RunWith(RobolectricTestRunner::class)
class MapProjectionTest {
    companion object {
        private const val DELTA = 0.5
    }

    private val center = LatLng(0.0, 0.0)

    @Test
    fun testCenterMapsToViewCenter() {
        val projection = Projection(center, 2f, 512, 512, 0f, 0f)

        assertEquals(Point(256, 256), projection.toScreenLocation(center))

        val latLng = projection.fromScreenLocation(Point(256, 256))
        assertEquals(0.0, latLng.latitude, DELTA)
        assertEquals(0.0, latLng.longitude, DELTA)
    }

    @Test
    fun testRoundTrip() {
        val projection = Projection(LatLng(51.4661, 7.2491), 12f, 1080, 1920, 0f, 0f)
        val location = LatLng(51.47, 7.26)

        val point = projection.toScreenLocation(location)
        val result = projection.fromScreenLocation(point)

        assertEquals(location.latitude, result.latitude, 0.001)
        assertEquals(location.longitude, result.longitude, 0.001)
    }

    @Test
    fun testEastIsRightAndNorthIsUp() {
        val projection = Projection(center, 2f, 512, 512, 0f, 0f)

        val east = projection.toScreenLocation(LatLng(0.0, 45.0))
        val north = projection.toScreenLocation(LatLng(45.0, 0.0))

        assertTrue(east.x > 256)
        assertEquals(256, east.y)
        assertTrue(north.y < 256)
        assertEquals(256, north.x)
    }

    @Test
    fun testPanOffsetMovesTheMap() {
        val projection = Projection(center, 2f, 512, 512, 100f, 50f)

        assertEquals(Point(156, 206), projection.toScreenLocation(center))
    }

    @Test
    fun testPaddingShiftsLogicalCenter() {
        val projection =
            Projection(center, 2f, 512, 512, 0f, 0f, paddingLeft = 0, paddingTop = 100, paddingRight = 0, paddingBottom = 0)

        // The logical center moves down by half the top padding
        assertEquals(Point(256, 306), projection.toScreenLocation(center))
    }

    @Test
    fun testVisibleRegion() {
        // At zoom 2 the world is 1024 pixels wide, so a 512 pixel view shows half of it
        val region = Projection(center, 2f, 512, 512, 0f, 0f).getVisibleRegion()

        assertEquals(-90.0, region.farLeft.longitude, DELTA)
        assertEquals(90.0, region.farRight.longitude, DELTA)
        assertEquals(region.farLeft.latitude, region.farRight.latitude, DELTA)
        assertEquals(region.nearLeft.latitude, region.nearRight.latitude, DELTA)
        assertTrue(region.farLeft.latitude > region.nearLeft.latitude)
        assertTrue(region.latLngBounds.contains(center))
    }

    @Test
    fun testVisibleRegionWithPadding() {
        val region =
            Projection(center, 2f, 512, 512, 0f, 0f, paddingLeft = 128, paddingTop = 0, paddingRight = 0, paddingBottom = 0)
                .getVisibleRegion()

        // Only 384 of 512 pixels remain, centered on the camera target: 135 instead of 180 degrees
        assertEquals(-67.5, region.farLeft.longitude, DELTA)
        assertEquals(67.5, region.farRight.longitude, DELTA)
    }

    @Test
    fun testControllerCreatesMatchingProjection() {
        val controller = MapController(RuntimeEnvironment.getApplication())
        try {
            controller.setViewSize(512, 512)
            controller.setCenter(center)
            controller.setZoom(2f)

            assertEquals(Point(256, 256), controller.createProjection().toScreenLocation(center))
        } finally {
            controller.onDestroy()
        }
    }
}
