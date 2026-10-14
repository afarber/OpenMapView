/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.OpenMapView

/**
 * Shows an [OpenMapView] in Compose.
 *
 * The map manages its own lifecycle, so no lifecycle code is needed. Its camera is kept in
 * [cameraPositionState]: hoist the state above the screen showing the map to keep the
 * camera when the screen is left and shown again.
 *
 * Example:
 * ```kotlin
 * val cameraPositionState = rememberCameraPositionState {
 *     position = CameraPosition(LatLng(51.4661, 7.2491), 14f)
 * }
 * OpenMap(
 *     modifier = Modifier.fillMaxSize(),
 *     cameraPositionState = cameraPositionState,
 *     onMapCreated = { map -> map.addMarker(Marker(position = LatLng(51.4661, 7.2491))) },
 * )
 * ```
 *
 * OpenMap sets the camera move and idle listeners of the map to update [cameraPositionState],
 * so observe the camera through the state (e.g. with `snapshotFlow`) instead of setting
 * those listeners in [onMapCreated].
 *
 * @param modifier Modifier for the map
 * @param cameraPositionState The camera position of the map
 * @param onMapCreated Called once when the map view is created, to add markers, shapes and
 *   other listeners. Set the initial camera through [cameraPositionState] instead.
 */
@Composable
fun OpenMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    onMapCreated: (OpenMapView) -> Unit = {},
) {
    AndroidView(
        factory = { context ->
            OpenMapView(context).apply {
                moveCamera(CameraUpdateFactory.newCameraPosition(cameraPositionState.position))
                onMapCreated(this)
                setOnCameraMoveListener { cameraPositionState.readFrom(this) }
                setOnCameraIdleListener { cameraPositionState.readFrom(this) }
                cameraPositionState.readFrom(this)
                cameraPositionState.map = this
            }
        },
        modifier = modifier,
        update = { map -> cameraPositionState.applyTo(map) },
        onRelease = { map ->
            // Pinch zooming alone doesn't report camera moves, so read the final position
            cameraPositionState.readFrom(map)
            if (cameraPositionState.map === map) cameraPositionState.map = null
        },
    )
}
