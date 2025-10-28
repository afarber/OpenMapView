/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example11maptypes

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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
    val configuration = LocalConfiguration.current
    var currentMapType by remember { mutableStateOf(MapType.NORMAL) }
    var mapView: OpenMapView? by remember { mutableStateOf(null) }

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.weight(1f),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(200.dp)
                    .fillMaxHeight(),
            ) {
                MapTypeControls(
                    currentMapType = currentMapType,
                    onMapTypeChange = { type ->
                        mapView?.setMapType(type)
                        currentMapType = type
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.weight(1f),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                MapTypeControls(
                    currentMapType = currentMapType,
                    onMapTypeChange = { type ->
                        mapView?.setMapType(type)
                        currentMapType = type
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun MapTypeControls(
    currentMapType: Int,
    onMapTypeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Current: ${getMapTypeName(currentMapType)}",
            style = MaterialTheme.typography.titleMedium,
        )

        Text(
            text = "Map Type:",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 8.dp),
        )

        MapTypeButton(
            text = "Normal",
            enabled = currentMapType != MapType.NORMAL,
            onClick = { onMapTypeChange(MapType.NORMAL) },
            modifier = Modifier.fillMaxWidth(),
        )

        MapTypeButton(
            text = "Terrain",
            enabled = currentMapType != MapType.TERRAIN,
            onClick = { onMapTypeChange(MapType.TERRAIN) },
            modifier = Modifier.fillMaxWidth(),
        )

        MapTypeButton(
            text = "Humanitarian",
            enabled = currentMapType != MapType.HUMANITARIAN,
            onClick = { onMapTypeChange(MapType.HUMANITARIAN) },
            modifier = Modifier.fillMaxWidth(),
        )

        MapTypeButton(
            text = "Cycle",
            enabled = currentMapType != MapType.CYCLE,
            onClick = { onMapTypeChange(MapType.CYCLE) },
            modifier = Modifier.fillMaxWidth(),
        )

        MapTypeButton(
            text = "None",
            enabled = currentMapType != MapType.NONE,
            onClick = { onMapTypeChange(MapType.NONE) },
            modifier = Modifier.fillMaxWidth(),
        )
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
        MapType.TERRAIN -> "Terrain"
        MapType.HUMANITARIAN -> "Humanitarian"
        MapType.CYCLE -> "Cycle"
        else -> "Unknown ($type)"
    }
