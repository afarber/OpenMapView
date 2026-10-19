/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.os.Looper
import android.os.SystemClock
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
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
import java.time.Duration

/**
 * Checks that camera animations step once per display frame, timed by the frame times
 * instead of the wall clock, and ease in and out by default.
 */
@RunWith(RobolectricTestRunner::class)
class CameraAnimationFramesTest {
    private lateinit var controller: MapController

    // The clock time and the zoom at each onCameraMove
    private val moves = mutableListOf<Pair<Long, Float>>()
    private var finished = 0
    private var cancelled = 0

    private val callback =
        object : CancelableCallback {
            override fun onFinish() {
                finished++
            }

            override fun onCancel() {
                cancelled++
            }
        }

    @Before
    fun setUp() {
        // Frames come only as the clock advances, one per 16 ms step
        ShadowChoreographer.setPaused(true)
        ShadowChoreographer.setFrameDelay(Duration.ofMillis(FRAME_MS))
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(400, 400)
        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(START_ZOOM)
        controller.onCameraMoveListener = OnCameraMoveListener { moves.add(SystemClock.uptimeMillis() to controller.getZoom()) }
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    private fun frames(count: Int) {
        repeat(count) { shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(FRAME_MS)) }
    }

    // Checks each move against the interpolated share of the time since the first frame,
    // one frame before the first move
    private fun assertMovesFollow(interpolator: android.animation.TimeInterpolator) {
        assertTrue("moves: $moves", moves.size >= 5)
        val startTime = moves.first().first - FRAME_MS
        for ((time, zoom) in moves) {
            val fraction = ((time - startTime).toFloat() / DURATION_MS).coerceAtMost(1f)
            val expected = START_ZOOM + (END_ZOOM - START_ZOOM) * interpolator.getInterpolation(fraction)
            assertEquals("zoom at ${time - startTime} ms", expected, zoom, 0.001f)
        }
        assertEquals(END_ZOOM, moves.last().second, 0.001f)
    }

    @Test
    fun testAnimation_IsDrivenByFramesNotWallClock() {
        controller.animateCamera(CameraUpdateFactory.zoomTo(END_ZOOM), DURATION_MS, callback)

        // Wall clock time alone doesn't move the camera
        Thread.sleep(DURATION_MS + 100L)
        assertEquals(START_ZOOM, controller.getZoom(), 0.001f)
        assertEquals(0, finished)

        frames(DURATION_MS / FRAME_MS.toInt() + 3)
        assertEquals(END_ZOOM, controller.getZoom(), 0.001f)
        assertEquals(1, finished)
    }

    @Test
    fun testAnimation_EasesInAndOutByDefault() {
        controller.animateCamera(CameraUpdateFactory.zoomTo(END_ZOOM), DURATION_MS, callback)
        frames(DURATION_MS / FRAME_MS.toInt() + 3)

        assertMovesFollow(AccelerateDecelerateInterpolator())
        // The first step is shorter than a linear one
        val linearStep = (END_ZOOM - START_ZOOM) * FRAME_MS / DURATION_MS
        assertTrue(moves.first().second - START_ZOOM < linearStep)
    }

    @Test
    fun testAnimation_WithLinearInterpolator() {
        controller.animateCamera(CameraUpdateFactory.zoomTo(END_ZOOM), DURATION_MS, LinearInterpolator(), callback)
        frames(DURATION_MS / FRAME_MS.toInt() + 3)

        assertMovesFollow(LinearInterpolator())
        assertEquals(1, finished)
    }

    @Test
    fun testStopAnimation_StopsTheFrames() {
        controller.animateCamera(CameraUpdateFactory.zoomTo(END_ZOOM), DURATION_MS, callback)
        frames(4)
        val zoom = controller.getZoom()
        val moveCount = moves.size
        assertTrue(zoom > START_ZOOM)

        controller.stopAnimation()
        frames(DURATION_MS / FRAME_MS.toInt())

        assertEquals(1, cancelled)
        assertEquals(0, finished)
        assertEquals(moveCount, moves.size)
        assertEquals(zoom, controller.getZoom(), 0.001f)
    }

    private companion object {
        const val FRAME_MS = 16L
        const val DURATION_MS = 160
        const val START_ZOOM = 10f
        const val END_ZOOM = 12f
    }
}
