/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Checks that ground overlays are live handles: their setters and placement methods redraw
 * the map that shows them, and remove() takes them off it.
 */
@RunWith(RobolectricTestRunner::class)
class GroundOverlayHandleTest {
    private lateinit var mapView: OpenMapView
    private val center = LatLng(51.4661, 7.2491)
    private val image = BitmapDescriptorFactory.defaultMarker()
    private val area = LatLngBounds(LatLng(51.46, 7.24), LatLng(51.47, 7.26))

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.layout(0, 0, 400, 400)
    }

    private fun drawn() {
        mapView.draw(Canvas(createBitmap(400, 400)))
        shadowOf(mapView).clearWasInvalidated()
    }

    @Test
    fun testSetters_RedrawTheMap() {
        val overlay = mapView.addGroundOverlay(GroundOverlay(image, position = center, width = 500f))
        val changes =
            listOf<() -> Unit>(
                { overlay.bearing = 45f },
                { overlay.transparency = 0.5f },
                { overlay.zIndex = 2f },
                { overlay.visible = false },
                { overlay.setPosition(LatLng(51.47, 7.25)) },
                { overlay.setDimensions(800f) },
                { overlay.setPositionFromBounds(area) },
            )

        for (change in changes) {
            drawn()
            change()
            assertTrue(shadowOf(mapView).wasInvalidated())
        }
        assertEquals(45f, overlay.bearing)
        assertEquals(area, overlay.bounds)
        assertNull(overlay.position)
    }

    @Test
    fun testPlacement_SwitchesBetweenPositionAndBounds() {
        val overlay = GroundOverlay(image, bounds = area)

        overlay.setPosition(center)

        // Moved to the position, keeping the size of the bounds
        assertEquals(center, overlay.position)
        assertNull(overlay.bounds)
        assertEquals(1390f, overlay.width!!, 20f)
        assertEquals(1112f, overlay.height!!, 20f)

        overlay.setDimensions(300f, 200f)
        assertEquals(300f, overlay.width)
        assertEquals(200f, overlay.height)
        assertEquals(center, overlay.position)
    }

    @Test
    fun testInvalidValues_KeepTheOldOnes() {
        val overlay = GroundOverlay(image, position = center, width = 500f)

        assertThrows(IllegalArgumentException::class.java) { overlay.transparency = 2f }
        assertThrows(IllegalArgumentException::class.java) { overlay.setDimensions(-1f) }

        assertEquals(0f, overlay.transparency)
        assertEquals(500f, overlay.width)
    }

    @Test
    fun testRemove_TakesOverlayOffTheMap() {
        val overlay = mapView.addGroundOverlay(GroundOverlay(image, bounds = area))

        overlay.remove()

        assertTrue(mapView.getGroundOverlays().isEmpty())
        drawn()
        overlay.bearing = 10f
        assertFalse(shadowOf(mapView).wasInvalidated())
    }
}
