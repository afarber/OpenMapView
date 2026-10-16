/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Settings for the map user interface controls and gestures.
 *
 * Configure which user interactions are enabled on the map.
 */
class UiSettings {
    // Redraws the map when a setting that is drawn changes, set by OpenMapView
    internal var onDrawnSettingChanged: (() -> Unit)? = null

    /**
     * Whether zoom gestures (pinch-to-zoom) are enabled.
     * Default is true.
     */
    var isZoomGesturesEnabled: Boolean = true

    /**
     * Whether scroll gestures (panning) are enabled.
     * Default is true.
     *
     * While enabled, a drag that starts on the map pans it, also inside a scrolling parent
     * such as a ScrollView or a Compose verticalScroll. Disable it (and zoom gestures) to let
     * such drags scroll the parent instead, e.g. for a static map in a list.
     */
    var isScrollGesturesEnabled: Boolean = true

    /**
     * Whether zoom controls (+/- buttons) are enabled.
     * Default is false to avoid UI clutter.
     */
    var isZoomControlsEnabled: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            onDrawnSettingChanged?.invoke()
        }

    /**
     * Whether the edge effect (visual glow at map boundaries) is enabled.
     * When enabled, a visual EdgeEffect glow appears when:
     * - Pinch-zoom gestures attempt to exceed min/max zoom limits (all edges glow)
     * - Pan gestures attempt to exceed camera target bounds (directional glow on hit edges)
     * Similar to Android's overscroll behavior.
     * Default is true.
     */
    var isEdgeEffectEnabled: Boolean = true

    /**
     * Whether scroll gestures are enabled during rotate or zoom gestures.
     * When true, moving both fingers while pinch-zooming also pans the map; when false,
     * the map only zooms around the fingers.
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
     * Whether the my-location button is shown while the my-location layer is enabled,
     * see [OpenMapView.setMyLocationEnabled]. Tapping it moves the camera to the location.
     * Default is true, as in Google Maps.
     */
    var isMyLocationButtonEnabled: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            onDrawnSettingChanged?.invoke()
        }

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
     * Duration after which info windows automatically dismiss.
     * Set to Duration.ZERO to disable auto-dismiss (default).
     * Non-positive values are normalized to Duration.ZERO (disabled).
     *
     * Example:
     * ```kotlin
     * import kotlin.time.Duration.Companion.seconds
     *
     * mapView.uiSettings.infoWindowAutoDismiss = 3.seconds
     * ```
     */
    var infoWindowAutoDismiss: Duration = Duration.ZERO
        set(value) {
            field = if (value.isPositive()) value else Duration.ZERO
        }

    /**
     * [infoWindowAutoDismiss] in milliseconds, e.g. for Java code. 0 disables auto-dismiss.
     *
     * Example:
     * ```java
     * mapView.getUiSettings().setInfoWindowAutoDismissMillis(3000);
     * ```
     */
    var infoWindowAutoDismissMillis: Long
        get() = infoWindowAutoDismiss.inWholeMilliseconds
        set(value) {
            infoWindowAutoDismiss = value.milliseconds
        }

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
