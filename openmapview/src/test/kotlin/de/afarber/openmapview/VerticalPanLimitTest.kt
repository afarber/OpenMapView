/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Checks that panning and pinching keep the world filling the view vertically, without an
 * empty band above the north or below the south edge of the map. At zoom 2 the world is
 * 1024 px high.
 */
@RunWith(RobolectricTestRunner::class)
class VerticalPanLimitTest {
    private lateinit var controller: MapController

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(1080, 800)
        controller.setZoom(2f)
        controller.setCenter(LatLng(0.0, 0.0))
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    // The y of the north edge of the world on the screen, give or take a pixel of rounding
    private fun northEdgeY(): Int = controller.createProjection().toScreenLocation(LatLng(85.0511, 0.0)).y

    private fun assertNear(
        expected: Int,
        actual: Int,
    ) = assertEquals(expected.toFloat(), actual.toFloat(), 1f)

    @Test
    fun testPanDown_StopsWithNorthEdgeAtViewTop() {
        // The finger moves down, the map follows it and shows more of the north
        controller.updatePanOffset(0f, 500f)

        assertNear(0, northEdgeY())
        controller.commitPan()
        assertNear(0, northEdgeY())
        assertTrue(controller.getCenter().latitude < 85.0)
    }

    @Test
    fun testPanUp_StopsWithSouthEdgeAtViewBottom() {
        controller.updatePanOffset(0f, -500f)

        val southEdgeY = controller.createProjection().toScreenLocation(LatLng(-85.0511, 0.0)).y
        assertNear(800, southEdgeY)
    }

    @Test
    fun testSmallPan_MovesFreely() {
        controller.updatePanOffset(0f, 50f)

        // 112 px from the north edge to the view top before the pan
        assertNear(-62, northEdgeY())
    }

    @Test
    fun testPinchOut_NearNorthEdge_KeepsNorthEdgeAtViewTop() {
        controller.updatePanOffset(0f, 500f)
        // Zooming out around the bottom of the view pulls the north edge down into the view
        controller.zoom(0.7f, 540f, 800f)

        assertNear(0, northEdgeY())
    }

    @Test
    fun testViewHigherThanWorld_KeepsWorldCentered() {
        controller.setViewSize(1080, 1920)
        controller.setCenter(LatLng(0.0, 0.0))

        controller.updatePanOffset(0f, 300f)

        assertEquals(0.0, controller.getCenter().latitude, 0.0001)
    }

    @Test
    fun testMapAlreadyBeyondLimit_DoesNotJumpButMayMoveBack() {
        // Placed far north by code, so the view top is already past the north edge
        controller.setCenter(LatLng(80.0, 0.0))
        val startY = northEdgeY()
        assertTrue(startY > 0)

        controller.updatePanOffset(0f, 10f)
        assertEquals("no further north", startY, northEdgeY())

        controller.updatePanOffset(0f, -10f)
        assertEquals("back toward the limit", startY - 10, northEdgeY())
    }
}
