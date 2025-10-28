/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MapControllerInstrumentationTest {
    private lateinit var controller: MapController

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        controller = MapController(context)
        controller.setViewSize(1080, 1920)
    }

    @Test
    fun testDraw_WithRealCanvas() {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        controller.draw(canvas)

        // Verify that drawing completed without crashes
        assertNotNull(bitmap)
        assertTrue(bitmap.width == 1080)
        assertTrue(bitmap.height == 1920)
    }

    @Test
    fun testDraw_WithMarkers() {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        // Add markers at different positions
        controller.addMarker(Marker(LatLng(51.4661, 7.2491)))
        controller.addMarker(Marker(LatLng(51.47, 7.25)))
        controller.addMarker(Marker(LatLng(51.46, 7.26)))

        controller.draw(canvas)

        // Verify that drawing with markers completed without crashes
        assertNotNull(bitmap)
    }

    @Test
    fun testDraw_AfterZoom() {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(10.0)
        controller.draw(canvas)

        // Zoom in
        controller.zoom(2.0f, 540f, 960f)
        controller.draw(canvas)

        // Zoom out
        controller.zoom(0.5f, 540f, 960f)
        controller.draw(canvas)

        assertNotNull(bitmap)
    }

    @Test
    fun testDraw_AfterPan() {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        // Pan the map
        controller.updatePanOffset(100f, 100f)
        controller.draw(canvas)

        controller.commitPan()
        controller.draw(canvas)

        assertNotNull(bitmap)
    }

    @Test
    fun testMarkerIcon_Creation() {
        // Test that default marker icon is created without issues
        val marker = Marker(LatLng(51.4661, 7.2491))
        controller.addMarker(marker)

        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        controller.draw(canvas)

        assertNotNull(bitmap)
    }

    @Test
    fun testMarkerIcon_CustomColor() {
        // Test custom colored marker icons
        val redMarker =
            Marker(
                LatLng(51.4661, 7.2491),
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
            )
        val blueMarker =
            Marker(
                LatLng(51.47, 7.25),
                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
            )

        controller.addMarker(redMarker)
        controller.addMarker(blueMarker)

        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        controller.draw(canvas)

        assertNotNull(bitmap)
    }

    @Test
    fun testLifecycle_OnDestroy() {
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)

        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        controller.draw(canvas)

        // Cleanup
        controller.onDestroy()

        // After destroy, controller should not crash
        // but may not function correctly
    }
}
