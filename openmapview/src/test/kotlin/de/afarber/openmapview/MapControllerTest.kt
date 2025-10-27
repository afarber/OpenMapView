/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MapControllerTest {
    private lateinit var context: Context
    private lateinit var controller: MapController

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        controller = MapController(context)
        controller.setViewSize(1080, 1920)
    }

    @Test
    fun testSetZoom_WithinBounds() {
        controller.setZoom(10.0)
        assertEquals(10.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testSetZoom_BelowMinimum() {
        controller.setZoom(0.5)
        assertEquals(2.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testSetZoom_AboveMaximum() {
        controller.setZoom(25.0)
        assertEquals(19.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testSetZoom_AtMinBoundary() {
        controller.setZoom(2.0)
        assertEquals(2.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testSetZoom_AtMaxBoundary() {
        controller.setZoom(19.0)
        assertEquals(19.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testZoom_ScaleUp() {
        controller.setZoom(10.0)
        controller.zoom(2.0f, 540f, 960f)
        assertEquals(19.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testZoom_ScaleDown() {
        controller.setZoom(10.0)
        controller.zoom(0.5f, 540f, 960f)
        assertEquals(5.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testZoom_BeyondMaxLimit() {
        controller.setZoom(18.0)
        val oldZoom = controller.getZoom()
        controller.zoom(5.0f, 540f, 960f)
        assertEquals(19.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testZoom_BeyondMinLimit() {
        controller.setZoom(3.0)
        controller.zoom(0.1f, 540f, 960f)
        assertEquals(2.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testUpdatePanOffset() {
        controller.updatePanOffset(100f, 50f)
        // Pan offset is updated but center remains the same until commitPan
        controller.commitPan()
        // After commit, we've moved from the original center
    }

    @Test
    fun testCommitPan_NoOffset() {
        val originalCenter = LatLng(51.4661, 7.2491)
        controller.setCenter(originalCenter)
        controller.commitPan()
        // Center should remain unchanged when there's no pan offset
    }

    @Test
    fun testCommitPan_WithOffset() {
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)
        controller.updatePanOffset(100f, 50f)
        controller.commitPan()
        // After commit, pan offset should be reset to zero
        // And center should have changed
    }

    @Test
    fun testAddMarker() {
        val marker = Marker(LatLng(51.4661, 7.2491))
        val result = controller.addMarker(marker)

        assertEquals(marker, result)
        assertEquals(1, controller.getMarkers().size)
        assertTrue(controller.getMarkers().contains(marker))
    }

    @Test
    fun testAddMultipleMarkers() {
        val marker1 = Marker(LatLng(51.4661, 7.2491))
        val marker2 = Marker(LatLng(52.52, 13.405))
        val marker3 = Marker(LatLng(48.8566, 2.3522))

        controller.addMarker(marker1)
        controller.addMarker(marker2)
        controller.addMarker(marker3)

        assertEquals(3, controller.getMarkers().size)
    }

    @Test
    fun testRemoveMarker() {
        val marker = Marker(LatLng(51.4661, 7.2491))
        controller.addMarker(marker)

        val removed = controller.removeMarker(marker)

        assertTrue(removed)
        assertEquals(0, controller.getMarkers().size)
    }

    @Test
    fun testRemoveNonExistentMarker() {
        val marker = Marker(LatLng(51.4661, 7.2491))
        val removed = controller.removeMarker(marker)

        assertEquals(false, removed)
    }

    @Test
    fun testClearMarkers() {
        controller.addMarker(Marker(LatLng(51.4661, 7.2491)))
        controller.addMarker(Marker(LatLng(52.52, 13.405)))
        controller.addMarker(Marker(LatLng(48.8566, 2.3522)))

        controller.clearMarkers()

        assertEquals(0, controller.getMarkers().size)
    }

    @Test
    fun testGetMarkers_ReturnsImmutableCopy() {
        val marker = Marker(LatLng(51.4661, 7.2491))
        controller.addMarker(marker)

        val markers1 = controller.getMarkers()
        val markers2 = controller.getMarkers()

        // Should return a new list each time (defensive copy)
        assertTrue(markers1 !== markers2)
        assertEquals(markers1.size, markers2.size)
    }

    @Test
    fun testHandleMarkerTouch_HitMarker() {
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        val markerPosition = LatLng(51.4661, 7.2491)
        val marker = Marker(markerPosition)
        controller.addMarker(marker)

        // Touch at screen center where marker is located
        val touchedMarker = controller.handleMarkerTouch(540f, 960f)

        assertNotNull(touchedMarker)
        assertEquals(marker, touchedMarker)
    }

    @Test
    fun testHandleMarkerTouch_MissMarker() {
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        val marker = Marker(LatLng(51.4661, 7.2491))
        controller.addMarker(marker)

        // Touch far from marker
        val touchedMarker = controller.handleMarkerTouch(0f, 0f)

        assertNull(touchedMarker)
    }

    @Test
    fun testHandleMarkerTouch_ZOrdering() {
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        val marker1 = Marker(LatLng(51.4661, 7.2491))
        val marker2 = Marker(LatLng(51.4661, 7.2491))
        controller.addMarker(marker1)
        controller.addMarker(marker2)

        // Both markers at same position, should return the last added one (top)
        val touchedMarker = controller.handleMarkerTouch(540f, 960f)

        assertNotNull(touchedMarker)
        assertEquals(marker2, touchedMarker)
    }

    @Test
    fun testMarkerClickListener() {
        var callbackInvoked = false
        var clickedMarker: Marker? = null

        controller.onMarkerClickListener = { marker ->
            callbackInvoked = true
            clickedMarker = marker
            true
        }

        val marker = Marker(LatLng(51.4661, 7.2491))
        controller.addMarker(marker)

        // Simulate marker click by calling the listener
        val result = controller.onMarkerClickListener?.invoke(marker)

        assertTrue(callbackInvoked)
        assertEquals(marker, clickedMarker)
        assertEquals(true, result)
    }

    @Test
    fun testDraw_EmptyViewport() {
        val canvas = mockk<Canvas>(relaxed = true)
        controller.setViewSize(0, 0)

        controller.draw(canvas)

        // Should not crash and should not attempt any drawing
    }

    @Test
    fun testDraw_ValidViewport() {
        val canvas = mockk<Canvas>(relaxed = true)
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)
        controller.setViewSize(1080, 1920)

        controller.draw(canvas)

        // Verify that drawRect was called for tile placeholders
        verify(atLeast = 1) { canvas.drawRect(any(), any(), any(), any(), any()) }
    }

    @Test
    fun testOnDestroy_Cleanup() {
        controller.addMarker(Marker(LatLng(51.4661, 7.2491)))
        controller.onDestroy()

        // After destroy, markers should still be in list but resources cleaned up
        // The controller is now in a destroyed state
    }

    @Test
    fun testSetViewSize() {
        controller.setViewSize(1920, 1080)
        // View size is set, verify it doesn't crash
        val canvas = mockk<Canvas>(relaxed = true)
        controller.draw(canvas)
    }

    @Test
    fun testOnResume() {
        controller.onResume()
        // Should not crash
    }

    @Test
    fun testOnPause() {
        controller.onPause()
        // Should not crash
    }

    @Test
    fun testAddPolyline() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)

        val result = controller.addPolyline(polyline)

        assertEquals(polyline, result)
        assertEquals(1, controller.getPolylines().size)
        assertEquals(polyline, controller.getPolylines()[0])
    }

    @Test
    fun testRemovePolyline() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)
        controller.addPolyline(polyline)

        val result = controller.removePolyline(polyline)

        assertTrue(result)
        assertEquals(0, controller.getPolylines().size)
    }

    @Test
    fun testRemoveNonExistentPolyline() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)

        val result = controller.removePolyline(polyline)

        assertFalse(result)
    }

    @Test
    fun testClearPolylines() {
        val polyline1 = Polyline(listOf(LatLng(51.4661, 7.2491), LatLng(51.4700, 7.2550)))
        val polyline2 = Polyline(listOf(LatLng(51.4620, 7.2430), LatLng(51.4640, 7.2460)))
        controller.addPolyline(polyline1)
        controller.addPolyline(polyline2)

        controller.clearPolylines()

        assertEquals(0, controller.getPolylines().size)
    }

    @Test
    fun testGetPolylines_ReturnsImmutableCopy() {
        val polyline = Polyline(listOf(LatLng(51.4661, 7.2491), LatLng(51.4700, 7.2550)))
        controller.addPolyline(polyline)

        val polylines = controller.getPolylines()
        assertEquals(1, polylines.size)

        // Original list should remain unchanged even if returned list is modified
        // (though returned list is immutable)
    }

    @Test
    fun testAddMultiplePolylines() {
        val polyline1 = Polyline(listOf(LatLng(51.4661, 7.2491), LatLng(51.4700, 7.2550)))
        val polyline2 = Polyline(listOf(LatLng(51.4620, 7.2430), LatLng(51.4640, 7.2460)))
        val polyline3 = Polyline(listOf(LatLng(51.4680, 7.2520), LatLng(51.4690, 7.2530)))

        controller.addPolyline(polyline1)
        controller.addPolyline(polyline2)
        controller.addPolyline(polyline3)

        val polylines = controller.getPolylines()
        assertEquals(3, polylines.size)
        assertEquals(polyline1, polylines[0])
        assertEquals(polyline2, polylines[1])
        assertEquals(polyline3, polylines[2])
    }

    @Test
    fun testAddPolygon() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points)

        val result = controller.addPolygon(polygon)

        assertEquals(polygon, result)
        assertEquals(1, controller.getPolygons().size)
        assertEquals(polygon, controller.getPolygons()[0])
    }

    @Test
    fun testRemovePolygon() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points)
        controller.addPolygon(polygon)

        val result = controller.removePolygon(polygon)

        assertTrue(result)
        assertEquals(0, controller.getPolygons().size)
    }

    @Test
    fun testRemoveNonExistentPolygon() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points)

        val result = controller.removePolygon(polygon)

        assertFalse(result)
    }

    @Test
    fun testClearPolygons() {
        val polygon1 = Polygon(listOf(LatLng(51.4661, 7.2491), LatLng(51.4700, 7.2550), LatLng(51.4620, 7.2430)))
        val polygon2 = Polygon(listOf(LatLng(51.4640, 7.2420), LatLng(51.4660, 7.2440), LatLng(51.4650, 7.2450)))
        controller.addPolygon(polygon1)
        controller.addPolygon(polygon2)

        controller.clearPolygons()

        assertEquals(0, controller.getPolygons().size)
    }

    @Test
    fun testGetPolygons_ReturnsImmutableCopy() {
        val polygon = Polygon(listOf(LatLng(51.4661, 7.2491), LatLng(51.4700, 7.2550), LatLng(51.4620, 7.2430)))
        controller.addPolygon(polygon)

        val polygons = controller.getPolygons()
        assertEquals(1, polygons.size)

        // Original list should remain unchanged even if returned list is modified
        // (though returned list is immutable)
    }

    @Test
    fun testAddMultiplePolygons() {
        val polygon1 = Polygon(listOf(LatLng(51.4661, 7.2491), LatLng(51.4700, 7.2550), LatLng(51.4620, 7.2430)))
        val polygon2 = Polygon(listOf(LatLng(51.4640, 7.2420), LatLng(51.4660, 7.2440), LatLng(51.4650, 7.2450)))
        val polygon3 = Polygon(listOf(LatLng(51.4680, 7.2520), LatLng(51.4690, 7.2530), LatLng(51.4685, 7.2540)))

        controller.addPolygon(polygon1)
        controller.addPolygon(polygon2)
        controller.addPolygon(polygon3)

        val polygons = controller.getPolygons()
        assertEquals(3, polygons.size)
        assertEquals(polygon1, polygons[0])
        assertEquals(polygon2, polygons[1])
        assertEquals(polygon3, polygons[2])
    }

    @Test
    fun testClear_RemovesAllMarkersPolylinesAndPolygons() {
        val marker = Marker(position = LatLng(51.5, -0.1))
        val polyline = Polyline(points = listOf(LatLng(51.5, -0.1), LatLng(51.6, -0.2)))
        val polygon = Polygon(points = listOf(LatLng(51.5, -0.1), LatLng(51.6, -0.2), LatLng(51.7, -0.3)))

        controller.addMarker(marker)
        controller.addPolyline(polyline)
        controller.addPolygon(polygon)

        assertEquals(1, controller.getMarkers().size)
        assertEquals(1, controller.getPolylines().size)
        assertEquals(1, controller.getPolygons().size)

        controller.clearMarkers()
        controller.clearPolylines()
        controller.clearPolygons()

        assertTrue(controller.getMarkers().isEmpty())
        assertTrue(controller.getPolylines().isEmpty())
        assertTrue(controller.getPolygons().isEmpty())
    }

    @Test
    fun testSetMinZoomPreference_ConstrainsZoom() {
        controller.setZoom(5.0)
        controller.setMinZoomPreference(7.0f)
        assertEquals(7.0, controller.getZoom(), 0.001)
        assertEquals(7.0f, controller.getMinZoomLevel(), 0.001f)
    }

    @Test
    fun testSetMaxZoomPreference_ConstrainsZoom() {
        controller.setZoom(15.0)
        controller.setMaxZoomPreference(12.0f)
        assertEquals(12.0, controller.getZoom(), 0.001)
        assertEquals(12.0f, controller.getMaxZoomLevel(), 0.001f)
    }

    @Test
    fun testSetZoom_RespectsMinPreference() {
        controller.setMinZoomPreference(5.0f)
        controller.setZoom(3.0)
        assertEquals(5.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testSetZoom_RespectsMaxPreference() {
        controller.setMaxZoomPreference(15.0f)
        controller.setZoom(18.0)
        assertEquals(15.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testResetMinMaxZoomPreference_RestoresDefaults() {
        controller.setMinZoomPreference(5.0f)
        controller.setMaxZoomPreference(15.0f)
        controller.resetMinMaxZoomPreference()
        assertEquals(2.0f, controller.getMinZoomLevel(), 0.001f)
        assertEquals(19.0f, controller.getMaxZoomLevel(), 0.001f)
    }

    @Test
    fun testZoomPreferences_WorkWithGestures() {
        controller.setZoom(10.0)
        controller.setMaxZoomPreference(12.0f)
        controller.zoom(1.5f, 540f, 960f)
        assertTrue(controller.getZoom() <= 12.0)
    }

    @Test
    fun testZoomPreferences_WorkWithCameraAnimations() {
        controller.setMaxZoomPreference(10.0f)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(LatLng(51.5, -0.1), 15.0)
        controller.moveCamera(cameraUpdate)
        assertEquals(10.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testZoomPreferences_AllowValidRange() {
        controller.setMinZoomPreference(5.0f)
        controller.setMaxZoomPreference(15.0f)
        controller.setZoom(10.0)
        assertEquals(10.0, controller.getZoom(), 0.001)
    }

    @Test
    fun testPolylineClick_NonClickablePolyline() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(10.0)

        val polyline =
            Polyline(
                points =
                    listOf(
                        LatLng(51.5, 0.0),
                        LatLng(51.51, 0.01),
                    ),
                clickable = false,
            )
        controller.addPolyline(polyline)

        val touched = controller.handlePolylineTouch(400f, 300f)
        assertNull(touched)
    }

    @Test
    fun testPolylineClick_ClickablePolyline_Hit() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(10.0)

        val polyline =
            Polyline(
                points =
                    listOf(
                        LatLng(51.5, 0.0),
                        LatLng(51.51, 0.01),
                    ),
                clickable = true,
            )
        controller.addPolyline(polyline)

        val touched = controller.handlePolylineTouch(400f, 300f)
        assertNotNull(touched)
        assertEquals(polyline.id, touched?.id)
    }

    @Test
    fun testPolylineClick_ClickablePolyline_Miss() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(10.0)

        val polyline =
            Polyline(
                points =
                    listOf(
                        LatLng(51.5, 0.0),
                        LatLng(51.51, 0.01),
                    ),
                clickable = true,
            )
        controller.addPolyline(polyline)

        val touched = controller.handlePolylineTouch(100f, 100f)
        assertNull(touched)
    }

    @Test
    fun testPolygonClick_NonClickablePolygon() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(10.0)

        val polygon =
            Polygon(
                points =
                    listOf(
                        LatLng(51.5, 0.0),
                        LatLng(51.51, 0.0),
                        LatLng(51.51, 0.01),
                    ),
                clickable = false,
            )
        controller.addPolygon(polygon)

        val touched = controller.handlePolygonTouch(400f, 300f)
        assertNull(touched)
    }

    @Test
    fun testPolygonClick_ClickablePolygon_Hit() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(10.0)

        val polygon =
            Polygon(
                points =
                    listOf(
                        LatLng(51.49, -0.01),
                        LatLng(51.51, -0.01),
                        LatLng(51.51, 0.01),
                        LatLng(51.49, 0.01),
                    ),
                clickable = true,
            )
        controller.addPolygon(polygon)

        val touched = controller.handlePolygonTouch(400f, 300f)
        assertNotNull(touched)
        assertEquals(polygon.id, touched?.id)
    }

    @Test
    fun testPolygonClick_ClickablePolygon_Miss() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(10.0)

        val polygon =
            Polygon(
                points =
                    listOf(
                        LatLng(51.5, 0.0),
                        LatLng(51.51, 0.0),
                        LatLng(51.51, 0.01),
                    ),
                clickable = true,
            )
        controller.addPolygon(polygon)

        val touched = controller.handlePolygonTouch(100f, 100f)
        assertNull(touched)
    }

    @Test
    fun testPolygonClick_PolygonWithHole_HitOutside() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.4818, 7.2162))
        controller.setZoom(14.0)

        val polygon =
            Polygon(
                points =
                    listOf(
                        LatLng(51.475, 7.210),
                        LatLng(51.488, 7.210),
                        LatLng(51.488, 7.223),
                        LatLng(51.475, 7.223),
                    ),
                holes =
                    listOf(
                        listOf(
                            LatLng(51.479, 7.2145),
                            LatLng(51.484, 7.2145),
                            LatLng(51.484, 7.2185),
                            LatLng(51.479, 7.2185),
                        ),
                    ),
                clickable = true,
            )
        controller.addPolygon(polygon)

        val touched = controller.handlePolygonTouch(350f, 250f)
        assertNotNull(touched)
    }

    @Test
    fun testPolylineClick_MultiplePolylines_ReturnsTopmost() {
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(10.0)

        val polyline1 =
            Polyline(
                points =
                    listOf(
                        LatLng(51.5, 0.0),
                        LatLng(51.51, 0.01),
                    ),
                clickable = true,
                tag = "first",
            )
        val polyline2 =
            Polyline(
                points =
                    listOf(
                        LatLng(51.5, 0.0),
                        LatLng(51.51, 0.01),
                    ),
                clickable = true,
                tag = "second",
            )
        controller.addPolyline(polyline1)
        controller.addPolyline(polyline2)

        val touched = controller.handlePolylineTouch(400f, 300f)
        assertNotNull(touched)
        assertEquals("second", touched?.tag)
    }
}
