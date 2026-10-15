/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import android.graphics.Bitmap as AndroidBitmap

// Provided around OpenMap, to check that the icon content sees the screen's locals
private val LocalIconColor = compositionLocalOf { Color.Red }

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MarkerComposableIconTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bochum = LatLng(51.4661, 7.2491)

    private lateinit var map: OpenMapView

    private fun showMap(content: @Composable () -> Unit) {
        composeRule.setContent {
            CompositionLocalProvider(LocalIconColor provides Color.Green) {
                OpenMap(
                    modifier = FillTestWindow,
                    cameraPositionState = rememberCameraPositionState { position = CameraPosition(bochum, 14f) },
                    onMapCreated = { map = it },
                    content = content,
                )
            }
        }
        composeRule.waitForIdle()
    }

    private fun iconBitmap(): AndroidBitmap {
        val icon = map.getMarkers().single().icon
        return (icon as BitmapDescriptor.BitmapMarker).bitmap
    }

    @Test
    fun testContent_BecomesMarkerIcon() {
        showMap {
            MarkerComposable(state = MarkerState(bochum)) {
                Box(Modifier.size(40.dp, 20.dp).background(LocalIconColor.current))
            }
        }

        val bitmap = iconBitmap()
        // Robolectric's default density is 1, so dp equal pixels
        assertEquals(40, bitmap.width)
        assertEquals(20, bitmap.height)
        assertEquals(Color.Green.toArgb(), bitmap.getPixel(20, 10))
    }

    @Test
    fun testChangedKey_DrawsIconAgain() {
        var count by mutableIntStateOf(0)
        showMap {
            MarkerComposable(count, state = MarkerState(bochum)) {
                Box(Modifier.size(20.dp).background(if (count == 0) Color.Red else Color.Blue))
            }
        }
        val oldMarker = map.getMarkers().single()
        assertEquals(Color.Red.toArgb(), iconBitmap().getPixel(10, 10))

        count = 1
        composeRule.waitForIdle()

        assertNotSame(oldMarker, map.getMarkers().single())
        assertEquals(Color.Blue.toArgb(), iconBitmap().getPixel(10, 10))
    }

    @Test
    fun testTapOnIcon_CallsOnClick() {
        var clicks = 0
        showMap {
            MarkerComposable(state = MarkerState(bochum), onClick = {
                clicks++
                false
            }) {
                Box(Modifier.size(40.dp).background(Color.Red))
            }
        }

        composeRule.runOnIdle {
            // The icon is anchored at its bottom center, so tap 20 px above the position
            val point = map.getProjection().toScreenLocation(bochum)
            for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
                val event = MotionEvent.obtain(0, time, action, point.x.toFloat(), point.y - 20f, 0)
                map.onTouchEvent(event)
                event.recycle()
            }
        }

        assertEquals(1, clicks)
    }

    @Test
    fun testDrawing_LeavesNoViewsInTheMap() {
        showMap {
            MarkerComposable(state = MarkerState(bochum)) {
                Box(Modifier.size(20.dp).background(Color.Red))
            }
        }

        assertEquals(0, map.childCount)
    }
}
