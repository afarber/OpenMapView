/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.graphics.Point
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Checks the default behavior of a marker click: the listener runs first and can consume the
 * click; otherwise the info window opens and the camera moves to the marker.
 */
@RunWith(RobolectricTestRunner::class)
class MarkerClickTest {
    private lateinit var mapView: OpenMapView
    private lateinit var marker: Marker
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.setMapType(MapType.NONE)
        activity.setContentView(mapView)
        // The window's first layout pass, before the map gets its test size: it would
        // otherwise run while a tap waits for its confirmation and move the marker
        shadowOf(Looper.getMainLooper()).idle()
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(SIZE, View.MeasureSpec.EXACTLY),
        )
        mapView.layout(0, 0, SIZE, SIZE)
        mapView.setCenter(LatLng(51.4661, 7.2491))
        mapView.setZoom(14f)
        // Off the center, so moving the camera to it is a real move
        val position = mapView.getProjection().fromScreenLocation(Point(300, 300))
        marker = mapView.addMarker(Marker(position = position, title = "Bochum"))
        mapView.setOnCameraMoveStartedListener { events.add("started:$it") }
        mapView.setOnCameraIdleListener { events.add("idle") }
    }

    // Taps the marker icon, which is anchored at the bottom above its position
    private fun tapMarker() {
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
            val event = MotionEvent.obtain(0, time, action, 300f, 270f, 0)
            mapView.onTouchEvent(event)
            event.recycle()
        }
        confirmTap()
    }

    // The camera animation runs on wall clock time
    private fun waitForIdle() {
        val deadline = System.nanoTime() + 2_000_000_000L
        while (events.lastOrNull() != "idle" && System.nanoTime() < deadline) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
        }
    }

    @Test
    fun testClick_OpensInfoWindowAndMovesCamera() {
        var clicks = 0
        mapView.setOnMarkerClickListener {
            // The listener runs before the default behavior
            assertFalse(it.isInfoWindowShown)
            clicks++
            false
        }

        tapMarker()

        assertEquals(1, clicks)
        assertTrue(marker.isInfoWindowShown)
        assertEquals(listOf("started:${OnCameraMoveStartedListener.REASON_API_ANIMATION}"), events)
        waitForIdle()
        val target = mapView.getCameraPosition().target
        assertEquals(marker.position.latitude, target.latitude, 1e-6)
        assertEquals(marker.position.longitude, target.longitude, 1e-6)
    }

    @Test
    fun testClickConsumedByListener_KeepsInfoWindowAndCamera() {
        val before = mapView.getCameraPosition()
        mapView.setOnMarkerClickListener { true }

        tapMarker()

        assertFalse(marker.isInfoWindowShown)
        assertEquals(emptyList<String>(), events)
        assertEquals(before, mapView.getCameraPosition())
    }

    @Test
    fun testSecondClick_KeepsInfoWindowOpen() {
        val closed = mutableListOf<Marker>()
        mapView.setOnInfoWindowCloseListener { closed.add(it) }

        tapMarker()
        waitForIdle()
        // The camera moved to the marker, so it is now at the center
        val point = mapView.getProjection().toScreenLocation(marker.position)
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
            val event = MotionEvent.obtain(0, time, action, point.x.toFloat(), point.y - 30f, 0)
            mapView.onTouchEvent(event)
            event.recycle()
        }
        confirmTap()

        assertTrue(marker.isInfoWindowShown)
        assertEquals(emptyList<Marker>(), closed)
    }

    @Test
    fun testShowInfoWindow_ReportsClosingTheOtherWindow() {
        val other = mapView.addMarker(Marker(position = LatLng(51.47, 7.25), title = "Other"))
        val closed = mutableListOf<String?>()
        mapView.setOnInfoWindowCloseListener { closed.add(it.title) }

        marker.showInfoWindow()
        other.showInfoWindow()

        assertFalse(marker.isInfoWindowShown)
        assertTrue(other.isInfoWindowShown)
        assertEquals(listOf<String?>("Bochum"), closed)
    }

    @Test
    fun testRemoveMarker_ClosesInfoWindow() {
        val closed = mutableListOf<String?>()
        mapView.setOnInfoWindowCloseListener { closed.add(it.title) }
        marker.showInfoWindow()

        mapView.removeMarker(marker)

        assertFalse(marker.isInfoWindowShown)
        assertEquals(listOf<String?>("Bochum"), closed)
        // Added again, the marker does not bring back its old info window
        mapView.addMarker(marker)
        assertFalse(marker.isInfoWindowShown)
    }

    @Test
    fun testClearMarkers_ClosesInfoWindow() {
        val closed = mutableListOf<String?>()
        mapView.setOnInfoWindowCloseListener { closed.add(it.title) }
        marker.showInfoWindow()

        mapView.clearMarkers()

        assertFalse(marker.isInfoWindowShown)
        assertEquals(listOf<String?>("Bochum"), closed)
    }

    @Test
    fun testTouch_StopsCameraAnimation() {
        val canceled = mutableListOf<String>()
        mapView.setOnCameraMoveCanceledListener { canceled.add("canceled") }
        var callback = ""
        mapView.animateCamera(
            CameraUpdateFactory.zoomTo(17f),
            60_000,
            object : CancelableCallback {
                override fun onFinish() {
                    callback = "finished"
                }

                override fun onCancel() {
                    callback = "canceled"
                }
            },
        )
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(50))

        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 50f, 50f, 0)
        mapView.onTouchEvent(down)
        down.recycle()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("canceled", callback)
        assertEquals(listOf("canceled"), canceled)
        assertTrue(mapView.getCameraPosition().zoom < 17f)
    }

    private companion object {
        const val SIZE = 400
    }
}
