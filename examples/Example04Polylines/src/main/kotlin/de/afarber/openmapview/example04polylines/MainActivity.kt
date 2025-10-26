/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example04polylines

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Polygon
import de.afarber.openmapview.Polyline

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
                setZoom(14.0)

                // Add a blue polyline (route)
                addPolyline(
                    Polyline(
                        points = listOf(
                            LatLng(51.4700, 7.2400),
                            LatLng(51.4680, 7.2450),
                            LatLng(51.4650, 7.2500),
                            LatLng(51.4620, 7.2550),
                        ),
                        strokeColor = Color.BLUE,
                        strokeWidth = 8f,
                    ),
                )

                // Add a red polyline (path)
                addPolyline(
                    Polyline(
                        points = listOf(
                            LatLng(51.4620, 7.2430),
                            LatLng(51.4640, 7.2460),
                            LatLng(51.4660, 7.2490),
                            LatLng(51.4680, 7.2520),
                            LatLng(51.4700, 7.2550),
                        ),
                        strokeColor = Color.RED,
                        strokeWidth = 6f,
                    ),
                )

                // Add a green polygon (park or area)
                addPolygon(
                    Polygon(
                        points = listOf(
                            LatLng(51.4640, 7.2380),
                            LatLng(51.4660, 7.2380),
                            LatLng(51.4660, 7.2420),
                            LatLng(51.4640, 7.2420),
                        ),
                        strokeColor = Color.rgb(0, 128, 0),
                        strokeWidth = 4f,
                        fillColor = Color.argb(100, 0, 255, 0),
                    ),
                )

                // Add a cyan polygon with a hole (donut shape)
                addPolygon(
                    Polygon(
                        points = listOf(
                            LatLng(51.4700, 7.2580),
                            LatLng(51.4720, 7.2580),
                            LatLng(51.4720, 7.2620),
                            LatLng(51.4700, 7.2620),
                        ),
                        holes = listOf(
                            listOf(
                                LatLng(51.4706, 7.2590),
                                LatLng(51.4714, 7.2590),
                                LatLng(51.4714, 7.2610),
                                LatLng(51.4706, 7.2610),
                            ),
                        ),
                        strokeColor = Color.CYAN,
                        strokeWidth = 4f,
                        fillColor = Color.argb(100, 0, 255, 255),
                    ),
                )

                // Set attribution click listener to open OSM copyright page
                setOnAttributionClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))
                    context.startActivity(intent)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
