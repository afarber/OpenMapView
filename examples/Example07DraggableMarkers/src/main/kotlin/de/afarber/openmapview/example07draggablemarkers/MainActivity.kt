/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example07draggablemarkers

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Marker
import de.afarber.openmapview.OnMarkerDragListener
import de.afarber.openmapview.OpenMapView
import kotlin.random.Random

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
    var mapView by remember { mutableStateOf<OpenMapView?>(null) }
    var markerCount by remember { mutableStateOf(0) }

    val bochumCenter = LatLng(51.4661, 7.2491)

    // Show initial instruction toast
    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "Drag the markers to move them.\nUse FABs to add random markers or clear all",
            Toast.LENGTH_LONG,
        ).show()
    }

    fun addRandomMarkers() {
        mapView?.let { map ->
            repeat(5) {
                val randomLat = bochumCenter.latitude + (Random.nextDouble() - 0.5) * 0.05
                val randomLng = bochumCenter.longitude + (Random.nextDouble() - 0.5) * 0.1
                val marker =
                    Marker(
                        position = LatLng(randomLat, randomLng),
                        title = "Marker ${++markerCount}",
                        snippet = "Drag me!",
                        draggable = true,
                    )
                map.addMarker(marker)
            }
            Toast.makeText(context, "Added 5 random markers", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    mapView = this
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(bochumCenter)
                    setZoom(13.0f)

                    // Set marker drag listener
                    setOnMarkerDragListener(
                        object : OnMarkerDragListener {
                            override fun onMarkerDragStart(marker: Marker) {
                                Toast.makeText(
                                    context,
                                    "Started dragging: ${marker.title}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }

                            override fun onMarkerDrag(marker: Marker) {
                                // Called continuously during drag
                            }

                            override fun onMarkerDragEnd(marker: Marker) {
                                val coordString = "%.4f, %.4f".format(marker.position.latitude, marker.position.longitude)
                                Toast.makeText(
                                    context,
                                    "${marker.title} at: $coordString",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                        },
                    )

                    // Set marker click listener
                    setOnMarkerClickListener { marker ->
                        Toast.makeText(
                            context,
                            "${marker.title}: ${marker.snippet}",
                            Toast.LENGTH_SHORT,
                        ).show()
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Floating Action Buttons
        Column(
            modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Add random markers FAB
            FloatingActionButton(
                onClick = { addRandomMarkers() },
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Add random markers",
                )
            }

            // Clear all markers FAB
            FloatingActionButton(
                onClick = {
                    mapView?.clear()
                    markerCount = 0
                    Toast.makeText(
                        context,
                        "All markers cleared",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear all markers",
                )
            }
        }
    }
}
