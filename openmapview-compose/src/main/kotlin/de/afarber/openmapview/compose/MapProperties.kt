/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Immutable
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.MapType
import de.afarber.openmapview.OpenMapView
import kotlin.time.Duration

/**
 * Properties of an [OpenMap], such as its map type and zoom limits.
 *
 * Pass a new instance to change them, e.g. from state:
 * ```kotlin
 * var mapType by remember { mutableIntStateOf(MapType.STANDARD) }
 * OpenMap(properties = MapProperties(mapType = mapType))
 * ```
 *
 * @param mapType The tile source, one of the [MapType] constants
 * @param minZoomPreference The lowest zoom level the camera can show (2 to 19)
 * @param maxZoomPreference The highest zoom level the camera can show (2 to 19)
 * @param latLngBoundsForCameraTarget The area the camera target is kept in, or null for no limit
 */
@Immutable
data class MapProperties(
    val mapType: Int = MapType.STANDARD,
    val minZoomPreference: Float = 2f,
    val maxZoomPreference: Float = 19f,
    val latLngBoundsForCameraTarget: LatLngBounds? = null,
) {
    internal fun applyTo(
        map: OpenMapView,
        previous: MapProperties?,
    ) {
        // Setting the same map type again would reload its tiles
        if (previous == null || previous.mapType != mapType) map.setMapType(mapType)
        if (previous == null ||
            previous.minZoomPreference != minZoomPreference ||
            previous.maxZoomPreference != maxZoomPreference
        ) {
            map.resetMinMaxZoomPreference()
            map.setMinZoomPreference(minZoomPreference)
            map.setMaxZoomPreference(maxZoomPreference)
        }
        if (previous == null || previous.latLngBoundsForCameraTarget != latLngBoundsForCameraTarget) {
            map.setLatLngBoundsForCameraTarget(latLngBoundsForCameraTarget)
        }
    }
}

/**
 * The controls and gestures of an [OpenMap].
 *
 * @param zoomControlsEnabled Whether the +/- zoom buttons are shown
 * @param zoomGesturesEnabled Whether pinch and double tap zoom the map
 * @param scrollGesturesEnabled Whether dragging pans the map
 * @param infoWindowAutoDismiss How long an info window stays open, or [Duration.ZERO] to keep it
 */
@Immutable
data class MapUiSettings(
    val zoomControlsEnabled: Boolean = false,
    val zoomGesturesEnabled: Boolean = true,
    val scrollGesturesEnabled: Boolean = true,
    val infoWindowAutoDismiss: Duration = Duration.ZERO,
) {
    internal fun applyTo(map: OpenMapView) {
        val uiSettings = map.getUiSettings()
        uiSettings.isZoomControlsEnabled = zoomControlsEnabled
        uiSettings.isZoomGesturesEnabled = zoomGesturesEnabled
        uiSettings.isScrollGesturesEnabled = scrollGesturesEnabled
        uiSettings.infoWindowAutoDismiss = infoWindowAutoDismiss
        // The zoom buttons are drawn by the map
        map.invalidate()
    }
}
