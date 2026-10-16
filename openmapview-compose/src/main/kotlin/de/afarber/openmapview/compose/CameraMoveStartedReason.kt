/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import de.afarber.openmapview.OnCameraMoveStartedListener

/**
 * Why the camera of an [OpenMap] started moving, see [CameraPositionState.cameraMoveStartedReason].
 *
 * The values match maps-compose, and [value] matches the `REASON_*` constants of
 * [OnCameraMoveStartedListener].
 *
 * @property value The reason as passed to [OnCameraMoveStartedListener], or a negative
 *   value for [UNKNOWN] and [NO_MOVEMENT_YET]
 */
enum class CameraMoveStartedReason(
    val value: Int,
) {
    /** A reason the map reported that isn't one of the values below. */
    UNKNOWN(-2),

    /** The camera hasn't moved since the state was created. */
    NO_MOVEMENT_YET(-1),

    /** The user moved the map, e.g. by panning or pinch zooming. */
    GESTURE(OnCameraMoveStartedListener.REASON_GESTURE),

    /** The map moved the camera by itself, e.g. for a tap on the zoom buttons. */
    API_ANIMATION(OnCameraMoveStartedListener.REASON_API_ANIMATION),

    /** The app moved the camera, e.g. with [CameraPositionState.animate]. */
    DEVELOPER_ANIMATION(OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION),
    ;

    companion object {
        /** Returns the reason for a `REASON_*` value of [OnCameraMoveStartedListener]. */
        fun fromInt(value: Int): CameraMoveStartedReason = entries.firstOrNull { it.value == value } ?: UNKNOWN
    }
}
