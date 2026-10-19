/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.os.Looper
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.OnCameraMoveStartedListener
import de.afarber.openmapview.OpenMapView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowChoreographer
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
                modifier = FillTestWindow,
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
        // Display frames come only as the clock advances, as on a device, so idling doesn't
        // run the frames of the whole animation. Compose waits for its own frames, so the
        // main looper is run directly until the choreographer is unpaused again.
        ShadowChoreographer.setPaused(true)
        try {
            shadowOf(Looper.getMainLooper()).idle()
            map.stopAnimation()
            shadowOf(Looper.getMainLooper()).idle()
        } finally {
            ShadowChoreographer.setPaused(false)
        }

        assertEquals("cancelled", result)
    }

    @Test
    fun testBearing_GoesToTheMapAndBack() {
        val state = CameraPositionState(bochum.copy(bearing = 90f))
        val map = showMap(state)
        assertEquals(90f, map.getCameraPosition().bearing)

        composeRule.runOnIdle { state.position = london.copy(bearing = 217f) }
        composeRule.waitForIdle()

        assertEquals(217f, map.getCameraPosition().bearing)
        assertEquals(217f, state.position.bearing)
    }

    @Test
    fun testMove_MovesAttachedMapAndState() {
        val state = CameraPositionState(bochum)
        val map = showMap(state)

        composeRule.runOnIdle { state.move(CameraUpdateFactory.newCameraPosition(london)) }
        composeRule.waitForIdle()

        assertPosition(london, map.getCameraPosition())
        assertPosition(london, state.position)
        assertFalse(state.isMoving)
    }

    @Test
    fun testMove_BeforeMapIsShown_FitsBoundsOnceLaidOut() {
        val state = CameraPositionState(bochum)
        val bounds = LatLngBounds(LatLng(51.44, 7.20), LatLng(51.50, 7.30))
        // Called before the map exists, e.g. when the screen is set up
        state.move(CameraUpdateFactory.newLatLngBounds(bounds, 0))
        var map: OpenMapView? = null

        composeRule.setContent {
            OpenMap(modifier = FillTestWindow, cameraPositionState = state, onMapCreated = { map = it })
        }
        composeRule.waitForIdle()

        // The bounds fit the laid out map, which needs its size
        val view = map!!
        val southwest = view.getProjection().toScreenLocation(bounds.southwest)
        val northeast = view.getProjection().toScreenLocation(bounds.northeast)
        assertTrue(southwest.x >= -1 && northeast.x <= view.width + 1)
        assertTrue(northeast.y >= -1 && southwest.y <= view.height + 1)
        assertTrue(northeast.x - southwest.x >= view.width - 2 || southwest.y - northeast.y >= view.height - 2)
        assertPosition(view.getCameraPosition(), state.position)
    }

    @Test
    fun testMove_LaterMoveReplacesPendingOne() {
        val state = CameraPositionState(bochum)
        state.move(CameraUpdateFactory.newCameraPosition(london))
        state.move(CameraUpdateFactory.zoomTo(9f))

        val map = showMap(state)

        assertEquals(9f, map.getCameraPosition().zoom, DELTA.toFloat())
        assertEquals(bochum.target.latitude, map.getCameraPosition().target.latitude, DELTA)
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
    fun testMovingState_StartsIdleWithoutMovement() {
        val state = CameraPositionState(bochum)
        showMap(state)

        assertFalse(state.isMoving)
        assertEquals(CameraMoveStartedReason.NO_MOVEMENT_YET, state.cameraMoveStartedReason)
    }

    @Test
    fun testMovingState_DuringAndAfterAnimation() {
        val state = CameraPositionState(bochum)
        val map = showMap(state)

        composeRule.runOnIdle {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    state.animate(CameraUpdateFactory.zoomTo(16f), 60_000)
                } catch (_: CancellationException) {
                }
            }
        }
        // As in testInterruptedAnimation_ThrowsCancellationException
        ShadowChoreographer.setPaused(true)
        try {
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(state.isMoving)
            assertEquals(CameraMoveStartedReason.DEVELOPER_ANIMATION, state.cameraMoveStartedReason)

            // A stopped animation reports no idle event, but the camera doesn't move anymore
            map.stopAnimation()
            shadowOf(Looper.getMainLooper()).idle()
        } finally {
            ShadowChoreographer.setPaused(false)
        }
        assertFalse(state.isMoving)
        assertEquals(CameraMoveStartedReason.DEVELOPER_ANIMATION, state.cameraMoveStartedReason)
    }

    @Test
    fun testMovingState_InstantMoveEndsIdle() {
        val state = CameraPositionState(bochum)
        val map = showMap(state)

        composeRule.runOnIdle { map.moveCamera(CameraUpdateFactory.newCameraPosition(london)) }
        composeRule.waitForIdle()

        assertFalse(state.isMoving)
        assertEquals(CameraMoveStartedReason.DEVELOPER_ANIMATION, state.cameraMoveStartedReason)
    }

    @Test
    fun testMovingState_PanGesture() {
        val state = CameraPositionState(bochum)
        val map = showMap(state)

        fun touch(
            time: Long,
            action: Int,
            x: Float,
        ) {
            val event = MotionEvent.obtain(0, time, action, x, 300f, 0)
            map.onTouchEvent(event)
            event.recycle()
        }
        composeRule.runOnIdle {
            touch(0, MotionEvent.ACTION_DOWN, 200f)
            touch(16, MotionEvent.ACTION_MOVE, 250f)
            touch(32, MotionEvent.ACTION_MOVE, 300f)
        }
        composeRule.waitForIdle()
        assertTrue(state.isMoving)
        assertEquals(CameraMoveStartedReason.GESTURE, state.cameraMoveStartedReason)

        composeRule.runOnIdle { touch(48, MotionEvent.ACTION_UP, 300f) }
        composeRule.waitForIdle()
        assertFalse(state.isMoving)
        assertEquals(CameraMoveStartedReason.GESTURE, state.cameraMoveStartedReason)
    }

    @Test
    fun testCameraMoveStartedReason_FromInt() {
        assertEquals(CameraMoveStartedReason.GESTURE, CameraMoveStartedReason.fromInt(OnCameraMoveStartedListener.REASON_GESTURE))
        assertEquals(CameraMoveStartedReason.API_ANIMATION, CameraMoveStartedReason.fromInt(OnCameraMoveStartedListener.REASON_API_ANIMATION))
        assertEquals(
            CameraMoveStartedReason.DEVELOPER_ANIMATION,
            CameraMoveStartedReason.fromInt(OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION),
        )
        assertEquals(CameraMoveStartedReason.UNKNOWN, CameraMoveStartedReason.fromInt(42))
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
