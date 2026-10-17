/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.toolbar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState
import de.afarber.openmapview.compose.rememberMarkerState
import de.afarber.openmapview.openLocationInExternalApp
import kotlinx.coroutines.launch

private class Place(
    val name: String,
    val position: LatLng,
)

private val PLACES =
    listOf(
        Place("Mining Museum", LatLng(51.4892, 7.2174)),
        Place("Main Station", LatLng(51.4783, 7.2231)),
        Place("Ruhr University", LatLng(51.4452, 7.2622)),
    )

// The panel width plus its margins: the camera centers in the area left of it
private val PANEL_WIDTH = 240.dp
private val PANEL_PADDING = PANEL_WIDTH + 32.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ToolbarScreen()
            }
        }
    }
}

@Composable
fun ToolbarScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4720, 7.2300), 13f) }
    var selected by remember { mutableStateOf<Place?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun select(place: Place) {
        selected = place
        message = null
        // The selected place becomes the camera target, which is opened in the map app
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(place.position, 15f), 800) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(end = PANEL_PADDING),
            onMapLongClick = { select(Place("Dropped pin", it)) },
        ) {
            selected?.let { place ->
                // A new place gets a new marker with its title
                key(place) {
                    Marker(state = rememberMarkerState(place.position), title = place.name)
                }
            }
        }

        // The selected place
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                val place = selected
                Text(place?.name ?: "Pick a place, or long-press the map", style = MaterialTheme.typography.titleMedium)
                val details = message ?: place?.position?.let { "%.5f, %.5f".format(it.latitude, it.longitude) }
                if (details != null) Text(details, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Places, and the button that hands the selected one to another app
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(PANEL_WIDTH),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PLACES.forEach { place ->
                FilledTonalButton(onClick = { select(place) }, modifier = Modifier.fillMaxWidth()) {
                    Text(place.name)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    // Opens the camera target in a map app through a geo: URI, with the name as
                    // the pin title. Without a map app, OpenStreetMap.org opens in the browser
                    // instead, which a car without a browser shows as a QR code for a phone.
                    val position = cameraPositionState.position
                    if (!openLocationInExternalApp(position.target, position.zoom, context, selected?.name)) {
                        message = "No app could open the location"
                    }
                },
                enabled = selected != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open in map app")
            }
        }
    }
}
