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
import android.view.MotionEvent
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/**
 * Checks that a long press or a canceled gesture is not also reported as a click.
 */
@RunWith(RobolectricTestRunner::class)
class MapLongPressTest {
    private lateinit var mapView: OpenMapView
    private var clicks = 0
    private var longClick: LatLng? = null

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.setCenter(LatLng(51.4661, 7.2491))
        mapView.setZoom(14f)
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        mapView.layout(0, 0, 400, 400)
        mapView.setOnMapClickListener { clicks++ }
        mapView.setOnMapLongClickListener { longClick = it }
    }

    private fun touch(
        action: Int,
        time: Long,
        x: Float = 200f,
        y: Float = 200f,
    ) {
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        mapView.onTouchEvent(event)
        event.recycle()
    }

    @Test
    fun testTap_IsClick() {
        touch(MotionEvent.ACTION_DOWN, 0L)
        touch(MotionEvent.ACTION_UP, 50L)

        assertEquals(1, clicks)
    }

    @Test
    fun testLongPress_IsNotAlsoClick() {
        touch(MotionEvent.ACTION_DOWN, 0L)
        // The gesture detector reports the long press after its timeout
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        touch(MotionEvent.ACTION_UP, 1000L)

        assertNotNull(longClick)
        assertEquals(0, clicks)
    }

    @Test
    fun testTapAfterLongPress_IsClick() {
        touch(MotionEvent.ACTION_DOWN, 0L)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        touch(MotionEvent.ACTION_UP, 1000L)

        touch(MotionEvent.ACTION_DOWN, 2000L)
        touch(MotionEvent.ACTION_UP, 2050L)

        assertEquals(1, clicks)
    }

    @Test
    fun testCanceledGesture_IsNotClick() {
        // e.g. when a parent takes over the gesture
        touch(MotionEvent.ACTION_DOWN, 0L)
        touch(MotionEvent.ACTION_CANCEL, 50L)

        assertEquals(0, clicks)
    }

    @Test
    fun testLongPressOnInfoWindow_IsInfoWindowLongClick() {
        val marker = mapView.addMarker(infoWindowMarker())
        mapView.showInfoWindow(marker)
        var longClicked: Marker? = null
        var infoWindowClicks = 0
        mapView.setOnInfoWindowLongClickListener { longClicked = it }
        mapView.setOnInfoWindowClickListener { infoWindowClicks++ }

        // The info window is 44 to 90 pixels above the marker at the view center
        touch(MotionEvent.ACTION_DOWN, 0L, 200f, 130f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        touch(MotionEvent.ACTION_UP, 1000L, 200f, 130f)

        assertSame(marker, longClicked)
        assertNull(longClick)
        assertEquals(0, infoWindowClicks)
    }

    @Test
    fun testLongPressBesideInfoWindow_IsMapLongClick() {
        val marker = mapView.addMarker(infoWindowMarker())
        mapView.showInfoWindow(marker)
        var longClicked: Marker? = null
        mapView.setOnInfoWindowLongClickListener { longClicked = it }

        touch(MotionEvent.ACTION_DOWN, 0L, 20f, 380f)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
        touch(MotionEvent.ACTION_UP, 1000L, 20f, 380f)

        assertNull(longClicked)
        assertNotNull(longClick)
    }

    // A 20x40 icon anchored at its bottom center, with a 46 pixels high info window at density 1
    private fun infoWindowMarker() =
        Marker(
            position = LatLng(51.4661, 7.2491),
            title = "Title",
            snippet = "Snippet",
            icon = BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888)),
        )
}
