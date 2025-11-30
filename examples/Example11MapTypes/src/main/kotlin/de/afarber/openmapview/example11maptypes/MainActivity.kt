/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example11maptypes

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
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
    var currentMapType by remember { mutableStateOf(MapType.STANDARD) }
    var mapView: OpenMapView? by remember { mutableStateOf(null) }

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    OpenMapView(ctx).apply {
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(LatLng(46.8182, 8.2275))
                        setZoom(12.0f)
                        mapView = this
                        setOnAttributionClickListener {
                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    getAttributionUrl().toUri(),
                                )
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier.weight(0.67f),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(0.33f),
            ) {
                MapTypeControls(
                    currentMapType = currentMapType,
                    onMapTypeChange = { type ->
                        mapView?.setMapType(type)
                        currentMapType = type
                    },
                    modifier = Modifier.padding(8.dp),
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
                        setZoom(12.0f)
                        mapView = this
                        setOnAttributionClickListener {
                            val intent =
                                Intent(
                                    Intent.ACTION_VIEW,
                                    getAttributionUrl().toUri(),
                                )
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier.weight(0.67f),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.weight(0.33f),
            ) {
                MapTypeControls(
                    currentMapType = currentMapType,
                    onMapTypeChange = { type ->
                        mapView?.setMapType(type)
                        currentMapType = type
                    },
                    modifier = Modifier.padding(8.dp),
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
        modifier =
        modifier
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Current: ${getMapTypeName(currentMapType)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
        )

        Text(
            text = "${getMapTypeCount()} available",
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        // None
        MapTypeButton(
            text = "None",
            description = "No base tiles",
            enabled = currentMapType != MapType.NONE,
            onClick = { onMapTypeChange(MapType.NONE) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Standard
        MapTypeButton(
            text = "Standard",
            description = "Default OSM Mapnik",
            enabled = currentMapType != MapType.STANDARD,
            onClick = { onMapTypeChange(MapType.STANDARD) },
            modifier = Modifier.fillMaxWidth(),
        )

        // CyclOSM
        MapTypeButton(
            text = "CyclOSM",
            description = "Cycling infrastructure",
            enabled = currentMapType != MapType.CYCLOSM,
            onClick = { onMapTypeChange(MapType.CYCLOSM) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Cycle Map (requires API key)
        MapTypeButton(
            text = "Cycle Map",
            description = "Thunderforest (API key required)",
            enabled = currentMapType != MapType.CYCLEMAP,
            onClick = { onMapTypeChange(MapType.CYCLEMAP) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Transport (requires API key)
        MapTypeButton(
            text = "Transport",
            description = "Public transit (API key required)",
            enabled = currentMapType != MapType.TRANSPORT,
            onClick = { onMapTypeChange(MapType.TRANSPORT) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Transport Dark (requires API key)
        MapTypeButton(
            text = "Transport Dark",
            description = "Dark transit map (API key required)",
            enabled = currentMapType != MapType.TRANSPORT_DARK,
            onClick = { onMapTypeChange(MapType.TRANSPORT_DARK) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Tracestrack Topo (requires API key)
        MapTypeButton(
            text = "Tracestrack Topo",
            description = "Topographic (API key required)",
            enabled = currentMapType != MapType.TRACESTRACK_TOPO,
            onClick = { onMapTypeChange(MapType.TRACESTRACK_TOPO) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Humanitarian
        MapTypeButton(
            text = "Humanitarian",
            description = "HOT emergency response",
            enabled = currentMapType != MapType.HUMANITARIAN,
            onClick = { onMapTypeChange(MapType.HUMANITARIAN) },
            modifier = Modifier.fillMaxWidth(),
        )

        // OPNVKarte
        MapTypeButton(
            text = "OPNVKarte",
            description = "German public transport",
            enabled = currentMapType != MapType.OPNVKARTE,
            onClick = { onMapTypeChange(MapType.OPNVKARTE) },
            modifier = Modifier.fillMaxWidth(),
        )

        // OpenTopoMap
        MapTypeButton(
            text = "OpenTopoMap",
            description = "Free topographic map",
            enabled = currentMapType != MapType.OPENTOPOMAP,
            onClick = { onMapTypeChange(MapType.OPENTOPOMAP) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Carto Light
        MapTypeButton(
            text = "Carto Light",
            description = "Minimalist light theme",
            enabled = currentMapType != MapType.CARTO_LIGHT,
            onClick = { onMapTypeChange(MapType.CARTO_LIGHT) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Carto Dark
        MapTypeButton(
            text = "Carto Dark",
            description = "Dark theme for night mode",
            enabled = currentMapType != MapType.CARTO_DARK,
            onClick = { onMapTypeChange(MapType.CARTO_DARK) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Carto Voyager
        MapTypeButton(
            text = "Carto Voyager",
            description = "Modern colorful basemap",
            enabled = currentMapType != MapType.CARTO_VOYAGER,
            onClick = { onMapTypeChange(MapType.CARTO_VOYAGER) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Stamen Toner
        MapTypeButton(
            text = "Stamen Toner",
            description = "High-contrast black & white",
            enabled = currentMapType != MapType.STAMEN_TONER,
            onClick = { onMapTypeChange(MapType.STAMEN_TONER) },
            modifier = Modifier.fillMaxWidth(),
        )

        // Stamen Watercolor
        MapTypeButton(
            text = "Stamen Watercolor",
            description = "Artistic watercolor rendering",
            enabled = currentMapType != MapType.STAMEN_WATERCOLOR,
            onClick = { onMapTypeChange(MapType.STAMEN_WATERCOLOR) },
            modifier = Modifier.fillMaxWidth(),
        )

        // API Key Info
        Text(
            text = "API key required: Cycle Map, Transport, Transport Dark, Tracestrack Topo\n\nConfigure in AndroidManifest.xml\nSee docs/API_KEYS.md",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun MapTypeButton(
    text: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        ) {
            Text(
                text = text,
                fontSize = 12.sp,
            )
        }
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 9.sp,
            modifier = Modifier.padding(start = 4.dp, top = 1.dp),
        )
    }
}

fun getMapTypeName(type: Int): String =
    when (type) {
        MapType.NONE -> "None"
        MapType.STANDARD -> "Standard"
        MapType.CYCLOSM -> "CyclOSM"
        MapType.CYCLEMAP -> "Cycle Map"
        MapType.TRANSPORT -> "Transport"
        MapType.TRANSPORT_DARK -> "Transport Dark"
        MapType.TRACESTRACK_TOPO -> "Tracestrack Topo"
        MapType.HUMANITARIAN -> "Humanitarian"
        MapType.OPNVKARTE -> "OPNVKarte"
        MapType.OPENTOPOMAP -> "OpenTopoMap"
        MapType.CARTO_LIGHT -> "Carto Light"
        MapType.CARTO_DARK -> "Carto Dark"
        MapType.CARTO_VOYAGER -> "Carto Voyager"
        MapType.STAMEN_TONER -> "Stamen Toner"
        MapType.STAMEN_WATERCOLOR -> "Stamen Watercolor"
        else -> "Unknown ($type)"
    }

fun getMapTypeCount(): Int = 15
