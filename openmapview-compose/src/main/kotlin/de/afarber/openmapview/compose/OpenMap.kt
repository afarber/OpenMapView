/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.OnMarkerDragListener
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Marker as MapMarker

/**
 * Shows an [OpenMapView] in Compose.
 *
 * The map manages its own lifecycle, so no lifecycle code is needed. Its camera is kept in
 * [cameraPositionState]: hoist the state above the screen showing the map to keep the
 * camera when the screen is left and shown again. Markers are declared in [content].
 *
 * Example:
 * ```kotlin
 * val cameraPositionState = rememberCameraPositionState {
 *     position = CameraPosition(LatLng(51.4661, 7.2491), 14f)
 * }
 * OpenMap(
 *     modifier = Modifier.fillMaxSize(),
 *     cameraPositionState = cameraPositionState,
 * ) {
 *     Marker(state = rememberMarkerState(LatLng(51.4661, 7.2491)), title = "Bochum")
 * }
 * ```
 *
 * OpenMap sets the camera move and idle listeners and the marker click and drag listeners of
 * the map. Observe the camera through [cameraPositionState] (e.g. with `snapshotFlow`) and
 * markers through [Marker]'s state and onClick instead of setting those listeners in
 * [onMapCreated].
 *
 * @param modifier Modifier for the map
 * @param cameraPositionState The camera position of the map
 * @param onMapCreated Called once when the map view is created, e.g. to add shapes or
 *   overlays. Set the initial camera through [cameraPositionState] instead.
 * @param content The markers of the map, see [Marker]
 */
@Composable
fun OpenMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    onMapCreated: (OpenMapView) -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    // Set once the view exists, so the content can add its markers to it
    var map by remember { mutableStateOf<OpenMapView?>(null) }
    val markerNodes = remember { HashMap<MapMarker, MarkerNode>() }

    AndroidView(
        factory = { context ->
            OpenMapView(context).apply {
                moveCamera(CameraUpdateFactory.newCameraPosition(cameraPositionState.position))
                onMapCreated(this)
                setOnCameraMoveListener { cameraPositionState.readFrom(this) }
                setOnCameraIdleListener { cameraPositionState.readFrom(this) }
                setOnMarkerClickListener { marker -> markerNodes[marker]?.onClick?.invoke(marker) ?: false }
                setOnMarkerDragListener(MarkerDragDispatcher(markerNodes))
                cameraPositionState.readFrom(this)
                cameraPositionState.map = this
                map = this
            }
        },
        modifier = modifier,
        update = { view -> cameraPositionState.applyTo(view) },
        onRelease = { view ->
            // Pinch zooming alone doesn't report camera moves, so read the final position
            cameraPositionState.readFrom(view)
            if (cameraPositionState.map === view) cameraPositionState.map = null
            if (map === view) map = null
        },
    )

    val currentMap = map ?: return
    val mapContext = remember(currentMap) { MapContext(currentMap, markerNodes) }
    CompositionLocalProvider(LocalMapContext provides mapContext) {
        content()
    }
}

// Writes the position of a dragged marker to its MarkerState
private class MarkerDragDispatcher(
    private val markerNodes: Map<MapMarker, MarkerNode>,
) : OnMarkerDragListener {
    override fun onMarkerDragStart(marker: MapMarker) = update(marker)

    override fun onMarkerDrag(marker: MapMarker) = update(marker)

    override fun onMarkerDragEnd(marker: MapMarker) = update(marker)

    private fun update(marker: MapMarker) {
        markerNodes[marker]?.state?.position = marker.position
    }
}
