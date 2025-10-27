/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example02zoom

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var zoomLevel by remember { mutableStateOf(14.0) }
    var mapView: OpenMapView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    // Register lifecycle observer for proper cleanup
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
                    setZoom(14.0)
                    mapView = this

                    // Set attribution click listener to open OSM copyright page
                    setOnAttributionClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))
                        context.startActivity(intent)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Zoom level title at the top
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            shape = MaterialTheme.shapes.small,
        ) {
            Text(
                text = "Zoom: ${zoomLevel.roundToInt()}",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        // Zoom in button
        FloatingActionButton(
            onClick = {
                mapView?.let {
                    val newZoom = (it.getZoom() + 1.0).coerceAtMost(19.0)
                    it.setZoom(newZoom)
                    zoomLevel = newZoom
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 88.dp),
        ) {
            Text("+", style = MaterialTheme.typography.headlineMedium)
        }

        // Zoom out button
        FloatingActionButton(
            onClick = {
                mapView?.let {
                    val newZoom = (it.getZoom() - 1.0).coerceAtLeast(2.0)
                    it.setZoom(newZoom)
                    zoomLevel = newZoom
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 16.dp),
        ) {
            Text("-", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
