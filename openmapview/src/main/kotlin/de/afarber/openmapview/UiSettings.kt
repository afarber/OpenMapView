/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Settings for the map user interface controls and gestures.
 *
 * Configure which user interactions are enabled on the map.
 */
class UiSettings {
    /**
     * Whether zoom gestures (pinch-to-zoom) are enabled.
     * Default is true.
     */
    var isZoomGesturesEnabled: Boolean = true

    /**
     * Whether scroll gestures (panning) are enabled.
     * Default is true.
     */
    var isScrollGesturesEnabled: Boolean = true

    /**
     * Whether rotate gestures are enabled.
     * Currently not implemented, always returns false.
     */
    val isRotateGesturesEnabled: Boolean = false

    /**
     * Whether tilt gestures are enabled.
     * Currently not implemented, always returns false.
     */
    val isTiltGesturesEnabled: Boolean = false

    /**
     * Enables or disables all gestures.
     *
     * @param enabled true to enable all gestures, false to disable all
     */
    fun setAllGesturesEnabled(enabled: Boolean) {
        isZoomGesturesEnabled = enabled
        isScrollGesturesEnabled = enabled
    }
}
