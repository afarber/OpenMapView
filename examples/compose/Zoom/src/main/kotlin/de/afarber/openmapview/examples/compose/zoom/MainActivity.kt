/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.zoom

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdate
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.CameraMoveStartedReason
import de.afarber.openmapview.compose.CameraPositionState
import de.afarber.openmapview.compose.MapProperties
import de.afarber.openmapview.compose.MapUiSettings
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState
import de.afarber.openmapview.compose.rememberMarkerState
import kotlinx.coroutines.launch

private val CITY_HALL = LatLng(51.4817, 7.2165)

// The zoom range while "Limit zoom" is on, and the full range otherwise
private const val MIN_ZOOM = 10f
private const val MAX_ZOOM = 16f

// Zoom levels from the whole city down to single streets
private val LEVELS = listOf("City" to 12f, "District" to 14f, "Street" to 17f)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ZoomScreen()
            }
        }
    }
}

@Composable
fun ZoomScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(CITY_HALL, 14f) }
    var limitZoom by rememberSaveable { mutableStateOf(false) }
    var zoomButtons by rememberSaveable { mutableStateOf(false) }
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
            // Zooming in or out beyond the limits makes the map edges glow
            properties =
            if (limitZoom) {
                MapProperties(minZoomPreference = MIN_ZOOM, maxZoomPreference = MAX_ZOOM)
            } else {
                MapProperties()
            },
            // The +/- buttons drawn by the map itself, at the bottom right
            uiSettings = MapUiSettings(zoomControlsEnabled = zoomButtons),
        ) {
            Marker(state = rememberMarkerState(CITY_HALL), title = "Bochum City Hall")
        }

        ZoomStatus(cameraPositionState, Modifier.align(Alignment.TopStart).padding(16.dp))

        // Zoom levels to fly to, and the zoom settings
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(250.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LEVELS.forEach { (name, zoom) ->
                Button(onClick = { animateTo(CameraUpdateFactory.zoomTo(zoom), 1000) }, modifier = Modifier.fillMaxWidth()) {
                    Text(name)
                }
            }
            Card {
                SwitchRow("Limit zoom to 10-16", limitZoom) { limitZoom = it }
                SwitchRow("Built-in zoom buttons", zoomButtons) { zoomButtons = it }
            }
        }

        // One level per tap, animated like the built-in zoom buttons
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ZoomButton(Icons.Filled.Remove, "Zoom out") { animateTo(CameraUpdateFactory.zoomOut(), 300) }
            ZoomButton(Icons.Filled.Add, "Zoom in") { animateTo(CameraUpdateFactory.zoomIn(), 300) }
        }
    }
}

// Zoom level and why the camera moves: a gesture, the app or the map's own buttons.
// Reading the state only here keeps the rest of the screen from recomposing while the map moves.
@Composable
fun ZoomStatus(
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
) {
    val state =
        when {
            !cameraPositionState.isMoving -> "idle"
            cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE -> "moving by gesture"
            cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.API_ANIMATION -> "moving by the map"
            else -> "moving by the app"
        }
    Card(modifier = modifier) {
        Text(
            text = "Zoom %.1f, %s".format(cameraPositionState.position.zoom, state),
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

// Big square zoom button, easy to hit on a car display
@Composable
fun ZoomButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(76.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(40.dp))
    }
}
