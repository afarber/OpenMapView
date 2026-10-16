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
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GroundOverlayComposableTest {
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

    private val image = BitmapDescriptorFactory.defaultMarker()
    private val bounds = LatLngBounds(LatLng(center.latitude - 0.005, center.longitude - 0.005), LatLng(center.latitude + 0.005, center.longitude + 0.005))

    @Test
    fun testOverlayWithBounds_IsAddedToMapAndRemovedWithComposition() {
        var show by mutableStateOf(true)
        showMap { if (show) GroundOverlay(image = image, bounds = bounds) }

        val overlays = map.getGroundOverlays()
        assertEquals(1, overlays.size)
        assertEquals(bounds, overlays[0].bounds)

        show = false
        composeRule.waitForIdle()

        assertEquals(0, map.getGroundOverlays().size)
    }

    @Test
    fun testOverlayWithPosition_IsAddedToMap() {
        showMap { GroundOverlay(image = image, position = center, width = 500f) }

        val overlay = map.getGroundOverlays().single()
        assertEquals(center, overlay.position)
        assertEquals(500f, overlay.width)
    }

    @Test
    fun testChangingTransparency_UpdatesOverlayInPlace() {
        var transparency by mutableStateOf(0f)
        showMap { GroundOverlay(image = image, bounds = bounds, transparency = transparency) }
        val old = map.getGroundOverlays()[0]

        transparency = 0.5f
        composeRule.waitForIdle()

        val overlays = map.getGroundOverlays()
        assertEquals(1, overlays.size)
        assertEquals(0.5f, overlays[0].transparency)
        // The same overlay, changed through its setters
        assertSame(old, overlays[0])
    }

    @Test
    fun testTappingClickableOverlay_CallsOnClick() {
        var clicks = 0
        showMap { GroundOverlay(image = image, bounds = bounds, clickable = true, onClick = { clicks++ }) }

        composeRule.runOnIdle { map.tapAt(center) }

        assertEquals(1, clicks)
    }
}
