/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdate
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.CancelableCallback
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Holds the camera position of an [OpenMap].
 *
 * The position follows the map while the user pans and zooms, and setting it moves the map.
 * Because the state lives outside the map, it can be hoisted above the screen that shows
 * the map, so the camera survives switching tabs, and [rememberCameraPositionState] also
 * keeps it across configuration changes and process death.
 *
 * Example:
 * ```kotlin
 * val cameraPositionState = rememberCameraPositionState {
 *     position = CameraPosition(LatLng(51.4661, 7.2491), 14f)
 * }
 * OpenMap(cameraPositionState = cameraPositionState)
 * ```
 *
 * @param position The initial camera position
 */
@Stable
class CameraPositionState(
    position: CameraPosition = DEFAULT_POSITION,
) {
    /**
     * The current camera position. Updated while the camera moves; setting it moves the
     * camera of the attached map without animation.
     */
    var position: CameraPosition by mutableStateOf(position)

    /**
     * Whether the camera is moving, by a gesture or an animation. Becomes false when the
     * camera is idle again or its animation was stopped.
     */
    var isMoving: Boolean by mutableStateOf(false)
        internal set

    /**
     * Why the camera started its last move, or [CameraMoveStartedReason.NO_MOVEMENT_YET]
     * before the first one. Kept after the camera stopped.
     */
    var cameraMoveStartedReason: CameraMoveStartedReason by mutableStateOf(CameraMoveStartedReason.NO_MOVEMENT_YET)
        internal set

    // The map shown by OpenMap, or null when no map is attached
    internal var map: OpenMapView? = null

    // The last position read from or applied to the map: a different position was set by the app
    internal var mapPosition: CameraPosition? = null

    /**
     * Animates the camera of the attached map and suspends until the animation is finished.
     *
     * Returns immediately if no map is attached.
     *
     * @param update The camera update to animate to
     * @param durationMs Duration of the animation in milliseconds
     * @throws CancellationException if the animation is interrupted, e.g. by a gesture or
     *   another animation. Cancelling the calling coroutine stops the animation.
     */
    suspend fun animate(
        update: CameraUpdate,
        durationMs: Int = DEFAULT_ANIMATION_MS,
    ) {
        val map = map ?: return
        suspendCancellableCoroutine { continuation ->
            map.animateCamera(
                update,
                durationMs,
                object : CancelableCallback {
                    override fun onFinish() {
                        continuation.resume(Unit)
                    }

                    override fun onCancel() {
                        if (continuation.isActive) {
                            continuation.resumeWithException(CancellationException("Camera animation was interrupted"))
                        }
                    }
                },
            )
            continuation.invokeOnCancellation { map.stopAnimation() }
        }
    }

    // A move() made before the map was attached or laid out, applied when it is
    private var pendingUpdate: CameraUpdate? = null

    /**
     * Moves the camera of the attached map without animation, e.g. to fit bounds with
     * `CameraUpdateFactory.newLatLngBounds()` when the screen is shown.
     *
     * If no map is attached yet, or it isn't laid out, so that updates depending on its size
     * can't be calculated, the update is applied as soon as it is. A later [move] replaces a
     * pending update.
     *
     * @param update The camera update to apply
     */
    fun move(update: CameraUpdate) {
        pendingUpdate = update
        map?.let { applyPendingUpdate(it) }
    }

    // Called when the map is attached and when it was laid out
    internal fun applyPendingUpdate(map: OpenMapView) {
        val update = pendingUpdate ?: return
        if (map !== this.map) return
        if (map.width == 0 || map.height == 0) {
            map.addOnLayoutChangeListener(
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        view: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        view.removeOnLayoutChangeListener(this)
                        applyPendingUpdate(map)
                    }
                },
            )
            return
        }
        pendingUpdate = null
        map.moveCamera(update)
        // moveCamera() reports the move, unless the camera already was there
        readFrom(map)
    }

    // Called when the map's camera starts moving
    internal fun onMoveStarted(reason: Int) {
        cameraMoveStartedReason = CameraMoveStartedReason.fromInt(reason)
        isMoving = true
    }

    // Called when the map's camera moved
    internal fun readFrom(map: OpenMapView) {
        val current = map.getCameraPosition()
        mapPosition = current
        position = current
    }

    // Moves the map if the app has set a new position
    internal fun applyTo(map: OpenMapView) {
        val target = position
        if (target != mapPosition) {
            mapPosition = target
            map.moveCamera(CameraUpdateFactory.newCameraPosition(target))
        }
    }

    companion object {
        /** The position used when none is given: the whole world. */
        val DEFAULT_POSITION = CameraPosition(LatLng(0.0, 0.0), 2f)

        private const val DEFAULT_ANIMATION_MS = 250

        /** Saves the position as latitude, longitude and zoom, e.g. for [rememberSaveable]. */
        val Saver: Saver<CameraPositionState, *> =
            listSaver(
                save = {
                    val position = it.position
                    listOf(position.target.latitude, position.target.longitude, position.zoom.toDouble())
                },
                restore = {
                    CameraPositionState(CameraPosition(LatLng(it[0], it[1]), it[2].toFloat()))
                },
            )
    }
}

/**
 * Creates a [CameraPositionState] that is kept across recompositions, configuration changes
 * and process death.
 *
 * @param init Sets up the state when it is created, e.g. its initial [CameraPositionState.position]
 */
@Composable
fun rememberCameraPositionState(init: CameraPositionState.() -> Unit = {}): CameraPositionState = rememberSaveable(saver = CameraPositionState.Saver) {
    CameraPositionState().apply(init)
}
