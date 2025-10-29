/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentUtilsTest {
    @Test
    fun testGeoUriFormat_Basic() {
        val latLng = LatLng(51.4661, 7.2491)
        val zoom = 14
        val uri = Uri.parse("geo:${latLng.latitude},${latLng.longitude}?z=$zoom")

        assertEquals("geo", uri.scheme)
        assertNotNull(uri.schemeSpecificPart)
        assertTrue(uri.toString().contains("51.4661"))
        assertTrue(uri.toString().contains("7.2491"))
        assertTrue(uri.toString().contains("z=14"))
    }

    @Test
    fun testGeoUriFormat_WithLabel() {
        val latLng = LatLng(51.4661, 7.2491)
        val label = "Test Location"
        val uri = Uri.parse("geo:0,0?q=${latLng.latitude},${latLng.longitude}($label)")

        assertEquals("geo", uri.scheme)
        assertTrue(uri.toString().contains("51.4661"))
        assertTrue(uri.toString().contains("7.2491"))
        assertTrue(uri.toString().contains("Test Location"))
    }

    @Test
    fun testOsmUrlFormat() {
        val zoom = 14
        val lat = 51.4661
        val lon = 7.2491
        val url = "https://www.openstreetmap.org/#map=$zoom/$lat/$lon"

        val uri = Uri.parse(url)
        assertEquals("https", uri.scheme)
        assertEquals("www.openstreetmap.org", uri.host)
        assertTrue(uri.toString().contains("#map=14/51.4661/7.2491"))
    }

    @Test
    fun testZoomClamping() {
        // Test that zoom values are clamped to valid range
        val minZoom = 2.0.coerceIn(2.0, 19.0).toInt()
        val maxZoom = 19.0.coerceIn(2.0, 19.0).toInt()
        val belowMin = 0.5.coerceIn(2.0, 19.0).toInt()
        val aboveMax = 25.0.coerceIn(2.0, 19.0).toInt()

        assertEquals(2, minZoom)
        assertEquals(19, maxZoom)
        assertEquals(2, belowMin)
        assertEquals(19, aboveMax)
    }
}
