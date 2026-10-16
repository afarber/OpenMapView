/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.content.Context
import android.location.Location
import android.view.View
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.InfoWindowAdapter
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LocationSource
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
 * OpenMap sets the camera move started, move, idle and canceled listeners, the marker click, drag and info window
 * listeners, the info window adapter, and the polyline, polygon, circle and ground overlay
 * click listeners of the map.
 * Observe the camera through [cameraPositionState] (its position, [CameraPositionState.isMoving]
 * and [CameraPositionState.cameraMoveStartedReason], e.g. with `snapshotFlow`), and markers and
 * shapes through the state and callbacks of their composables, instead of setting those
 * listeners in [onMapCreated].
 *
 * @param modifier Modifier for the map
 * @param cameraPositionState The camera position of the map
 * @param properties The map type, zoom limits and camera bounds
 * @param uiSettings The zoom controls and gestures
 * @param onMapClick Called with the location when the map is tapped
 * @param onMapLongClick Called with the location when the map is long-pressed
 * @param onMapLoaded Called once when the tiles of the first screen are loaded
 * @param locationSource The source of device locations for [MapProperties.isMyLocationEnabled],
 *   or null for the platform LocationManager
 * @param onMyLocationButtonClick Called when the my-location button is tapped; return true to
 *   keep the camera where it is
 * @param onMyLocationClick Called with the location when the my-location dot is tapped
 * @param contentPadding Padding for the UI on top of the map, e.g. a toolbar or a bottom
 *   sheet: the camera centers in the area inside it, and the map's own controls (zoom buttons,
 *   my-location button and attribution) are drawn inside it
 * @param onMapCreated Called once when the map view is created, e.g. to add shapes or
 *   overlays. Set the initial camera through [cameraPositionState] instead.
 * @param content The markers and shapes of the map, see [Marker] and [Polyline]
 */
@Composable
fun OpenMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    properties: MapProperties = MapProperties(),
    uiSettings: MapUiSettings = MapUiSettings(),
    onMapClick: (LatLng) -> Unit = {},
    onMapLongClick: (LatLng) -> Unit = {},
    onMapLoaded: () -> Unit = {},
    locationSource: LocationSource? = null,
    onMyLocationButtonClick: (() -> Boolean)? = null,
    onMyLocationClick: (Location) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
    onMapCreated: (OpenMapView) -> Unit = {},
    content: @Composable () -> Unit = {},
) {
    // Set once the view exists, so the content can add its markers to it
    var map by remember { mutableStateOf<OpenMapView?>(null) }
    val markerNodes = remember { HashMap<MapMarker, MarkerNode>() }
    // Keyed by the shape added to the map, see MapElement
    val shapeClickHandlers = remember { HashMap<Any, () -> Unit>() }
    val cameraIdleListeners = remember { ArrayList<() -> Unit>() }
    val markerClickHandlers = remember { ArrayList<(MapMarker) -> Boolean>() }
    val applied = remember { AppliedSettings() }
    // Lets info window content use the composition locals of the screen, e.g. MaterialTheme
    val compositionContext = rememberCompositionContext()
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    val currentOnMapLongClick by rememberUpdatedState(onMapLongClick)
    val currentOnMapLoaded by rememberUpdatedState(onMapLoaded)
    val currentOnMyLocationButtonClick by rememberUpdatedState(onMyLocationButtonClick)
    val currentOnMyLocationClick by rememberUpdatedState(onMyLocationClick)
    val padding = contentPadding.toPixels(LocalDensity.current, LocalLayoutDirection.current)

    AndroidView(
        factory = { context ->
            OpenMapView(context).apply {
                // Before the camera, so the zoom limits and bounds apply to it
                applied.apply(this, locationSource, properties, uiSettings, padding, initial = true)
                moveCamera(CameraUpdateFactory.newCameraPosition(cameraPositionState.position))
                // Before onMapCreated, which may still replace them
                setOnMapClickListener { currentOnMapClick(it) }
                setOnMapLongClickListener { currentOnMapLongClick(it) }
                setOnMapLoadedCallback { currentOnMapLoaded() }
                setOnMyLocationButtonClickListener { currentOnMyLocationButtonClick?.invoke() ?: false }
                setOnMyLocationClickListener { currentOnMyLocationClick(it) }
                onMapCreated(this)
                setOnCameraMoveStartedListener { cameraPositionState.onMoveStarted(it) }
                setOnCameraMoveListener { cameraPositionState.readFrom(this) }
                setOnCameraIdleListener {
                    cameraPositionState.readFrom(this)
                    cameraPositionState.isMoving = false
                    cameraIdleListeners.toList().forEach { it() }
                }
                // A stopped animation reports no idle event
                setOnCameraMoveCanceledListener { cameraPositionState.isMoving = false }
                setOnMarkerClickListener { marker ->
                    markerNodes[marker]?.onClick?.invoke(marker)
                        ?: markerClickHandlers.toList().any { it(marker) }
                }
                setOnMarkerDragListener(MarkerDragDispatcher(markerNodes))
                setOnInfoWindowClickListener { marker -> markerNodes[marker]?.onInfoWindowClick?.invoke(marker) }
                setOnInfoWindowCloseListener { marker -> markerNodes[marker]?.onInfoWindowClose?.invoke(marker) }
                setInfoWindowAdapter(ComposeInfoWindowAdapter(context, markerNodes, compositionContext))
                setOnPolylineClickListener { shapeClickHandlers[it]?.invoke() }
                setOnPolygonClickListener { shapeClickHandlers[it]?.invoke() }
                setOnCircleClickListener { shapeClickHandlers[it]?.invoke() }
                setOnGroundOverlayClickListener { shapeClickHandlers[it]?.invoke() }
                cameraPositionState.readFrom(this)
                cameraPositionState.map = this
                cameraPositionState.applyPendingUpdate(this)
                map = this
            }
        },
        modifier = modifier,
        update = { view ->
            cameraPositionState.applyTo(view)
            applied.apply(view, locationSource, properties, uiSettings, padding, initial = false)
        },
        onRelease = { view ->
            // Pinch zooming alone doesn't report camera moves, so read the final position
            cameraPositionState.readFrom(view)
            if (cameraPositionState.map === view) {
                cameraPositionState.map = null
                cameraPositionState.isMoving = false
            }
            if (map === view) map = null
        },
    )

    val currentMap = map ?: return
    val mapContext =
        remember(currentMap) {
            MapContext(currentMap, markerNodes, shapeClickHandlers, cameraIdleListeners, markerClickHandlers)
        }
    CompositionLocalProvider(LocalMapContext provides mapContext) {
        content()
    }
}

// Shows the content of MarkerInfoWindow and MarkerInfoWindowContent; other markers get the
// default window. The map draws the returned view into an image.
private class ComposeInfoWindowAdapter(
    private val context: Context,
    private val markerNodes: Map<MapMarker, MarkerNode>,
    private val compositionContext: CompositionContext,
) : InfoWindowAdapter {
    override fun getInfoWindow(marker: MapMarker): View? = markerNodes[marker]?.infoWindow?.let { composeView(marker, it) }

    override fun getInfoContents(marker: MapMarker): View? = markerNodes[marker]?.infoContent?.let { composeView(marker, it) }

    private fun composeView(
        marker: MapMarker,
        content: @Composable (MapMarker) -> Unit,
    ): View = ComposeView(context).apply {
        setParentCompositionContext(compositionContext)
        setContent { content(marker) }
    }
}

// The location source, properties and UI settings last set on the map, so only changes are
// set again
private class AppliedSettings {
    private var locationSource: LocationSource? = null
    private var properties: MapProperties? = null
    private var uiSettings: MapUiSettings? = null
    private var padding: IntArray? = null

    fun apply(
        map: OpenMapView,
        locationSource: LocationSource?,
        properties: MapProperties,
        uiSettings: MapUiSettings,
        padding: IntArray,
        initial: Boolean,
    ) {
        // Before the properties: with a custom source, my location needs no permission
        if (initial || locationSource !== this.locationSource) {
            map.setLocationSource(locationSource)
            this.locationSource = locationSource
        }
        if (initial || properties != this.properties) {
            properties.applyTo(map, if (initial) null else this.properties)
            this.properties = properties
        }
        if (initial || uiSettings != this.uiSettings) {
            uiSettings.applyTo(map)
            this.uiSettings = uiSettings
        }
        if (initial || !padding.contentEquals(this.padding)) {
            map.setMapPadding(padding[0], padding[1], padding[2], padding[3])
            this.padding = padding
        }
    }
}

// The padding as left, top, right and bottom pixels, as setMapPadding() takes them
private fun PaddingValues.toPixels(
    density: Density,
    layoutDirection: LayoutDirection,
): IntArray = with(density) {
    intArrayOf(
        calculateLeftPadding(layoutDirection).roundToPx(),
        calculateTopPadding().roundToPx(),
        calculateRightPadding(layoutDirection).roundToPx(),
        calculateBottomPadding().roundToPx(),
    )
}

// Writes the position of a dragged marker to its MarkerState
private class MarkerDragDispatcher(
    private val markerNodes: Map<MapMarker, MarkerNode>,
) : OnMarkerDragListener {
    override fun onMarkerDragStart(marker: MapMarker) = update(marker, dragging = true)

    override fun onMarkerDrag(marker: MapMarker) = update(marker, dragging = true)

    override fun onMarkerDragEnd(marker: MapMarker) = update(marker, dragging = false)

    private fun update(
        marker: MapMarker,
        dragging: Boolean,
    ) {
        val state = markerNodes[marker]?.state ?: return
        state.position = marker.position
        state.isDragging = dragging
    }
}
