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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Camera: $cameraState",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Center: $centerPosition",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "Bounds: ${if (boundsEnabled) "On" else "Off"}",
                style = MaterialTheme.typography.bodySmall,
                color = if (boundsEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        // Arrow toolbar at bottom left
        ArrowToolbar(
            onLeftClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(-100f, 0f)) },
            onUpClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(0f, -100f)) },
            onDownClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(0f, 100f)) },
            onRightClick = { mapView?.moveCamera(CameraUpdateFactory.scrollBy(100f, 0f)) },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp),
        )

        // Location toolbar at top right
        LocationToolbar(
            locations = listOf(location1, location2, location3),
            onLocationClick = { location ->
                mapView?.animateCamera(CameraUpdateFactory.newLatLng(location), 500)
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp),
        )

        // Control panel at bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Row 3: Bounds toggle and Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Button(
                    onClick = {
                        if (boundsEnabled) {
                            mapView?.setLatLngBoundsForCameraTarget(null)
                            boundsEnabled = false
                        } else {
                            mapView?.setLatLngBoundsForCameraTarget(bochumBounds)
                            boundsEnabled = true
                        }
                    },
                    colors = if (boundsEnabled) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Text(
                        text = if (boundsEnabled) "Bounds On" else "Bounds Off",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Button(
                    onClick = {
                        mapView?.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(initialLocation, 14.0f),
                        )
                    },
                ) {
                    Text("Reset", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
