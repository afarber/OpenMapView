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

    /** Whether the user is dragging the marker right now. Observable, e.g. to show a status. */
    var isDragging: Boolean by mutableStateOf(false)
        internal set

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
        /** Saves the position, which is Parcelable, e.g. for [rememberSaveable]. */
        val Saver: Saver<MarkerState, LatLng> =
            Saver(
                save = { it.position },
                restore = { MarkerState(it) },
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
    // Called when the camera stops, and for taps on markers of no Marker composable,
    // e.g. by Clustering
    val cameraIdleListeners: MutableList<() -> Unit>,
    val markerClickHandlers: MutableList<(MapMarker) -> Boolean>,
)

internal class MarkerNode(
    val state: MarkerState,
) {
    var marker: MapMarker? = null
    var onClick: (MapMarker) -> Boolean = { false }
    var onInfoWindowClick: (MapMarker) -> Unit = {}
    var onInfoWindowClose: (MapMarker) -> Unit = {}
    var onInfoWindowLongClick: (MapMarker) -> Unit = {}

    // Custom info window content, see MarkerInfoWindow and MarkerInfoWindowContent
    var infoWindow: (@Composable (MapMarker) -> Unit)? = null
    var infoContent: (@Composable (MapMarker) -> Unit)? = null
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
 * @param infoWindowAnchor Point of the icon the info window points at, as fractions of its size
 * @param rotation Clockwise rotation of the icon around the anchor, in degrees
 * @param flat Whether the icon lies flat on the map and turns with it, instead of facing the screen
 * @param visible Whether the marker is drawn
 * @param alpha Opacity from 0 (transparent) to 1 (opaque)
 * @param draggable Whether the user can drag the marker
 * @param zIndex Drawing order relative to other markers and shapes
 * @param tag Any object to associate with the marker
 * @param onClick Called when the marker is tapped. Return true to consume the tap; otherwise
 *   its info window opens and the camera moves to the marker
 * @param onInfoWindowClick Called when the info window of the marker is tapped
 * @param onInfoWindowClose Called when the info window of the marker is closed
 * @param onInfoWindowLongClick Called when the info window of the marker is long pressed
 */
@Composable
fun Marker(
    state: MarkerState,
    title: String? = null,
    snippet: String? = null,
    icon: BitmapDescriptor? = null,
    anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    infoWindowAnchor: Pair<Float, Float> = Pair(0.5f, 0.0f),
    rotation: Float = 0f,
    flat: Boolean = false,
    visible: Boolean = true,
    alpha: Float = 1.0f,
    draggable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapMarker) -> Boolean = { false },
    onInfoWindowClick: (MapMarker) -> Unit = {},
    onInfoWindowClose: (MapMarker) -> Unit = {},
    onInfoWindowLongClick: (MapMarker) -> Unit = {},
) {
    MarkerImpl(
        state, title, snippet, icon, anchor, infoWindowAnchor, rotation, flat, visible, alpha, draggable, zIndex, tag,
        onClick, onInfoWindowClick, onInfoWindowClose, onInfoWindowLongClick,
        infoWindow = null,
        infoContent = null,
    )
}

/**
 * A [Marker] whose whole info window is drawn by [content], without the default frame.
 *
 * The content is drawn once into an image when the info
 * window opens: call [MarkerState.showInfoWindow] again to show changes. Taps on the window
 * call [onInfoWindowClick], not clickables inside the content.
 *
 * Example:
 * ```kotlin
 * MarkerInfoWindow(state = rememberMarkerState(LatLng(51.4661, 7.2491))) { marker ->
 *     Card { Text("Custom window for ${marker.position}", Modifier.padding(8.dp)) }
 * }
 * ```
 *
 * The other parameters are the same as for [Marker].
 *
 * @param content The info window, given the marker
 */
@Composable
fun MarkerInfoWindow(
    state: MarkerState,
    title: String? = null,
    snippet: String? = null,
    icon: BitmapDescriptor? = null,
    anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    infoWindowAnchor: Pair<Float, Float> = Pair(0.5f, 0.0f),
    rotation: Float = 0f,
    flat: Boolean = false,
    visible: Boolean = true,
    alpha: Float = 1.0f,
    draggable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapMarker) -> Boolean = { false },
    onInfoWindowClick: (MapMarker) -> Unit = {},
    onInfoWindowClose: (MapMarker) -> Unit = {},
    onInfoWindowLongClick: (MapMarker) -> Unit = {},
    content: @Composable (MapMarker) -> Unit,
) {
    MarkerImpl(
        state, title, snippet, icon, anchor, infoWindowAnchor, rotation, flat, visible, alpha, draggable, zIndex, tag,
        onClick, onInfoWindowClick, onInfoWindowClose, onInfoWindowLongClick,
        infoWindow = content,
        infoContent = null,
    )
}

/**
 * A [Marker] whose info window shows [content] inside the default white frame, instead of
 * the title and snippet.
 *
 * The content is drawn once into an image when the info window opens, see [MarkerInfoWindow].
 *
 * Example:
 * ```kotlin
 * MarkerInfoWindowContent(state = rememberMarkerState(LatLng(51.4661, 7.2491)), title = "Bochum") { marker ->
 *     Column {
 *         Text(marker.title ?: "", fontWeight = FontWeight.Bold)
 *         Text("Population 365,000")
 *     }
 * }
 * ```
 *
 * The other parameters are the same as for [Marker].
 *
 * @param content The info window contents, given the marker
 */
@Composable
fun MarkerInfoWindowContent(
    state: MarkerState,
    title: String? = null,
    snippet: String? = null,
    icon: BitmapDescriptor? = null,
    anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    infoWindowAnchor: Pair<Float, Float> = Pair(0.5f, 0.0f),
    rotation: Float = 0f,
    flat: Boolean = false,
    visible: Boolean = true,
    alpha: Float = 1.0f,
    draggable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapMarker) -> Boolean = { false },
    onInfoWindowClick: (MapMarker) -> Unit = {},
    onInfoWindowClose: (MapMarker) -> Unit = {},
    onInfoWindowLongClick: (MapMarker) -> Unit = {},
    content: @Composable (MapMarker) -> Unit,
) {
    MarkerImpl(
        state, title, snippet, icon, anchor, infoWindowAnchor, rotation, flat, visible, alpha, draggable, zIndex, tag,
        onClick, onInfoWindowClick, onInfoWindowClose, onInfoWindowLongClick,
        infoWindow = null,
        infoContent = content,
    )
}

@Composable
private fun MarkerImpl(
    state: MarkerState,
    title: String?,
    snippet: String?,
    icon: BitmapDescriptor?,
    anchor: Pair<Float, Float>,
    infoWindowAnchor: Pair<Float, Float>,
    rotation: Float,
    flat: Boolean,
    visible: Boolean,
    alpha: Float,
    draggable: Boolean,
    zIndex: Float,
    tag: Any?,
    onClick: (MapMarker) -> Boolean,
    onInfoWindowClick: (MapMarker) -> Unit,
    onInfoWindowClose: (MapMarker) -> Unit,
    onInfoWindowLongClick: (MapMarker) -> Unit,
    infoWindow: (@Composable (MapMarker) -> Unit)?,
    infoContent: (@Composable (MapMarker) -> Unit)?,
) {
    val context = LocalMapContext.current ?: return
    val node = remember(state) { MarkerNode(state) }

    // The marker is added once; changed properties are set on it below
    DisposableEffect(context, node) {
        val marker =
            context.map.addMarker(
                MapMarker(
                    position = state.position,
                    title = title,
                    snippet = snippet,
                    icon = icon,
                    anchor = anchor,
                    infoWindowAnchor = infoWindowAnchor,
                    rotation = rotation,
                    isFlat = flat,
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
        node.onInfoWindowLongClick = onInfoWindowLongClick
        node.infoWindow = infoWindow
        node.infoContent = infoContent
        // Each setter redraws the map only when the value changed
        val marker = node.marker ?: return@SideEffect
        marker.position = position
        marker.title = title
        marker.snippet = snippet
        marker.icon = icon
        marker.anchor = anchor
        marker.infoWindowAnchor = infoWindowAnchor
        marker.rotation = rotation
        marker.isFlat = flat
        marker.visible = visible
        marker.alpha = alpha
        marker.draggable = draggable
        marker.zIndex = zIndex
        marker.tag = tag
    }
}
