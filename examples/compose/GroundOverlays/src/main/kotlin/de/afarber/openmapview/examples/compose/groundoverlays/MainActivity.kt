/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.groundoverlays

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.compose.GroundOverlay
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState
import kotlin.math.roundToInt

private const val FESTIVAL_WIDTH = 1000f
private val FESTIVAL_POSITION = LatLng(51.4840, 7.2310)
private val CAMPUS_BOUNDS = LatLngBounds(LatLng(51.4400, 7.2540), LatLng(51.4500, 7.2730))

// The bearing and transparency of one plan, changed by the sliders
private class Plan(
    val title: String,
) {
    var bearing by mutableFloatStateOf(0f)
    var transparency by mutableFloatStateOf(0f)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GroundOverlaysScreen()
            }
        }
    }
}

@Composable
fun GroundOverlaysScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4630, 7.2450), 13.2f) }
    val festival = remember { Plan("Festival plan, ${FESTIVAL_WIDTH.roundToInt()} m wide") }
    val campus = remember { Plan("Campus plan, stretched over its bounds") }
    var selected by remember { mutableStateOf<Plan?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(modifier = Modifier.fillMaxSize(), cameraPositionState = cameraPositionState) {
            // Placed by position: the image is 1 km wide at the Stadtpark, its height follows the image
            GroundOverlay(
                image = BitmapDescriptorFactory.fromResource(R.drawable.plan_festival),
                position = FESTIVAL_POSITION,
                width = FESTIVAL_WIDTH,
                bearing = festival.bearing,
                transparency = festival.transparency,
                clickable = true,
                onClick = { selected = festival },
            )
            // Placed by bounds: the image is stretched over the area of the campus
            GroundOverlay(
                image = BitmapDescriptorFactory.fromResource(R.drawable.plan_campus),
                bounds = CAMPUS_BOUNDS,
                bearing = campus.bearing,
                transparency = campus.transparency,
                clickable = true,
                onClick = { selected = campus },
            )
        }

        // The selected plan
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                val plan = selected
                if (plan == null) {
                    Text("Tap a plan to select it", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text(plan.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Bearing ${plan.bearing.roundToInt()}°, ${(plan.transparency * 100).roundToInt()}% transparent",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }

        // Bearing and transparency of the selected plan: changed parameters update the
        // same overlay on the map while the sliders move
        Card(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(260.dp)) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Bearing", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = selected?.bearing ?: 0f,
                    onValueChange = { selected?.bearing = it },
                    valueRange = 0f..360f,
                    enabled = selected != null,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Text("Transparency", modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.labelLarge)
                Slider(
                    value = selected?.transparency ?: 0f,
                    onValueChange = { selected?.transparency = it },
                    valueRange = 0f..0.9f,
                    enabled = selected != null,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        }
    }
}
