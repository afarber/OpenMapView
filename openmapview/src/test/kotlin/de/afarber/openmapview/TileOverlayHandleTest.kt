/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.graphics.Canvas
import android.os.Looper
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Checks that tile overlays are live handles: their setters redraw the map that shows them,
 * and remove() takes them off it.
 */
@RunWith(RobolectricTestRunner::class)
class TileOverlayHandleTest {
    private lateinit var mapView: OpenMapView

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
        val overlay = mapView.addTileOverlay(TileOverlayOptions().tileProvider(PredefinedTileProviders.openSeaMap()))

        for (change in listOf<() -> Unit>({ overlay.transparency = 0.5f }, { overlay.zIndex = -1f }, { overlay.visible = false })) {
            drawn()
            change()
            assertTrue(shadowOf(mapView).wasInvalidated())
        }
        assertEquals(0.5f, mapView.getTileOverlays()[0].transparency)
        assertThrows(IllegalArgumentException::class.java) { overlay.transparency = 2f }
        assertEquals(0.5f, overlay.transparency)
    }

    @Test
    fun testRemove_TakesOverlayOffTheMap() {
        val overlay = mapView.addTileOverlay(TileOverlay(PredefinedTileProviders.openSeaMap()))

        overlay.remove()

        assertTrue(mapView.getTileOverlays().isEmpty())
        assertNull(overlay.mapView)
        drawn()
        overlay.transparency = 0.3f
        assertFalse(shadowOf(mapView).wasInvalidated())
    }

    @Test
    fun testClearTileCache_RedrawsTheMap() {
        val overlay = mapView.addTileOverlay(TileOverlay(PredefinedTileProviders.openSeaMap()))
        drawn()

        overlay.clearTileCache()

        assertTrue(shadowOf(mapView).wasInvalidated())
    }

    @Test
    fun testSlowOverlay_DoesNotBlockOtherOverlays() {
        mapView.setMapType(MapType.NONE)
        mapView.setCenter(LatLng(51.4661, 7.2491))
        mapView.setZoom(13f)
        // A server that never answers holds every download it gets
        val release = CountDownLatch(1)
        val slow =
            TileProvider { _, _, _ ->
                release.await()
                null
            }
        val fastCalls = AtomicInteger()
        val fast =
            TileProvider { _, _, _ ->
                fastCalls.incrementAndGet()
                null
            }
        mapView.addTileOverlay(TileOverlay(slow))
        mapView.addTileOverlay(TileOverlay(fast))

        try {
            drawn()
            val deadline = System.nanoTime() + 2_000_000_000L
            while (fastCalls.get() == 0 && System.nanoTime() < deadline) {
                Thread.sleep(20)
                shadowOf(Looper.getMainLooper()).idle()
            }

            // The other overlay still gets its tiles requested
            assertTrue(fastCalls.get() > 0)
        } finally {
            release.countDown()
        }
    }
}
