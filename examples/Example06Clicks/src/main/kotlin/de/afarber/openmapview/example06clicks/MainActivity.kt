/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example06clicks

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OnMapClickListener
import de.afarber.openmapview.OnMapLongClickListener
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

    // Show initial instruction toast
    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "Try dragging the map around.\nTry short and long clicks on the map",
            Toast.LENGTH_LONG,
        ).show()
    }

    AndroidView(
        factory = { ctx ->
            OpenMapView(ctx).apply {
                // Register lifecycle observer for proper cleanup
                lifecycleOwner.lifecycle.addObserver(this)

                setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
                setZoom(14.0f)

                // Set map click listener
                setOnMapClickListener(
                    OnMapClickListener { latLng ->
                        val coordString = "%.4f, %.4f".format(latLng.latitude, latLng.longitude)
                        Toast.makeText(
                            context,
                            "Clicked at: $coordString",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )

                // Set map long-click listener
                setOnMapLongClickListener(
                    OnMapLongClickListener { latLng ->
                        val coordString = "%.4f, %.4f".format(latLng.latitude, latLng.longitude)
                        Toast.makeText(
                            context,
                            "Long-clicked at: $coordString",
                            Toast.LENGTH_SHORT,
                        ).show()
                    },
                )

                // Set attribution click listener to open OSM copyright page
                setOnAttributionClickListener {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://www.openstreetmap.org/copyright".toUri(),
                    )
                    context.startActivity(intent)
                }
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
