/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.pan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdate
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.compose.CameraPositionState
import de.afarber.openmapview.compose.MapProperties
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.Polygon
import de.afarber.openmapview.compose.rememberCameraPositionState
import de.afarber.openmapview.compose.rememberMarkerState
import kotlinx.coroutines.launch

private val CITY_HALL = LatLng(51.4817, 7.2165)

private class Place(
    val name: String,
    val title: String,
    val position: LatLng,
    val hue: Float,
)

private val PLACES =
    listOf(
        Place("Mining Museum", "German Mining Museum", LatLng(51.4893, 7.2171), BitmapDescriptorFactory.HUE_ORANGE),
        Place("Main Station", "Bochum Main Station", LatLng(51.4785, 7.2232), BitmapDescriptorFactory.HUE_AZURE),
        Place("Ruhr University", "Ruhr University", LatLng(51.4447, 7.2617), BitmapDescriptorFactory.HUE_GREEN),
    )

// While "Stay in Bochum" is on, the camera target can't leave this area
private val BOCHUM_AREA = LatLngBounds(LatLng(51.43, 7.13), LatLng(51.53, 7.33))

// The corners of the area, to outline it with a polygon
private val BOCHUM_OUTLINE =
    listOf(LatLng(51.43, 7.13), LatLng(51.43, 7.33), LatLng(51.53, 7.33), LatLng(51.53, 7.13))

// How far one arrow button pans the map, in pixels
private const val PAN_STEP = 200f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PanScreen()
            }
        }
    }
}

@Composable
fun PanScreen() {
    // The camera position survives rotation
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(CITY_HALL, 14f) }
    var stayInBochum by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun animateTo(
        update: CameraUpdate,
        durationMs: Int,
    ) {
        scope.launch { cameraPositionState.animate(update, durationMs) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // A null area lets the camera go anywhere again
            properties = MapProperties(latLngBoundsForCameraTarget = if (stayInBochum) BOCHUM_AREA else null),
        ) {
            PLACES.forEach { place ->
                key(place.name) {
                    Marker(
                        state = rememberMarkerState(place.position),
                        title = place.title,
                        icon = BitmapDescriptorFactory.defaultMarker(place.hue),
                    )
                }
            }
            // Shapes come and go with the composition, like any other UI
            if (stayInBochum) {
                Polygon(
                    points = BOCHUM_OUTLINE,
                    strokeColor = Color(0xFF1565C0),
                    strokeWidth = 6f,
                    fillColor = Color(0x201565C0),
                )
            }
        }

        CameraStatus(cameraPositionState, Modifier.align(Alignment.TopStart).padding(16.dp))

        // Places to fly to, and the camera bounds switch
        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(16.dp).width(200.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PLACES.forEach { place ->
                Button(
                    onClick = { animateTo(CameraUpdateFactory.newLatLng(place.position), 1000) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(place.name)
                }
            }
            Card {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Stay in Bochum", modifier = Modifier.weight(1f))
                    Switch(
                        checked = stayInBochum,
                        onCheckedChange = {
                            stayInBochum = it
                            // Zoom out to show the whole area, with some padding in pixels
                            if (it) animateTo(CameraUpdateFactory.newLatLngBounds(BOCHUM_AREA, 48), 1000)
                        },
                    )
                }
            }
        }

        // Arrow pad that pans the map by a fixed distance, e.g. for a d-pad in a car
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ArrowButton(Icons.AutoMirrored.Filled.ArrowBack, "Pan left") { animateTo(CameraUpdateFactory.scrollBy(-PAN_STEP, 0f), 300) }
            ArrowButton(Icons.Filled.ArrowUpward, "Pan up") { animateTo(CameraUpdateFactory.scrollBy(0f, -PAN_STEP), 300) }
            ArrowButton(Icons.Filled.ArrowDownward, "Pan down") { animateTo(CameraUpdateFactory.scrollBy(0f, PAN_STEP), 300) }
            ArrowButton(Icons.AutoMirrored.Filled.ArrowForward, "Pan right") { animateTo(CameraUpdateFactory.scrollBy(PAN_STEP, 0f), 300) }
        }
    }
}

// Camera position, updated while the map moves. Reading the state only here keeps the
// rest of the screen from recomposing on every frame of a pan.
@Composable
fun CameraStatus(
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
) {
    val camera = cameraPositionState.position
    Card(modifier = modifier) {
        Text(
            text = "%.4f, %.4f at zoom %.1f".format(camera.target.latitude, camera.target.longitude, camera.zoom),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

// Big square arrow button, easy to hit on a car display
@Composable
fun ArrowButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(76.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(40.dp))
    }
}
