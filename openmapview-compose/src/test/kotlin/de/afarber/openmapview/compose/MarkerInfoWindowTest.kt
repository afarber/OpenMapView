/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.MapType
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

// Provided around OpenMap, to check that info window content sees the screen's locals
private val LocalWindowColor = compositionLocalOf { Color.Red }

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MarkerInfoWindowTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bochum = LatLng(51.4661, 7.2491)
    private val green = Color.Green.toArgb()
    private val white = Color.White.toArgb()

    private lateinit var map: OpenMapView

    private fun showMap(content: @Composable () -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(LocalWindowColor provides Color.Green) {
                OpenMap(
                    modifier = FillTestWindow,
                    cameraPositionState = rememberCameraPositionState { position = CameraPosition(bochum, 14f) },
                    properties = MapProperties(mapType = MapType.NONE),
                    onMapCreated = { map = it },
                    content = content,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Composable
    private fun GreenBox() {
        Box(Modifier.size(60.dp, 30.dp).background(LocalWindowColor.current))
    }

    // Draws the map; returns it with the marker's screen position. The default icon is
    // 42 px high, and its info window ends 4 px above it.
    private fun drawMap(): Triple<Bitmap, Int, Int> {
        val bitmap = createBitmap(map.width, map.height)
        map.draw(Canvas(bitmap))
        val point = map.getProjection().toScreenLocation(bochum)
        return Triple(bitmap, point.x, point.y - 46)
    }

    @Test
    fun testMarkerInfoWindow_DrawsContentAsWholeWindow() {
        val state = MarkerState(bochum)
        showMap { MarkerInfoWindow(state = state) { GreenBox() } }

        composeRule.runOnIdle { state.showInfoWindow() }
        val (bitmap, x, windowBottom) = composeRule.runOnIdle { drawMap() }

        assertTrue(state.isInfoWindowShown)
        // A 60x30 window of the green box, without a frame
        assertEquals(green, bitmap.getPixel(x, windowBottom - 15))
        assertEquals(green, bitmap.getPixel(x - 28, windowBottom - 2))
    }

    @Test
    fun testMarkerInfoWindowContent_IsDrawnInDefaultFrame() {
        val state = MarkerState(bochum)
        showMap { MarkerInfoWindowContent(state = state) { GreenBox() } }

        composeRule.runOnIdle { state.showInfoWindow() }
        val (bitmap, x, windowBottom) = composeRule.runOnIdle { drawMap() }

        // The green box with 8 px of white frame padding around it
        assertEquals(green, bitmap.getPixel(x, windowBottom - 23))
        assertEquals(white, bitmap.getPixel(x, windowBottom - 4))
    }

    @Test
    fun testContentGetsTheMarker() {
        val state = MarkerState(bochum)
        var titleSeen: String? = null
        showMap {
            MarkerInfoWindow(state = state, title = "Bochum") { marker ->
                titleSeen = marker.title
                GreenBox()
            }
        }

        composeRule.runOnIdle { state.showInfoWindow() }

        assertEquals("Bochum", titleSeen)
    }

    @Test
    fun testPlainMarker_KeepsDefaultWindow() {
        val state = MarkerState(bochum)
        showMap { Marker(state = state, title = "Bochum") }

        composeRule.runOnIdle { state.showInfoWindow() }
        val (bitmap, x, windowBottom) = composeRule.runOnIdle { drawMap() }

        // The default box is white where it has no text
        assertEquals(white, bitmap.getPixel(x, windowBottom - 3))
    }
}
