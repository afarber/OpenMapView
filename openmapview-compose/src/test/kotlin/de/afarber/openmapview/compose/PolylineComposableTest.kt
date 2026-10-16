/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PolylineComposableTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val center = LatLng(51.4661, 7.2491)

    // A horizontal line through the center of the map
    private val points = listOf(LatLng(center.latitude, center.longitude - 0.01), LatLng(center.latitude, center.longitude + 0.01))

    private lateinit var map: OpenMapView

    private fun showMap(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenMap(
                modifier = FillTestWindow,
                cameraPositionState = CameraPositionState(CameraPosition(center, 14f)),
                onMapCreated = { map = it },
                content = content,
            )
        }
        composeRule.waitForIdle()
        check(map.width > 0 && map.height > 0) { "The map must be laid out for touch tests" }
    }

    @Test
    fun testPolyline_IsAddedToMapAndRemovedWithComposition() {
        var show by mutableStateOf(true)
        showMap { if (show) Polyline(points = points, strokeColor = Color.Red, strokeWidth = 20f) }

        val polylines = map.getPolylines()
        assertEquals(1, polylines.size)
        assertEquals(points, polylines[0].points)
        assertEquals(Color.Red, polylines[0].strokeColor)
        assertEquals(20f, polylines[0].strokeWidth)

        show = false
        composeRule.waitForIdle()

        assertEquals(0, map.getPolylines().size)
    }

    @Test
    fun testChangingParameter_UpdatesPolylineInPlace() {
        var color by mutableStateOf(Color.Red)
        showMap { Polyline(points = points, strokeColor = color) }
        val old = map.getPolylines()[0]

        color = Color.Blue
        composeRule.waitForIdle()

        val polylines = map.getPolylines()
        assertEquals(1, polylines.size)
        assertEquals(Color.Blue, polylines[0].strokeColor)
        // The same live shape, changed through its setters
        assertSame(old, polylines[0])
    }

    @Test
    fun testTappingClickablePolyline_CallsOnClick() {
        val clicked = mutableListOf<de.afarber.openmapview.Polyline>()
        showMap { Polyline(points = points, strokeWidth = 30f, clickable = true, onClick = { clicked.add(it) }) }

        composeRule.runOnIdle { map.tapAt(center) }

        assertEquals(listOf(map.getPolylines()[0]), clicked)
    }

    @Test
    fun testTappingNonClickablePolyline_DoesNotCallOnClick() {
        var clicks = 0
        showMap { Polyline(points = points, strokeWidth = 30f, onClick = { clicks++ }) }

        composeRule.runOnIdle { map.tapAt(center) }

        assertEquals(0, clicks)
    }
}
