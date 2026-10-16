/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.markers

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
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.MarkerState
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private class Place(
    val title: String,
    val snippet: String,
    val position: LatLng,
    val hue: Float,
)

private val PLACES =
    listOf(
        Place("Main Station", "Trains to the whole Ruhr area", LatLng(51.4783, 7.2231), BitmapDescriptorFactory.HUE_RED),
        Place("City Hall", "Bochum Rathaus", LatLng(51.4816, 7.2166), BitmapDescriptorFactory.HUE_GREEN),
        Place("Bermuda3eck", "Bars and restaurants", LatLng(51.4761, 7.2161), BitmapDescriptorFactory.HUE_ORANGE),
        Place("Schauspielhaus", "Drama theater", LatLng(51.4722, 7.2177), BitmapDescriptorFactory.HUE_YELLOW),
        Place("Mining Museum", "Deutsches Bergbau-Museum", LatLng(51.4892, 7.2174), BitmapDescriptorFactory.HUE_MAGENTA),
        Place("Planetarium", "Zeiss Planetarium Bochum", LatLng(51.4854, 7.2278), BitmapDescriptorFactory.HUE_VIOLET),
        Place("Starlight Express", "Musical theater", LatLng(51.4927, 7.2342), BitmapDescriptorFactory.HUE_CYAN),
        Place("Ruhr University", "University campus", LatLng(51.4452, 7.2622), BitmapDescriptorFactory.HUE_BLUE),
    )

private val ALL_PLACES =
    LatLngBounds.builder().apply { PLACES.forEach { include(it.position) } }.build()

// The panel width plus its margins: the camera centers in the area left of it
private val PANEL_WIDTH = 200.dp
private val PANEL_PADDING = PANEL_WIDTH + 32.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MarkersScreen()
            }
        }
    }
}

@Composable
fun MarkersScreen() {
    val cameraPositionState =
        rememberCameraPositionState { position = CameraPosition(LatLng(51.4687, 7.2392), 13f) }
    val markerStates = remember { PLACES.map { MarkerState(it.position) } }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var event by rememberSaveable { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun select(index: Int) {
        selected = index
        // Only one info window is shown at a time, so this closes the previous one
        markerStates[index].showInfoWindow()
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLng(PLACES[index].position), 1000) }
    }

    fun showAll() {
        if (selected >= 0) markerStates[selected].hideInfoWindow()
        selected = -1
        // Fits all places into the area left of the panel, with 64 px to spare
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(ALL_PLACES, 64), 1000) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // The camera centers in the area left of the panel, so the selected marker isn't hidden
            contentPadding = PaddingValues(end = PANEL_PADDING),
        ) {
            PLACES.forEachIndexed { index, place ->
                // Title and snippet are shown in the marker's info window
                Marker(
                    state = markerStates[index],
                    title = place.title,
                    snippet = place.snippet,
                    icon = BitmapDescriptorFactory.defaultMarker(place.hue),
                    // Returning false keeps the default: the info window opens and the camera moves there
                    onClick = {
                        selected = index
                        false
                    },
                    onInfoWindowClick = { event = "Info window tapped: ${place.title}" },
                    onInfoWindowClose = { event = "Info window closed: ${place.title}" },
                )
            }
        }

        // The selected marker and the last info window event
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = if (selected < 0) "Tap a marker or Next" else "${selected + 1} of ${PLACES.size}: ${PLACES[selected].title}",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (event.isNotEmpty()) {
                    Text(event, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Selection buttons. The content padding keeps the selected marker out from under them.
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(PANEL_WIDTH),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { select((if (selected <= 0) PLACES.size else selected) - 1) }, modifier = Modifier.fillMaxWidth()) {
                Text("Previous")
            }
            Button(onClick = { select((selected + 1) % PLACES.size) }, modifier = Modifier.fillMaxWidth()) {
                Text("Next")
            }
            FilledTonalButton(onClick = { showAll() }, modifier = Modifier.fillMaxWidth()) {
                Text("Show all")
            }
        }
    }
}
