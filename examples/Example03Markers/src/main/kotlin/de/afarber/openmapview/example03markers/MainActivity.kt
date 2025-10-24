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
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Marker
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

    AndroidView(
        factory = { ctx ->
            OpenMapView(ctx).apply {
                // Center on Bochum, Germany
                setCenter(LatLng(51.4661, 7.2491))
                setZoom(14.0)

                // Add several markers around Bochum
                addMarker(
                    Marker(
                        position = LatLng(51.4661, 7.2491),
                        title = "Bochum City Center",
                        snippet = "Welcome to Bochum!",
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4700, 7.2550),
                        title = "North Location",
                        snippet = "A place north of center",
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4620, 7.2430),
                        title = "South Location",
                        snippet = "A place south of center",
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4680, 7.2380),
                        title = "West Location",
                        snippet = "A place west of center",
                    ),
                )

                addMarker(
                    Marker(
                        position = LatLng(51.4640, 7.2600),
                        title = "East Location",
                        snippet = "A place east of center",
                    ),
                )

                // Set marker click listener
                setOnMarkerClickListener { marker ->
                    val message = buildString {
                        append(marker.title ?: "Marker")
                        if (marker.snippet != null) {
                            append("\n")
                            append(marker.snippet)
                        }
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    true // Consume the click event
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
