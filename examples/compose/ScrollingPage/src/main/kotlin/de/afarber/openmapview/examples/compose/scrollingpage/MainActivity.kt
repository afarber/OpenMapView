/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.scrollingpage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.compose.MapUiSettings
import de.afarber.openmapview.compose.Marker
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.Polyline
import de.afarber.openmapview.compose.rememberCameraPositionState
import de.afarber.openmapview.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.util.Locale

private val PAGE_PADDING = 24.dp

private val MINING_MUSEUM = LatLng(51.4892, 7.2174)
private val PLANETARIUM = LatLng(51.4890, 7.2263)
private val MAIN_STATION = LatLng(51.4783, 7.2231)
private val RUHR_UNIVERSITY = LatLng(51.4452, 7.2622)

private const val INTRO =
    "Start at the German Mining Museum, one of the largest mining museums in the world. " +
        "Its headframe towers over the city center and a visitor mine lies below the building. " +
        "From there, walk south through the Stadtpark to the Main Station."
private const val CAMPUS =
    "In the afternoon, take the U35 subway from the Main Station to the Ruhr University campus. " +
        "Its concrete buildings line up along a central forum, and the Botanical Garden lies just south of it."
private const val EVENING =
    "End the day at the Zeiss Planetarium next to the Stadtpark, where the evening shows project " +
        "the night sky onto its dome."
private const val MORE =
    "The text goes on below the map, so the page scrolls past it in both directions. " +
        "Swipe over the map to pan it, and swipe over the text to scroll the page."

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    ScrollingPageScreen()
                }
            }
        }
    }
}

@Composable
fun ScrollingPageScreen() {
    // The page scrolls; the maps are just two of its children
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var interactiveTop by remember { mutableIntStateOf(0) }
    val scrollToInteractive: () -> Unit = { scope.launch { scrollState.animateScrollTo(interactiveTop) } }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(PAGE_PADDING)) {
        Text("A day in Bochum", style = MaterialTheme.typography.headlineMedium)

        // A static preview: with its gestures off the map never claims a drag, so the
        // page scrolls when a finger moves over it
        OpenMap(
            modifier = Modifier.fillMaxWidth().height(180.dp).padding(top = 16.dp),
            cameraPositionState = rememberCameraPositionState { position = CameraPosition(MINING_MUSEUM, 14f) },
            uiSettings = MapUiSettings(zoomGesturesEnabled = false, scrollGesturesEnabled = false),
            // Taps still arrive: a tap scrolls down to the interactive map
            onMapClick = { scrollToInteractive() },
        ) {
            Marker(
                state = rememberMarkerState(MINING_MUSEUM),
                title = "Mining Museum",
                onClick = {
                    scrollToInteractive()
                    true
                },
            )
        }
        Text(
            "A static map with all gestures off: swiping over it scrolls the page, a tap jumps to the interactive map",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
        Paragraph(INTRO)

        Text(
            "Explore the route",
            // The scroll offset counts from the page edge, outside the padding of the column
            modifier =
            Modifier.padding(top = 24.dp).onGloballyPositioned {
                interactiveTop = it.positionInParent().y.toInt() + with(density) { PAGE_PADDING.roundToPx() }
            },
            style = MaterialTheme.typography.titleLarge,
        )
        Text("Drag, pinch or use the buttons: the map moves and the page stays put", style = MaterialTheme.typography.bodyMedium)

        // The interactive map keeps its gestures: while a finger pans or pinches it, the
        // map stops the page from taking over the drag
        val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4680, 7.2400), 12.5f) }
        OpenMap(
            modifier = Modifier.fillMaxWidth().height(300.dp).padding(top = 8.dp),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(zoomControlsEnabled = true),
        ) {
            Place(MINING_MUSEUM, "Mining Museum", BitmapDescriptorFactory.HUE_RED)
            Place(PLANETARIUM, "Planetarium", BitmapDescriptorFactory.HUE_VIOLET)
            Place(MAIN_STATION, "Main Station", BitmapDescriptorFactory.HUE_BLUE)
            Place(RUHR_UNIVERSITY, "Ruhr University", BitmapDescriptorFactory.HUE_GREEN)
            Polyline(
                points = listOf(MINING_MUSEUM, PLANETARIUM, MAIN_STATION, RUHR_UNIVERSITY),
                strokeColor = Color(0xFFF4511E),
                strokeWidth = 8f,
            )
        }
        val camera = cameraPositionState.position
        Text(
            String.format(Locale.US, "Camera at %.4f, %.4f, zoom %.1f", camera.target.latitude, camera.target.longitude, camera.zoom),
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )

        Paragraph(CAMPUS)
        Paragraph(EVENING)
        Paragraph(MORE)
    }
}

@Composable
private fun Place(
    position: LatLng,
    title: String,
    hue: Float,
) {
    Marker(state = rememberMarkerState(position), title = title, icon = BitmapDescriptorFactory.defaultMarker(hue))
}

@Composable
private fun Paragraph(text: String) {
    Text(text, modifier = Modifier.padding(top = 16.dp), style = MaterialTheme.typography.bodyLarge)
}
