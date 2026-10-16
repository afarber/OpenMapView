/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.circles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.Circle
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState

private val COLORS = listOf(Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFF4511E), Color(0xFF8E24AA), Color(0xFFFDD835))

// A circle on the map; its radius changes while the slider moves
private class Area(
    val id: Int,
    val name: String,
    val center: LatLng,
    radius: Float,
    val color: Color,
) {
    var radius by mutableFloatStateOf(radius)
}

private fun Area.radiusText() = if (radius < 1000) "%.0f m".format(radius) else "%.1f km".format(radius / 1000)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CirclesScreen()
            }
        }
    }
}

@Composable
fun CirclesScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4660, 7.2380), 12.4f) }
    val areas = remember { mutableStateListOf<Area>() }
    var nextId by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Area?>(null) }
    var message by remember { mutableStateOf("Tap a circle to select it") }

    fun add(
        name: String,
        center: LatLng,
        radius: Float,
    ): Area = Area(nextId++, name, center, radius, COLORS[areas.size % COLORS.size]).also { areas.add(it) }

    if (areas.isEmpty() && nextId == 0) {
        // Radii are in meters, so a circle keeps its size on the ground at any zoom
        add("Main Station", LatLng(51.4783, 7.2231), 500f)
        add("Mining Museum", LatLng(51.4892, 7.2174), 1000f)
        add("Ruhr University", LatLng(51.4452, 7.2622), 2000f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            areas.forEach { area ->
                key(area.id) {
                    val isSelected = area == selected
                    // Changed parameters update the same circle on the map
                    Circle(
                        center = area.center,
                        radius = area.radius,
                        strokeColor = area.color,
                        // The selected circle gets a wide outline and is drawn on top
                        strokeWidth = if (isSelected) 12f else 4f,
                        fillColor = area.color.copy(alpha = 0.19f),
                        clickable = true,
                        zIndex = if (isSelected) 1f else 0f,
                        // Where circles overlap, the topmost one gets the tap
                        onClick = { selected = area },
                    )
                }
            }
        }

        // The selected circle
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text(
                text = selected?.let { "${it.name}: radius ${it.radiusText()}" } ?: message,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                onClick = { selected = add("Circle ${areas.size + 1}", cameraPositionState.position.target, 800f) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add circle")
            }
            FilledTonalButton(
                onClick = {
                    areas.clear()
                    selected = null
                    message = "Circles removed"
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear")
            }
            // Changes the radius of the selected circle while dragging
            Card {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Radius", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = selected?.radius ?: 1000f,
                        onValueChange = { value -> selected?.radius = value },
                        valueRange = 100f..3000f,
                        steps = 28,
                        enabled = selected != null,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}
