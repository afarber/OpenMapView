/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LocationSource
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MyLocationComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bochum = LatLng(51.4661, 7.2491)

    private lateinit var map: OpenMapView

    private class FakeLocationSource : LocationSource {
        var listener: LocationSource.OnLocationChangedListener? = null
        var activations = 0

        override fun activate(listener: LocationSource.OnLocationChangedListener) {
            this.listener = listener
            activations++
        }

        override fun deactivate() {
            listener = null
        }
    }

    private fun location(position: LatLng) = Location(LocationManager.GPS_PROVIDER).apply {
        latitude = position.latitude
        longitude = position.longitude
    }

    private fun tap(
        x: Float,
        y: Float,
    ) {
        // The my-location overlay knows its button and dot positions once drawn
        map.draw(Canvas(Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)))
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
            val event = MotionEvent.obtain(0, time, action, x, y, 0)
            map.onTouchEvent(event)
            event.recycle()
        }
    }

    @Test
    fun testMyLocationEnabled_ActivatesLocationSource() {
        val source = FakeLocationSource()
        var properties by mutableStateOf(MapProperties(isMyLocationEnabled = true))
        composeRule.setContent {
            OpenMap(
                modifier = FillTestWindow,
                properties = properties,
                locationSource = source,
                onMapCreated = { map = it },
            )
        }
        composeRule.waitForIdle()

        assertTrue(map.isMyLocationEnabled())
        assertEquals(1, source.activations)

        properties = MapProperties(isMyLocationEnabled = false)
        composeRule.waitForIdle()

        assertFalse(map.isMyLocationEnabled())
    }

    @Test
    fun testMyLocationButtonEnabled_IsSetOnMap() {
        var uiSettings by mutableStateOf(MapUiSettings())
        composeRule.setContent { OpenMap(uiSettings = uiSettings, onMapCreated = { map = it }) }
        composeRule.waitForIdle()
        assertTrue(map.getUiSettings().isMyLocationButtonEnabled)

        uiSettings = MapUiSettings(myLocationButtonEnabled = false)
        composeRule.waitForIdle()

        assertFalse(map.getUiSettings().isMyLocationButtonEnabled)
    }

    @Test
    fun testCallbacks_ReceiveButtonAndDotTaps() {
        val source = FakeLocationSource()
        var buttonClicks = 0
        var clickedLocation: Location? = null
        composeRule.setContent {
            OpenMap(
                modifier = FillTestWindow,
                cameraPositionState = rememberCameraPositionState { position = CameraPosition(bochum, 14f) },
                properties = MapProperties(isMyLocationEnabled = true),
                locationSource = source,
                onMyLocationButtonClick = {
                    buttonClicks++
                    true
                },
                onMyLocationClick = { clickedLocation = it },
                onMapCreated = { map = it },
            )
        }
        composeRule.waitForIdle()

        composeRule.runOnIdle {
            source.listener!!.onLocationChanged(location(bochum))
            // The button: 16 dp margin and 20 dp radius from the top right corner
            tap(map.width - 36f, 36f)
            val point = map.getProjection().toScreenLocation(bochum)
            tap(point.x.toFloat(), point.y.toFloat())
        }

        assertEquals(1, buttonClicks)
        assertNotNull(clickedLocation)
        assertEquals(bochum.latitude, clickedLocation!!.latitude, 1e-9)
    }

    @Test
    fun testChangedLocationSource_IsUsed() {
        val first = FakeLocationSource()
        val second = FakeLocationSource()
        var source by mutableStateOf(first)
        composeRule.setContent {
            OpenMap(
                properties = MapProperties(isMyLocationEnabled = true),
                locationSource = source,
                onMapCreated = { map = it },
            )
        }
        composeRule.waitForIdle()
        assertEquals(1, first.activations)

        source = second
        composeRule.waitForIdle()

        assertEquals(1, second.activations)
        assertEquals(null, first.listener)
    }
}
