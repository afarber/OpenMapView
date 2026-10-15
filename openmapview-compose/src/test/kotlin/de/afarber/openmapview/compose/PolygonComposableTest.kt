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
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PolygonComposableTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val center = LatLng(51.4661, 7.2491)

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

    private fun square(halfSize: Double) = listOf(
        LatLng(center.latitude - halfSize, center.longitude - halfSize),
        LatLng(center.latitude - halfSize, center.longitude + halfSize),
        LatLng(center.latitude + halfSize, center.longitude + halfSize),
        LatLng(center.latitude + halfSize, center.longitude - halfSize),
    )

    @Test
    fun testPolygon_IsAddedToMapAndRemovedWithComposition() {
        var show by mutableStateOf(true)
        showMap { if (show) Polygon(points = square(0.01), fillColor = Color.Green) }

        val polygons = map.getPolygons()
        assertEquals(1, polygons.size)
        assertEquals(square(0.01), polygons[0].points)
        assertEquals(Color.Green, polygons[0].fillColor)

        show = false
        composeRule.waitForIdle()

        assertEquals(0, map.getPolygons().size)
    }

    @Test
    fun testChangingParameter_ReplacesPolygon() {
        var fill by mutableStateOf(Color.Green)
        showMap { Polygon(points = square(0.01), fillColor = fill) }
        val old = map.getPolygons()[0]

        fill = Color.Red
        composeRule.waitForIdle()

        val polygons = map.getPolygons()
        assertEquals(1, polygons.size)
        assertEquals(Color.Red, polygons[0].fillColor)
        assertNotSame(old, polygons[0])
    }

    @Test
    fun testTappingInsideClickablePolygon_CallsOnClick() {
        var clicks = 0
        showMap { Polygon(points = square(0.01), clickable = true, onClick = { clicks++ }) }

        composeRule.runOnIdle { map.tapAt(center) }

        assertEquals(1, clicks)
    }

    @Test
    fun testTappingInsideHole_DoesNotCallOnClick() {
        var clicks = 0
        showMap { Polygon(points = square(0.01), holes = listOf(square(0.005)), clickable = true, onClick = { clicks++ }) }

        composeRule.runOnIdle { map.tapAt(center) }

        assertEquals(0, clicks)
    }
}
