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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import kotlin.math.roundToInt

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
    var zoomLevel by remember { mutableStateOf(14.0) }
    var mapView: OpenMapView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                OpenMapView(context).apply {
                    // Register lifecycle observer for proper cleanup
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
                    setZoom(14.0)
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Zoom controls overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            // Zoom level display
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "Zoom: ${zoomLevel.roundToInt()}",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zoom in button
            FloatingActionButton(
                onClick = {
                    mapView?.let {
                        val newZoom = (it.getZoom() + 1.0).coerceAtMost(19.0)
                        it.setZoom(newZoom)
                        zoomLevel = newZoom
                    }
                },
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zoom out button
            FloatingActionButton(
                onClick = {
                    mapView?.let {
                        val newZoom = (it.getZoom() - 1.0).coerceAtLeast(2.0)
                        it.setZoom(newZoom)
                        zoomLevel = newZoom
                    }
                },
            ) {
                Text("-", style = MaterialTheme.typography.headlineMedium)
            }
        }
    }
}
