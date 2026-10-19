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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowChoreographer

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
        // Display frames come only as the clock advances, as on a device, instead of
        // Robolectric running the frames of an animation all at once
        ShadowChoreographer.setPaused(true)
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
    fun testAnimateCamera_ReportsDeveloperAnimation() {
        val reasons = mutableListOf<Int>()
        controller.onCameraMoveStartedListener = OnCameraMoveStartedListener { reasons.add(it) }

        animate(CountingCallback())

        assertEquals(listOf(OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION), reasons)
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
    fun testMoveToOtherPosition_FiresStartedAndIdleLater() {
        controller.moveCamera(CameraUpdateFactory.zoomTo(12f))

        // The camera has moved, the listeners hear about it on the main thread
        assertEquals(12f, controller.getCameraPosition().zoom)
        assertEquals(emptyList<String>(), events)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("started", "idle"), events)
    }

    @Test
    fun testListenerSetAfterMove_HearsAboutIt() {
        controller.onCameraIdleListener = null

        controller.moveCamera(CameraUpdateFactory.zoomTo(12f))
        controller.onCameraIdleListener = OnCameraIdleListener { events.add("idle") }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf("started", "idle"), events)
    }

    @Test
    fun testTwoMoves_ReportEventsInOrder() {
        controller.moveCamera(CameraUpdateFactory.zoomTo(12f))
        controller.moveCamera(CameraUpdateFactory.zoomTo(13f))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(listOf("started", "idle", "started", "idle"), events)
    }

    @Test
    fun testAnimationAfterMove_ReportsMoveFirst() {
        controller.moveCamera(CameraUpdateFactory.zoomTo(12f))
        controller.animateCamera(CameraUpdateFactory.zoomTo(15f), ANIMATION_MS, null)

        assertEquals(listOf("started", "idle", "started"), events)
    }

    @Test
    fun testMoveBeforeLayout_ReportsIdleAfterLayout() {
        val map = MapController(RuntimeEnvironment.getApplication())
        var visibleWidth = 0.0
        map.onCameraIdleListener =
            OnCameraIdleListener {
                val region = map.createProjection().getVisibleRegion()
                visibleWidth = region.latLngBounds.northeast.longitude - region.latLngBounds.southwest.longitude
                events.add("idle")
            }

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(51.4661, 7.2491), 12f))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(emptyList<String>(), events)

        map.setViewSize(400, 400)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(listOf("idle"), events)
        assertTrue(visibleWidth > 0.0)
        map.onDestroy()
    }

    @Test
    fun testDestroyedMap_ReportsNoMove() {
        controller.moveCamera(CameraUpdateFactory.zoomTo(12f))
        controller.onDestroy()
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(emptyList<String>(), events)
    }

    private companion object {
        const val ANIMATION_MS = 60_000
    }
}
