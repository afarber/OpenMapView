/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.clicks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.Circle
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState
import de.afarber.openmapview.compose.rememberMarkerState

private val CITY_HALL = LatLng(51.4817, 7.2165)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ClicksScreen()
            }
        }
    }
}

// A marker dropped by a long press, numbered from 1
private class Drop(
    val number: Int,
    val position: LatLng,
)

private fun LatLng.format() = "%.5f, %.5f".format(latitude, longitude)

@Composable
fun ClicksScreen() {
    // A fractional zoom: taps are converted to coordinates at any zoom
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(CITY_HALL, 14.5f) }
    var lastTap by remember { mutableStateOf<LatLng?>(null) }
    val drops = remember { mutableStateListOf<Drop>() }
    var nextNumber by rememberSaveable { mutableStateOf(1) }
    var status by rememberSaveable { mutableStateOf("Tap the map, or long-press to drop a marker") }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // A tap reports its coordinates
            onMapClick = {
                lastTap = it
                status = "Tap at ${it.format()}"
            },
            // A long press drops a numbered marker; tapping it opens its info window
            onMapLongClick = {
                drops.add(Drop(nextNumber, it))
                status = "Marker $nextNumber at ${it.format()}"
                nextNumber++
            },
        ) {
            // A changed center moves the same circle on the map
            lastTap?.let { tap ->
                Circle(
                    center = tap,
                    radius = 80f,
                    strokeColor = Color(0xFF1E88E5),
                    strokeWidth = 4f,
                    fillColor = Color(0x401E88E5),
                )
            }
            drops.forEach { drop ->
                key(drop.number) {
                    Marker(
                        state = rememberMarkerState(drop.position),
                        title = "Marker ${drop.number}",
                        snippet = drop.position.format(),
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    )
                }
            }
        }

        // The last tap or long press
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text(status, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
        }

        FilledTonalButton(
            onClick = {
                drops.clear()
                status = "Markers removed"
            },
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
        ) {
            Text("Clear markers")
        }
    }
}
