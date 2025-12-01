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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.OnCameraMoveStartedListener
import de.afarber.openmapview.OpenMapView

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

@Composable
fun MapViewScreen() {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    // Preset locations around Bochum city, Germany
    val initialLocation = LatLng(51.4661, 7.2491)
    val location1 = LatLng(51.4700, 7.2400) // North-West
    val location2 = LatLng(51.4620, 7.2600) // East
    val location3 = LatLng(51.4550, 7.2350) // South-West

    // Bochum area bounds for constraint demo
    val bochumBounds = LatLngBounds(
        southwest = LatLng(51.4400, 7.1800),
        northeast = LatLng(51.5000, 7.3200),
    )

    // State variables
    var mapView: OpenMapView? by remember { mutableStateOf(null) }
    var cameraState by remember { mutableStateOf("Idle") }
    var centerPosition by remember { mutableStateOf("%.4f, %.4f".format(initialLocation.latitude, initialLocation.longitude)) }
    var boundsEnabled by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(initialLocation)
                    setZoom(14.0f)

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
                        val pos = getCameraPosition().target
                        centerPosition = "%.4f, %.4f".format(pos.latitude, pos.longitude)
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
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .clip(RoundedCornerShape(ToolbarCornerRadius))
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Camera: $cameraState",
                color = Color.Black,
            )
            Text(
                text = "Center: $centerPosition",
                color = Color.Black,
            )
            Text(
                text = "Bounds: ${if (boundsEnabled) "On" else "Off"}",
                color = if (boundsEnabled) {
                    Color.Black
                } else {
                    Color.Red
                },
            )
        }

        // Arrow toolbar at bottom
        ArrowToolbar(
            onLeftClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(-100f, 0f)) },
            onUpClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(0f, -100f)) },
            onDownClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(0f, 100f)) },
            onRightClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(100f, 0f)) },
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
                    boundsEnabled = false
                } else {
                    mapView?.setLatLngBoundsForCameraTarget(bochumBounds)
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
