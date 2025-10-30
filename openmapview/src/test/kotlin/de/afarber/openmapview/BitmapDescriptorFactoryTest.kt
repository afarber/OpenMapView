/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapDescriptorFactoryTest {
    @Test
    fun testHueConstantsAreInValidRange() {
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
            assertTrue("Hue $hue must be in 0-360 range", hue in 0f..360f)
        }
    }

    @Test
    fun testHueConstantsAreUnique() {
        val hues =
            setOf(
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
        assertEquals(10, hues.size)
    }

    @Test
    fun testDefaultMarker_Red() {
        val descriptor = BitmapDescriptorFactory.defaultMarker()
        assertNotNull(descriptor)
        assertTrue(descriptor is BitmapDescriptor.DefaultMarker)
        assertEquals(0f, (descriptor as BitmapDescriptor.DefaultMarker).hue, 0.001f)
    }

    @Test
    fun testDefaultMarker_WithHue() {
        val descriptor = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        assertNotNull(descriptor)
        assertTrue(descriptor is BitmapDescriptor.DefaultMarker)
        assertEquals(240f, (descriptor as BitmapDescriptor.DefaultMarker).hue, 0.001f)
    }

    @Test
    fun testDefaultMarker_AllPredefinedColors() {
        // Test that all predefined colors generate valid descriptors
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
            val descriptor = BitmapDescriptorFactory.defaultMarker(hue)
            assertNotNull("Descriptor for hue $hue should not be null", descriptor)
            assertTrue(descriptor is BitmapDescriptor.DefaultMarker)
            assertEquals(hue, (descriptor as BitmapDescriptor.DefaultMarker).hue, 0.001f)
        }
    }

    @Test
    fun testDefaultMarker_CustomHue() {
        // Test custom hue value (45 degrees = orange-ish)
        val descriptor = BitmapDescriptorFactory.defaultMarker(45f)
        assertNotNull(descriptor)
        assertTrue(descriptor is BitmapDescriptor.DefaultMarker)
        assertEquals(45f, (descriptor as BitmapDescriptor.DefaultMarker).hue, 0.001f)
    }

    @Test
    fun testDefaultMarker_HueRange() {
        // Test edge cases of hue range
        val descriptor0 = BitmapDescriptorFactory.defaultMarker(0f)
        val descriptor180 = BitmapDescriptorFactory.defaultMarker(180f)
        val descriptor359 = BitmapDescriptorFactory.defaultMarker(359f)

        assertNotNull(descriptor0)
        assertNotNull(descriptor180)
        assertNotNull(descriptor359)
        assertTrue(descriptor0 is BitmapDescriptor.DefaultMarker)
        assertTrue(descriptor180 is BitmapDescriptor.DefaultMarker)
        assertTrue(descriptor359 is BitmapDescriptor.DefaultMarker)
    }

    @Test
    fun testDefaultMarker_HueWraparound() {
        // Test that hue > 360 is handled
        val descriptor0 = BitmapDescriptorFactory.defaultMarker(0f)
        val descriptor360 = BitmapDescriptorFactory.defaultMarker(360f)
        val descriptor720 = BitmapDescriptorFactory.defaultMarker(720f)

        // All should produce valid descriptors
        assertNotNull(descriptor0)
        assertNotNull(descriptor360)
        assertNotNull(descriptor720)
        assertTrue(descriptor0 is BitmapDescriptor.DefaultMarker)
        assertTrue(descriptor360 is BitmapDescriptor.DefaultMarker)
        assertTrue(descriptor720 is BitmapDescriptor.DefaultMarker)
    }

    @Test
    fun testFromResource() {
        val descriptor = BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass)
        assertNotNull(descriptor)
        assertTrue(descriptor is BitmapDescriptor.ResourceMarker)
        assertEquals(android.R.drawable.ic_menu_compass, (descriptor as BitmapDescriptor.ResourceMarker).resourceId)
    }

    @Test
    fun testFromAsset() {
        val descriptor = BitmapDescriptorFactory.fromAsset("markers/custom.png")
        assertNotNull(descriptor)
        assertTrue(descriptor is BitmapDescriptor.AssetMarker)
        assertEquals("markers/custom.png", (descriptor as BitmapDescriptor.AssetMarker).assetName)
    }

    @Test
    fun testFromBitmap() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val descriptor = BitmapDescriptorFactory.fromBitmap(bitmap)
        assertNotNull(descriptor)
        assertTrue(descriptor is BitmapDescriptor.BitmapMarker)
        assertEquals(bitmap, (descriptor as BitmapDescriptor.BitmapMarker).bitmap)
    }

    @Test
    fun testBitmapDescriptor_DefaultMarker() {
        val descriptor = BitmapDescriptor.DefaultMarker(120f)
        assertEquals(120f, descriptor.hue, 0.001f)
    }

    @Test
    fun testBitmapDescriptor_BitmapMarker() {
        val bitmap = Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
        val descriptor = BitmapDescriptor.BitmapMarker(bitmap)
        assertEquals(bitmap, descriptor.bitmap)
    }

    @Test
    fun testBitmapDescriptor_ResourceMarker() {
        val descriptor = BitmapDescriptor.ResourceMarker(android.R.drawable.ic_dialog_alert)
        assertEquals(android.R.drawable.ic_dialog_alert, descriptor.resourceId)
    }

    @Test
    fun testBitmapDescriptor_AssetMarker() {
        val descriptor = BitmapDescriptor.AssetMarker("path/to/marker.png")
        assertEquals("path/to/marker.png", descriptor.assetName)
    }
}
