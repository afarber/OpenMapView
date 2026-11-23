/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.graphics.Bitmap
import android.os.Looper
import androidx.core.graphics.createBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class SnapshotTest {
    private lateinit var mapView: OpenMapView
    private val results = mutableListOf<Bitmap?>()
    private val mainLooper get() = shadowOf(Looper.getMainLooper())

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.setMapType(MapType.NONE)
        activity.setContentView(mapView)
        mainLooper.idle()
        mapView.layout(0, 0, 320, 240)
    }

    @Test
    fun testSnapshot_ReturnsBitmapAtViewSize() {
        mapView.snapshot { results.add(it) }

        assertTrue(results.isEmpty())
        mainLooper.idle()

        val bitmap = assertNotNull(results.single())
        assertEquals(320, bitmap.width)
        assertEquals(240, bitmap.height)
    }

    @Test
    fun testSnapshot_ReusesMatchingBitmap() {
        val bitmap = createBitmap(320, 240)

        mapView.snapshot({ results.add(it) }, bitmap)
        mainLooper.idle()

        assertSame(bitmap, results.single())
    }

    @Test
    fun testSnapshot_ReplacesMismatchedBitmap() {
        val bitmap = createBitmap(100, 100)

        mapView.snapshot({ results.add(it) }, bitmap)
        mainLooper.idle()

        val result = assertNotNull(results.single())
        assertNotSame(bitmap, result)
        assertEquals(320, result.width)
        assertEquals(240, result.height)
    }

    @Test
    fun testSnapshot_OnZeroSizeViewReturnsNull() {
        mapView.layout(0, 0, 0, 0)

        mapView.snapshot { results.add(it) }
        mapView.snapshot({ results.add(it) }, createBitmap(100, 100))
        mainLooper.idle()

        assertEquals(listOf<Bitmap?>(null, null), results)
    }
}
