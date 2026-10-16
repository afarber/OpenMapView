/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.camera

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.CameraMoveStartedReason
import de.afarber.openmapview.compose.CameraPositionState
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private class Destination(
    val name: String,
    val target: LatLng,
    val zoom: Float,
)

// Each destination has its own zoom, so the camera moves and zooms at the same time
private val DESTINATIONS =
    listOf(
        Destination("Mining Museum", LatLng(51.4892, 7.2174), 16f),
        Destination("Ruhr University", LatLng(51.4452, 7.2622), 15f),
        Destination("Dortmund", LatLng(51.5136, 7.4653), 13f),
        Destination("Ruhr area", LatLng(51.4800, 7.2500), 10f),
    )

private val DURATIONS = listOf("0.5 s" to 500, "2 s" to 2000, "5 s" to 5000)

// The panel width plus its margins: the camera centers in the area left of it
private val PANEL_WIDTH = 280.dp
private val PANEL_PADDING = PANEL_WIDTH + 32.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CameraScreen()
            }
        }
    }
}

@Composable
fun CameraScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4818, 7.2162), 13f) }
    var duration by rememberSaveable { mutableIntStateOf(1) }
    var callback by rememberSaveable { mutableStateOf("Pick a destination and a duration") }
    // The running flight: cancelling it stops the camera animation
    var flight by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    fun fly(destination: Destination) {
        val (label, durationMs) = DURATIONS[duration]
        callback = "Flying to ${destination.name} in $label"
        flight =
            scope.launch {
                try {
                    // Suspends until the animation is finished
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(destination.target, destination.zoom), durationMs)
                    callback = "Finished: ${destination.name} in $label"
                } catch (e: CancellationException) {
                    // A new flight, a tap on the map or Stop interrupts this one
                    callback = "Canceled: ${destination.name}"
                    throw e
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // The camera centers in the area left of the panel, so the destination isn't hidden
            contentPadding = PaddingValues(end = PANEL_PADDING),
        )

        // The camera state and the last animation callback
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                CameraStatus(cameraPositionState)
                Text(callback, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Destinations, the animation duration and the stop button
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(PANEL_WIDTH),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DESTINATIONS.forEach { destination ->
                Button(onClick = { fly(destination) }, modifier = Modifier.fillMaxWidth()) {
                    Text(destination.name)
                }
            }
            Card {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                    DURATIONS.forEachIndexed { index, (label, _) ->
                        SegmentedButton(
                            selected = index == duration,
                            onClick = { duration = index },
                            shape = SegmentedButtonDefaults.itemShape(index, DURATIONS.size),
                        ) {
                            Text(label)
                        }
                    }
                }
            }
            FilledTonalButton(onClick = { flight?.cancel() }, modifier = Modifier.fillMaxWidth()) {
                Text("Stop")
            }
        }
    }
}

// Zoom and why the camera moves, read only here, so the rest of the screen
// doesn't recompose on every frame of a flight
@Composable
fun CameraStatus(cameraPositionState: CameraPositionState) {
    val state =
        when {
            !cameraPositionState.isMoving -> "idle"
            cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE -> "moving by gesture"
            cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.API_ANIMATION -> "moving by the map"
            else -> "moving by the app"
        }
    Text("Zoom %.1f, %s".format(cameraPositionState.position.zoom, state), style = MaterialTheme.typography.titleMedium)
}
