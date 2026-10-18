/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Checks that a newLatLngBounds() camera update made before the layout, e.g. in onCreate(),
 * fits the bounds once the map has its size.
 */
@RunWith(RobolectricTestRunner::class)
class BoundsBeforeLayoutTest {
    private val bounds = LatLngBounds(LatLng(51.44, 7.20), LatLng(51.50, 7.30))
    private lateinit var activity: Activity

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    }

    private fun layout(mapView: OpenMapView) {
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
        )
        mapView.layout(0, 0, 400, 600)
    }

    // The camera position for the bounds when the map already has its size
    private fun fittedPosition(): CameraPosition {
        val mapView = OpenMapView(activity)
        layout(mapView)
        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20))
        return mapView.getCameraPosition()
    }

    private fun assertPosition(
        expected: CameraPosition,
        actual: CameraPosition,
    ) {
        assertEquals(expected.zoom, actual.zoom, 1e-4f)
        assertEquals(expected.target.latitude, actual.target.latitude, 1e-6)
        assertEquals(expected.target.longitude, actual.target.longitude, 1e-6)
    }

    @Test
    fun testMoveCameraBeforeLayout_FitsBoundsAfterLayout() {
        val mapView = OpenMapView(activity)

        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20))
        layout(mapView)

        assertPosition(fittedPosition(), mapView.getCameraPosition())
        assertTrue(mapView.getCameraPosition().zoom > 10f)
    }

    @Test
    fun testAnimateCameraBeforeLayout_FitsBoundsAndFinishesAfterLayout() {
        val mapView = OpenMapView(activity)
        var finished = 0
        var canceled = 0

        mapView.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, 20),
            object : CancelableCallback {
                override fun onFinish() {
                    finished++
                }

                override fun onCancel() {
                    canceled++
                }
            },
        )
        assertEquals(0, finished)
        layout(mapView)

        assertPosition(fittedPosition(), mapView.getCameraPosition())
        assertEquals(1, finished)
        assertEquals(0, canceled)
    }

    @Test
    fun testLaterCameraUpdateBeforeLayout_ReplacesBounds() {
        val mapView = OpenMapView(activity)
        val target = LatLng(48.137, 11.575)

        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20))
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(target, 12f))
        layout(mapView)

        assertPosition(CameraPosition(target, 12f), mapView.getCameraPosition())
    }

    @Test
    fun testSetZoomBeforeLayout_ReplacesBounds() {
        val mapView = OpenMapView(activity)

        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20))
        mapView.setZoom(5f)
        layout(mapView)

        assertEquals(5f, mapView.getCameraPosition().zoom, 1e-4f)
    }

    @Test
    fun testZoomPreferenceAfterBounds_KeepsBounds() {
        val mapView = OpenMapView(activity)

        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 20))
        mapView.setMaxZoomPreference(18f)
        layout(mapView)

        assertPosition(fittedPosition(), mapView.getCameraPosition())
    }
}
