/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Checks that a touch hits what is drawn there: hidden markers and shapes are not hit, the
 * one with the highest zIndex is hit first, and geodesic shapes are hit along their curves.
 */
@RunWith(RobolectricTestRunner::class)
class HitTestingTest {
    private lateinit var controller: MapController

    private val center = LatLng(51.5, 0.0)
    private val bounds = LatLngBounds(LatLng(51.49, -0.01), LatLng(51.51, 0.01))
    private val line = listOf(LatLng(51.5, 0.0), LatLng(51.51, 0.01))
    private val square = listOf(LatLng(51.49, -0.01), LatLng(51.51, -0.01), LatLng(51.51, 0.01), LatLng(51.49, 0.01))

    // The map center is drawn in the middle of the 800x600 view
    private val centerX = 400f
    private val centerY = 300f

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(800, 600)
        controller.setCenter(center)
        controller.setZoom(10.0f)
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    @Test
    fun testHiddenMarker_IsNotHit() {
        val marker = controller.addMarker(Marker(center))
        // The icon stands above the position
        assertSame(marker, controller.handleMarkerTouch(centerX, centerY - 10f))

        marker.visible = false

        assertNull(controller.handleMarkerTouch(centerX, centerY - 10f))
    }

    @Test
    fun testHiddenShapes_AreNotHit() {
        controller.addPolyline(Polyline(points = line, clickable = true, visible = false))
        controller.addPolygon(Polygon(points = square, clickable = true, visible = false))
        controller.addCircle(Circle(center = center, radius = 500f, clickable = true, visible = false))
        controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds, clickable = true, visible = false))

        assertNull(controller.handlePolylineTouch(centerX, centerY))
        assertNull(controller.handlePolygonTouch(centerX, centerY))
        assertNull(controller.handleCircleTouch(centerX, centerY))
        assertNull(controller.handleGroundOverlayTouch(centerX, centerY))
    }

    @Test
    fun testHiddenShape_LetsTheOneBelowBeHit() {
        val below = controller.addCircle(Circle(center = center, radius = 500f, clickable = true))
        controller.addCircle(Circle(center = center, radius = 500f, clickable = true, visible = false))

        assertSame(below, controller.handleCircleTouch(centerX, centerY))
    }

    // Added first, but drawn on top because of its zIndex
    @Test
    fun testHighestZIndex_IsHitFirst() {
        val marker = controller.addMarker(Marker(center, zIndex = 1f))
        controller.addMarker(Marker(center))
        val polyline = controller.addPolyline(Polyline(points = line, clickable = true, zIndex = 1f))
        controller.addPolyline(Polyline(points = line, clickable = true))
        val polygon = controller.addPolygon(Polygon(points = square, clickable = true, zIndex = 1f))
        controller.addPolygon(Polygon(points = square, clickable = true))
        val circle = controller.addCircle(Circle(center = center, radius = 500f, clickable = true, zIndex = 1f))
        controller.addCircle(Circle(center = center, radius = 500f, clickable = true))
        val overlay =
            controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds, clickable = true, zIndex = 1f))
        controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds, clickable = true))

        assertSame(marker, controller.handleMarkerTouch(centerX, centerY - 10f))
        assertSame(polyline, controller.handlePolylineTouch(centerX, centerY))
        assertSame(polygon, controller.handlePolygonTouch(centerX, centerY))
        assertSame(circle, controller.handleCircleTouch(centerX, centerY))
        assertSame(overlay, controller.handleGroundOverlayTouch(centerX, centerY))
    }

    @Test
    fun testSameZIndex_LastAddedIsHitFirst() {
        controller.addPolygon(Polygon(points = square, clickable = true, zIndex = 1f))
        val top = controller.addPolygon(Polygon(points = square, clickable = true, zIndex = 1f))

        assertSame(top, controller.handlePolygonTouch(centerX, centerY))
    }

    // Between two points on latitude 50, 120 degrees apart, the great circle runs up to
    // latitude 67, far north of the straight line on the map
    @Test
    fun testGeodesicPolyline_IsHitAlongTheGreatCircle() {
        val west = LatLng(50.0, -60.0)
        val east = LatLng(50.0, 60.0)
        controller.setCenter(LatLng(50.0, 0.0))
        controller.setZoom(2.0f)
        val polyline = controller.addPolyline(Polyline(points = listOf(west, east), geodesic = true, clickable = true))
        val onCurve = controller.createProjection().toScreenLocation(SphericalUtil.interpolate(west, east, 0.5))

        assertSame(polyline, controller.handlePolylineTouch(onCurve.x.toFloat(), onCurve.y.toFloat()))
        // The straight line between the end points passes through the map center
        assertNull(controller.handlePolylineTouch(centerX, centerY))
    }

    // The edge closing the outline, from the last point back to the first, is a great
    // circle like the others
    @Test
    fun testGeodesicPolygon_IsHitInsideItsCurvedClosingEdge() {
        controller.setCenter(LatLng(50.0, 0.0))
        controller.setZoom(2.0f)
        val points = listOf(LatLng(50.0, 60.0), LatLng(30.0, 0.0), LatLng(50.0, -60.0))
        val polygon = controller.addPolygon(Polygon(points = points, geodesic = true, clickable = true))
        val straight = controller.addPolygon(Polygon(points = points, clickable = true, zIndex = -1f))
        // North of latitude 50, where the outline ends without the curve
        val inside = controller.createProjection().toScreenLocation(LatLng(60.0, 0.0))

        assertSame(polygon, controller.handlePolygonTouch(inside.x.toFloat(), inside.y.toFloat()))
        polygon.visible = false
        assertNull(controller.handlePolygonTouch(inside.x.toFloat(), inside.y.toFloat()))
        assertSame(straight, controller.handlePolygonTouch(centerX, centerY + 20f))
    }

    private fun image(): BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888))
}
