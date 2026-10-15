/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

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

        assertEquals(11f, mapView.getCameraPosition().zoom)
        assertEquals(
            listOf("started:${OnCameraMoveStartedListener.REASON_API_ANIMATION}", "move", "idle"),
            events,
        )
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
