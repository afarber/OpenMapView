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
import androidx.compose.material.icons.filled.LocationOn
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
import kotlin.time.Duration.Companion.seconds

/**
 * Main activity demonstrating OpenMapView marker navigation.
 *
 * This example showcases:
 * - Displaying markers with different colors at real Bochum locations
 * - Navigating between markers with prev/next buttons
 * - Toggling info windows via FAB or marker tap
 * - Camera animation when centering on markers
 * - Real-time selection index and info window state tracking
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
 * Main composable screen containing the map and marker navigation controls.
 *
 * Displays an OpenMapView with markers at notable Bochum locations,
 * a status toolbar showing marker count and selection state,
 * and a marker toolbar for navigating between markers and toggling info windows.
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
    var selectedIndex by remember { mutableIntStateOf(0) }
    var selectedMarker: Marker? by remember { mutableStateOf(null) }
    var cameraState by remember { mutableStateOf("Idle") }
    var isInfoWindowShown by remember { mutableStateOf(false) }

    /**
     * Creates initial markers on the map and selects the first one.
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
        selectedIndex = 0
        selectedMarker = map.getMarkers().firstOrNull()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(initialLocation)
                    setZoom(initialZoom)
                    getUiSettings().infoWindowAutoDismiss = 10.seconds

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
                        isInfoWindowShown = marker.isInfoWindowShown
                        val index = getMarkers().indexOf(marker)
                        if (index >= 0) {
                            selectedIndex = index
                        }
                        true // Consume the click event (info window will still show)
                    }

                    // Info window click listener
                    setOnInfoWindowClickListener { marker ->
                        Toast.makeText(context, "Clicked: ${marker.title}", Toast.LENGTH_SHORT).show()
                    }

                    // Info window close listener - updates state when closed (manual or auto-dismiss)
                    setOnInfoWindowCloseListener {
                        isInfoWindowShown = false
                    }

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Status overlay at top
        StatusToolbar(
            selectedIndex = selectedIndex,
            selectedMarkerTitle = selectedMarker?.title,
            cameraState = cameraState,
            isInfoWindowShown = isInfoWindowShown,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp),
        )

        // Marker toolbar at bottom
        MarkerToolbar(
            onPrevClick = {
                mapView?.apply {
                    val markers = getMarkers()
                    if (markers.isNotEmpty()) {
                        selectedIndex = (selectedIndex - 1 + markers.size) % markers.size
                        val marker = markers[selectedIndex]
                        selectedMarker = marker
                        animateCamera(CameraUpdateFactory.newLatLng(marker.position), 500)
                    }
                }
            },
            onNextClick = {
                mapView?.apply {
                    val markers = getMarkers()
                    if (markers.isNotEmpty()) {
                        selectedIndex = (selectedIndex + 1) % markers.size
                        val marker = markers[selectedIndex]
                        selectedMarker = marker
                        animateCamera(CameraUpdateFactory.newLatLng(marker.position), 500)
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        FloatingActionButton(
            onClick = {
                selectedMarker?.let { marker ->
                    if (marker.isInfoWindowShown) {
                        marker.hideInfoWindow()
                    } else {
                        marker.showInfoWindow()
                        isInfoWindowShown = true
                        mapView?.animateCamera(CameraUpdateFactory.newLatLng(marker.position), 500)
                    }
                }
            },
            containerColor = OsmHighwayPink,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Toggle Info Window",
            )
        }
    }
}
