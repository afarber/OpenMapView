/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Checks that a map inside a ScrollView pans on vertical drags instead of the page scrolling.
 */
@RunWith(RobolectricTestRunner::class)
class MapScrollParentTest {
    private lateinit var scrollView: ScrollView
    private lateinit var mapView: OpenMapView
    private val center = LatLng(51.5074, -0.1278)

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.setCenter(center)
        mapView.setZoom(12f)

        // The map on top of content much taller than the screen
        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL
        content.addView(mapView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, MAP_HEIGHT))
        content.addView(View(activity), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 5000))
        scrollView = ScrollView(activity)
        scrollView.addView(content)
        activity.setContentView(scrollView)

        scrollView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY),
        )
        scrollView.layout(0, 0, 400, 800)
    }

    // Drags upwards over the map, dispatched to the ScrollView like real touches
    private fun dragUp() {
        var time = 0L
        val x = 200f
        val startY = 300f
        send(MotionEvent.ACTION_DOWN, x, startY, time)
        for (step in 1..20) {
            time += 16
            send(MotionEvent.ACTION_MOVE, x, startY - step * 10f, time)
        }
        send(MotionEvent.ACTION_UP, x, startY - 200f, time + 16)
    }

    private fun send(
        action: Int,
        x: Float,
        y: Float,
        time: Long,
    ) {
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        scrollView.dispatchTouchEvent(event)
        event.recycle()
    }

    @Test
    fun testVerticalDrag_PansMapInsteadOfScrollingPage() {
        dragUp()

        assertEquals(0, scrollView.scrollY)
        assertNotEquals(center, mapView.getCameraPosition().target)
    }

    @Test
    fun testVerticalDrag_WithMapGesturesDisabled_ScrollsPage() {
        mapView.getUiSettings().isScrollGesturesEnabled = false
        mapView.getUiSettings().isZoomGesturesEnabled = false

        dragUp()

        assertTrue("page scrolled", scrollView.scrollY > 0)
        assertEquals(center, mapView.getCameraPosition().target)
    }

    private companion object {
        const val MAP_HEIGHT = 400
    }
}
