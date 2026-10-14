/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example13compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Marker
import de.afarber.openmapview.compose.CameraPositionState
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val BOCHUM = LatLng(51.4661, 7.2491)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TabsScreen()
            }
        }
    }
}

@Composable
fun TabsScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Hoisted above the tabs: the map is created again on every visit of the map tab,
    // but keeps its camera
    val cameraPositionState =
        rememberCameraPositionState {
            position = CameraPosition(BOCHUM, 14f)
        }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Place, contentDescription = null) },
                    label = { Text("Map") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("Camera") },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> MapTab(cameraPositionState)
                else -> CameraTab(cameraPositionState) { selectedTab = 0 }
            }
        }
    }
}

@Composable
fun MapTab(cameraPositionState: CameraPositionState) {
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // No lifecycle code and no clipping needed: the map cleans up when the tab is left
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapCreated = { map ->
                map.addMarker(Marker(position = BOCHUM, title = "Bochum"))
            },
        )
        Button(
            onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } },
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
        ) {
            Text("Zoom in")
        }
    }
}

@Composable
fun CameraTab(
    cameraPositionState: CameraPositionState,
    showMap: () -> Unit,
) {
    val position = cameraPositionState.position

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Camera kept while the map tab is not shown:")
        Text(
            "%.4f, %.4f at zoom %.1f".format(position.target.latitude, position.target.longitude, position.zoom),
            style = MaterialTheme.typography.titleMedium,
        )
        Button(
            onClick = {
                cameraPositionState.position = CameraPosition(BOCHUM, 14f)
                showMap()
            },
        ) {
            Text("Show Bochum")
        }
    }
}
