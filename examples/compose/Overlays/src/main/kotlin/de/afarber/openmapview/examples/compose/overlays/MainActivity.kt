/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.overlays

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.PredefinedTileProviders
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.TileOverlay
import de.afarber.openmapview.compose.rememberCameraPositionState
import kotlin.math.roundToInt

private val NAMES = listOf("Hiking trails", "Cycling routes", "Mountain bike routes")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                OverlaysScreen()
            }
        }
    }
}

@Composable
fun OverlaysScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4720, 7.2300), 13f) }
    // Each overlay draws transparent tiles of a public OpenStreetMap service over the base map.
    // Remembered, so recompositions keep the overlays and their downloaded tiles.
    val providers =
        remember {
            listOf(
                PredefinedTileProviders.waymarkedTrailsHiking(),
                PredefinedTileProviders.waymarkedTrailsCycling(),
                PredefinedTileProviders.waymarkedTrailsMtb(),
            )
        }
    val visible = remember { mutableStateListOf(true, true, true) }
    var transparency by rememberSaveable { mutableFloatStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            providers.forEachIndexed { index, provider ->
                // A changed transparency or visibility updates the same overlay, which keeps
                // its tiles, so showing it again is instant
                TileOverlay(tileProvider = provider, transparency = transparency, visible = visible[index])
            }
        }

        // The visible overlays and their transparency
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text(
                text = "${visible.count { it }} of 3 overlays, ${(transparency * 100).roundToInt()}% transparent",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        // One switch per overlay, and the transparency of all overlays
        Card(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(260.dp)) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                NAMES.forEachIndexed { index, name ->
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(name, modifier = Modifier.weight(1f))
                        Switch(checked = visible[index], onCheckedChange = { visible[index] = it })
                    }
                }
                Text(
                    "Transparency",
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = transparency,
                    onValueChange = { transparency = it },
                    valueRange = 0f..0.9f,
                    steps = 8,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                // The overlay services require attribution, like the base map
                Text(
                    "Overlays: © Waymarked Trails, © OpenStreetMap contributors",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
