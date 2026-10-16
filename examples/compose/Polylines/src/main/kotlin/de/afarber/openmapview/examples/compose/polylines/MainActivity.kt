/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.polylines

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.Polygon
import de.afarber.openmapview.compose.Polyline
import de.afarber.openmapview.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val BOCHUM = LatLng(51.4818, 7.2162)
private val REYKJAVIK = LatLng(64.1466, -21.9426)

// A line or an area to select
private sealed class Item(
    val title: String,
    val snippet: String,
    val points: List<LatLng>,
) {
    val bounds: LatLngBounds = LatLngBounds.builder().apply { points.forEach { include(it) } }.build()
}

private class Line(
    title: String,
    snippet: String,
    points: List<LatLng>,
    val color: Color,
    val width: Float,
    val geodesic: Boolean = false,
) : Item(title, snippet, points)

private class Area(
    title: String,
    snippet: String,
    points: List<LatLng>,
    val strokeColor: Color,
    val holes: List<List<LatLng>> = emptyList(),
) : Item(title, snippet, points)

private val ITEMS =
    listOf(
        Line(
            "Springorum cycle path",
            "Cycle path on an old railway line",
            listOf(LatLng(51.4565, 7.2145), LatLng(51.4590, 7.2210), LatLng(51.4620, 7.2280), LatLng(51.4655, 7.2350), LatLng(51.4690, 7.2420)),
            Color(0xFF1E88E5),
            8f,
        ),
        Line(
            "Station to Bermuda3eck",
            "Walk through the city center",
            listOf(LatLng(51.4783, 7.2231), LatLng(51.4778, 7.2210), LatLng(51.4772, 7.2190), LatLng(51.4765, 7.2175), LatLng(51.4761, 7.2161)),
            Color(0xFFF4511E),
            8f,
        ),
        Area(
            "Stadtpark",
            "City park north of the center",
            listOf(
                LatLng(51.4820, 7.2260),
                LatLng(51.4850, 7.2260),
                LatLng(51.4870, 7.2310),
                LatLng(51.4860, 7.2360),
                LatLng(51.4830, 7.2370),
                LatLng(51.4810, 7.2330),
            ),
            Color(0xFF43A047),
        ),
        Area(
            "Westpark",
            "Park on a former steelworks site",
            listOf(
                LatLng(51.4750, 7.1980),
                LatLng(51.4780, 7.1990),
                LatLng(51.4790, 7.2050),
                LatLng(51.4770, 7.2080),
                LatLng(51.4740, 7.2060),
                LatLng(51.4735, 7.2010),
            ),
            Color(0xFF7CB342),
        ),
        // A polygon with a hole, cut out of its interior
        Area(
            "Ruhr University",
            "Campus with a hole for the lake",
            listOf(
                LatLng(51.4410, 7.2550),
                LatLng(51.4480, 7.2540),
                LatLng(51.4500, 7.2620),
                LatLng(51.4490, 7.2720),
                LatLng(51.4420, 7.2730),
                LatLng(51.4400, 7.2650),
            ),
            Color(0xFF3949AB),
            holes = listOf(listOf(LatLng(51.4440, 7.2620), LatLng(51.4460, 7.2615), LatLng(51.4465, 7.2660), LatLng(51.4445, 7.2665))),
        ),
        // A geodesic line follows the shortest way on the globe, which is curved on the map
        Line("Flight to Reykjavik", "Geodesic: the shortest way on the globe", listOf(BOCHUM, REYKJAVIK), Color(0xFFD81B60), 6f, geodesic = true),
    )

// All shapes in Bochum, without the flight
private val OVERVIEW =
    LatLngBounds.builder().apply { ITEMS.dropLast(1).forEach { include(it.bounds.southwest).include(it.bounds.northeast) } }.build()

// The panel width plus its margins: the camera fits shapes into the area left of it
private val PANEL_WIDTH = 200.dp
private val PANEL_PADDING = PANEL_WIDTH + 32.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                PolylinesScreen()
            }
        }
    }
}

@Composable
fun PolylinesScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(BOCHUM, 12f) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    // The bottom of the status card: the camera fits shapes into the area below it
    var cardBottom by remember { mutableStateOf(0.dp) }
    var fittedOverview by rememberSaveable { mutableStateOf(false) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    fun select(index: Int) {
        selected = index
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(ITEMS[index].bounds, 96), 1000) }
    }

    fun showOverview() {
        selected = -1
        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(OVERVIEW, 64), 1000) }
    }

    // Once the card is measured, show all shapes in Bochum, without animation
    LaunchedEffect(cardBottom) {
        if (cardBottom > 0.dp && !fittedOverview) {
            // After the next frame, when the map has the new content padding
            withFrameNanos {}
            cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(OVERVIEW, 64))
            fittedOverview = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // Neither the status card nor the panel covers the selected shape
            contentPadding = PaddingValues(top = cardBottom, end = PANEL_PADDING),
        ) {
            // The dashed straight line shows how the geodesic flight is curved
            Polyline(
                points = listOf(BOCHUM, REYKJAVIK),
                strokeColor = Color(0xFF757575),
                strokeWidth = 4f,
                strokePattern = PathEffect.dashPathEffect(floatArrayOf(20f, 15f)),
            )
            ITEMS.forEachIndexed { index, item ->
                val highlighted = index == selected
                // A changed parameter updates the shape on the map, so it stands out right away
                when (item) {
                    is Line ->
                        Polyline(
                            points = item.points,
                            strokeColor = item.color,
                            strokeWidth = if (highlighted) item.width * 2 else item.width,
                            geodesic = item.geodesic,
                            clickable = true,
                            zIndex = if (highlighted) 1f else 0f,
                            onClick = { select(index) },
                        )

                    is Area ->
                        Polygon(
                            points = item.points,
                            holes = item.holes,
                            strokeColor = item.strokeColor,
                            strokeWidth = if (highlighted) 10f else 4f,
                            fillColor = item.strokeColor.copy(alpha = if (highlighted) 0.56f else 0.25f),
                            clickable = true,
                            onClick = { select(index) },
                        )
                }
            }
        }

        // The selected shape and its description
        Card(
            modifier =
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .onGloballyPositioned { cardBottom = with(density) { it.boundsInParent().bottom.toDp() } },
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (selected < 0) {
                    Text("Tap a line or an area, or Next", style = MaterialTheme.typography.titleMedium)
                } else {
                    Text("${selected + 1} of ${ITEMS.size}: ${ITEMS[selected].title}", style = MaterialTheme.typography.titleMedium)
                    Text(ITEMS[selected].snippet, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        // Selection buttons
        Column(
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(PANEL_WIDTH),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(onClick = { select((if (selected <= 0) ITEMS.size else selected) - 1) }, modifier = Modifier.fillMaxWidth()) {
                Text("Previous")
            }
            Button(onClick = { select((selected + 1) % ITEMS.size) }, modifier = Modifier.fillMaxWidth()) {
                Text("Next")
            }
            FilledTonalButton(onClick = { showOverview() }, modifier = Modifier.fillMaxWidth()) {
                Text("Overview")
            }
        }
    }
}
