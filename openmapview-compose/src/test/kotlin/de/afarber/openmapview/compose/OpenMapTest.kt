/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.coroutines.cancellation.CancellationException

@RunWith(RobolectricTestRunner::class)
class OpenMapTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val bochum = CameraPosition(LatLng(51.4661, 7.2491), 14f)
    private val london = CameraPosition(LatLng(51.5074, -0.1278), 12f)

    private fun assertPosition(
        expected: CameraPosition,
        actual: CameraPosition,
    ) {
        assertEquals(expected.target.latitude, actual.target.latitude, DELTA)
        assertEquals(expected.target.longitude, actual.target.longitude, DELTA)
        assertEquals(expected.zoom, actual.zoom, DELTA.toFloat())
    }

    private fun showMap(state: CameraPositionState): OpenMapView {
        var map: OpenMapView? = null
        composeRule.setContent {
            OpenMap(
                cameraPositionState = state,
                onMapCreated = { map = it },
            )
        }
        composeRule.waitForIdle()
        return map!!
    }

    @Test
    fun testMap_StartsAtStatePosition() {
        val map = showMap(CameraPositionState(bochum))

        assertPosition(bochum, map.getCameraPosition())
    }

    @Test
    fun testSettingPosition_MovesMap() {
        val state = CameraPositionState(bochum)
        val map = showMap(state)

        composeRule.runOnIdle { state.position = london }
        composeRule.waitForIdle()

        assertPosition(london, map.getCameraPosition())
    }

    @Test
    fun testCameraMove_UpdatesState() {
        val state = CameraPositionState(bochum)
        val map = showMap(state)

        composeRule.runOnIdle { map.moveCamera(CameraUpdateFactory.newCameraPosition(london)) }
        composeRule.waitForIdle()

        assertPosition(london, state.position)
        // The update coming from the map is not applied back to it
        assertPosition(london, map.getCameraPosition())
    }

    @Test
    fun testHoistedState_KeepsCameraWhenMapIsShownAgain() {
        val state = CameraPositionState(bochum)
        var showMap by mutableStateOf(true)
        val maps = mutableListOf<OpenMapView>()
        composeRule.setContent {
            if (showMap) {
                OpenMap(
                    cameraPositionState = state,
                    onMapCreated = { maps.add(it) },
                )
            }
        }
        composeRule.waitForIdle()

        // Like pinch zooming, setZoom() doesn't report a camera move
        composeRule.runOnIdle { maps[0].setZoom(16f) }
        showMap = false
        composeRule.waitForIdle()
        assertEquals(16f, state.position.zoom, DELTA.toFloat())

        showMap = true
        composeRule.waitForIdle()
        assertEquals(2, maps.size)
        assertNotSame(maps[0], maps[1])
        assertEquals(16f, maps[1].getCameraPosition().zoom, DELTA.toFloat())
    }

    @Test
    fun testInterruptedAnimation_ThrowsCancellationException() {
        val state = CameraPositionState(bochum)
        val map = showMap(state)
        var result = ""

        composeRule.runOnIdle {
            CoroutineScope(Dispatchers.Main).launch {
                result =
                    try {
                        state.animate(CameraUpdateFactory.zoomTo(16f), 60_000)
                        "finished"
                    } catch (e: CancellationException) {
                        "cancelled"
                    }
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { map.stopAnimation() }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("cancelled", result)
    }

    @Test
    fun testAnimate_WithoutMap_ReturnsImmediately() {
        val state = CameraPositionState(bochum)
        var result = ""

        CoroutineScope(Dispatchers.Main).launch {
            state.animate(CameraUpdateFactory.zoomTo(16f))
            result = "returned"
        }
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals("returned", result)
        assertPosition(bochum, state.position)
    }

    @Test
    fun testSaver_RestoresPosition() {
        @Suppress("UNCHECKED_CAST")
        val saver = CameraPositionState.Saver as Saver<CameraPositionState, Any>
        val saved = with(saver) { SaverScope { true }.save(CameraPositionState(bochum)) }

        val restored = saver.restore(saved!!)

        assertPosition(bochum, restored!!.position)
    }

    private companion object {
        const val DELTA = 1e-4
    }
}
