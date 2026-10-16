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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.PredefinedTileProviders
import de.afarber.openmapview.TileProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TileOverlayComposableTest {
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

    // Provides no tiles, so nothing is downloaded
    private val provider = TileProvider { _, _, _ -> null }

    @Test
    fun testOverlay_IsAddedToMapAndRemovedWithComposition() {
        var show by mutableStateOf(true)
        showMap { if (show) TileOverlay(tileProvider = provider, zIndex = 2f) }

        val overlays = map.getTileOverlays()
        assertEquals(1, overlays.size)
        assertEquals(provider, overlays[0].tileProvider)
        assertEquals(2f, overlays[0].zIndex)

        show = false
        composeRule.waitForIdle()

        assertEquals(0, map.getTileOverlays().size)
    }

    @Test
    fun testChangingTransparency_UpdatesOverlayInPlace() {
        var transparency by mutableStateOf(0f)
        showMap { TileOverlay(tileProvider = provider, transparency = transparency) }
        val old = map.getTileOverlays()[0]

        transparency = 0.5f
        composeRule.waitForIdle()

        val overlays = map.getTileOverlays()
        assertEquals(1, overlays.size)
        assertEquals(0.5f, overlays[0].transparency)
        // The same overlay, which keeps its downloaded tiles
        assertSame(old, overlays[0])
    }

    @Test
    fun testChangingProvider_ReplacesOverlay() {
        val other = PredefinedTileProviders.openRailwayMap()
        var current by mutableStateOf(provider)
        showMap { TileOverlay(tileProvider = current) }
        val old = map.getTileOverlays()[0]

        current = other
        composeRule.waitForIdle()

        val overlays = map.getTileOverlays()
        assertEquals(1, overlays.size)
        assertSame(other, overlays[0].tileProvider)
        assertFalse(old === overlays[0])
    }

    @Test
    fun testRecomposition_KeepsOverlayWithRememberedProvider() {
        var counter by mutableStateOf(0)
        showMap {
            val remembered = remember { provider }
            // Reading the counter recomposes the content without changing the overlay
            if (counter >= 0) TileOverlay(tileProvider = remembered)
        }
        val old = map.getTileOverlays()[0]

        counter++
        composeRule.waitForIdle()

        assertEquals(listOf(old), map.getTileOverlays())
        assertEquals(true, map.getTileOverlays()[0] === old)
    }
}
