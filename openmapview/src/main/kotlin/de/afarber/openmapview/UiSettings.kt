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
     * Whether zoom controls (+/- buttons) are enabled.
     * Default is false to avoid UI clutter.
     */
    var isZoomControlsEnabled: Boolean = false

    /**
     * Whether the zoom edge effect (visual glow when attempting to zoom beyond limits) is enabled.
     * When enabled, a visual EdgeEffect glow appears on all four edges when pinch-zoom gestures
     * attempt to exceed min/max zoom limits, similar to overscroll behavior.
     * Default is true.
     */
    var isZoomEdgeEffectEnabled: Boolean = true

    /**
     * Whether scroll gestures are enabled during rotate or zoom gestures.
     * When false, panning is disabled while pinch-zooming.
     * Default is true.
     */
    var isScrollGesturesEnabledDuringRotateOrZoom: Boolean = true

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
     * Whether the compass is enabled.
     * Compass requires rotation support which is not implemented.
     * Always returns false.
     */
    val isCompassEnabled: Boolean = false

    /**
     * Whether the my-location button is enabled.
     * My-location button is not implemented in OpenMapView.
     * Always returns false.
     */
    val isMyLocationButtonEnabled: Boolean = false

    /**
     * Whether the indoor level picker is enabled.
     * Indoor mapping is not supported by standard OSM tiles.
     * Always returns false.
     */
    val isIndoorLevelPickerEnabled: Boolean = false

    /**
     * Whether the map toolbar is enabled.
     * Map toolbar (Google Maps feature) is not implemented.
     * Always returns false.
     */
    val isMapToolbarEnabled: Boolean = false

    /**
     * Enables or disables all gestures.
     *
     * This affects zoom gestures, scroll gestures, and scroll-during-zoom,
     * but does not affect UI control visibility (zoom controls, etc.).
     *
     * @param enabled true to enable all gestures, false to disable all
     */
    fun setAllGesturesEnabled(enabled: Boolean) {
        isZoomGesturesEnabled = enabled
        isScrollGesturesEnabled = enabled
        isScrollGesturesEnabledDuringRotateOrZoom = enabled
    }
}
