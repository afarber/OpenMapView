/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Checks that a tap may move as far as the system touch slop, which scales with the density.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
class TapSlopTest {
    private lateinit var mapView: OpenMapView
    private var clicks = 0

    @Before
    fun setUp() {
        mapView = OpenMapView(ApplicationProvider.getApplicationContext())
        mapView.setMapType(MapType.NONE)
        mapView.layout(0, 0, 800, 800)
        mapView.setCenter(LatLng(51.4661, 7.2491))
        mapView.setZoom(10f)
        mapView.setOnMapClickListener { clicks++ }
    }

    private fun touch(
        action: Int,
        time: Long,
        x: Float,
    ) {
        val event = MotionEvent.obtain(0, time, action, x, 300f, 0)
        mapView.onTouchEvent(event)
        event.recycle()
    }

    @Test
    fun testShakyTapAtHighDensity_IsClick() {
        // 15 px are 5 dp at density 3, within the touch slop of 8 dp
        touch(MotionEvent.ACTION_DOWN, 0, 300f)
        touch(MotionEvent.ACTION_MOVE, 30, 315f)
        touch(MotionEvent.ACTION_UP, 60, 315f)
        confirmTap()

        assertEquals(1, clicks)
    }

    @Test
    fun testMoveBeyondSlop_IsNoClick() {
        val beyond = ViewConfiguration.get(ApplicationProvider.getApplicationContext()).scaledTouchSlop + 10f
        touch(MotionEvent.ACTION_DOWN, 0, 300f)
        touch(MotionEvent.ACTION_MOVE, 30, 300f + beyond)
        touch(MotionEvent.ACTION_UP, 60, 300f + beyond)
        confirmTap()

        assertEquals(0, clicks)
    }
}
