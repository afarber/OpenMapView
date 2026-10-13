/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GroundOverlayTest {
    private val image = BitmapDescriptorFactory.defaultMarker()
    private val position = LatLng(51.4661, 7.2491)
    private val bounds = LatLngBounds(LatLng(51.0, 7.0), LatLng(52.0, 8.0))

    @Test
    fun testPositionDefaults() {
        val overlay = GroundOverlay(image = image, position = position, width = 500f)

        assertEquals(position, overlay.position)
        assertEquals(500f, overlay.width)
        assertNull(overlay.height)
        assertNull(overlay.bounds)
        assertEquals(Pair(0.5f, 0.5f), overlay.anchor)
        assertEquals(0f, overlay.bearing)
        assertEquals(0f, overlay.transparency)
        assertTrue(overlay.visible)
        assertFalse(overlay.clickable)
    }

    @Test
    fun testBounds() {
        val overlay = GroundOverlay(image = image, bounds = bounds)

        assertEquals(bounds, overlay.bounds)
        assertNull(overlay.position)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNeitherPositionNorBoundsThrows() {
        GroundOverlay(image = image)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPositionWithoutWidthThrows() {
        GroundOverlay(image = image, position = position)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPositionAndBoundsThrows() {
        GroundOverlay(image = image, position = position, width = 500f, bounds = bounds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTransparencyOutOfRangeThrows() {
        GroundOverlay(image = image, bounds = bounds, transparency = 1.5f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testAnchorOutOfRangeThrows() {
        GroundOverlay(image = image, bounds = bounds, anchor = Pair(0.5f, -0.1f))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroWidthThrows() {
        GroundOverlay(image = image, position = position, width = 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroHeightThrows() {
        GroundOverlay(image = image, position = position, width = 500f, height = 0f)
    }

    @Test
    fun testEqualityIsByIdentity() {
        val overlay1 = GroundOverlay(image = image, bounds = bounds)
        val overlay2 = GroundOverlay(image = image, bounds = bounds)

        assertEquals(overlay1, overlay1)
        assertEquals(overlay1.hashCode(), overlay1.hashCode())
        assertNotEquals(overlay1, overlay2)
        assertNotEquals(overlay1, "not an overlay")
    }
}
