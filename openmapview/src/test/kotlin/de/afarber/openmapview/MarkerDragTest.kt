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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkerDragTest {
    private lateinit var mapView: OpenMapView
    private lateinit var marker: Marker
    private val center = LatLng(51.5074, -0.1278)
    private var dragStarts = 0
    private var cameraMoveStarts = 0

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
        mapView.layout(0, 0, 400, 400)
        mapView.setCenter(center)
        mapView.setZoom(12f)
        marker = mapView.addMarker(Marker(position = center, draggable = true))
        mapView.setOnMarkerDragListener(
            object : OnMarkerDragListener {
                override fun onMarkerDragStart(marker: Marker) {
                    dragStarts++
                }

                override fun onMarkerDrag(marker: Marker) {}

                override fun onMarkerDragEnd(marker: Marker) {}
            },
        )
        mapView.setOnCameraMoveStartedListener { cameraMoveStarts++ }
    }

    // Sends DOWN at (x, startY), MOVE events of stepY pixels each, then UP
    private fun swipe(
        x: Float,
        startY: Float,
        stepY: Float,
        steps: Int,
    ) {
        var time = 0L
        val down = MotionEvent.obtain(0, time, MotionEvent.ACTION_DOWN, x, startY, 0)
        mapView.onTouchEvent(down)
        down.recycle()
        for (i in 1..steps) {
            time += 16
            val move = MotionEvent.obtain(0, time, MotionEvent.ACTION_MOVE, x, startY + i * stepY, 0)
            mapView.onTouchEvent(move)
            move.recycle()
        }
        time += 16
        val up = MotionEvent.obtain(0, time, MotionEvent.ACTION_UP, x, startY + steps * stepY, 0)
        mapView.onTouchEvent(up)
        up.recycle()
    }

    @Test
    fun testSmallMoves_OnDraggableMarker_DragMarkerWithoutPanning() {
        // Each step is below the 10 px slop, but together they exceed it
        swipe(x = 200f, startY = 190f, stepY = 5f, steps = 20)

        assertEquals(1, dragStarts)
        assertEquals(0, cameraMoveStarts)
        assertNotEquals(center, marker.position)
        assertEquals(center, mapView.getCameraPosition().target)
    }

    @Test
    fun testDrag_MovesTheMarkerByTheFingerMovement() {
        val start = mapView.getProjection().toScreenLocation(marker.position)

        // Grab the pin by its head, 10 px above its tip at the anchor, and move it 100 px down
        swipe(x = start.x.toFloat(), startY = start.y - 10f, stepY = 5f, steps = 20)

        // The tip moved as far as the finger, it did not jump to the finger first
        val end = mapView.getProjection().toScreenLocation(marker.position)
        assertEquals(start.x, end.x)
        assertEquals(start.y + 100, end.y)
    }

    @Test
    fun testMoveWithinSlop_OnDraggableMarker_NeitherDragsNorPans() {
        swipe(x = 200f, startY = 190f, stepY = 4f, steps = 2)

        assertEquals(0, dragStarts)
        assertEquals(0, cameraMoveStarts)
        assertEquals(center, marker.position)
    }

    @Test
    fun testMove_OnEmptyMap_Pans() {
        swipe(x = 50f, startY = 50f, stepY = 5f, steps = 20)

        assertEquals(0, dragStarts)
        assertEquals(1, cameraMoveStarts)
        assertEquals(center, marker.position)
    }
}
