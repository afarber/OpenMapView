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
 * a status toolbar showing selection state, and a marker toolbar for navigation.
 */
@Composable
fun MapViewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Calculate initial location from POI marker positions
    val initialLocation = LatLng(
        poiMarkers.map { it.position.latitude }.average(),
        poiMarkers.map { it.position.longitude }.average(),
    )
    val initialZoom = 13.0f

    // State variables - mapView is nullable because AndroidView.factory runs after first composition
    var mapView: OpenMapView? by remember { mutableStateOf(null) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var cameraState by remember { mutableStateOf("Idle") }
    var isInfoWindowShown by remember { mutableStateOf(false) }

    // Derived state - selectedMarker is computed from mapView and selectedIndex (SSOT)
    val selectedMarker: Marker? = mapView?.getMarkers()?.getOrNull(selectedIndex)

    /**
     * Creates POI markers on the map.
     */
    fun createMarkers(map: OpenMapView) {
        poiMarkers.forEach { data ->
            map.addMarker(
                Marker(
                    position = data.position,
                    title = data.title,
                    snippet = data.snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(data.hue),
                ),
            )
        }
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

                    createMarkers(this)

                    setOnCameraMoveStartedListener { reason ->
                        cameraState = when (reason) {
                            OnCameraMoveStartedListener.REASON_GESTURE -> "Moving (gesture)"
                            OnCameraMoveStartedListener.REASON_API_ANIMATION -> "Moving (animation)"
                            OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> "Moving (programmatic)"
                            else -> "Moving"
                        }
                    }

                    setOnCameraIdleListener {
                        cameraState = "Idle"
                    }

                    setOnMarkerClickListener { marker ->
                        isInfoWindowShown = marker.isInfoWindowShown
                        selectedIndex = getMarkers().indexOf(marker).coerceAtLeast(0)
                        true
                    }

                    setOnInfoWindowClickListener { marker ->
                        Toast.makeText(context, "Clicked: ${marker.title}", Toast.LENGTH_SHORT).show()
                    }

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
                        animateCamera(CameraUpdateFactory.newLatLng(markers[selectedIndex].position), 500)
                    }
                }
            },
            onNextClick = {
                mapView?.apply {
                    val markers = getMarkers()
                    if (markers.isNotEmpty()) {
                        selectedIndex = (selectedIndex + 1) % markers.size
                        animateCamera(CameraUpdateFactory.newLatLng(markers[selectedIndex].position), 500)
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
