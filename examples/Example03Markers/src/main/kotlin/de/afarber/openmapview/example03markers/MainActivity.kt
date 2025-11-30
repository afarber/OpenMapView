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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Marker
import de.afarber.openmapview.MarkerOptions
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
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            OpenMapView(ctx).apply {
                // Register lifecycle observer for proper cleanup
                lifecycleOwner.lifecycle.addObserver(this)

                // Center on Bochum, Germany
                setCenter(LatLng(51.4661, 7.2491))
                setZoom(14.0f)

                // Add several markers around Bochum with different colors
                addMarker(
                    Marker(
                        position = LatLng(51.4661, 7.2491),
                        title = "Bochum City Center",
                        snippet = "Welcome to Bochum!",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4700, 7.2550),
                        title = "North Location",
                        snippet = "A place north of center",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE),
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4620, 7.2430),
                        title = "South Location",
                        snippet = "A place south of center",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN),
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4680, 7.2380),
                        title = "West Location",
                        snippet = "A place west of center",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4640, 7.2600),
                        title = "East Location",
                        snippet = "A place east of center",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA),
                    ),
                )

                // Alternative: Google Maps API style using MarkerOptions builder
                addMarker(
                    MarkerOptions()
                        .position(LatLng(51.4650, 7.2500))
                        .title("Builder Pattern")
                        .snippet("Created with MarkerOptions")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                        .alpha(0.8f),
                )

                // Set marker click listener
                // Info window is shown automatically if marker has title or snippet
                setOnMarkerClickListener { marker ->
                    // Can still add custom logic here
                    true // Consume the click event
                }

                // Set info window click listener
                setOnInfoWindowClickListener { marker ->
                    Toast.makeText(context, "Clicked: ${marker.title}", Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
