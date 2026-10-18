/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.Canvas
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for the rotation and the info window anchor of markers.
 *
 * The markers have a 20x40 icon anchored at its bottom center, at the center (400, 300)
 * of the 800x600 view: upright, the icon covers x 390..410 and y 260..300.
 */
@RunWith(RobolectricTestRunner::class)
class MarkerRotationTest {
    private lateinit var controller: MapController

    private val center = LatLng(51.5, 0.0)

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

    private fun image(): BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888))

    private fun addMarker(
        rotation: Float = 0f,
        infoWindowAnchor: Pair<Float, Float> = Pair(0.5f, 0f),
    ): Marker =
        controller.addMarker(
            Marker(
                position = center,
                title = "Title",
                snippet = "Snippet",
                icon = image(),
                infoWindowAnchor = infoWindowAnchor,
                rotation = rotation,
            ),
        )

    @Test
    fun testDefaults() {
        val marker = Marker(position = center)

        assertEquals(Pair(0.5f, 0f), marker.infoWindowAnchor)
        assertEquals(0f, marker.rotation)
        assertFalse(marker.isFlat)
        assertEquals(Pair(0.5f, 0f), MarkerOptions().position(center).build().infoWindowAnchor)
    }

    @Test
    fun testOptionsAndCopy() {
        val marker =
            MarkerOptions()
                .position(center)
                .infoWindowAnchor(1f, 0.5f)
                .rotation(45f)
                .flat(true)
                .build()
        val copy = marker.copy()

        assertEquals(Pair(1f, 0.5f), copy.infoWindowAnchor)
        assertEquals(45f, copy.rotation)
        assertEquals(true, copy.isFlat)

        marker.setInfoWindowAnchor(0f, 1f)
        assertEquals(Pair(0f, 1f), marker.infoWindowAnchor)
    }

    @Test
    fun testInfoWindow_DefaultAnchorKeepsPosition() {
        // The window covers y 202..248, 12 pixels above the icon top, as before info window anchors
        addMarker().showInfoWindow()

        assertNull(controller.handleInfoWindowTouch(400f, 252f))
        assertEquals("Title", controller.handleInfoWindowTouch(400f, 245f)?.title)
        assertEquals("Title", controller.handleInfoWindowTouch(400f, 205f)?.title)
    }

    @Test
    fun testInfoWindow_PointsAtInfoWindowAnchor() {
        // Anchored at the icon bottom, the window covers the icon
        val marker = addMarker(infoWindowAnchor = Pair(0.5f, 1f))
        marker.showInfoWindow()

        assertSame(marker, controller.handleInfoWindowTouch(400f, 280f))
        assertNull(controller.handleInfoWindowTouch(400f, 215f))
    }

    @Test
    fun testInfoWindow_AnchorTurnsWithMarker() {
        // Turned by 90 degrees, the icon top is right of the position, at (440, 300)
        val marker = addMarker(rotation = 90f)
        marker.showInfoWindow()

        assertSame(marker, controller.handleInfoWindowTouch(440f, 280f))
        assertNull(controller.handleInfoWindowTouch(400f, 215f))
    }

    @Test
    fun testTouch_RotatedIcon() {
        val upright = addMarker()
        assertSame(upright, controller.handleMarkerTouch(400f, 270f))
        assertNull(controller.handleMarkerTouch(430f, 300f))

        // Turned clockwise by 90 degrees around its bottom center, the icon covers x 400..440, y 290..310
        upright.rotation = 90f
        assertSame(upright, controller.handleMarkerTouch(430f, 300f))
        assertNull(controller.handleMarkerTouch(400f, 270f))
    }

    @Test
    fun testDraw_RotatesCanvasAroundPosition() {
        val canvas = mockk<Canvas>(relaxed = true)
        addMarker(rotation = 30f)

        controller.draw(canvas)

        verify { canvas.rotate(30f, 400f, 300f) }
    }
}
