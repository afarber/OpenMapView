/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.rotation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.SphericalUtil
import de.afarber.openmapview.compose.CameraMoveStartedReason
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.Polyline
import de.afarber.openmapview.compose.rememberCameraPositionState
import de.afarber.openmapview.compose.rememberMarkerState
import kotlin.math.roundToInt

// A bus loop around the center of Bochum, closed by repeating the first stop
private val ROUTE =
    listOf(
        LatLng(51.4787, 7.2232),
        LatLng(51.4823, 7.2160),
        LatLng(51.4862, 7.2125),
        LatLng(51.4895, 7.2174),
        LatLng(51.4870, 7.2265),
        LatLng(51.4835, 7.2330),
        LatLng(51.4790, 7.2345),
        LatLng(51.4760, 7.2290),
        LatLng(51.4787, 7.2232),
    )
private val ROUTE_LENGTH = SphericalUtil.computeLength(ROUTE)

// A fast bus, in meters per second, so it gets around in under a minute
private const val SPEED = 80.0

// How fast the bus turns at a corner, in degrees per second
private const val TURN_SPEED = 120f

// How fast the following camera turns, in degrees per second
private const val CAMERA_TURN_SPEED = 180f

// How the camera moves: following the bus and turning with it, following it facing north,
// or left alone after the user turned or moved the map
private enum class Mode { HEADING_UP, NORTH_UP, FREE }

// The position and the direction of the road at a distance along the loop
private fun routeAt(distance: Double): Pair<LatLng, Float> {
    var rest = distance % ROUTE_LENGTH
    for ((from, to) in ROUTE.zipWithNext()) {
        val length = SphericalUtil.computeDistanceBetween(from, to)
        if (rest <= length) {
            return SphericalUtil.interpolate(from, to, rest / length) to SphericalUtil.computeHeading(from, to).toFloat()
        }
        rest -= length
    }
    return ROUTE.last() to 0f
}

// Turns `heading` toward `target` the shorter way, by at most `maxTurn` degrees
private fun turnToward(
    heading: Float,
    target: Float,
    maxTurn: Float,
): Float {
    val difference = ((target - heading) % 360f + 540f) % 360f - 180f
    return heading + difference.coerceIn(-maxTurn, maxTurn)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                RotationScreen()
            }
        }
    }
}

@Composable
fun RotationScreen() {
    val start = routeAt(0.0)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(start.first, 15.5f, start.second) }
    val bus = rememberMarkerState(start.first)
    var busHeading by remember { mutableFloatStateOf(start.second) }
    var mode by remember { mutableStateOf(Mode.HEADING_UP) }
    var flat by remember { mutableStateOf(true) }

    // Moves the bus along the loop once per display frame, and the camera with it
    LaunchedEffect(Unit) {
        var distance = 0.0
        var lastFrame = withFrameNanos { it }
        while (true) {
            withFrameNanos { frame ->
                val seconds = (frame - lastFrame) / 1e9
                lastFrame = frame
                distance += SPEED * seconds
                val (position, roadHeading) = routeAt(distance)
                bus.position = position
                busHeading = turnToward(busHeading, roadHeading, (TURN_SPEED * seconds).toFloat())

                // Following, like a navigation app: the camera turns smoothly toward the bus
                // heading or north, around the camera target, which is the bus
                if (mode != Mode.FREE) {
                    val camera = cameraPositionState.position
                    val wanted = if (mode == Mode.HEADING_UP) busHeading else 0f
                    val bearing = turnToward(camera.bearing, wanted, (CAMERA_TURN_SPEED * seconds).toFloat())
                    cameraPositionState.move(CameraUpdateFactory.newCameraPosition(CameraPosition(position, camera.zoom, bearing)))
                }
            }
        }
    }

    // Touching the map, e.g. to turn it with two fingers, stops following the bus. The
    // compass, one of the map's own buttons, turns a heading-up map north up.
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) return@LaunchedEffect
        when (cameraPositionState.cameraMoveStartedReason) {
            CameraMoveStartedReason.GESTURE -> mode = Mode.FREE
            CameraMoveStartedReason.API_ANIMATION -> if (mode == Mode.HEADING_UP) mode = Mode.NORTH_UP
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // While heading up, the camera target is in the lower part of the map, so more of
            // the road ahead is visible. The map turns around the camera target.
            contentPadding = PaddingValues(top = if (mode == Mode.HEADING_UP) 160.dp else 0.dp, end = 272.dp),
        ) {
            Polyline(points = ROUTE, strokeColor = Color(0xC01565C0), strokeWidth = 10f)
            // A flat icon lies on the map and turns with it, so its rotation is relative to
            // north. Any other icon faces the screen, so its rotation is relative to the screen.
            Marker(
                state = bus,
                icon = BitmapDescriptorFactory.fromResource(R.drawable.bus_arrow),
                anchor = 0.5f to 0.5f,
                rotation = busHeading,
                flat = flat,
            )
        }

        val bearing = cameraPositionState.position.bearing
        Card(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(
                text =
                when (mode) {
                    Mode.HEADING_UP -> "Heading up: the map turns with the bus, heading ${busHeading.degrees()}"
                    Mode.NORTH_UP -> "North up: the map follows the bus"
                    Mode.FREE -> "Bearing ${bearing.degrees()}: turn with two fingers, the slider or the compass"
                },
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(240.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { mode = Mode.HEADING_UP }, enabled = mode != Mode.HEADING_UP, modifier = Modifier.fillMaxWidth()) {
                Text("Heading up")
            }
            FilledTonalButton(onClick = { mode = Mode.NORTH_UP }, enabled = mode != Mode.NORTH_UP, modifier = Modifier.fillMaxWidth()) {
                Text("North up")
            }
            Card {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Flat bus icon", modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                    Switch(checked = flat, onCheckedChange = { flat = it })
                }
            }
            // Turns the map and stops following; shows the bearing set by gestures and the compass
            Card {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text("Bearing", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = bearing,
                        onValueChange = { value ->
                            mode = Mode.FREE
                            cameraPositionState.move(CameraUpdateFactory.newCameraPosition(cameraPositionState.position.copy(bearing = value)))
                        },
                        valueRange = 0f..359f,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

// A bearing or heading as whole degrees from 0 to 359
private fun Float.degrees(): String = "${(((this % 360f) + 360f) % 360f).roundToInt() % 360}°"
