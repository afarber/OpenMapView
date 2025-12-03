/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example03markers

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Marker
import de.afarber.openmapview.OnCameraMoveStartedListener
import de.afarber.openmapview.OpenMapView

/**
 * Main activity demonstrating OpenMapView marker management.
 *
 * This example showcases:
 * - Adding markers with different colors at real Bochum locations
 * - Marker click listener and info window display
 * - Interactive marker management (add, remove, clear)
 * - Real-time marker count and selection tracking
 * - Camera state monitoring
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MapViewScreen()
                }
            }
        }
    }
}

/**
 * Main composable screen containing the map and marker controls.
 *
 * Displays an OpenMapView with markers at notable Bochum locations,
 * a status toolbar showing marker count and selection state,
 * and a marker toolbar for adding, removing, and clearing markers.
 */
@Composable
fun MapViewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Calculate initial location from marker positions (Single Source of Truth)
    val initialLocation = LatLng(
        initialMarkerData.map { it.position.latitude }.average(),
        initialMarkerData.map { it.position.longitude }.average(),
    )
    val initialZoom = 13.0f

    // State variables
    var mapView: OpenMapView? by remember { mutableStateOf(null) }
    var markerCount by remember { mutableIntStateOf(initialMarkerData.size) }
    var selectedMarker: Marker? by remember { mutableStateOf(null) }
    var cameraState by remember { mutableStateOf("Idle") }
    var addedMarkerCounter by remember { mutableIntStateOf(0) }

    /**
     * Creates initial markers on the map.
     */
    fun createInitialMarkers(map: OpenMapView) {
        initialMarkerData.forEach { data ->
            map.addMarker(
                Marker(
                    position = data.position,
                    title = data.title,
                    snippet = data.snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(data.hue),
                ),
            )
        }
        markerCount = map.getMarkers().size
        selectedMarker = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(initialLocation)
                    setZoom(initialZoom)

                    // Create initial markers
                    createInitialMarkers(this)

                    // Camera move started listener
                    setOnCameraMoveStartedListener { reason ->
                        cameraState = when (reason) {
                            OnCameraMoveStartedListener.REASON_GESTURE -> "Moving (gesture)"
                            OnCameraMoveStartedListener.REASON_API_ANIMATION -> "Moving (animation)"
                            OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> "Moving (programmatic)"
                            else -> "Moving"
                        }
                    }

                    // Camera idle listener
                    setOnCameraIdleListener {
                        cameraState = "Idle"
                    }

                    // Marker click listener - tracks selection and shows info window
                    setOnMarkerClickListener { marker ->
                        selectedMarker = marker
                        true // Consume the click event (info window will still show)
                    }

                    // Info window click listener
                    setOnInfoWindowClickListener { marker ->
                        Toast.makeText(context, "Clicked: ${marker.title}", Toast.LENGTH_SHORT).show()
                    }

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Status overlay at top
        StatusToolbar(
            markerCount = markerCount,
            selectedMarkerTitle = selectedMarker?.title,
            cameraState = cameraState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )

        // Marker toolbar at bottom
        MarkerToolbar(
            onAddClick = {
                mapView?.apply {
                    val center = getCameraPosition().target
                    val hue = markerHues[addedMarkerCounter % markerHues.size]
                    addedMarkerCounter++
                    val newMarker = addMarker(
                        Marker(
                            position = center,
                            title = "Marker $addedMarkerCounter",
                            snippet = "Added at map center",
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                        ),
                    )
                    markerCount = getMarkers().size
                    selectedMarker = newMarker
                }
            },
            onRemoveClick = {
                mapView?.apply {
                    selectedMarker?.let { marker ->
                        removeMarker(marker)
                        markerCount = getMarkers().size
                        selectedMarker = null
                    }
                }
            },
            onClearClick = {
                mapView?.apply {
                    clearMarkers()
                    markerCount = 0
                    selectedMarker = null
                    addedMarkerCounter = 0
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        // Reset FAB at bottom-end
        FloatingActionButton(
            onClick = {
                mapView?.apply {
                    clearMarkers()
                    createInitialMarkers(this)
                    addedMarkerCounter = 0
                    animateCamera(
                        CameraUpdateFactory.newLatLngZoom(initialLocation, initialZoom),
                        500,
                    )
                }
            },
            containerColor = OsmHighwayPink,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Reset",
            )
        }
    }
}
