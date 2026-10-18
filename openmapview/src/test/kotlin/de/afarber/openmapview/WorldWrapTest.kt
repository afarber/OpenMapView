/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Checks that the world repeats horizontally: locations east of the antimeridian are drawn,
 * hit and projected next to those west of it.
 */
@RunWith(RobolectricTestRunner::class)
class WorldWrapTest {
    private lateinit var controller: MapController

    // At zoom 5 the world is 8192 px wide, 2 degrees are 45.5 px
    private val twoDegrees = 2 * 8192 / 360f

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(1080, 1920)
        controller.setZoom(5f)
        controller.setCenter(LatLng(0.0, 179.0))
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    @Test
    fun testToScreenLocation_EastOfAntimeridianIsRightOfCenter() {
        val point = controller.createProjection().toScreenLocation(LatLng(0.0, -179.0))

        assertEquals(540f + twoDegrees, point.x.toFloat(), 1f)
        assertEquals(960, point.y)
    }

    @Test
    fun testFromScreenLocation_RightOfAntimeridianIsNormalized() {
        val latLng = controller.screenToLatLng(540f + twoDegrees, 960f)

        assertEquals(-179.0, latLng.longitude, 0.01)
    }

    @Test
    fun testMarkerEastOfAntimeridian_IsHitWhereDrawn() {
        val marker = controller.addMarker(Marker(position = LatLng(0.0, -179.0)))

        // Just above the anchor at the bottom of the icon
        assertSame(marker, controller.handleMarkerTouch(540f + twoDegrees, 950f))
        assertNull(controller.handleMarkerTouch(540f - twoDegrees, 950f))
    }

    @Test
    fun testCircleEastOfAntimeridian_IsHitWhereDrawn() {
        val circle = controller.addCircle(Circle(center = LatLng(0.0, -179.0), radius = 1000f, clickable = true))

        assertSame(circle, controller.handleCircleTouch(540f + twoDegrees, 960f))
    }

    @Test
    fun testPolylineAcrossAntimeridian_TakesTheShortWay() {
        controller.setCenter(LatLng(0.0, 180.0))
        val polyline =
            controller.addPolyline(
                Polyline(points = listOf(LatLng(0.0, 179.0), LatLng(0.0, -179.0)), clickable = true),
            )

        // The line crosses the view center instead of going around the world
        assertSame(polyline, controller.handlePolylineTouch(540f, 960f))
    }

    @Test
    fun testPolygonAcrossAntimeridian_ContainsTheAntimeridian() {
        controller.setCenter(LatLng(0.0, 180.0))
        val square = listOf(LatLng(-1.0, 179.0), LatLng(-1.0, -179.0), LatLng(1.0, -179.0), LatLng(1.0, 179.0))
        val polygon = controller.addPolygon(Polygon(points = square, clickable = true))

        assertSame(polygon, controller.handlePolygonTouch(540f, 960f))
    }

    @Test
    fun testCommitPan_AcrossAntimeridian_KeepsLongitudeInRange() {
        // Two degrees east, from 179 to -179
        controller.updatePanOffset(-twoDegrees, 0f)
        controller.commitPan()

        assertEquals(-179.0, controller.getCenter().longitude, 0.01)
    }

    @Test
    fun testVisibleRegion_AcrossAntimeridian() {
        controller.setCenter(LatLng(0.0, 180.0))

        val bounds = controller.createProjection().getVisibleRegion().latLngBounds

        // The southwest corner is east of the northeast one
        assertTrue(bounds.southwest.longitude > 0)
        assertTrue(bounds.northeast.longitude < 0)
        assertTrue(bounds.contains(LatLng(0.0, -175.0)))
        assertTrue(bounds.contains(LatLng(0.0, 175.0)))
    }

    @Test
    fun testVisibleRegion_ViewWiderThanWorld_SpansAllLongitudes() {
        // At zoom 2 the world is 1024 px wide, less than the view
        controller.setZoom(2f)

        val bounds = controller.createProjection().getVisibleRegion().latLngBounds

        assertEquals(-180.0, bounds.southwest.longitude, 1e-9)
        assertEquals(180.0, bounds.northeast.longitude, 1e-9)
    }

    @Test
    fun testGroundOverlayAcrossAntimeridian_IsHitAtTheAntimeridian() {
        controller.setCenter(LatLng(0.0, 180.0))
        val image = BitmapDescriptorFactory.defaultMarker()
        val overlay =
            controller.addGroundOverlay(
                GroundOverlay(image, bounds = LatLngBounds(LatLng(-1.0, 179.0), LatLng(1.0, -179.0)), clickable = true),
            )

        assertSame(overlay, controller.handleGroundOverlayTouch(540f, 960f))
        assertSame(overlay, controller.handleGroundOverlayTouch(540f + twoDegrees / 2, 960f))
    }
}
