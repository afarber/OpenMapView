/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroundOverlayBitmapTest {
    private lateinit var context: Context
    private lateinit var controller: MapController

    private val position = LatLng(51.5, 0.0)

    // btn_default is an XML selector, which is rasterized like a vector drawable
    private val xmlImage = BitmapDescriptorFactory.fromResource(android.R.drawable.btn_default)
    private val pngImage = BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = MapController(context)
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    private fun overlay(
        image: BitmapDescriptor,
        width: Float,
    ) = GroundOverlay(image = image, position = position, width = width)

    @Test
    fun testXmlDrawable_LargeOverlayIsCappedAtMaxSize() {
        val drawable = ContextCompat.getDrawable(context, android.R.drawable.btn_default)!!

        val bitmap = controller.loadGroundOverlayBitmap(overlay(xmlImage, 1000f))

        assertEquals(MapController.MAX_GROUND_OVERLAY_BITMAP_SIZE, maxOf(bitmap.width, bitmap.height))
        assertTrue(bitmap.width > drawable.intrinsicWidth)
    }

    @Test
    fun testXmlDrawable_IsRasterizedAtPowerOfTwoWidth() {
        // 20 m are about 107 px at zoom 19 and latitude 51.5
        val bitmap = controller.loadGroundOverlayBitmap(overlay(xmlImage, 20f))

        assertEquals(128, bitmap.width)
    }

    @Test
    fun testXmlDrawable_FollowsMaxZoomPreference() {
        val atMaxZoom = controller.loadGroundOverlayBitmap(overlay(xmlImage, 20f))
        controller.setMaxZoomPreference(18f)

        val atLowerZoom = controller.loadGroundOverlayBitmap(overlay(xmlImage, 20f))

        assertEquals(atMaxZoom.width / 2, atLowerZoom.width)
    }

    @Test
    fun testXmlDrawable_IsCached() {
        val first = controller.loadGroundOverlayBitmap(overlay(xmlImage, 1000f))
        val second = controller.loadGroundOverlayBitmap(overlay(xmlImage, 1000f))

        assertSame(first, second)
    }

    @Test
    fun testBoundsOverlay_IsRasterized() {
        val bounds = LatLngBounds(LatLng(51.49, -0.01), LatLng(51.51, 0.01))

        val bitmap = controller.loadGroundOverlayBitmap(GroundOverlay(image = xmlImage, bounds = bounds))

        assertEquals(MapController.MAX_GROUND_OVERLAY_BITMAP_SIZE, maxOf(bitmap.width, bitmap.height))
    }

    @Test
    fun testPngResource_IsNotRasterized() {
        val bitmap = controller.loadGroundOverlayBitmap(overlay(pngImage, 1000f))

        assertSame(controller.loadBitmap(pngImage), bitmap)
    }

    @Test
    fun testMissingResource_FallsBackToDefaultIcon() {
        val missing = BitmapDescriptorFactory.fromResource(0)

        val bitmap = controller.loadGroundOverlayBitmap(overlay(missing, 1000f))

        assertSame(controller.loadBitmap(null), bitmap)
    }
}
