/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.draggablemarkers

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.MarkerState
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.Polyline
import de.afarber.openmapview.compose.rememberCameraPositionState

private val START = LatLng(51.4783, 7.2231)
private val DESTINATION = LatLng(51.4452, 7.2622)

// A marker of the route, with a state that follows the drags
private class Stop(
    val id: Int,
    val title: String,
    val hue: Float,
    position: LatLng,
) {
    val state = MarkerState(position)
}

private fun LatLng.format() = "%.5f, %.5f".format(latitude, longitude)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                DraggableMarkersScreen()
            }
        }
    }
}

@Composable
fun DraggableMarkersScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4630, 7.2380), 13.5f) }
    // The route from the start over the stops to the destination
    val route = remember { mutableStateListOf<Stop>() }
    var nextId by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var lastDragged by remember { mutableStateOf<Stop?>(null) }

    // Inserts a stop before the destination, halfway from the previous point
    fun addStop() {
        val previous = route[route.size - 2].state.position
        val next = route.last().state.position
        val middle = LatLng((previous.latitude + next.latitude) / 2, (previous.longitude + next.longitude) / 2)
        val title = "Stop ${route.size - 1}"
        route.add(route.size - 1, Stop(nextId++, title, BitmapDescriptorFactory.HUE_ORANGE, middle))
        message = "$title added"
    }

    fun reset() {
        route.clear()
        route.add(Stop(nextId++, "Start", BitmapDescriptorFactory.HUE_GREEN, START))
        route.add(Stop(nextId++, "Destination", BitmapDescriptorFactory.HUE_RED, DESTINATION))
        addStop()
        message = "Drag a marker to change the route"
        lastDragged = null
    }

    if (route.isEmpty()) reset()

    // The marker being dragged, if any: isDragging is true from drag start to drag end
    val dragging = route.firstOrNull { it.state.isDragging }
    LaunchedEffect(dragging) {
        if (dragging != null) {
            lastDragged = dragging
            message = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            // Reads the marker positions, so the route follows the dragged marker
            Polyline(points = route.map { it.state.position }, strokeColor = Color(0xFF1E88E5), strokeWidth = 8f)
            route.forEach { stop ->
                key(stop.id) {
                    Marker(
                        state = stop.state,
                        title = stop.title,
                        draggable = true,
                        icon = BitmapDescriptorFactory.defaultMarker(stop.hue),
                    )
                }
            }
        }

        // The last drag event
        val status =
            when {
                dragging != null -> "Dragging ${dragging.title}: ${dragging.state.position.format()}"
                message != null -> message.orEmpty()
                else -> lastDragged?.let { "Dropped ${it.title} at ${it.state.position.format()}" }.orEmpty()
            }
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Text(status, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.titleMedium)
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { addStop() }, modifier = Modifier.fillMaxWidth()) {
                Text("Add stop")
            }
            FilledTonalButton(onClick = { reset() }, modifier = Modifier.fillMaxWidth()) {
                Text("Reset")
            }
        }
    }
}
