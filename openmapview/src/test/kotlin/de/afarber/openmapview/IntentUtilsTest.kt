/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun testOpenLocation_StartsGeoIntentWhenMapAppExists() {
        val started = mutableListOf<Intent>()
        val context = mockk<Context>()
        every { context.startActivity(any()) } answers { started += firstArg<Intent>() }

        assertTrue(openLocationInExternalApp(LatLng(51.4661, 7.2491), 14f, context))

        assertEquals(1, started.size)
        assertEquals(Intent.ACTION_VIEW, started[0].action)
        assertEquals("geo", started[0].data?.scheme)
    }

    @Test
    fun testOpenLocation_FallsBackToBrowserWithoutMapApp() {
        val started = mutableListOf<Intent>()
        val context = mockk<Context>()
        every { context.startActivity(any()) } answers {
            val intent = firstArg<Intent>()
            if (intent.data?.scheme == "geo") throw ActivityNotFoundException()
            started += intent
        }

        assertTrue(openLocationInExternalApp(LatLng(51.4661, 7.2491), 14f, context))

        assertEquals(1, started.size)
        assertEquals("https", started[0].data?.scheme)
        assertEquals("www.openstreetmap.org", started[0].data?.host)
    }

    @Test
    fun testOpenLocation_ReturnsFalseWhenNothingCanBeStarted() {
        val context = mockk<Context>()
        every { context.startActivity(any()) } throws ActivityNotFoundException()

        assertFalse(openLocationInExternalApp(LatLng(51.4661, 7.2491), 14f, context))
    }

    @Test
    fun testOpenLocation_EncodesTheLabel() {
        val started = mutableListOf<Intent>()
        val context = mockk<Context>()
        every { context.startActivity(any()) } answers { started += firstArg<Intent>() }

        openLocationInExternalApp(LatLng(51.4783, 7.2231), 15f, context, "Main Station (Hbf) & Bars")

        // The label is encoded, so its "&" and parentheses don't end the query or the label
        val uri = started[0].data!!
        assertEquals("0,0?q=51.478300,7.223100(Main%20Station%20%28Hbf%29%20%26%20Bars)", uri.encodedSchemeSpecificPart)
        assertEquals("0,0?q=51.478300,7.223100(Main Station (Hbf) & Bars)", uri.schemeSpecificPart)
    }

    @Test
    fun testOpenLocation_BrowserFallbackMarksTheLocation() {
        val started = mutableListOf<Intent>()
        val context = mockk<Context>()
        every { context.startActivity(any()) } answers {
            val intent = firstArg<Intent>()
            if (intent.data?.scheme == "geo") throw ActivityNotFoundException()
            started += intent
        }

        openLocationInExternalApp(LatLng(51.4783, 7.2231), 15f, context)

        val uri = started[0].data!!
        assertEquals("51.478300", uri.getQueryParameter("mlat"))
        assertEquals("7.223100", uri.getQueryParameter("mlon"))
        assertEquals("map=15/51.478300/7.223100", uri.fragment)
    }

    @Test
    fun testOpenLocation_RoundsToSixDecimals() {
        val started = mutableListOf<Intent>()
        val context = mockk<Context>()
        every { context.startActivity(any()) } answers { started += firstArg<Intent>() }

        openLocationInExternalApp(LatLng(51.4765919477482, 7.217931747436523), 15f, context)

        assertEquals("geo:51.476592,7.217932?z=15", started[0].data.toString())
    }
}
