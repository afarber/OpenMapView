/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScaleBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testMetersPerPixel_HalvesPerZoomLevelAndShrinksWithLatitude() {
        assertEquals(156543.03, metersPerPixel(0.0, 0f), 0.01)
        assertEquals(156543.03 / 1024, metersPerPixel(0.0, 10f), 0.001)
        // At 60 degrees the Mercator scale is cos(60) = 0.5
        assertEquals(156543.03 / 2, metersPerPixel(60.0, 0f), 0.01)
        // A fractional zoom scales smoothly
        assertEquals(metersPerPixel(0.0, 10f) / Math.sqrt(2.0), metersPerPixel(0.0, 10.5f), 0.001)
        // Tiles are 256 dp, so a screen pixel at density 3 covers a third
        assertEquals(156543.03 / 3, metersPerPixel(0.0, 0f, 3f), 0.01)
    }

    @Test
    fun testRoundDistance_PicksOneTwoOrFiveTimesPowerOfTen() {
        assertEquals(500.0, roundDistance(730.0), 0.0)
        assertEquals(200.0, roundDistance(499.0), 0.0)
        assertEquals(100.0, roundDistance(199.9), 0.0)
        assertEquals(5.0, roundDistance(5.0), 0.0)
        assertEquals(0.5, roundDistance(0.9), 1e-9)
        assertEquals(0.0, roundDistance(0.0), 0.0)
    }

    @Test
    fun testMetricScale_SwitchesToKilometers() {
        assertEquals(Scale(500.0, "500 m"), metricScale(730.0))
        assertEquals(Scale(2000.0, "2 km"), metricScale(2400.0))
    }

    @Test
    fun testImperialScale_SwitchesToMiles() {
        // 300 m are 984 ft
        val feet = imperialScale(300.0)
        assertEquals("500 ft", feet.label)
        assertEquals(152.4, feet.meters, 0.001)

        // 5 km are 3.1 mi
        val miles = imperialScale(5000.0)
        assertEquals("2 mi", miles.label)
        assertEquals(3218.69, miles.meters, 0.01)
    }

    @Test
    fun testScaleBar_ShowsLabelsForCamera() {
        val cameraPositionState = CameraPositionState(CameraPosition(LatLng(51.4661, 7.2491), 14f))
        composeRule.setContent { ScaleBar(cameraPositionState) }

        // At zoom 14 in Bochum a pixel is 5.98 m, so the 65 px bar can show up to 389 m
        composeRule.onNodeWithText("200 m").assertIsDisplayed()
        composeRule.onNodeWithText("1000 ft").assertIsDisplayed()

        composeRule.runOnIdle { cameraPositionState.position = CameraPosition(LatLng(51.4661, 7.2491), 10f) }

        composeRule.onNodeWithText("5 km").assertIsDisplayed()
        composeRule.onNodeWithText("2 mi").assertIsDisplayed()
    }

    @Test
    fun testDisappearingScaleBar_HidesAfterCameraStops() {
        val cameraPositionState = CameraPositionState(CameraPosition(LatLng(51.4661, 7.2491), 14f))
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { DisappearingScaleBar(cameraPositionState, visibilityDurationMillis = 1000) }

        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText("200 m").assertIsDisplayed()

        composeRule.mainClock.advanceTimeBy(2000)
        composeRule.onNodeWithText("200 m").assertDoesNotExist()

        // A camera change shows it again
        cameraPositionState.position = CameraPosition(LatLng(51.4661, 7.2491), 15f)
        // With the clock paused, the test must announce its state change itself
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(500)
        composeRule.onNodeWithText("100 m").assertIsDisplayed()
    }
}
