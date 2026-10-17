/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class CameraInterpolationTest {
    private val atlantic = CameraPosition(LatLng(0.0, 0.0), 2f)
    private val bochum = CameraPosition(LatLng(51.4661, 7.2491), 17f)

    // How far the location is from the camera target on the screen, in pixels
    private fun screenDistance(
        camera: CameraPosition,
        location: LatLng,
    ): Double {
        val (cameraX, cameraY) = ProjectionUtils.latLngToPixelExact(camera.target, camera.zoom)
        val (x, y) = ProjectionUtils.latLngToPixelExact(location, camera.zoom)
        return hypot(x - cameraX, y - cameraY)
    }

    private fun steps(
        start: CameraPosition,
        end: CameraPosition,
    ) = (0..20).map { interpolateCamera(start, end, it / 20f) }

    @Test
    fun testEnds_AreTheStartAndEndCamera() {
        val first = interpolateCamera(atlantic, bochum, 0f)
        val last = interpolateCamera(atlantic, bochum, 1f)

        assertEquals(0.0, screenDistance(first, atlantic.target), 1e-6)
        assertEquals(atlantic.zoom, first.zoom, 0f)
        assertEquals(bochum.target.latitude, last.target.latitude, 1e-9)
        assertEquals(bochum.target.longitude, last.target.longitude, 1e-9)
        assertEquals(bochum.zoom, last.zoom, 0f)
    }

    @Test
    fun testZoomingIn_EndTargetSlidesToTheCenter() {
        val distances = steps(atlantic, bochum).map { screenDistance(it, bochum.target) }

        // Bochum is 173 px from the center at zoom 2, on screen, and comes closer at every
        // step instead of flying off the screen at the higher zoom levels in between
        val startDistance = distances.first()
        assertTrue("start $startDistance", startDistance in 150.0..200.0)
        distances.zipWithNext().forEach { (before, after) -> assertTrue("$before -> $after", after <= before + 1e-6) }
        assertEquals(0.0, distances.last(), 1e-6)
    }

    @Test
    fun testZoomingOut_StartTargetSlidesAwayFromTheCenter() {
        val distances = steps(bochum, atlantic).map { screenDistance(it, bochum.target) }

        distances.zipWithNext().forEach { (before, after) -> assertTrue("$before -> $after", after >= before - 1e-6) }
        assertTrue("ends on screen: ${distances.last()}", distances.last() in 150.0..200.0)
    }

    @Test
    fun testSameZoom_MovesAtConstantScreenSpeed() {
        val start = CameraPosition(LatLng(51.4661, 7.2491), 12f)
        val end = CameraPosition(LatLng(51.5136, 7.4653), 12f)

        val moves = steps(start, end).zipWithNext().map { (a, b) -> screenDistance(a, b.target) }

        moves.forEach { assertEquals(moves.first(), it, 1e-3) }
    }
}
