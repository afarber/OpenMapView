/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Point
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Checks the map at its highest zoom on a dense screen, far east. The world is then about
 * 400 million pixels wide, where a Float only has every 16th or 32nd pixel: positions must
 * be calculated with double precision to be exact to the pixel.
 */
@RunWith(RobolectricTestRunner::class)
class HighZoomPrecisionTest {
    private lateinit var controller: MapController

    private val tokyo = LatLng(35.6586, 139.7454)
    private val tileSize = 3 * ProjectionUtils.TILE_SIZE

    // 200 px right of and 300 px below the map center
    private lateinit var location: LatLng
    private val screen = Point(540 + 200, 960 + 300)

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(1080, 1920)
        controller.setTileScale(3f)
        controller.setZoom(19f)
        controller.setCenter(tokyo)
        val (centerX, centerY) = ProjectionUtils.latLngToPixelExact(tokyo, 19f, tileSize)
        location = ProjectionUtils.pixelToLatLng(centerX + 200, centerY + 300, 19f, tileSize)
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    private fun assertOnScreenAt(
        expected: Point,
        location: LatLng,
    ) {
        val point = controller.createProjection().toScreenLocation(location)
        assertEquals(expected.x.toFloat(), point.x.toFloat(), 1f)
        assertEquals(expected.y.toFloat(), point.y.toFloat(), 1f)
    }

    @Test
    fun testToScreenLocation_IsExactToThePixel() {
        assertOnScreenAt(screen, location)
    }

    @Test
    fun testFromScreenLocation_IsExactToThePixel() {
        // A pixel is about 9e-7 degrees of longitude here
        val fromProjection = controller.createProjection().fromScreenLocation(screen)
        assertEquals(location.latitude, fromProjection.latitude, 1e-6)
        assertEquals(location.longitude, fromProjection.longitude, 1e-6)

        val fromController = controller.screenToLatLng(screen.x.toFloat(), screen.y.toFloat())
        assertEquals(location.latitude, fromController.latitude, 1e-6)
        assertEquals(location.longitude, fromController.longitude, 1e-6)
    }

    @Test
    fun testCommitPan_DoesNotMoveTheMap() {
        controller.updatePanOffset(-100f, 50f)
        assertOnScreenAt(Point(screen.x - 100, screen.y + 50), location)

        controller.commitPan()

        assertOnScreenAt(Point(screen.x - 100, screen.y + 50), location)
    }

    @Test
    fun testScrollBy_MovesTheMapByThatManyPixels() {
        controller.moveCamera(CameraUpdateFactory.scrollBy(100f, -50f))

        assertOnScreenAt(Point(screen.x - 100, screen.y + 50), location)
    }

    // The circle has a radius of about 6 px: it is hit where it is drawn, not 8 px beside it
    @Test
    fun testCircle_IsHitWhereItIs() {
        val circle = controller.addCircle(Circle(center = location, radius = 0.5f, strokeWidth = 0f, clickable = true))

        assertSame(circle, controller.handleCircleTouch(screen.x.toFloat(), screen.y.toFloat()))
    }
}
