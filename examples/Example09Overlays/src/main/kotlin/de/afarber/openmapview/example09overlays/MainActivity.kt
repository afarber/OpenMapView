/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example09overlays

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.PredefinedTileProviders
import de.afarber.openmapview.TileOverlay

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

data class OverlayState(
    val name: String,
    val overlay: TileOverlay,
    var isVisible: Boolean = false,
    var transparency: Float = 0.5f,
)

@Composable
fun MapViewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<OpenMapView?>(null) }

    // Overlay states
    var overlays by remember {
        mutableStateOf(
            listOf(
                OverlayState(
                    "OpenSeaMap",
                    TileOverlay(
                        tileProvider = PredefinedTileProviders.openSeaMap(),
                        zIndex = 1f,
                        visible = false,
                    ),
                ),
                OverlayState(
                    "OpenRailwayMap",
                    TileOverlay(
                        tileProvider = PredefinedTileProviders.openRailwayMap(),
                        zIndex = 2f,
                        visible = false,
                    ),
                ),
                OverlayState(
                    "Hiking Trails",
                    TileOverlay(
                        tileProvider = PredefinedTileProviders.waymarkedTrailsHiking(),
                        zIndex = 3f,
                        visible = false,
                    ),
                ),
                OverlayState(
                    "OpenSnowMap",
                    TileOverlay(
                        tileProvider = PredefinedTileProviders.openSnowMap(),
                        zIndex = 4f,
                        visible = false,
                    ),
                ),
            ),
        )
    }

    var transparency by remember { mutableFloatStateOf(0.5f) }
    val bochumCenter = LatLng(51.4661, 7.2491)

    // Show initial instruction toast
    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "Toggle overlays and adjust transparency.\nSwitch between different tile layers",
            Toast.LENGTH_LONG,
        ).show()
    }

    fun toggleOverlay(
        index: Int,
        enabled: Boolean,
    ) {
        mapView?.let { map ->
            val updated = overlays.toMutableList()
            val state = updated[index]

            if (enabled) {
                val newOverlay =
                    state.overlay.copy(
                        visible = true,
                        transparency = transparency,
                    )
                map.addTileOverlay(newOverlay)
                updated[index] = state.copy(overlay = newOverlay, isVisible = true, transparency = transparency)
            } else {
                map.removeTileOverlay(state.overlay)
                updated[index] = state.copy(isVisible = false)
            }

            overlays = updated
        }
    }

    fun updateTransparency(newTransparency: Float) {
        transparency = newTransparency
        mapView?.let { map ->
            // Update all visible overlays
            val updated =
                overlays.map { state ->
                    if (state.isVisible) {
                        map.removeTileOverlay(state.overlay)
                        val newOverlay =
                            state.overlay.copy(
                                transparency = newTransparency,
                            )
                        map.addTileOverlay(newOverlay)
                        state.copy(overlay = newOverlay, transparency = newTransparency)
                    } else {
                        state
                    }
                }
            overlays = updated
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    OpenMapView(ctx).apply {
                        mapView = this
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(bochumCenter)
                        setZoom(12.0f)
                        setOnAttributionClickListener {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://www.openstreetmap.org/copyright".toUri(),
                            )
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight(),
            ) {
                OverlayControls(
                    overlays = overlays,
                    transparency = transparency,
                    onToggleOverlay = { index, enabled -> toggleOverlay(index, enabled) },
                    onTransparencyChange = { newValue -> updateTransparency(newValue) },
                    onClearAll = {
                        mapView?.clearTileOverlays()
                        overlays = overlays.map { state -> state.copy(isVisible = false) }
                        transparency = 0.5f
                        Toast.makeText(context, "All overlays cleared", Toast.LENGTH_SHORT).show()
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
                        mapView = this
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(bochumCenter)
                        setZoom(12.0f)
                        setOnAttributionClickListener {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                "https://www.openstreetmap.org/copyright".toUri(),
                            )
                            context.startActivity(intent)
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                OverlayControls(
                    overlays = overlays,
                    transparency = transparency,
                    onToggleOverlay = { index, enabled -> toggleOverlay(index, enabled) },
                    onTransparencyChange = { newValue -> updateTransparency(newValue) },
                    onClearAll = {
                        mapView?.clearTileOverlays()
                        overlays = overlays.map { state -> state.copy(isVisible = false) }
                        transparency = 0.5f
                        Toast.makeText(context, "All overlays cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun OverlayControls(
    overlays: List<OverlayState>,
    transparency: Float,
    onToggleOverlay: (Int, Boolean) -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Tile Overlays",
            style = MaterialTheme.typography.titleMedium,
        )

        overlays.forEachIndexed { index, state ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = state.isVisible,
                    onCheckedChange = { enabled ->
                        onToggleOverlay(index, enabled)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Transparency: ${(transparency * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = transparency,
            onValueChange = onTransparencyChange,
            valueRange = 0f..1f,
        )

        Button(
            onClick = onClearAll,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Clear All")
        }
    }
}
