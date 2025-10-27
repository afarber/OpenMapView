/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

class CameraPositionTest {
    @Test
    fun `create valid camera position`() {
        val position =
            CameraPosition(
                target = LatLng(51.4661, 7.2491),
                zoom = 14.0,
            )

        assertEquals(51.4661, position.target.latitude, 0.0001)
        assertEquals(7.2491, position.target.longitude, 0.0001)
        assertEquals(14.0, position.zoom, 0.0001)
    }

    @Test
    fun `create camera position with minimum zoom`() {
        val position =
            CameraPosition(
                target = LatLng(0.0, 0.0),
                zoom = 2.0,
            )

        assertEquals(2.0, position.zoom, 0.0001)
    }

    @Test
    fun `create camera position with maximum zoom`() {
        val position =
            CameraPosition(
                target = LatLng(0.0, 0.0),
                zoom = 19.0,
            )

        assertEquals(19.0, position.zoom, 0.0001)
    }

    @Test
    fun `create camera position with below minimum zoom throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            CameraPosition(
                target = LatLng(0.0, 0.0),
                zoom = 1.9,
            )
        }
    }

    @Test
    fun `create camera position with above maximum zoom throws exception`() {
        assertFailsWith<IllegalArgumentException> {
            CameraPosition(
                target = LatLng(0.0, 0.0),
                zoom = 19.1,
            )
        }
    }

    @Test
    fun `camera position data class equality`() {
        val position1 =
            CameraPosition(
                target = LatLng(51.4661, 7.2491),
                zoom = 14.0,
            )

        val position2 =
            CameraPosition(
                target = LatLng(51.4661, 7.2491),
                zoom = 14.0,
            )

        assertEquals(position1, position2)
    }

    @Test
    fun `camera position data class copy`() {
        val position =
            CameraPosition(
                target = LatLng(51.4661, 7.2491),
                zoom = 14.0,
            )

        val copied = position.copy(zoom = 15.0)

        assertEquals(51.4661, copied.target.latitude, 0.0001)
        assertEquals(7.2491, copied.target.longitude, 0.0001)
        assertEquals(15.0, copied.zoom, 0.0001)
    }
}
