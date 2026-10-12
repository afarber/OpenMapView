/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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
        var clickReceived = false
        openMapView.setOnMapClickListener { clickReceived = true }

        val downTime = System.currentTimeMillis()

        // ACTION_DOWN at (100, 100)
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        openMapView.onTouchEvent(down)
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
            openMapView.onTouchEvent(move)
            move.recycle()
        }

        // ACTION_UP at (300, 300) - 200 pixels away from initial position
        val up = MotionEvent.obtain(downTime, downTime + 150, MotionEvent.ACTION_UP, 300f, 300f, 0)
        openMapView.onTouchEvent(up)
        up.recycle()

        // Should NOT have registered as a click (distance 200px > 10px threshold)
        assertFalse("Long drag should not register as click", clickReceived)
    }

    @Test
    fun testSmallMovement_ShouldRegisterAsClick() {
        var clickReceived = false
        openMapView.setOnMapClickListener { clickReceived = true }

        val downTime = System.currentTimeMillis()

        // ACTION_DOWN at (100, 100)
        val down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 100f, 100f, 0)
        openMapView.onTouchEvent(down)
        down.recycle()

        // Small movement (3 pixels)
        val move = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_MOVE, 103f, 103f, 0)
        openMapView.onTouchEvent(move)
        move.recycle()

        // ACTION_UP at (103, 103) - only 3 pixels away
        val up = MotionEvent.obtain(downTime, downTime + 100, MotionEvent.ACTION_UP, 103f, 103f, 0)
        openMapView.onTouchEvent(up)
        up.recycle()

        // Should have registered as a click (distance 3px < 10px threshold)
        assertTrue("Small movement should register as click", clickReceived)
    }

    private fun OpenMapView.getMapControllerForTesting(): MapController {
        val field = OpenMapView::class.java.getDeclaredField("controller")
        field.isAccessible = true
        return field.get(this) as MapController
    }
}
