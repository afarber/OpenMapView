/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example11maptypes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import de.afarber.openmapview.MapType
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
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }
    var mapView: OpenMapView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(LatLng(46.8182, 8.2275))
                    setZoom(12.0)
                    mapView = this

                    setOnAttributionClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))
                        context.startActivity(intent)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "Current: ${getMapTypeName(currentMapType)}",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small,
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Switch Map Type:",
                        style = MaterialTheme.typography.labelLarge,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MapTypeButton(
                            text = "Normal",
                            enabled = currentMapType != MapType.NORMAL,
                            onClick = {
                                mapView?.setMapType(MapType.NORMAL)
                                currentMapType = MapType.NORMAL
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MapTypeButton(
                            text = "Terrain",
                            enabled = currentMapType != MapType.TERRAIN,
                            onClick = {
                                mapView?.setMapType(MapType.TERRAIN)
                                currentMapType = MapType.TERRAIN
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MapTypeButton(
                            text = "Humanitarian",
                            enabled = currentMapType != MapType.HUMANITARIAN,
                            onClick = {
                                mapView?.setMapType(MapType.HUMANITARIAN)
                                currentMapType = MapType.HUMANITARIAN
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MapTypeButton(
                            text = "Cycle",
                            enabled = currentMapType != MapType.CYCLE,
                            onClick = {
                                mapView?.setMapType(MapType.CYCLE)
                                currentMapType = MapType.CYCLE
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MapTypeButton(
                            text = "None",
                            enabled = currentMapType != MapType.NONE,
                            onClick = {
                                mapView?.setMapType(MapType.NONE)
                                currentMapType = MapType.NONE
                            },
                            modifier = Modifier.weight(1f),
                        )
                        MapTypeButton(
                            text = "Satellite",
                            enabled = true,
                            onClick = {
                                try {
                                    mapView?.setMapType(MapType.SATELLITE)
                                    currentMapType = MapType.SATELLITE
                                } catch (e: UnsupportedOperationException) {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Satellite type not supported:\n${e.message}",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapTypeButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
    ) {
        Text(text)
    }
}

fun getMapTypeName(type: Int): String =
    when (type) {
        MapType.NONE -> "None"
        MapType.NORMAL -> "Normal"
        MapType.SATELLITE -> "Satellite"
        MapType.TERRAIN -> "Terrain"
        MapType.HYBRID -> "Hybrid"
        MapType.HUMANITARIAN -> "Humanitarian"
        MapType.TOPO -> "Topo"
        MapType.CYCLE -> "Cycle"
        else -> "Unknown ($type)"
    }
