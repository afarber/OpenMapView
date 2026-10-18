/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Checks that polylines, polygons and circles are live handles: their setters redraw the map
 * that shows them, and remove() takes them off it.
 */
@RunWith(RobolectricTestRunner::class)
class ShapeHandleTest {
    private lateinit var mapView: OpenMapView
    private val a = LatLng(51.47, 7.20)
    private val b = LatLng(51.48, 7.22)
    private val c = LatLng(51.46, 7.23)

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.layout(0, 0, 400, 400)
    }

    // Draws the map and forgets that it was invalidated before
    private fun drawn() {
        mapView.draw(Canvas(createBitmap(400, 400)))
        shadowOf(mapView).clearWasInvalidated()
    }

    @Test
    fun testSetters_RedrawTheMap() {
        val polyline = mapView.addPolyline(Polyline(listOf(a, b)))
        val polygon = mapView.addPolygon(Polygon(listOf(a, b, c)))
        val circle = mapView.addCircle(Circle(a, 100f))
        val marker = mapView.addMarker(Marker(a, title = "A"))
        val changes =
            listOf<() -> Unit>(
                { marker.position = b },
                { marker.title = "B" },
                { marker.icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE) },
                { marker.setAnchor(0.5f, 0.5f) },
                { marker.alpha = 0.5f },
                { marker.visible = false },
                { polyline.strokeWidth = 20f },
                { polyline.points = listOf(a, b, c) },
                { polygon.fillColor = Color.Red },
                { polygon.holes = listOf(listOf(a, b, c)) },
                { circle.radius = 200f },
                { circle.center = b },
                { circle.visible = false },
            )

        for (change in changes) {
            drawn()
            change()
            assertTrue(shadowOf(mapView).wasInvalidated())
        }
        assertEquals(20f, mapView.getPolylines()[0].strokeWidth)
        assertEquals(200f, mapView.getCircles()[0].radius)
        assertEquals(b, mapView.getMarkers()[0].position)
        assertEquals(Pair(0.5f, 0.5f), mapView.getMarkers()[0].anchor)
    }

    @Test
    fun testMarkerRemove_TakesItOffTheMap() {
        val marker = mapView.addMarker(Marker(a, title = "A"))
        marker.showInfoWindow()

        marker.remove()

        assertTrue(mapView.getMarkers().isEmpty())
        assertFalse(marker.isInfoWindowShown)
        // A removed marker no longer redraws the map
        drawn()
        marker.title = "B"
        assertFalse(shadowOf(mapView).wasInvalidated())
    }

    @Test
    fun testMarkerAlphaOutOfRange_Throws() {
        val marker = Marker(a)
        assertThrows(IllegalArgumentException::class.java) { marker.alpha = 1.5f }
        assertEquals(1f, marker.alpha)
    }

    @Test
    fun testSameValue_DoesNotRedraw() {
        val polyline = mapView.addPolyline(Polyline(listOf(a, b), strokeWidth = 12f))
        drawn()

        polyline.strokeWidth = 12f

        assertFalse(shadowOf(mapView).wasInvalidated())
    }

    @Test
    fun testSetters_CheckValues() {
        val polyline = Polyline(listOf(a, b))
        val polygon = Polygon(listOf(a, b, c))
        val circle = Circle(a, 100f)

        assertThrows(IllegalArgumentException::class.java) { polyline.points = listOf(a) }
        assertThrows(IllegalArgumentException::class.java) { polygon.points = listOf(a, b) }
        assertThrows(IllegalArgumentException::class.java) { polygon.holes = listOf(listOf(a, b)) }
        assertThrows(IllegalArgumentException::class.java) { circle.radius = 0f }
        // A rejected value leaves the old one
        assertEquals(listOf(a, b), polyline.points)
        assertEquals(100f, circle.radius)
    }

    @Test
    fun testRemove_TakesShapesOffTheMap() {
        val polyline = mapView.addPolyline(Polyline(listOf(a, b)))
        val polygon = mapView.addPolygon(Polygon(listOf(a, b, c)))
        val circle = mapView.addCircle(Circle(a, 100f))

        polyline.remove()
        polygon.remove()
        circle.remove()

        assertTrue(mapView.getPolylines().isEmpty())
        assertTrue(mapView.getPolygons().isEmpty())
        assertTrue(mapView.getCircles().isEmpty())
        // A removed shape no longer redraws the map
        drawn()
        polyline.strokeWidth = 30f
        assertFalse(shadowOf(mapView).wasInvalidated())
    }

    @Test
    fun testClear_UnlinksShapes() {
        val circle = mapView.addCircle(Circle(a, 100f))
        val overlay = mapView.addGroundOverlay(GroundOverlay(BitmapDescriptorFactory.defaultMarker(), position = a, width = 500f))

        mapView.clear()

        assertNull(circle.mapView)
        assertTrue(mapView.getGroundOverlays().isEmpty())
        assertNull(overlay.mapView)
    }

    @Test
    fun testCopy_IsANewShapeWithChangedProperties() {
        val polyline = mapView.addPolyline(Polyline(listOf(a, b), strokeColor = Color.Blue, clickable = true))

        val copy = polyline.copy(strokeWidth = 25f)

        assertNotSame(polyline, copy)
        assertEquals(Color.Blue, copy.strokeColor)
        assertEquals(true, copy.clickable)
        assertEquals(25f, copy.strokeWidth)
        assertNull(copy.mapView)
        // Shapes are compared by identity, as before
        assertFalse(polyline == copy)
    }

    @Test
    fun testGeoJsonShapes_AreLive() {
        val result =
            mapView.addGeoJson(
                """{"type":"LineString","coordinates":[[7.20,51.47],[7.22,51.48]]}""",
            )
        val polyline = result.polylines[0]
        assertSame(mapView, polyline.mapView)
        drawn()

        polyline.strokeWidth = 15f

        assertTrue(shadowOf(mapView).wasInvalidated())
    }
}
