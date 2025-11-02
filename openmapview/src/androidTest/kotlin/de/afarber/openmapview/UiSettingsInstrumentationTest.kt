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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UiSettingsInstrumentationTest {
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
        controller.setZoom(10.0)
    }

    @Test
    fun testZoomGestures_WhenDisabled_DoesNotZoom() {
        val initialZoom = controller.getZoom()

        openMapView.getUiSettings().isZoomGesturesEnabled = false

        val downTime = System.currentTimeMillis()
        val eventTime = System.currentTimeMillis()

        val pointer1Down =
            MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                540f,
                960f,
                0,
            )
        openMapView.onTouchEvent(pointer1Down)
        pointer1Down.recycle()

        val pointer2Down =
            MotionEvent.obtain(
                downTime,
                eventTime + 10,
                MotionEvent.ACTION_POINTER_DOWN,
                540f,
                1000f,
                1,
            )
        openMapView.onTouchEvent(pointer2Down)
        pointer2Down.recycle()

        val pointer2Move =
            MotionEvent.obtain(
                downTime,
                eventTime + 100,
                MotionEvent.ACTION_MOVE,
                540f,
                1200f,
                1,
            )
        openMapView.onTouchEvent(pointer2Move)
        pointer2Move.recycle()

        val pointer2Up =
            MotionEvent.obtain(
                downTime,
                eventTime + 200,
                MotionEvent.ACTION_POINTER_UP,
                540f,
                1200f,
                1,
            )
        openMapView.onTouchEvent(pointer2Up)
        pointer2Up.recycle()

        val pointer1Up =
            MotionEvent.obtain(
                downTime,
                eventTime + 210,
                MotionEvent.ACTION_UP,
                540f,
                960f,
                0,
            )
        openMapView.onTouchEvent(pointer1Up)
        pointer1Up.recycle()

        val finalZoom = controller.getZoom()
        assertEquals(initialZoom, finalZoom, 0.001)
    }

    @Test
    fun testScrollGestures_WhenDisabled_DoesNotScroll() {
        val initialCenter = controller.getCenter()

        openMapView.getUiSettings().isScrollGesturesEnabled = false

        val downTime = System.currentTimeMillis()
        val eventTime = System.currentTimeMillis()

        val down =
            MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                540f,
                960f,
                0,
            )
        openMapView.onTouchEvent(down)
        down.recycle()

        val move =
            MotionEvent.obtain(
                downTime,
                eventTime + 100,
                MotionEvent.ACTION_MOVE,
                540f,
                760f,
                0,
            )
        openMapView.onTouchEvent(move)
        move.recycle()

        val up =
            MotionEvent.obtain(
                downTime,
                eventTime + 200,
                MotionEvent.ACTION_UP,
                540f,
                760f,
                0,
            )
        openMapView.onTouchEvent(up)
        up.recycle()

        val finalCenter = controller.getCenter()
        assertEquals(initialCenter.latitude, finalCenter.latitude, 0.0001)
        assertEquals(initialCenter.longitude, finalCenter.longitude, 0.0001)
    }

    @Test
    fun testSetAllGestures_False_DisablesBoth() {
        val initialZoom = controller.getZoom()
        val initialCenter = controller.getCenter()

        openMapView.getUiSettings().setAllGesturesEnabled(false)

        val downTime = System.currentTimeMillis()
        val eventTime = System.currentTimeMillis()

        val down =
            MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                540f,
                960f,
                0,
            )
        openMapView.onTouchEvent(down)
        down.recycle()

        val move =
            MotionEvent.obtain(
                downTime,
                eventTime + 100,
                MotionEvent.ACTION_MOVE,
                540f,
                760f,
                0,
            )
        openMapView.onTouchEvent(move)
        move.recycle()

        val up =
            MotionEvent.obtain(
                downTime,
                eventTime + 200,
                MotionEvent.ACTION_UP,
                540f,
                760f,
                0,
            )
        openMapView.onTouchEvent(up)
        up.recycle()

        val finalZoom = controller.getZoom()
        val finalCenter = controller.getCenter()

        assertEquals(initialZoom, finalZoom, 0.001)
        assertEquals(initialCenter.latitude, finalCenter.latitude, 0.0001)
        assertEquals(initialCenter.longitude, finalCenter.longitude, 0.0001)
    }

    @Test
    fun testSetAllGestures_True_EnablesBoth() {
        openMapView.getUiSettings().setAllGesturesEnabled(false)
        openMapView.getUiSettings().setAllGesturesEnabled(true)

        val initialCenter = controller.getCenter()

        val downTime = System.currentTimeMillis()
        val eventTime = System.currentTimeMillis()

        val down =
            MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                540f,
                960f,
                0,
            )
        openMapView.onTouchEvent(down)
        down.recycle()

        val move =
            MotionEvent.obtain(
                downTime,
                eventTime + 100,
                MotionEvent.ACTION_MOVE,
                540f,
                760f,
                0,
            )
        openMapView.onTouchEvent(move)
        move.recycle()

        val up =
            MotionEvent.obtain(
                downTime,
                eventTime + 200,
                MotionEvent.ACTION_UP,
                540f,
                760f,
                0,
            )
        openMapView.onTouchEvent(up)
        up.recycle()

        val finalCenter = controller.getCenter()
        assertNotEquals(initialCenter.latitude, finalCenter.latitude, 0.0001)
    }

    private fun OpenMapView.getMapControllerForTesting(): MapController {
        val field = OpenMapView::class.java.getDeclaredField("controller")
        field.isAccessible = true
        return field.get(this) as MapController
    }
}
