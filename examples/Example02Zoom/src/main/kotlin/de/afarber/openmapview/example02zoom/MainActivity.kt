/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example02zoom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationSearching
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OnCameraMoveStartedListener
import de.afarber.openmapview.OpenMapView

/**
 * Main activity demonstrating OpenMapView zoom controls.
 *
 * This example showcases:
 * - Custom zoom toolbar with +/- buttons
 * - Pinch-to-zoom gesture support
 * - Real-time zoom level display
 * - Camera state tracking
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
 * Main composable screen containing the map and zoom controls.
 *
 * Displays an OpenMapView with a status toolbar showing zoom level and camera state,
 * and a zoom toolbar for programmatic zoom control.
 * The map is centered on Bochum, Germany.
 */
@Composable
fun MapViewScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Initial location: Bochum, Germany
    val initialLocation = LatLng(51.4661, 7.2491)
    val initialZoom = 14.0f

    // State variables
    var mapView: OpenMapView? by remember { mutableStateOf(null) }
    var zoomLevel by remember { mutableStateOf("%.1f".format(initialZoom)) }
    var cameraState by remember { mutableStateOf("Idle") }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(initialLocation)
                    setZoom(initialZoom)

                    // Camera move started listener
                    setOnCameraMoveStartedListener { reason ->
                        cameraState = when (reason) {
                            OnCameraMoveStartedListener.REASON_GESTURE -> "Moving (gesture)"
                            OnCameraMoveStartedListener.REASON_API_ANIMATION -> "Moving (animation)"
                            OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> "Moving (programmatic)"
                            else -> "Moving"
                        }
                    }

                    // Camera move listener - updates zoom level during movement
                    setOnCameraMoveListener {
                        zoomLevel = "%.1f".format(getZoom())
                    }

                    // Camera idle listener
                    setOnCameraIdleListener {
                        cameraState = "Idle"
                        zoomLevel = "%.1f".format(getZoom())
                    }

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Status overlay at top
        StatusToolbar(
            zoomLevel = zoomLevel,
            cameraState = cameraState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )

        // Zoom toolbar at bottom
        ZoomToolbar(
            onZoomInClick = {
                mapView?.apply {
                    setZoom(getZoom() + 1.0f)
                    zoomLevel = "%.1f".format(getZoom())
                }
            },
            onZoomOutClick = {
                mapView?.apply {
                    setZoom(getZoom() - 1.0f)
                    zoomLevel = "%.1f".format(getZoom())
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        FloatingActionButton(
            onClick = {
                mapView?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(initialLocation, initialZoom),
                    500,
                )
            },
            containerColor = Color.Red,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.LocationSearching,
                contentDescription = "Reset",
            )
        }
    }
}
