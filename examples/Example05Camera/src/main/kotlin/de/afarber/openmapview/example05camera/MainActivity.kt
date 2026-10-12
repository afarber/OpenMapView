/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example05camera

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.CancelableCallback
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
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mapView: OpenMapView? by remember { mutableStateOf(null) }

    val location1 = LatLng(51.4700, 7.2400)
    val location2 = LatLng(51.4620, 7.2600)
    val location3 = LatLng(51.4550, 7.2350)

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(LatLng(51.4620, 7.2480))
                    setZoom(13.0f)

                    // Enable built-in zoom controls
                    getUiSettings().isZoomControlsEnabled = true

                    addMarker(
                        Marker(
                            position = location1,
                            title = "Location 1",
                            snippet = "Bochum North",
                        ),
                    )

                    addMarker(
                        Marker(
                            position = location2,
                            title = "Location 2",
                            snippet = "Bochum East",
                        ),
                    )

                    addMarker(
                        Marker(
                            position = location3,
                            title = "Location 3",
                            snippet = "Bochum South",
                        ),
                    )

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier =
            Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                FloatingActionButton(
                    onClick = {
                        mapView?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(location1, 15.0f),
                            1000,
                            object : CancelableCallback {
                                override fun onFinish() {
                                    Toast.makeText(context, "Animation finished", Toast.LENGTH_SHORT).show()
                                }

                                override fun onCancel() {
                                    Toast.makeText(context, "Animation cancelled", Toast.LENGTH_SHORT).show()
                                }
                            },
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                ) {
                    Text("Loc 1", style = MaterialTheme.typography.bodyMedium)
                }

                FloatingActionButton(
                    onClick = {
                        mapView?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(location2, 15.0f),
                            1000,
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.secondary,
                ) {
                    Text("Loc 2", style = MaterialTheme.typography.bodyMedium)
                }

                FloatingActionButton(
                    onClick = {
                        mapView?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(location3, 15.0f),
                            2000,
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                ) {
                    Text("Loc 3", style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FloatingActionButton(
                    onClick = {
                        mapView?.stopAnimation()
                        Toast.makeText(context, "Animation stopped", Toast.LENGTH_SHORT).show()
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                ) {
                    Text("Stop", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
