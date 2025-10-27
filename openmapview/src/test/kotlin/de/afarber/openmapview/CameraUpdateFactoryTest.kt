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

class CameraUpdateFactoryTest {
    @Test
    fun `newLatLng creates NewLatLng update`() {
        val target = LatLng(51.4661, 7.2491)
        val update = CameraUpdateFactory.newLatLng(target)

        assertTrue(update is CameraUpdate.NewLatLng)
        assertEquals(target, (update as CameraUpdate.NewLatLng).target)
    }

    @Test
    fun `newLatLngZoom creates NewLatLngZoom update`() {
        val target = LatLng(51.4661, 7.2491)
        val zoom = 14.0
        val update = CameraUpdateFactory.newLatLngZoom(target, zoom)

        assertTrue(update is CameraUpdate.NewLatLngZoom)
        val newLatLngZoom = update as CameraUpdate.NewLatLngZoom
        assertEquals(target, newLatLngZoom.target)
        assertEquals(zoom, newLatLngZoom.zoom, 0.0001)
    }

    @Test
    fun `newCameraPosition creates NewCameraPosition update`() {
        val position =
            CameraPosition(
                target = LatLng(51.4661, 7.2491),
                zoom = 14.0,
            )
        val update = CameraUpdateFactory.newCameraPosition(position)

        assertTrue(update is CameraUpdate.NewCameraPosition)
        assertEquals(position, (update as CameraUpdate.NewCameraPosition).position)
    }

    @Test
    fun `zoomIn creates ZoomIn update`() {
        val update = CameraUpdateFactory.zoomIn()

        assertTrue(update is CameraUpdate.ZoomIn)
    }

    @Test
    fun `zoomOut creates ZoomOut update`() {
        val update = CameraUpdateFactory.zoomOut()

        assertTrue(update is CameraUpdate.ZoomOut)
    }

    @Test
    fun `zoomTo creates ZoomTo update`() {
        val zoom = 15.0
        val update = CameraUpdateFactory.zoomTo(zoom)

        assertTrue(update is CameraUpdate.ZoomTo)
        assertEquals(zoom, (update as CameraUpdate.ZoomTo).zoom, 0.0001)
    }

    @Test
    fun `zoomBy creates ZoomBy update with positive amount`() {
        val amount = 2.5
        val update = CameraUpdateFactory.zoomBy(amount)

        assertTrue(update is CameraUpdate.ZoomBy)
        assertEquals(amount, (update as CameraUpdate.ZoomBy).amount, 0.0001)
    }

    @Test
    fun `zoomBy creates ZoomBy update with negative amount`() {
        val amount = -1.5
        val update = CameraUpdateFactory.zoomBy(amount)

        assertTrue(update is CameraUpdate.ZoomBy)
        assertEquals(amount, (update as CameraUpdate.ZoomBy).amount, 0.0001)
    }
}
