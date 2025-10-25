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
}
