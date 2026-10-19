/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Canvas
import android.graphics.Point
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Tests for taps and the tap zoom gestures: clicks after the double-tap timeout,
 * double tap, two-finger tap and one-finger zoom.
 */
@RunWith(RobolectricTestRunner::class)
class TapGesturesTest {
    private lateinit var mapView: OpenMapView
    private val clicks = mutableListOf<LatLng>()
    private val events = mutableListOf<String>()

    // Event times continue from the clock, so the GestureDetector sees real intervals
    private var time = 0L

    @Before
    fun setUp() {
        mapView = OpenMapView(ApplicationProvider.getApplicationContext())
        mapView.setMapType(MapType.NONE)
        mapView.layout(0, 0, SIZE, SIZE)
        mapView.setCenter(LatLng(51.4661, 7.2491))
        mapView.setZoom(ZOOM)
        mapView.setOnMapClickListener { clicks.add(it) }
        mapView.setOnCameraMoveStartedListener { events.add("started:$it") }
        mapView.setOnCameraIdleListener { events.add("idle") }
        time = SystemClock.uptimeMillis()
    }

    private fun event(
        action: Int,
        vararg points: Pair<Float, Float>,
    ): MotionEvent {
        val properties =
            Array(points.size) { i ->
                MotionEvent.PointerProperties().apply {
                    id = i
                    toolType = MotionEvent.TOOL_TYPE_FINGER
                }
            }
        val coords =
            Array(points.size) { i ->
                MotionEvent.PointerCoords().apply {
                    x = points[i].first
                    y = points[i].second
                    pressure = 1f
                    size = 1f
                }
            }
        return MotionEvent.obtain(time, time, action, points.size, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    private fun send(
        afterMs: Long,
        action: Int,
        vararg points: Pair<Float, Float>,
    ) {
        time += afterMs
        val event = event(action, *points)
        mapView.onTouchEvent(event)
        event.recycle()
    }

    private fun tap(
        x: Float = 200f,
        y: Float = 200f,
    ) {
        send(0, MotionEvent.ACTION_DOWN, x to y)
        send(50, MotionEvent.ACTION_UP, x to y)
    }

    private fun doubleTap(
        x: Float = 200f,
        y: Float = 200f,
    ) {
        tap(x, y)
        time += 100
        tap(x, y)
    }

    // The camera animation runs on wall clock time
    private fun waitForIdle() {
        val deadline = System.nanoTime() + 2_000_000_000L
        while (events.lastOrNull() != "idle" && System.nanoTime() < deadline) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
        }
    }

    private fun locationAt(
        x: Int,
        y: Int,
    ): LatLng = mapView.getProjection().fromScreenLocation(Point(x, y))

    @Test
    fun testTap_IsReportedAfterDoubleTapTimeout() {
        tap()
        assertTrue(clicks.isEmpty())

        confirmTap()
        assertEquals(1, clicks.size)
    }

    @Test
    fun testDoubleTap_ZoomsInAroundTapWithoutClick() {
        val before = locationAt(200, 200)

        doubleTap()
        confirmTap()
        waitForIdle()

        assertTrue(clicks.isEmpty())
        assertEquals(ZOOM + 1f, mapView.getCameraPosition().zoom, 0.001f)
        assertEquals(listOf("started:${OnCameraMoveStartedListener.REASON_GESTURE}", "idle"), events)
        // The location under the double tap stays there
        val after = locationAt(200, 200)
        assertEquals(before.latitude, after.latitude, 1e-4)
        assertEquals(before.longitude, after.longitude, 1e-4)
    }

    @Test
    fun testDoubleTap_WithoutZoomGesturesIsTwoClicks() {
        mapView.getUiSettings().isZoomGesturesEnabled = false

        doubleTap()

        // Reported right away, as there is no double tap to wait for
        assertEquals(2, clicks.size)
        confirmTap()
        assertEquals(ZOOM, mapView.getCameraPosition().zoom, 0.001f)
    }

    @Test
    fun testTwoFingerTap_ZoomsOutWithoutClick() {
        val pointerDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val pointerUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(0, MotionEvent.ACTION_DOWN, 300f to 400f)
        send(10, pointerDown, 300f to 400f, 500f to 400f)
        send(80, pointerUp, 300f to 400f, 500f to 400f)
        send(10, MotionEvent.ACTION_UP, 300f to 400f)
        confirmTap()
        waitForIdle()

        assertTrue(clicks.isEmpty())
        assertEquals(ZOOM - 1f, mapView.getCameraPosition().zoom, 0.001f)
        assertEquals(listOf("started:${OnCameraMoveStartedListener.REASON_GESTURE}", "idle"), events)
    }

    @Test
    fun testPinch_IsNoTwoFingerTap() {
        val pointerDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val pointerUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(0, MotionEvent.ACTION_DOWN, 350f to 400f)
        send(10, pointerDown, 350f to 400f, 450f to 400f)
        for (step in 1..10) send(10, MotionEvent.ACTION_MOVE, 350f - step * 10 to 400f, 450f + step * 10 to 400f)
        send(10, pointerUp, 250f to 400f, 550f to 400f)
        send(10, MotionEvent.ACTION_UP, 250f to 400f)
        confirmTap()
        Thread.sleep(400)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(400))

        // The pinch zoomed in, and no two-finger tap zoomed out by a level after it
        val zoom = mapView.getCameraPosition().zoom
        assertTrue("zoom $zoom", zoom > ZOOM)
        assertTrue(clicks.isEmpty())
    }

    @Test
    fun testOneFingerZoom_SlidingDownZoomsIn() {
        tap()
        time += 100
        send(0, MotionEvent.ACTION_DOWN, 200f to 200f)
        // 100 dp per zoom level, at density 1
        for (step in 1..30) send(16, MotionEvent.ACTION_MOVE, 200f to 200f + step * 10)
        assertTrue(mapView.getCameraPosition().zoom > ZOOM + 2.5f)
        assertFalse("idle" in events)

        send(16, MotionEvent.ACTION_UP, 200f to 500f)
        val zoom = mapView.getCameraPosition().zoom
        confirmTap()
        Thread.sleep(300)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300))

        assertTrue(clicks.isEmpty())
        assertEquals("idle", events.last())
        // No double tap zoom on top of it
        assertEquals(zoom, mapView.getCameraPosition().zoom, 0.001f)
    }

    @Test
    fun testOneFingerZoom_SlidingUpZoomsOut() {
        tap(200f, 500f)
        time += 100
        send(0, MotionEvent.ACTION_DOWN, 200f to 500f)
        for (step in 1..20) send(16, MotionEvent.ACTION_MOVE, 200f to 500f - step * 10)
        send(16, MotionEvent.ACTION_UP, 200f to 300f)

        assertTrue(mapView.getCameraPosition().zoom < ZOOM - 1.5f)
    }

    @Test
    fun testDoubleTapOnZoomButton_IsTwoButtonTaps() {
        mapView.getUiSettings().isZoomControlsEnabled = true
        // Drawing places the buttons; the zoom-in button is at the same place as in PinchZoomTest
        mapView.draw(Canvas(createBitmap(SIZE, SIZE)))

        doubleTap(SIZE - 36f, SIZE - 108f)
        waitForIdle()

        // Both taps zoom by the button, which reports an API animation, not a gesture
        assertFalse(events.any { it == "started:${OnCameraMoveStartedListener.REASON_GESTURE}" })
        assertTrue(events.count { it == "started:${OnCameraMoveStartedListener.REASON_API_ANIMATION}" } >= 1)
        assertTrue(mapView.getCameraPosition().zoom > ZOOM)
    }

    private companion object {
        const val SIZE = 800
        const val ZOOM = 10f
    }
}
