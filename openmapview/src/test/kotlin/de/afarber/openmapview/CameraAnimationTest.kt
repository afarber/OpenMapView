/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.os.Looper
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class CameraAnimationTest {
    private lateinit var controller: MapController
    private val events = mutableListOf<String>()

    private class CountingCallback : CancelableCallback {
        var finished = 0
        var cancelled = 0

        override fun onFinish() {
            finished++
        }

        override fun onCancel() {
            cancelled++
        }
    }

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(400, 400)
        controller.onCameraMoveStartedListener = OnCameraMoveStartedListener { events.add("started") }
        controller.onCameraMoveListener = OnCameraMoveListener { events.add("move") }
        controller.onCameraIdleListener = OnCameraIdleListener { events.add("idle") }
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    private fun animate(callback: CancelableCallback) {
        controller.animateCamera(CameraUpdateFactory.zoomTo(15f), ANIMATION_MS, callback)
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun testStopAnimation_CallsOnCancel() {
        val callback = CountingCallback()
        animate(callback)

        controller.stopAnimation()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, callback.cancelled)
        assertEquals(0, callback.finished)
    }

    @Test
    fun testNewAnimation_CancelsOnlyThePreviousOne() {
        val first = CountingCallback()
        val second = CountingCallback()
        animate(first)

        animate(second)

        assertEquals(1, first.cancelled)
        assertEquals(0, second.cancelled)

        // The second animation is still running and can be stopped
        controller.stopAnimation()
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, first.cancelled)
        assertEquals(1, second.cancelled)
    }

    @Test
    fun testOnDestroy_CallsOnCancel() {
        val callback = CountingCallback()
        animate(callback)

        controller.onDestroy()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, callback.cancelled)
    }

    @Test
    fun testAnimateToCurrentPosition_FinishesWithoutCameraEvents() {
        val callback = CountingCallback()

        controller.animateCamera(
            CameraUpdateFactory.newCameraPosition(controller.getCameraPosition()),
            ANIMATION_MS,
            callback,
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, callback.finished)
        assertEquals(0, callback.cancelled)
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun testAnimateToOtherPosition_FiresCameraEvents() {
        animate(CountingCallback())

        assertEquals("started", events.first())
    }

    @Test
    fun testMoveToCurrentPosition_FiresNoCameraEvents() {
        val position = controller.getCameraPosition()

        controller.moveCamera(CameraUpdateFactory.newCameraPosition(position))

        assertEquals(emptyList<String>(), events)
        assertEquals(position, controller.getCameraPosition())
    }

    @Test
    fun testMoveToOtherPosition_FiresStartedAndIdle() {
        controller.moveCamera(CameraUpdateFactory.zoomTo(12f))

        assertEquals(listOf("started", "idle"), events)
        assertEquals(12f, controller.getCameraPosition().zoom)
    }

    private companion object {
        const val ANIMATION_MS = 60_000
    }
}
