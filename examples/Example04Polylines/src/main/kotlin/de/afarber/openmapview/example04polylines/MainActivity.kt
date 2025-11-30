/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example04polylines

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
                setZoom(14.0f)

                // Add a blue polyline (route)
                addPolyline(
                    Polyline(
                        points = listOf(
                            LatLng(51.4700, 7.2400),
                            LatLng(51.4680, 7.2450),
                            LatLng(51.4650, 7.2500),
                            LatLng(51.4620, 7.2550),
                        ),
                        strokeColor = Color.Blue,
                        strokeWidth = 8f,
                        clickable = true,
                        tag = "Blue Route",
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
                        strokeColor = Color.Red,
                        strokeWidth = 6f,
                        clickable = true,
                        tag = "Red Path",
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
                        strokeColor = Color(red = 0, green = 128, blue = 0),
                        strokeWidth = 4f,
                        fillColor = Color(red = 0, green = 255, blue = 0, alpha = 100),
                        clickable = true,
                        tag = "Green Park",
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
                        strokeColor = Color.Cyan,
                        strokeWidth = 4f,
                        fillColor = Color(red = 0, green = 255, blue = 255, alpha = 100),
                        clickable = true,
                        tag = "Cyan Donut",
                    ),
                )

                // Set click listeners
                setOnPolylineClickListener { polyline ->
                    Toast.makeText(context, "Clicked: ${polyline.tag}", Toast.LENGTH_SHORT).show()
                }

                setOnPolygonClickListener { polygon ->
                    Toast.makeText(context, "Clicked: ${polygon.tag}", Toast.LENGTH_SHORT).show()
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
