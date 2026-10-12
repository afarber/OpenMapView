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
        val zoom = 14.0f
        val update = CameraUpdateFactory.newLatLngZoom(target, zoom)

        assertTrue(update is CameraUpdate.NewLatLngZoom)
        val newLatLngZoom = update as CameraUpdate.NewLatLngZoom
        assertEquals(target, newLatLngZoom.target)
        assertEquals(zoom, newLatLngZoom.zoom, 0.0001f)
    }

    @Test
    fun `newCameraPosition creates NewCameraPosition update`() {
        val position =
            CameraPosition(
                target = LatLng(51.4661, 7.2491),
                zoom = 14.0f,
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
        val zoom = 15.0f
        val update = CameraUpdateFactory.zoomTo(zoom)

        assertTrue(update is CameraUpdate.ZoomTo)
        assertEquals(zoom, (update as CameraUpdate.ZoomTo).zoom, 0.0001f)
    }

    @Test
    fun `zoomBy creates ZoomBy update with positive amount`() {
        val amount = 2.5f
        val update = CameraUpdateFactory.zoomBy(amount)

        assertTrue(update is CameraUpdate.ZoomBy)
        assertEquals(amount, (update as CameraUpdate.ZoomBy).amount, 0.0001f)
    }

    @Test
    fun `zoomBy creates ZoomBy update with negative amount`() {
        val amount = -1.5f
        val update = CameraUpdateFactory.zoomBy(amount)

        assertTrue(update is CameraUpdate.ZoomBy)
        assertEquals(amount, (update as CameraUpdate.ZoomBy).amount, 0.0001f)
    }

    @Test
    fun `scrollBy creates ScrollBy update`() {
        val xPixels = 100f
        val yPixels = -50f
        val update = CameraUpdateFactory.scrollBy(xPixels, yPixels)

        assertTrue(update is CameraUpdate.ScrollBy)
        val scrollUpdate = update as CameraUpdate.ScrollBy
        assertEquals(xPixels, scrollUpdate.xPixels, 0.01f)
        assertEquals(yPixels, scrollUpdate.yPixels, 0.01f)
    }

    @Test
    fun `newLatLngBounds creates NewLatLngBounds update`() {
        val bounds = LatLngBounds(LatLng(51.46, 7.24), LatLng(51.47, 7.25))
        val padding = 100
        val update = CameraUpdateFactory.newLatLngBounds(bounds, padding)

        assertTrue(update is CameraUpdate.NewLatLngBounds)
        val boundsUpdate = update as CameraUpdate.NewLatLngBounds
        assertEquals(bounds, boundsUpdate.bounds)
        assertEquals(padding, boundsUpdate.padding)
    }

    @Test
    fun `newLatLngBounds with dimensions creates NewLatLngBoundsWithSize update`() {
        val bounds = LatLngBounds(LatLng(51.46, 7.24), LatLng(51.47, 7.25))
        val width = 1080
        val height = 1920
        val padding = 50
        val update = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding)

        assertTrue(update is CameraUpdate.NewLatLngBoundsWithSize)
        val boundsUpdate = update as CameraUpdate.NewLatLngBoundsWithSize
        assertEquals(bounds, boundsUpdate.bounds)
        assertEquals(width, boundsUpdate.width)
        assertEquals(height, boundsUpdate.height)
        assertEquals(padding, boundsUpdate.padding)
    }
}
