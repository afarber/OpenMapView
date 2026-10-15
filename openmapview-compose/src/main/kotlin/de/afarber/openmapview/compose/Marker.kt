/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Marker as MapMarker

/**
 * Holds the position of a [Marker] and opens its info window.
 *
 * Setting [position] moves the marker, and dragging a draggable marker updates it.
 *
 * @param position The initial marker position
 */
@Stable
class MarkerState(
    position: LatLng = LatLng(0.0, 0.0),
) {
    /** The marker position, updated while the user drags the marker. */
    var position: LatLng by mutableStateOf(position)

    // The marker currently shown for this state and its map, set by the Marker composable
    internal var shown: Pair<OpenMapView, MapMarker>? = null

    /**
     * Whether the info window of the marker is open. Not observable: read it when needed,
     * e.g. in a click handler.
     */
    val isInfoWindowShown: Boolean
        get() = shown?.second?.isInfoWindowShown ?: false

    /**
     * Opens the info window of the marker, if it has a title or snippet, and closes any other.
     * Does nothing while the marker is not on a map.
     */
    fun showInfoWindow() {
        shown?.let { (map, marker) -> map.showInfoWindow(marker) }
    }

    /** Closes the info window of the marker. */
    fun hideInfoWindow() {
        shown?.let { (map, marker) -> map.hideInfoWindow(marker) }
    }

    companion object {
        /** Saves the position as latitude and longitude, e.g. for [rememberSaveable]. */
        val Saver: Saver<MarkerState, *> =
            listSaver(
                save = { listOf(it.position.latitude, it.position.longitude) },
                restore = { MarkerState(LatLng(it[0], it[1])) },
            )
    }
}

/**
 * Creates a [MarkerState] that is kept across recompositions, configuration changes and
 * process death.
 *
 * @param position The initial marker position; later changes of this argument are ignored,
 *   set [MarkerState.position] instead
 */
@Composable
fun rememberMarkerState(position: LatLng = LatLng(0.0, 0.0)): MarkerState = rememberSaveable(saver = MarkerState.Saver) {
    MarkerState(position)
}

// The map an OpenMap's content adds its markers and shapes to, the markers' states and
// callbacks, and the click callbacks of shapes and ground overlays
internal class MapContext(
    val map: OpenMapView,
    val markerNodes: MutableMap<MapMarker, MarkerNode>,
    val shapeClickHandlers: MutableMap<Any, () -> Unit>,
)

internal class MarkerNode(
    val state: MarkerState,
) {
    var marker: MapMarker? = null
    var onClick: (MapMarker) -> Boolean = { false }
    var onInfoWindowClick: (MapMarker) -> Unit = {}
    var onInfoWindowClose: (MapMarker) -> Unit = {}
}

internal val LocalMapContext = staticCompositionLocalOf<MapContext?> { null }

/**
 * A marker on an [OpenMap], declared in its content.
 *
 * The marker is added to the map when it enters the composition and removed when it leaves.
 * Changing [MarkerState.position] moves it; changing any other parameter replaces it.
 * Outside of [OpenMap]'s content it does nothing.
 *
 * Example:
 * ```kotlin
 * OpenMap {
 *     val state = rememberMarkerState(LatLng(51.4661, 7.2491))
 *     Marker(state = state, title = "Bochum", draggable = true)
 * }
 * ```
 *
 * @param state The marker position, also updated while the marker is dragged
 * @param title Title shown in the info window
 * @param snippet Text shown below the title in the info window
 * @param icon Custom icon, or null for the default marker
 * @param anchor Point of the icon placed at the position, as fractions of its size
 * @param visible Whether the marker is drawn
 * @param alpha Opacity from 0 (transparent) to 1 (opaque)
 * @param draggable Whether the user can drag the marker
 * @param zIndex Drawing order relative to other markers and shapes
 * @param tag Any object to associate with the marker
 * @param onClick Called when the marker is tapped, which also opens or closes its info window
 * @param onInfoWindowClick Called when the info window of the marker is tapped
 * @param onInfoWindowClose Called when the info window of the marker is closed
 */
@Composable
fun Marker(
    state: MarkerState,
    title: String? = null,
    snippet: String? = null,
    icon: BitmapDescriptor? = null,
    anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    visible: Boolean = true,
    alpha: Float = 1.0f,
    draggable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapMarker) -> Boolean = { false },
    onInfoWindowClick: (MapMarker) -> Unit = {},
    onInfoWindowClose: (MapMarker) -> Unit = {},
) {
    val context = LocalMapContext.current ?: return
    val node = remember(state) { MarkerNode(state) }

    // The marker's properties can't be changed, so a change replaces it
    DisposableEffect(context, node, title, snippet, icon, anchor, visible, alpha, draggable, zIndex, tag) {
        val marker =
            context.map.addMarker(
                MapMarker(
                    position = state.position,
                    title = title,
                    snippet = snippet,
                    icon = icon,
                    anchor = anchor,
                    visible = visible,
                    alpha = alpha,
                    draggable = draggable,
                    zIndex = zIndex,
                    tag = tag,
                ),
            )
        node.marker = marker
        context.markerNodes[marker] = node
        state.shown = context.map to marker
        onDispose {
            context.markerNodes.remove(marker)
            context.map.removeMarker(marker)
            if (node.marker === marker) node.marker = null
            if (state.shown?.second === marker) state.shown = null
        }
    }

    // Reading the position here recomposes this marker when it changes
    val position = state.position
    SideEffect {
        node.onClick = onClick
        node.onInfoWindowClick = onInfoWindowClick
        node.onInfoWindowClose = onInfoWindowClose
        val marker = node.marker ?: return@SideEffect
        if (marker.position != position) {
            marker.position = position
            context.map.invalidate()
        }
    }
}
