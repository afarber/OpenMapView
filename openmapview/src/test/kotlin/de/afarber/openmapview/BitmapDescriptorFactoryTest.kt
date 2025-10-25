/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class BitmapDescriptorFactoryTest {
    @Test
    fun testHueConstants() {
        // Verify all HUE constants have expected values
        assertEquals(0f, BitmapDescriptorFactory.HUE_RED, 0.001f)
        assertEquals(30f, BitmapDescriptorFactory.HUE_ORANGE, 0.001f)
        assertEquals(60f, BitmapDescriptorFactory.HUE_YELLOW, 0.001f)
        assertEquals(120f, BitmapDescriptorFactory.HUE_GREEN, 0.001f)
        assertEquals(180f, BitmapDescriptorFactory.HUE_CYAN, 0.001f)
        assertEquals(210f, BitmapDescriptorFactory.HUE_AZURE, 0.001f)
        assertEquals(240f, BitmapDescriptorFactory.HUE_BLUE, 0.001f)
        assertEquals(270f, BitmapDescriptorFactory.HUE_VIOLET, 0.001f)
        assertEquals(300f, BitmapDescriptorFactory.HUE_MAGENTA, 0.001f)
        assertEquals(330f, BitmapDescriptorFactory.HUE_ROSE, 0.001f)
    }

    @Test
    fun testDefaultMarker_Red() {
        val bitmap = BitmapDescriptorFactory.defaultMarker()
        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(72, bitmap.height)
    }

    @Test
    fun testDefaultMarker_WithHue() {
        val bitmap = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(72, bitmap.height)
    }

    @Test
    fun testDefaultMarker_AllPredefinedColors() {
        // Test that all predefined colors generate valid bitmaps
        val hues =
            listOf(
                BitmapDescriptorFactory.HUE_RED,
                BitmapDescriptorFactory.HUE_ORANGE,
                BitmapDescriptorFactory.HUE_YELLOW,
                BitmapDescriptorFactory.HUE_GREEN,
                BitmapDescriptorFactory.HUE_CYAN,
                BitmapDescriptorFactory.HUE_AZURE,
                BitmapDescriptorFactory.HUE_BLUE,
                BitmapDescriptorFactory.HUE_VIOLET,
                BitmapDescriptorFactory.HUE_MAGENTA,
                BitmapDescriptorFactory.HUE_ROSE,
            )

        hues.forEach { hue ->
            val bitmap = BitmapDescriptorFactory.defaultMarker(hue)
            assertNotNull("Bitmap for hue $hue should not be null", bitmap)
            assertEquals(48, bitmap.width)
            assertEquals(72, bitmap.height)
        }
    }

    @Test
    fun testDefaultMarker_CustomHue() {
        // Test custom hue value (45 degrees = orange-ish)
        val bitmap = BitmapDescriptorFactory.defaultMarker(45f)
        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(72, bitmap.height)
    }

    @Test
    fun testDefaultMarker_HueRange() {
        // Test edge cases of hue range
        val bitmap0 = BitmapDescriptorFactory.defaultMarker(0f)
        val bitmap180 = BitmapDescriptorFactory.defaultMarker(180f)
        val bitmap359 = BitmapDescriptorFactory.defaultMarker(359f)

        assertNotNull(bitmap0)
        assertNotNull(bitmap180)
        assertNotNull(bitmap359)
    }

    @Test
    fun testDefaultMarker_HueWraparound() {
        // Test that hue > 360 wraps around
        val bitmap0 = BitmapDescriptorFactory.defaultMarker(0f)
        val bitmap360 = BitmapDescriptorFactory.defaultMarker(360f)
        val bitmap720 = BitmapDescriptorFactory.defaultMarker(720f)

        // All should produce valid bitmaps
        assertNotNull(bitmap0)
        assertNotNull(bitmap360)
        assertNotNull(bitmap720)
    }
}
