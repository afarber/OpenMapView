/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for circles, ground overlays, tile overlays and info windows in [MapController],
 * and for drawing all shape types onto a canvas.
 */
@RunWith(RobolectricTestRunner::class)
class MapControllerOverlaysTest {
    private lateinit var controller: MapController

    private val center = LatLng(51.5, 0.0)
    private val bounds = LatLngBounds(LatLng(51.49, -0.01), LatLng(51.51, 0.01))

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
    fun testAddRemoveAndClearCircles() {
        val circle1 = controller.addCircle(Circle(center = center, radius = 100f))
        val circle2 = controller.addCircle(Circle(center = center, radius = 200f))
        assertEquals(listOf(circle1, circle2), controller.getCircles())

        assertTrue(controller.removeCircle(circle1))
        assertFalse(controller.removeCircle(circle1))
        assertEquals(listOf(circle2), controller.getCircles())

        controller.clearCircles()
        assertTrue(controller.getCircles().isEmpty())
    }

    @Test
    fun testHandleCircleTouch() {
        val circle = controller.addCircle(Circle(center = center, radius = 500f, clickable = true))

        assertSame(circle, controller.handleCircleTouch(centerX, centerY))
        assertNull(controller.handleCircleTouch(10f, 10f))
    }

    @Test
    fun testHandleCircleTouch_IgnoresNonClickable() {
        controller.addCircle(Circle(center = center, radius = 500f))

        assertNull(controller.handleCircleTouch(centerX, centerY))
    }

    @Test
    fun testHandleCircleTouch_ReturnsTopmost() {
        controller.addCircle(Circle(center = center, radius = 500f, clickable = true))
        val top = controller.addCircle(Circle(center = center, radius = 500f, clickable = true))

        assertSame(top, controller.handleCircleTouch(centerX, centerY))
    }

    @Test
    fun testAddRemoveAndClearGroundOverlays() {
        val overlay1 = controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds))
        val overlay2 = controller.addGroundOverlay(GroundOverlay(image = image(), position = center, width = 1000f))
        assertEquals(listOf(overlay1, overlay2), controller.getGroundOverlays())

        assertTrue(controller.removeGroundOverlay(overlay1))
        assertFalse(controller.removeGroundOverlay(overlay1))
        assertEquals(listOf(overlay2), controller.getGroundOverlays())

        controller.clearGroundOverlays()
        assertTrue(controller.getGroundOverlays().isEmpty())
    }

    @Test
    fun testHandleGroundOverlayTouch_Bounds() {
        val overlay = controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds, clickable = true))

        assertSame(overlay, controller.handleGroundOverlayTouch(centerX, centerY))
        assertNull(controller.handleGroundOverlayTouch(10f, 10f))
    }

    @Test
    fun testHandleGroundOverlayTouch_RotatedBounds() {
        val overlay =
            controller.addGroundOverlay(
                GroundOverlay(image = image(), bounds = bounds, bearing = 45f, clickable = true),
            )

        assertSame(overlay, controller.handleGroundOverlayTouch(centerX, centerY))
        assertNull(controller.handleGroundOverlayTouch(10f, 10f))
    }

    @Test
    fun testHandleGroundOverlayTouch_Position() {
        val overlay =
            controller.addGroundOverlay(
                GroundOverlay(image = image(), position = center, width = 1000f, height = 1000f, clickable = true),
            )

        assertSame(overlay, controller.handleGroundOverlayTouch(centerX, centerY))
        assertNull(controller.handleGroundOverlayTouch(10f, 10f))
    }

    @Test
    fun testHandleGroundOverlayTouch_RotatedPosition() {
        val overlay =
            controller.addGroundOverlay(
                GroundOverlay(image = image(), position = center, width = 1000f, bearing = 30f, clickable = true),
            )

        assertSame(overlay, controller.handleGroundOverlayTouch(centerX, centerY))
    }

    @Test
    fun testHandleGroundOverlayTouch_BottomCenterAnchorAtPosition() {
        val overlay =
            controller.addGroundOverlay(
                GroundOverlay(
                    image = image(),
                    position = center,
                    width = 5000f,
                    height = 5000f,
                    anchor = Pair(0.5f, 1f),
                    clickable = true,
                ),
            )

        // The image stands above the position, like a pin on its tip
        assertSame(overlay, controller.handleGroundOverlayTouch(centerX, centerY - 10f))
        assertNull(controller.handleGroundOverlayTouch(centerX, centerY + 10f))
    }

    @Test
    fun testHandleGroundOverlayTouch_TopLeftAnchorAtPosition() {
        val overlay =
            controller.addGroundOverlay(
                GroundOverlay(
                    image = image(),
                    position = center,
                    width = 5000f,
                    height = 5000f,
                    anchor = Pair(0f, 0f),
                    clickable = true,
                ),
            )

        assertSame(overlay, controller.handleGroundOverlayTouch(centerX + 10f, centerY + 10f))
        assertNull(controller.handleGroundOverlayTouch(centerX - 10f, centerY + 10f))
        assertNull(controller.handleGroundOverlayTouch(centerX + 10f, centerY - 10f))
    }

    @Test
    fun testHandleGroundOverlayTouch_RotatesAroundAnchorAtPosition() {
        val overlay =
            controller.addGroundOverlay(
                GroundOverlay(
                    image = image(),
                    position = center,
                    width = 5000f,
                    height = 5000f,
                    anchor = Pair(0.5f, 1f),
                    bearing = 180f,
                    clickable = true,
                ),
            )

        // Turned upside down around its tip, the image hangs below the position
        assertSame(overlay, controller.handleGroundOverlayTouch(centerX, centerY + 10f))
        assertNull(controller.handleGroundOverlayTouch(centerX, centerY - 10f))
    }

    @Test
    fun testHandleGroundOverlayTouch_IgnoresNonClickable() {
        controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds))

        assertNull(controller.handleGroundOverlayTouch(centerX, centerY))
    }

    @Test
    fun testAddRemoveAndClearTileOverlays() {
        val overlay1 = controller.addTileOverlay(TileOverlay(tileProvider = { _, _, _ -> null }))
        val overlay2 = controller.addTileOverlay(TileOverlay(tileProvider = { _, _, _ -> null }))
        assertEquals(listOf(overlay1, overlay2), controller.getTileOverlays())

        assertTrue(controller.removeTileOverlay(overlay1))
        assertFalse(controller.removeTileOverlay(overlay1))
        assertEquals(listOf(overlay2), controller.getTileOverlays())

        controller.clearTileOverlays()
        assertTrue(controller.getTileOverlays().isEmpty())
    }

    @Test
    fun testHandleInfoWindowTouch() {
        // A 20x40 icon anchored at its bottom center, so the info window starts 44 pixels above the marker
        // and is 46 pixels high at density 1
        val marker =
            controller.addMarker(
                Marker(position = center, title = "Title", snippet = "Snippet", icon = image()),
            )

        assertNull(controller.handleInfoWindowTouch(centerX, centerY - 70f))

        marker.showInfoWindow()
        assertSame(marker, controller.handleInfoWindowTouch(centerX, centerY - 70f))
        assertNull(controller.handleInfoWindowTouch(centerX, centerY + 100f))
    }

    @Test
    fun testDraw_AllShapeTypes() {
        val canvas = mockk<Canvas>(relaxed = true)
        val square =
            listOf(LatLng(51.49, -0.01), LatLng(51.51, -0.01), LatLng(51.51, 0.01), LatLng(51.49, 0.01))
        val hole = listOf(LatLng(51.495, -0.005), LatLng(51.505, -0.005), LatLng(51.505, 0.005))
        val dashed = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))

        controller.addPolygon(Polygon(points = square, holes = listOf(hole), strokePattern = dashed))
        controller.addPolygon(Polygon(points = square, geodesic = true, zIndex = 1f))
        controller.addPolygon(Polygon(points = square, visible = false))
        controller.addPolyline(Polyline(points = square, strokePattern = dashed))
        controller.addPolyline(Polyline(points = square, geodesic = true, zIndex = 2f))
        controller.addPolyline(
            Polyline(
                points = square,
                spans = listOf(StyleSpan(Color.Red, segments = 2), StyleSpan(Color.Blue)),
                zIndex = 2f,
            ),
        )
        controller.addPolyline(Polyline(points = square, visible = false))
        controller.addCircle(Circle(center = center, radius = 500f, strokePattern = dashed))
        controller.addCircle(Circle(center = center, radius = 500f, visible = false))
        controller.addMarker(Marker(position = center, alpha = 0.5f))
        controller.addMarker(Marker(position = center, visible = false))
        controller
            .addMarker(Marker(position = center, title = "Title", snippet = "Snippet", zIndex = 3f))
            .showInfoWindow()
        controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds, transparency = 0.5f))
        controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds, bearing = 45f))
        controller.addGroundOverlay(GroundOverlay(image = image(), position = center, width = 1000f))
        controller.addGroundOverlay(GroundOverlay(image = image(), position = center, width = 1000f, bearing = 90f))
        controller.addGroundOverlay(GroundOverlay(image = image(), bounds = bounds, visible = false))
        controller.addTileOverlay(TileOverlay(tileProvider = { _, _, _ -> null }, zIndex = -1f))
        controller.addTileOverlay(TileOverlay(tileProvider = { _, _, _ -> null }, zIndex = 1f, transparency = 0.5f))
        controller.addTileOverlay(TileOverlay(tileProvider = { _, _, _ -> null }, visible = false))

        controller.draw(canvas)

        verify(atLeast = 1) { canvas.drawPath(any(), any()) }
        verify(atLeast = 1) { canvas.drawCircle(any(), any(), any(), any()) }
        verify(atLeast = 1) { canvas.drawBitmap(any<Bitmap>(), any<Float>(), any<Float>(), any()) }
        // The info window frame with its pointer is one path, filled white
        verify { canvas.drawPath(any(), match { it.style == Paint.Style.FILL && it.color == android.graphics.Color.WHITE }) }
    }

    private fun image(): BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888))
}
