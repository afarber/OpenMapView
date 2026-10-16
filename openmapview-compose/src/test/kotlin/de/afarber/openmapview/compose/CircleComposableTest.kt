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
class CircleComposableTest {
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

    @Test
    fun testCircle_IsAddedToMapAndRemovedWithComposition() {
        var show by mutableStateOf(true)
        showMap { if (show) Circle(center = center, radius = 300f, fillColor = Color.Blue) }

        val circles = map.getCircles()
        assertEquals(1, circles.size)
        assertEquals(center, circles[0].center)
        assertEquals(300f, circles[0].radius)
        assertEquals(Color.Blue, circles[0].fillColor)

        show = false
        composeRule.waitForIdle()

        assertEquals(0, map.getCircles().size)
    }

    @Test
    fun testMovingCenter_UpdatesCircleInPlace() {
        val other = LatLng(51.47, 7.25)
        var circleCenter by mutableStateOf(center)
        showMap { Circle(center = circleCenter, radius = 300f) }
        val old = map.getCircles()[0]

        circleCenter = other
        composeRule.waitForIdle()

        val circles = map.getCircles()
        assertEquals(1, circles.size)
        assertEquals(other, circles[0].center)
        // The same live shape, changed through its setters
        assertSame(old, circles[0])
    }

    @Test
    fun testTappingClickableCircle_CallsOnClick() {
        var clicks = 0
        showMap { Circle(center = center, radius = 300f, clickable = true, onClick = { clicks++ }) }

        composeRule.runOnIdle { map.tapAt(center) }

        assertEquals(1, clicks)
    }

    @Test
    fun testTappingNonClickableCircle_DoesNotCallOnClick() {
        var clicks = 0
        showMap { Circle(center = center, radius = 300f, onClick = { clicks++ }) }

        composeRule.runOnIdle { map.tapAt(center) }

        assertEquals(0, clicks)
    }
}
