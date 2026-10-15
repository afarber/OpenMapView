/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Constraints
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkerComposableTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bochum = LatLng(51.4661, 7.2491)
    private val dortmund = LatLng(51.5136, 7.4653)

    private lateinit var map: OpenMapView

    private fun showMap(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent {
            OpenMap(
                // Without a size, AndroidView measures the map as 0x0
                modifier = Modifier.layout { measurable, constraints ->
                    val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
                    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                },
                cameraPositionState = rememberBochumCamera(),
                onMapCreated = { map = it },
                content = content,
            )
        }
        composeRule.waitForIdle()
        check(map.width > 0 && map.height > 0) { "The map must be laid out for touch tests" }
    }

    @androidx.compose.runtime.Composable
    private fun rememberBochumCamera() = rememberCameraPositionState { position = CameraPosition(bochum, 14f) }

    @Test
    fun testMarker_IsAddedToMap() {
        showMap { Marker(state = MarkerState(bochum), title = "Bochum") }

        val markers = map.getMarkers()
        assertEquals(1, markers.size)
        assertEquals("Bochum", markers[0].title)
        assertEquals(bochum, markers[0].position)
    }

    @Test
    fun testMarkerLeavingComposition_IsRemovedFromMap() {
        var show by mutableStateOf(true)
        showMap { if (show) Marker(state = MarkerState(bochum)) }
        assertEquals(1, map.getMarkers().size)

        show = false
        composeRule.waitForIdle()

        assertEquals(0, map.getMarkers().size)
    }

    @Test
    fun testSettingStatePosition_MovesMarker() {
        val state = MarkerState(bochum)
        showMap { Marker(state = state) }
        val marker = map.getMarkers()[0]

        composeRule.runOnIdle { state.position = dortmund }
        composeRule.waitForIdle()

        assertEquals(1, map.getMarkers().size)
        assertEquals(dortmund, marker.position)
    }

    @Test
    fun testChangingTitle_ReplacesMarker() {
        var title by mutableStateOf("Old")
        showMap { Marker(state = MarkerState(bochum), title = title) }
        val oldMarker = map.getMarkers()[0]

        title = "New"
        composeRule.waitForIdle()

        val markers = map.getMarkers()
        assertEquals(1, markers.size)
        assertEquals("New", markers[0].title)
        assertNotSame(oldMarker, markers[0])
    }

    @Test
    fun testDraggingMarker_UpdatesState() {
        val state = MarkerState(bochum)
        showMap { Marker(state = state, draggable = true) }

        composeRule.runOnIdle {
            // Start on the icon, just above its bottom anchor, and drag 100 px down
            val point = map.getProjection().toScreenLocation(bochum)
            val x = point.x.toFloat()
            val startY = point.y - 10f
            send(MotionEvent.ACTION_DOWN, x, startY, 0)
            for (step in 1..20) send(MotionEvent.ACTION_MOVE, x, startY + step * 5f, step * 16L)
            send(MotionEvent.ACTION_UP, x, startY + 100f, 21 * 16L)
        }
        composeRule.waitForIdle()

        assertNotEquals(bochum, state.position)
        assertEquals(map.getMarkers()[0].position, state.position)
        assertEquals(true, state.position.latitude < bochum.latitude)
    }

    @Test
    fun testTappingMarker_CallsOnClick() {
        var clicks = 0
        showMap {
            Marker(state = MarkerState(bochum), onClick = {
                clicks++
                false
            })
        }

        composeRule.runOnIdle {
            val point = map.getProjection().toScreenLocation(bochum)
            send(MotionEvent.ACTION_DOWN, point.x.toFloat(), point.y - 10f, 0)
            send(MotionEvent.ACTION_UP, point.x.toFloat(), point.y - 10f, 50)
        }

        assertEquals(1, clicks)
    }

    @Test
    fun testSaver_RestoresPosition() {
        @Suppress("UNCHECKED_CAST")
        val saver = MarkerState.Saver as Saver<MarkerState, Any>
        val saved = with(saver) { SaverScope { true }.save(MarkerState(bochum)) }

        assertEquals(bochum, saver.restore(saved!!)!!.position)
    }

    private fun send(
        action: Int,
        x: Float,
        y: Float,
        time: Long,
    ) {
        val event = MotionEvent.obtain(0, time, action, x, y, 0)
        map.onTouchEvent(event)
        event.recycle()
    }
}
