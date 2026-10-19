/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TouchGestureInstrumentationTest {
    private lateinit var openMapView: OpenMapView
    private lateinit var controller: MapController

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Create and setup view on main thread to avoid Handler creation issues
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            openMapView = OpenMapView(context)
            openMapView.layout(0, 0, 1080, 1920)
        }

        controller = openMapView.getMapControllerForTesting()
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(10.0f)
    }

    @Test
    fun testLongDrag_ShouldNotRegisterAsClick() {
        val click = CountDownLatch(1)
        openMapView.setOnMapClickListener { click.countDown() }

        val downTime = SystemClock.uptimeMillis()

        // ACTION_DOWN at (100, 100)
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        send(down)
        down.recycle()

        // Simulate multiple MOVE events (drag 200 pixels)
        for (i in 1..10) {
            val move =
                MotionEvent.obtain(
                    downTime,
                    downTime + i * 10L,
                    MotionEvent.ACTION_MOVE,
                    100f + i * 20f,
                    100f + i * 20f,
                    0,
                )
            send(move)
            move.recycle()
        }

        // ACTION_UP at (300, 300) - 200 pixels away from initial position
        val up = MotionEvent.obtain(downTime, downTime + 150, MotionEvent.ACTION_UP, 300f, 300f, 0)
        send(up)
        up.recycle()

        // Should NOT have registered as a click (200px is more than the touch slop), also not
        // after the double-tap timeout, after which a tap is reported
        assertFalse("Long drag should not register as click", click.await(CLICK_WAIT_MS, TimeUnit.MILLISECONDS))
    }

    @Test
    fun testSmallMovement_ShouldRegisterAsClick() {
        val click = CountDownLatch(1)
        openMapView.setOnMapClickListener { click.countDown() }

        val downTime = SystemClock.uptimeMillis()

        // ACTION_DOWN at (100, 100)
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        send(down)
        down.recycle()

        // Small movement (3 pixels)
        val move = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_MOVE, 103f, 103f, 0)
        send(move)
        move.recycle()

        // ACTION_UP at (103, 103) - only 3 pixels away
        val up = MotionEvent.obtain(downTime, downTime + 100, MotionEvent.ACTION_UP, 103f, 103f, 0)
        send(up)
        up.recycle()

        // Should have registered as a click (3px is within the touch slop), reported once no
        // second tap followed within the double-tap timeout
        assertTrue("Small movement should register as click", click.await(CLICK_WAIT_MS, TimeUnit.MILLISECONDS))
    }

    // Delivers a touch on the main thread, like the system does
    private fun send(event: MotionEvent) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync { openMapView.onTouchEvent(event) }
    }

    private fun OpenMapView.getMapControllerForTesting(): MapController {
        val field = OpenMapView::class.java.getDeclaredField("controller")
        field.isAccessible = true
        return field.get(this) as MapController
    }

    private companion object {
        // With zoom gestures, OpenMapView reports a tap as a click only once no second tap
        // followed: after the double-tap timeout, 300 ms by default. The wait adds a second
        // for the main thread on a slow emulator.
        val CLICK_WAIT_MS = ViewConfiguration.getDoubleTapTimeout() + 1_000L
    }
}
