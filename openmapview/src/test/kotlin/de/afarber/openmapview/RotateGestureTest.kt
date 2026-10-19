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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sign
import kotlin.math.sin

/**
 * Tests for the rotate gesture and the compass button.
 */
@RunWith(RobolectricTestRunner::class)
class RotateGestureTest {
    private lateinit var mapView: OpenMapView
    private val events = mutableListOf<String>()
    private val clicks = mutableListOf<LatLng>()
    private var time = 0L

    @Before
    fun setUp() {
        mapView = OpenMapView(ApplicationProvider.getApplicationContext())
        mapView.setMapType(MapType.NONE)
        mapView.layout(0, 0, SIZE, SIZE)
        mapView.setCenter(LatLng(51.4661, 7.2491))
        mapView.setZoom(12f)
        mapView.setOnCameraMoveStartedListener { events.add("started:$it") }
        mapView.setOnCameraIdleListener { events.add("idle") }
        mapView.setOnMapClickListener { clicks.add(it) }
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
        return MotionEvent.obtain(0, time, action, points.size, properties, coords, 0, 0, 1f, 1f, 0, 0, 0, 0)
    }

    private fun send(
        action: Int,
        vararg points: Pair<Float, Float>,
    ) {
        time += 16
        val event = event(action, *points)
        mapView.onTouchEvent(event)
        event.recycle()
    }

    // Two fingers on a circle around (centerX, centerY), turned clockwise on the screen from
    // 0 to `degrees` in steps, with their distance changing from startRadius to endRadius
    private fun twist(
        degrees: Float,
        centerX: Float = 400f,
        centerY: Float = 400f,
        startRadius: Float = 150f,
        endRadius: Float = startRadius,
        steps: Int = 20,
    ) {
        fun fingers(
            angle: Float,
            radius: Float,
        ): Array<Pair<Float, Float>> {
            val radians = Math.toRadians(angle.toDouble())
            val dx = (radius * cos(radians)).toFloat()
            val dy = (radius * sin(radians)).toFloat()
            return arrayOf(centerX - dx to centerY - dy, centerX + dx to centerY + dy)
        }
        val pointerDown = MotionEvent.ACTION_POINTER_DOWN or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val pointerUp = MotionEvent.ACTION_POINTER_UP or (1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        send(MotionEvent.ACTION_DOWN, fingers(0f, startRadius)[0])
        send(pointerDown, *fingers(0f, startRadius))
        for (step in 1..steps) {
            val share = step.toFloat() / steps
            send(MotionEvent.ACTION_MOVE, *fingers(degrees * share, startRadius + (endRadius - startRadius) * share))
        }
        val end = fingers(degrees, endRadius)
        send(pointerUp, *end)
        send(MotionEvent.ACTION_UP, end[0])
    }

    private fun locationAt(
        x: Int,
        y: Int,
    ): LatLng = mapView.getProjection().fromScreenLocation(Point(x, y))

    private fun waitForIdle() {
        repeat(100) {
            if (events.lastOrNull() == "idle") return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(20))
        }
    }

    // The turn of the bearing for fingers turned by `degrees` clockwise in 20 steps: the map
    // starts turning at the first step at or past the 10 degree threshold, and turns the
    // other way, as turning the map clockwise lowers the bearing
    private fun expectedTurn(degrees: Float): Float {
        val step = abs(degrees) / 20
        val start = ceil(10f / step) * step
        return -sign(degrees) * (abs(degrees) - start)
    }

    // A turn of the bearing from 0, between -180 and 180
    private fun turn(): Float {
        val bearing = mapView.getCameraPosition().bearing
        return if (bearing > 180f) bearing - 360f else bearing
    }

    @Test
    fun testTwist_TurnsTheMapWithTheFingers() {
        val before = locationAt(400, 400)

        twist(60f)

        // The fingers turned 60 degrees clockwise, the map turned clockwise with them once
        // past the threshold
        assertEquals(expectedTurn(60f), turn(), 0.01f)
        assertEquals(listOf("started:${OnCameraMoveStartedListener.REASON_GESTURE}", "idle"), events)
        // The location between the fingers stays there
        val after = locationAt(400, 400)
        assertEquals(before.latitude, after.latitude, 1e-4)
        assertEquals(before.longitude, after.longitude, 1e-4)
    }

    @Test
    fun testTwist_AroundAnOffCenterPoint() {
        val before = locationAt(250, 600)

        twist(-90f, centerX = 250f, centerY = 600f)

        assertEquals(expectedTurn(-90f), turn(), 0.01f)
        val after = locationAt(250, 600)
        assertEquals(before.latitude, after.latitude, 1e-4)
        assertEquals(before.longitude, after.longitude, 1e-4)
    }

    @Test
    fun testSmallTwist_DoesNotTurn() {
        twist(8f)

        assertEquals(0f, mapView.getCameraPosition().bearing)
    }

    @Test
    fun testPinchWithoutTwist_DoesNotTurn() {
        twist(0f, startRadius = 100f, endRadius = 300f)

        assertEquals(0f, mapView.getCameraPosition().bearing)
        assertTrue(mapView.getCameraPosition().zoom > 12.5f)
    }

    @Test
    fun testTwist_WithRotateGesturesDisabled() {
        mapView.getUiSettings().isRotateGesturesEnabled = false

        twist(60f)

        assertEquals(0f, mapView.getCameraPosition().bearing)
    }

    @Test
    fun testTwistAndPinch_TurnAndZoomTogether() {
        twist(90f, startRadius = 100f, endRadius = 200f)

        assertEquals(expectedTurn(90f), turn(), 0.01f)
        assertTrue(mapView.getCameraPosition().zoom > 12.5f)
    }

    // The compass is centered 36 px from the top and left at density 1; drawing places it
    private fun tapCompass() {
        mapView.draw(Canvas(createBitmap(SIZE, SIZE)))
        send(MotionEvent.ACTION_DOWN, 36f to 36f)
        send(MotionEvent.ACTION_UP, 36f to 36f)
        confirmTap()
    }

    @Test
    fun testCompass_TurnsTheMapBackNorth() {
        mapView.setBearing(120f)

        tapCompass()
        waitForIdle()

        assertEquals(0f, mapView.getCameraPosition().bearing)
        assertEquals(listOf("started:${OnCameraMoveStartedListener.REASON_API_ANIMATION}", "idle"), events)
        assertTrue(clicks.isEmpty())
    }

    @Test
    fun testCompass_IsHiddenWhileTheMapFacesNorth() {
        tapCompass()

        // A tap at its place is a map click
        assertEquals(1, clicks.size)
        assertTrue(events.isEmpty())
    }

    @Test
    fun testCompass_CanBeDisabled() {
        mapView.getUiSettings().isCompassEnabled = false
        mapView.setBearing(120f)

        tapCompass()

        assertEquals(1, clicks.size)
        assertEquals(120f, mapView.getCameraPosition().bearing)
    }

    private companion object {
        const val SIZE = 800
    }
}
