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
public fun interface OnCameraMoveStartedListener {
    public companion object {
        /**
         * Camera movement initiated by a user gesture (pan or pinch zoom)
         */
        public const val REASON_GESTURE: Int = 1

        /**
         * Camera movement initiated by the map itself: a tap on the zoom buttons (see
         * [UiSettings.isZoomControlsEnabled]), the my-location button or a marker.
         */
        public const val REASON_API_ANIMATION: Int = 2

        /**
         * Camera movement initiated by the app, with [OpenMapView.moveCamera] or
         * [OpenMapView.animateCamera].
         */
        public const val REASON_DEVELOPER_ANIMATION: Int = 3
    }

    /**
     * Called when the camera starts moving.
     *
     * @param reason The reason for the camera movement. One of:
     *               [REASON_GESTURE], [REASON_API_ANIMATION], [REASON_DEVELOPER_ANIMATION]
     */
    public fun onCameraMoveStarted(reason: Int)
}
