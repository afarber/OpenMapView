/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapFeaturesTest {
    private lateinit var context: Context
    private lateinit var mapView: OpenMapView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
    }

    @Test
    fun testIsIndoorEnabled_ReturnsFalse() {
        assertFalse(mapView.isIndoorEnabled())
    }

    @Test
    fun testIsTrafficEnabled_ReturnsFalse() {
        assertFalse(mapView.isTrafficEnabled())
    }

    @Test
    fun testIsBuildingsEnabled_ReturnsTrue() {
        assertTrue(mapView.isBuildingsEnabled())
    }

    @Test
    fun testIsMyLocationEnabled_ReturnsFalse() {
        assertFalse(mapView.isMyLocationEnabled())
    }

    @Test
    fun testTap_PerformsClick() {
        var clicks = 0
        mapView.setOnClickListener { clicks++ }
        mapView.layout(0, 0, 400, 400)

        val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 200f, 200f, 0)
        val up = MotionEvent.obtain(0, 50, MotionEvent.ACTION_UP, 200f, 200f, 0)
        mapView.onTouchEvent(down)
        mapView.onTouchEvent(up)
        confirmTap()
        down.recycle()
        up.recycle()

        assertTrue(clicks == 1)
    }
}
