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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkerIconLoadingTest {
    private lateinit var context: Context
    private lateinit var controller: MapController

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        controller = MapController(context)
    }

    @Test
    fun testXmlDrawableResource_IsRenderedAtIntrinsicSize() {
        // btn_default is an XML selector, which BitmapFactory cannot decode
        val resourceId = android.R.drawable.btn_default
        val drawable = ContextCompat.getDrawable(context, resourceId)!!

        val bitmap = controller.loadBitmap(BitmapDescriptorFactory.fromResource(resourceId))

        assertNotSame(controller.loadBitmap(null), bitmap)
        assertEquals(drawable.intrinsicWidth, bitmap.width)
        assertEquals(drawable.intrinsicHeight, bitmap.height)
    }

    @Test
    fun testResourceBitmap_IsDecodedOnce() {
        val descriptor = BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass)

        val first = controller.loadBitmap(descriptor)
        val second = controller.loadBitmap(BitmapDescriptorFactory.fromResource(android.R.drawable.ic_menu_compass))

        assertSame(first, second)
    }

    @Test
    fun testMissingResource_FallsBackToDefaultIcon() {
        val bitmap = controller.loadBitmap(BitmapDescriptorFactory.fromResource(0))

        assertSame(controller.loadBitmap(null), bitmap)
    }

    @Test
    fun testMissingAsset_FallsBackToDefaultIcon() {
        val bitmap = controller.loadBitmap(BitmapDescriptorFactory.fromAsset("missing.png"))

        assertSame(controller.loadBitmap(null), bitmap)
    }
}
