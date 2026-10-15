/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving camera movement start events.
 *
 * Implement this interface and set it using [OpenMapView.setOnCameraMoveStartedListener]
 * to receive callbacks when the camera starts moving.
 */
fun interface OnCameraMoveStartedListener {
    companion object {
        /**
         * Camera movement initiated by a user gesture (pan or pinch zoom)
         */
        const val REASON_GESTURE = 1

        /**
         * Camera movement initiated by an [OpenMapView.animateCamera] call or a tap on the
         * zoom buttons (see [UiSettings.isZoomControlsEnabled]).
         */
        const val REASON_API_ANIMATION = 2

        /**
         * Camera movement initiated by [OpenMapView.moveCamera] call.
         */
        const val REASON_DEVELOPER_ANIMATION = 3
    }

    /**
     * Called when the camera starts moving.
     *
     * @param reason The reason for the camera movement. One of:
     *               [REASON_GESTURE], [REASON_API_ANIMATION], [REASON_DEVELOPER_ANIMATION]
     */
    fun onCameraMoveStarted(reason: Int)
}
