/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.geojson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.GeoJsonParser
import de.afarber.openmapview.GeoJsonResult
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.CameraPositionState
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.Polygon
import de.afarber.openmapview.compose.Polyline
import de.afarber.openmapview.compose.rememberCameraPositionState
import de.afarber.openmapview.compose.rememberMarkerState
import kotlinx.coroutines.launch
import org.json.JSONObject

// The panel width plus its margins: the camera fits the data into the area left of it
private val PANEL_WIDTH = 220.dp
private val PANEL_PADDING = PANEL_WIDTH + 32.dp

// The name and description of a GeoJSON feature, kept in the tag of its shape
private class Feature(
    val name: String,
    val description: String,
)

private fun Any?.toFeature(): Feature? = (this as? JSONObject)?.let { Feature(it.optString("name"), it.optString("description")) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                GeoJsonScreen()
            }
        }
    }
}

@Composable
fun GeoJsonScreen() {
    val context = LocalContext.current
    // Points become markers, LineStrings polylines and Polygons polygons, styled with the
    // simplestyle properties stroke, stroke-width, fill and fill-opacity
    val result = remember { GeoJsonParser.parse(context.assets.open("bochum.geojson").bufferedReader().use { it.readText() }) }
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4720, 7.2300), 12f) }
    var showPoints by rememberSaveable { mutableStateOf(true) }
    var showLines by rememberSaveable { mutableStateOf(true) }
    var showAreas by rememberSaveable { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Feature?>(null) }
    var cardBottom by remember { mutableStateOf(0.dp) }
    var fitted by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Once the status card is measured, the camera fits the data into the area below it,
    // which also leaves room for the marker icons above their points
    val measuredCardBottom = cardBottom
    LaunchedEffect(measuredCardBottom) {
        if (measuredCardBottom > 0.dp && !fitted) {
            fitted = true
            showAll(cameraPositionState, result)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            contentPadding = PaddingValues(top = cardBottom, end = PANEL_PADDING),
        ) {
            // Each shape's tag holds the properties of its GeoJSON feature
            result.polygons.forEach { area ->
                Polygon(
                    points = area.points,
                    holes = area.holes,
                    strokeColor = area.strokeColor,
                    strokeWidth = area.strokeWidth,
                    fillColor = area.fillColor,
                    visible = showAreas,
                    clickable = true,
                    tag = area.tag,
                    onClick = { selected = it.tag.toFeature() },
                )
            }
            result.polylines.forEach { line ->
                Polyline(
                    points = line.points,
                    strokeColor = line.strokeColor,
                    strokeWidth = line.strokeWidth,
                    visible = showLines,
                    clickable = true,
                    tag = line.tag,
                    onClick = { selected = it.tag.toFeature() },
                )
            }
            if (showPoints) {
                result.markers.forEach { point ->
                    // Markers show name and description in their info window, and in the card
                    Marker(
                        state = rememberMarkerState(point.position),
                        title = point.title,
                        snippet = point.snippet,
                        tag = point.tag,
                        onClick = {
                            selected = it.tag.toFeature()
                            false
                        },
                    )
                }
            }
        }

        // The tapped feature, or what was loaded
        Card(
            modifier =
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .onGloballyPositioned { cardBottom = with(density) { it.boundsInParent().bottom.toDp() } },
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                val feature = selected
                Text(
                    feature?.name
                        ?: "${result.markers.size} points, ${result.polylines.size} lines, ${result.polygons.size} areas",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(feature?.description ?: "Tap a feature for its details", style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Hide and show features, and fit them all again
        Card(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(PANEL_WIDTH)) {
            Column(modifier = Modifier.padding(8.dp)) {
                LabeledSwitch("Points", showPoints) { showPoints = it }
                LabeledSwitch("Lines", showLines) { showLines = it }
                LabeledSwitch("Areas", showAreas) { showAreas = it }
                FilledTonalButton(
                    onClick = {
                        selected = null
                        scope.launch { showAll(cameraPositionState, result) }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Show all")
                }
            }
        }
    }
}

// GeoJsonResult.bounds contains all features
private suspend fun showAll(
    cameraPositionState: CameraPositionState,
    result: GeoJsonResult,
) {
    result.bounds?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(it, 64), 800) }
}

@Composable
private fun LabeledSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
