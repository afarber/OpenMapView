/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.os.Looper
import android.view.MotionEvent
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Checks that pinch zooming and the zoom buttons report camera moves like panning does.
 */
@RunWith(RobolectricTestRunner::class)
class PinchZoomTest {
    private lateinit var mapView: OpenMapView
    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
        mapView.layout(0, 0, SIZE, SIZE)
        mapView.setCenter(LatLng(51.5074, -0.1278))
        mapView.setZoom(10f)
        mapView.setOnCameraMoveStartedListener { events.add("started:$it") }
        mapView.setOnCameraMoveListener { events.add("move") }
        mapView.setOnCameraIdleListener { events.add("idle") }
    }

    private fun event(
        time: Long,
        action: Int,
        vararg points: Pair<Float, Float>,
    ): MotionEvent = eventWithIds(time, action, IntArray(points.size) { it }, *points)

    private fun eventWithIds(
        time: Long,
        action: Int,
        ids: IntArray,
        vararg points: Pair<Float, Float>,
    ): MotionEvent {
        val properties =
            Array(points.size) { i ->
                MotionEvent.PointerProperties().apply {
                    id = ids[i]
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
        return MotionEvent.obtain(0, time, action, points.size, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    private fun send(event: MotionEvent) {
        mapView.onTouchEvent(event)
        event.recycle()
    }

    // Two fingers on a horizontal line through (centerX, centerY), spreading from
    // startSpan to endSpan pixels apart
    private fun pinch(
        centerX: Float,
        centerY: Float,
        startSpan: Float,
        endSpan: Float,
    ) {
        fun fingers(span: Float) = arrayOf(centerX - span / 2 to centerY, centerX + span / 2 to centerY)
        var time = 0L
        send(event(time, MotionEvent.ACTION_DOWN, fingers(startSpan)[0]))
        time += 10
        val pointerDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(event(time, pointerDown, *fingers(startSpan)))
        for (step in 1..STEPS) {
            time += 16
            send(event(time, MotionEvent.ACTION_MOVE, *fingers(startSpan + (endSpan - startSpan) * step / STEPS)))
        }
        time += 16
        val pointerUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(event(time, pointerUp, *fingers(endSpan)))
        time += 16
        send(event(time, MotionEvent.ACTION_UP, fingers(endSpan)[0]))
    }

    @Test
    fun testPinchAroundCenter_ReportsGestureMoveAndIdle() {
        pinch(SIZE / 2f, SIZE / 2f, startSpan = 200f, endSpan = 600f)

        assertTrue("zoomed in", mapView.getCameraPosition().zoom > 10f)
        assertEquals("started:${OnCameraMoveStartedListener.REASON_GESTURE}", events.first())
        assertTrue("move events", events.count { it == "move" } >= 1)
        assertEquals("idle", events.last())
        assertEquals(1, events.count { it.startsWith("started") })
        assertEquals(1, events.count { it == "idle" })
    }

    @Test
    fun testPinchOffCenter_ReportsGestureMoveAndIdle() {
        pinch(SIZE / 3f, SIZE / 3f, startSpan = 200f, endSpan = 600f)

        assertEquals("started:${OnCameraMoveStartedListener.REASON_GESTURE}", events.first())
        assertEquals("idle", events.last())
        assertEquals(1, events.count { it == "idle" })
    }

    @Test
    fun testPinch_KeepsPinchCenterInPlace() {
        val center = 266
        val before = mapView.getProjection().fromScreenLocation(Point(center, center))

        pinch(center.toFloat(), center.toFloat(), startSpan = 200f, endSpan = 600f)

        // The fingers move apart around the pinch center, so the map must not pan away
        // under them, neither before the scale detector starts nor while it zooms
        val after = mapView.getProjection().toScreenLocation(before)
        assertTrue("zoomed in", mapView.getCameraPosition().zoom > 10.5f)
        // Within the rounding of screen points to whole pixels
        assertEquals(center.toFloat(), after.x.toFloat(), 2f)
        assertEquals(center.toFloat(), after.y.toFloat(), 2f)
    }

    @Test
    fun testTwoFingerPan_LiftingFirstFingerDoesNotJump() {
        val place = mapView.getCameraPosition().target
        val start = mapView.getProjection().toScreenLocation(place)

        // Two fingers move right together by 60 px, then the first one is lifted and
        // the second one moves on by 10 px
        send(eventWithIds(0, MotionEvent.ACTION_DOWN, intArrayOf(0), 300f to 400f))
        val pointerDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(eventWithIds(10, pointerDown, intArrayOf(0, 1), 300f to 400f, 500f to 400f))
        for (step in 1..6) {
            val dx = step * 10f
            send(eventWithIds(10L + step * 16, MotionEvent.ACTION_MOVE, intArrayOf(0, 1), 300f + dx to 400f, 500f + dx to 400f))
        }
        val pointerUp = MotionEvent.ACTION_POINTER_UP or (0 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(eventWithIds(120, pointerUp, intArrayOf(0, 1), 360f to 400f, 560f to 400f))
        send(eventWithIds(136, MotionEvent.ACTION_MOVE, intArrayOf(1), 570f to 400f))

        val end = mapView.getProjection().toScreenLocation(place)
        assertEquals(start.x + 70, end.x)
        assertEquals(start.y, end.y)
        send(eventWithIds(152, MotionEvent.ACTION_UP, intArrayOf(1), 570f to 400f))
    }

    // Two fingers spreading from 200 to 600 px apart while their center moves from
    // (startX, 400) to (endX, 400)
    private fun movingPinch(
        startX: Float,
        endX: Float,
    ) {
        fun fingers(
            centerX: Float,
            span: Float,
        ) = arrayOf(centerX - span / 2 to 400f, centerX + span / 2 to 400f)
        send(event(0, MotionEvent.ACTION_DOWN, fingers(startX, 200f)[0]))
        val pointerDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(event(10, pointerDown, *fingers(startX, 200f)))
        for (step in 1..STEPS) {
            val centerX = startX + (endX - startX) * step / STEPS
            send(event(10L + step * 16, MotionEvent.ACTION_MOVE, *fingers(centerX, 200f + 400f * step / STEPS)))
        }
    }

    @Test
    fun testPinchWhileMoving_PansWithTheFingers() {
        val place = mapView.getProjection().fromScreenLocation(Point(300, 400))

        movingPinch(startX = 300f, endX = 450f)

        // The content under the fingers moves with them
        val point = mapView.getProjection().toScreenLocation(place)
        assertTrue("zoomed in", mapView.getCameraPosition().zoom > 10.5f)
        assertEquals(450f, point.x.toFloat(), 3f)
        assertEquals(400f, point.y.toFloat(), 3f)
    }

    @Test
    fun testPinchWhileMoving_WithoutScrollDuringZoom_OnlyZooms() {
        mapView.getUiSettings().isScrollGesturesEnabledDuringRotateOrZoom = false
        val place = mapView.getProjection().fromScreenLocation(Point(300, 400))

        movingPinch(startX = 300f, endX = 450f)

        // The map zooms around the moving fingers, but doesn't follow them
        val point = mapView.getProjection().toScreenLocation(place)
        assertTrue("zoomed in", mapView.getCameraPosition().zoom > 10.5f)
        assertTrue("not panned along, x = ${point.x}", point.x < 400)
    }

    @Test
    fun testZoomControlsEnabled_RedrawsMap() {
        mapView.draw(Canvas(createBitmap(SIZE, SIZE)))
        shadowOf(mapView).clearWasInvalidated()

        mapView.getUiSettings().isZoomControlsEnabled = true

        assertTrue(shadowOf(mapView).wasInvalidated())
    }

    @Test
    fun testPinchAtMaxZoom_ReportsNothing() {
        mapView.setMaxZoomPreference(10f)

        pinch(SIZE / 2f, SIZE / 2f, startSpan = 200f, endSpan = 600f)

        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun testZoomButton_ReportsStartedMoveAndIdle() {
        mapView.getUiSettings().isZoomControlsEnabled = true
        // Drawing places the buttons; at density 1 the zoom-in button is centered
        // 36 px from the right edge and 60 px from the bottom
        mapView.draw(Canvas(createBitmap(SIZE, SIZE)))

        tap(SIZE - 36f, SIZE - 60f)

        // The buttons animate the zoom instead of jumping to the next level
        assertEquals(10f, mapView.getCameraPosition().zoom)
        assertEquals(listOf("started:${OnCameraMoveStartedListener.REASON_API_ANIMATION}"), events)

        // The animation runs on wall clock time
        val deadline = System.nanoTime() + 2_000_000_000L
        while (events.lastOrNull() != "idle" && System.nanoTime() < deadline) {
            Thread.sleep(20)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
        }
        assertEquals(11f, mapView.getCameraPosition().zoom)
        assertTrue("move events", events.count { it == "move" } >= 1)
        assertEquals("idle", events.last())
        assertEquals(1, events.count { it.startsWith("started") })
    }

    @Test
    fun testZoomButtons_StayInsideMapPadding() {
        mapView.getUiSettings().isZoomControlsEnabled = true
        // E.g. a side panel on the right and a bottom bar
        mapView.setMapPadding(0, 0, 200, 100)
        mapView.draw(Canvas(createBitmap(SIZE, SIZE)))

        // The old place of the zoom-in button is now under the side panel
        tap(SIZE - 36f, SIZE - 60f)
        assertEquals(emptyList<String>(), events)

        // The button moved left and up by the padding
        tap(SIZE - 200 - 36f, SIZE - 100 - 60f)
        assertEquals(listOf("started:${OnCameraMoveStartedListener.REASON_API_ANIMATION}"), events)
    }

    private fun tap(
        x: Float,
        y: Float,
    ) {
        send(event(0, MotionEvent.ACTION_DOWN, x to y))
        send(event(50, MotionEvent.ACTION_UP, x to y))
    }

    private companion object {
        const val SIZE = 800
        const val STEPS = 10
    }
}
