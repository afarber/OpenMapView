/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.graphics.Canvas
import android.os.Looper
import android.view.MotionEvent
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.MapType
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
class MapPropertiesTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bochum = LatLng(51.4661, 7.2491)
    private val ruhrArea = LatLngBounds(LatLng(51.3, 6.8), LatLng(51.7, 7.7))

    private lateinit var map: OpenMapView

    private fun showMap(
        properties: () -> MapProperties = { MapProperties() },
        uiSettings: () -> MapUiSettings = { MapUiSettings() },
        onMapClick: (LatLng) -> Unit = {},
        onMapLongClick: (LatLng) -> Unit = {},
        content: @Composable () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenMap(
                modifier = FillTestWindow,
                cameraPositionState = rememberCameraPositionState { position = CameraPosition(bochum, 14f) },
                properties = properties(),
                uiSettings = uiSettings(),
                onMapClick = onMapClick,
                onMapLongClick = onMapLongClick,
                onMapCreated = { map = it },
                content = content,
            )
        }
        composeRule.waitForIdle()
        check(map.width > 0 && map.height > 0) { "The map must be laid out for touch tests" }
    }

    private fun touch(
        action: Int,
        location: LatLng,
        time: Long,
        yOffset: Int = 0,
    ) {
        val point = map.getProjection().toScreenLocation(location)
        val event = MotionEvent.obtain(0, time, action, point.x.toFloat(), (point.y + yOffset).toFloat(), 0)
        map.onTouchEvent(event)
        event.recycle()
    }

    @Test
    fun testProperties_AreSetOnTheMap() {
        showMap(
            properties = {
                MapProperties(
                    mapType = MapType.CARTO_LIGHT,
                    minZoomPreference = 10f,
                    maxZoomPreference = 16f,
                    latLngBoundsForCameraTarget = ruhrArea,
                )
            },
        )

        assertEquals(MapType.CARTO_LIGHT, map.getMapType())
        assertEquals(10f, map.getMinZoomLevel(), 0.001f)
        assertEquals(16f, map.getMaxZoomLevel(), 0.001f)
        assertEquals(ruhrArea, map.getLatLngBoundsForCameraTarget())
    }

    @Test
    fun testChangedProperties_UpdateTheMap() {
        var properties by mutableStateOf(MapProperties(maxZoomPreference = 16f))
        showMap(properties = { properties })
        assertEquals(MapType.STANDARD, map.getMapType())

        properties = MapProperties(mapType = MapType.CARTO_DARK, maxZoomPreference = 12f)
        composeRule.waitForIdle()

        assertEquals(MapType.CARTO_DARK, map.getMapType())
        assertEquals(12f, map.getMaxZoomLevel(), 0.001f)
        // The camera was at zoom 14, above the new limit
        assertEquals(12f, map.getCameraPosition().zoom, 0.001f)
        assertNull(map.getLatLngBoundsForCameraTarget())
    }

    @Test
    fun testLowerMaxZoom_UpdatesCameraState() {
        val state = CameraPositionState(CameraPosition(bochum, 17f))
        var properties by mutableStateOf(MapProperties())
        composeRule.setContent {
            OpenMap(modifier = FillTestWindow, cameraPositionState = state, properties = properties)
        }
        composeRule.waitForIdle()

        properties = MapProperties(minZoomPreference = 10f, maxZoomPreference = 16f)
        composeRule.waitForIdle()

        // The map zoomed out to the new limit, and the state follows it
        assertEquals(16f, state.position.zoom, 0.001f)
        assertFalse(state.isMoving)
    }

    @Test
    fun testContentPadding_MovesZoomButtons() {
        val state = CameraPositionState(CameraPosition(bochum, 14f))
        var map: OpenMapView? = null
        composeRule.setContent {
            OpenMap(
                modifier = FillTestWindow,
                cameraPositionState = state,
                uiSettings = MapUiSettings(zoomControlsEnabled = true),
                contentPadding = PaddingValues(end = 200.dp, bottom = 100.dp),
                onMapCreated = { map = it },
            )
        }
        composeRule.waitForIdle()
        val view = map!!
        val density = view.resources.displayMetrics.density
        // Drawing places the buttons
        composeRule.runOnIdle { view.draw(Canvas(createBitmap(view.width, view.height))) }

        // The zoom-in button sits inside the padding: 36 dp from its right and 60 dp from its bottom
        val x = view.width - (200 + 36) * density
        val y = view.height - (100 + 60) * density
        composeRule.runOnIdle {
            for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
                val event = MotionEvent.obtain(0, time, action, x, y, 0)
                view.onTouchEvent(event)
                event.recycle()
            }
        }
        composeRule.waitForIdle()

        assertEquals(CameraMoveStartedReason.API_ANIMATION, state.cameraMoveStartedReason)
    }

    @Test
    fun testUiSettings_AreSetAndUpdated() {
        var uiSettings by mutableStateOf(MapUiSettings(zoomControlsEnabled = true, scrollGesturesEnabled = false))
        showMap(uiSettings = { uiSettings })
        assertTrue(map.getUiSettings().isZoomControlsEnabled)
        assertFalse(map.getUiSettings().isScrollGesturesEnabled)

        uiSettings = MapUiSettings(zoomGesturesEnabled = false, infoWindowAutoDismiss = 3.seconds)
        composeRule.waitForIdle()

        val settings = map.getUiSettings()
        assertFalse(settings.isZoomControlsEnabled)
        assertTrue(settings.isScrollGesturesEnabled)
        assertFalse(settings.isZoomGesturesEnabled)
        assertEquals(3.seconds, settings.infoWindowAutoDismiss)
    }

    @Test
    fun testTap_CallsOnMapClick() {
        var clicked: LatLng? = null
        showMap(onMapClick = { clicked = it })

        composeRule.runOnIdle { map.tapAt(bochum) }

        val location = assertNotNullAndGet(clicked)
        assertEquals(bochum.latitude, location.latitude, 0.001)
        assertEquals(bochum.longitude, location.longitude, 0.001)
    }

    @Test
    fun testLongPress_CallsOnMapLongClick() {
        var longClicked: LatLng? = null
        showMap(onMapLongClick = { longClicked = it })

        composeRule.runOnIdle {
            touch(MotionEvent.ACTION_DOWN, bochum, 0L)
            // The gesture detector reports the long press after its timeout
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1))
            touch(MotionEvent.ACTION_UP, bochum, 1000L)
        }

        val location = assertNotNullAndGet(longClicked)
        assertEquals(bochum.latitude, location.latitude, 0.001)
    }

    @Test
    fun testMarkerState_ShowsAndHidesInfoWindow() {
        val state = MarkerState(bochum)
        var closed = 0
        showMap { Marker(state = state, title = "Bochum", onInfoWindowClose = { closed++ }) }
        assertFalse(state.isInfoWindowShown)

        composeRule.runOnIdle { state.showInfoWindow() }
        assertTrue(state.isInfoWindowShown)
        assertTrue(map.getMarkers()[0].isInfoWindowShown)

        composeRule.runOnIdle { state.hideInfoWindow() }
        assertFalse(state.isInfoWindowShown)
        assertEquals(1, closed)
    }

    @Test
    fun testTappingInfoWindow_CallsOnInfoWindowClick() {
        val state = MarkerState(bochum)
        var clickedTitle: String? = null
        showMap { Marker(state = state, title = "Bochum", onInfoWindowClick = { clickedTitle = it.title }) }

        composeRule.runOnIdle {
            state.showInfoWindow()
            // The window is drawn above the 72 px default icon, its title box is 76 px high
            touch(MotionEvent.ACTION_DOWN, bochum, 0L, yOffset = -120)
            touch(MotionEvent.ACTION_UP, bochum, 50L, yOffset = -120)
        }

        assertEquals("Bochum", clickedTitle)
    }

    @Test
    fun testMarkerLeavingComposition_DetachesState() {
        val state = MarkerState(bochum)
        var show by mutableStateOf(true)
        showMap { if (show) Marker(state = state, title = "Bochum") }

        show = false
        composeRule.waitForIdle()

        // Does nothing without a marker on the map
        state.showInfoWindow()
        assertFalse(state.isInfoWindowShown)
    }

    private fun <T : Any> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
