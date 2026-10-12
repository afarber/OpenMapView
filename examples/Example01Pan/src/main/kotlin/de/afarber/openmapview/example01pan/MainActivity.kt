/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example01pan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.Marker
import de.afarber.openmapview.OnCameraMoveStartedListener
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Polyline

/**
 * Main activity demonstrating OpenMapView panning and camera controls.
 *
 * This example showcases:
 * - Map panning with arrow buttons
 * - Preset location navigation with animated camera moves
 * - Camera bounds constraints with visual polyline indicator
 * - Real-time camera state and position display
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
 * Main composable screen containing the map and control toolbars.
 *
 * Displays an OpenMapView with overlaid toolbars for navigation and status display.
 * The map is centered on Bochum, Germany with preset locations marked.
 */
@Composable
fun MapViewScreen() {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Bochum area bounds for constraint demo
    val bochumBounds = LatLngBounds(
        southwest = LatLng(51.4400, 7.1800),
        northeast = LatLng(51.5000, 7.3200),
    )

    // Initial location: center of the bounds
    val initialLocation = LatLng(
        (bochumBounds.southwest.latitude + bochumBounds.northeast.latitude) / 2,
        (bochumBounds.southwest.longitude + bochumBounds.northeast.longitude) / 2,
    )

    // Preset locations around Bochum city, Germany
    val location1 = LatLng(51.4700, 7.2400) // North-West
    val location2 = LatLng(51.4620, 7.2600) // East
    val location3 = LatLng(51.4550, 7.2350) // South-West

    // State variables
    var mapView: OpenMapView? by remember { mutableStateOf(null) }
    var cameraState by remember { mutableStateOf("Idle") }
    var centerPosition by remember { mutableStateOf("%.4f, %.4f".format(initialLocation.latitude, initialLocation.longitude)) }
    var boundsEnabled by remember { mutableStateOf(false) }
    var boundsPolyline: Polyline? by remember { mutableStateOf(null) }

    // Polyline to visualize the bounds rectangle
    val boundsOutline = Polyline(
        points = listOf(
            bochumBounds.southwest,
            LatLng(bochumBounds.southwest.latitude, bochumBounds.northeast.longitude),
            bochumBounds.northeast,
            LatLng(bochumBounds.northeast.latitude, bochumBounds.southwest.longitude),
            bochumBounds.southwest, // Close the rectangle
        ),
        strokeColor = androidx.compose.ui.graphics.Color.Blue,
        strokeWidth = 3f,
    )

    // Helper to update center position after moveCamera() calls.
    // Note: moveCamera() moves the camera instantly without animation, and unlike
    // animateCamera(), it does not trigger OnCameraMoveListener. Therefore, we must
    // manually update centerPosition after each moveCamera() call (e.g., arrow buttons).
    fun updateCenterPosition() {
        mapView?.getCameraPosition()?.target?.let { pos ->
            centerPosition = "%.4f, %.4f".format(pos.latitude, pos.longitude)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(initialLocation)
                    setZoom(14.0f)

                    // Add markers for preset locations
                    addMarker(
                        Marker(
                            position = initialLocation,
                            title = "Initial location",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                        ),
                    )
                    addMarker(
                        Marker(
                            position = location1,
                            title = "Location 1",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        ),
                    )
                    addMarker(
                        Marker(
                            position = location2,
                            title = "Location 2",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                        ),
                    )
                    addMarker(
                        Marker(
                            position = location3,
                            title = "Location 3",
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
                        ),
                    )

                    // Camera move started listener
                    setOnCameraMoveStartedListener { reason ->
                        cameraState = when (reason) {
                            OnCameraMoveStartedListener.REASON_GESTURE -> "Moving (gesture)"
                            OnCameraMoveStartedListener.REASON_API_ANIMATION -> "Moving (animation)"
                            OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> "Moving (programmatic)"
                            else -> "Moving"
                        }
                    }

                    // Camera move listener - updates position during movement
                    setOnCameraMoveListener {
                        updateCenterPosition()
                    }

                    // Camera idle listener
                    setOnCameraIdleListener {
                        cameraState = "Idle"
                    }

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Status overlay at top
        StatusToolbar(
            cameraState = cameraState,
            centerPosition = centerPosition,
            boundsEnabled = boundsEnabled,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )

        // Arrow toolbar at bottom
        ArrowToolbar(
            onLeftClick = {
                mapView?.moveCamera(CameraUpdateFactory.scrollBy(-100f, 0f))
                updateCenterPosition()
            },
            onUpClick = {
                mapView?.moveCamera(CameraUpdateFactory.scrollBy(0f, -100f))
                updateCenterPosition()
            },
            onDownClick = {
                mapView?.moveCamera(CameraUpdateFactory.scrollBy(0f, 100f))
                updateCenterPosition()
            },
            onRightClick = {
                mapView?.moveCamera(CameraUpdateFactory.scrollBy(100f, 0f))
                updateCenterPosition()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        // Location toolbar at right
        LocationToolbar(
            locations = listOf(location1, location2, location3),
            onLocationClick = { location ->
                mapView?.animateCamera(CameraUpdateFactory.newLatLng(location), 500)
            },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 36.dp),
        )

        // Control toolbar at left
        ControlToolbar(
            boundsEnabled = boundsEnabled,
            onBoundsClick = {
                if (boundsEnabled) {
                    mapView?.setLatLngBoundsForCameraTarget(null)
                    boundsPolyline?.let { mapView?.removePolyline(it) }
                    boundsPolyline = null
                    boundsEnabled = false
                } else {
                    mapView?.setLatLngBoundsForCameraTarget(bochumBounds)
                    boundsPolyline = boundsOutline
                    mapView?.addPolyline(boundsOutline)
                    boundsEnabled = true
                }
            },
            onResetClick = {
                mapView?.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(initialLocation, 14.0f),
                )
            },
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 36.dp),
        )
    }
}
