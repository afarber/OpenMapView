/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example12toolbar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
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
                    MapWithToolbarScreen()
                }
            }
        }
    }
}

@Composable
fun MapWithToolbarScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<OpenMapView?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    mapView?.openInExternalApp("Current Location")
                },
            ) {
                Text("Open")
            }
        },
    ) { paddingValues ->
        Box(
            modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            AndroidView(
                factory = { context ->
                    OpenMapView(context).apply {
                        lifecycleOwner.lifecycle.addObserver(this)

                        // Set initial location (Bochum, Germany)
                        setCenter(LatLng(51.4661, 7.2491))
                        setZoom(14.0)

                        // Long-press to open location in external app
                        setOnMapLongClickListener { latLng ->
                            moveCamera(CameraUpdateFactory.newLatLng(latLng))
                            openInExternalApp("Selected Location")
                        }

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            Text(
                text = "Tap 'Open' to launch external map app\nLong-press map to select a location",
                modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
