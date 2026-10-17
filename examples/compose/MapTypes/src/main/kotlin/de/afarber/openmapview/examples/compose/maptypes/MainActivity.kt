/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.examples.compose.maptypes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.MapType
import de.afarber.openmapview.TileSource
import de.afarber.openmapview.compose.MapProperties
import de.afarber.openmapview.compose.OpenMap
import de.afarber.openmapview.compose.rememberCameraPositionState

private val MAP_TYPES =
    listOf(
        MapType.STANDARD, MapType.HUMANITARIAN, MapType.CYCLOSM, MapType.OPENTOPOMAP, MapType.OPNVKARTE,
        MapType.CARTO_LIGHT, MapType.CARTO_DARK, MapType.CARTO_VOYAGER,
        MapType.CYCLEMAP, MapType.TRANSPORT, MapType.TRANSPORT_DARK, MapType.TRACESTRACK_TOPO,
        MapType.STAMEN_TONER, MapType.STAMEN_WATERCOLOR, MapType.NONE,
    )

// The list width plus its margins: the camera center and the API key notice stay clear of it
private val LIST_WIDTH = 280.dp
private val LIST_PADDING = LIST_WIDTH + 32.dp

private fun needsKey(mapType: Int) = mapType != MapType.NONE && TileSource.fromMapType(mapType).requiresApiKey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MapTypesScreen()
            }
        }
    }
}

@Composable
fun MapTypesScreen() {
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition(LatLng(51.4720, 7.2300), 13f) }
    var mapType by rememberSaveable { mutableIntStateOf(MapType.STANDARD) }

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            // Each map type keeps its own tile cache, so switching back shows its tiles right
            // away. The attribution in the corner changes with the map type.
            properties = MapProperties(mapType = mapType),
            contentPadding = PaddingValues(end = LIST_PADDING),
        )

        // The selected map type and whether it needs an API key
        Card(modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(TileSource.getMapTypeName(mapType), style = MaterialTheme.typography.titleMedium)
                val source = TileSource.fromMapType(mapType)
                // Keys come from the app manifest or ApiKeyManager.setApiKey()
                val key =
                    when {
                        !needsKey(mapType) -> "No API key needed"
                        source.hasApiKey() -> "Uses your ${source.getProviderName()} API key"
                        else -> "Needs a ${source.getProviderName()} API key: shows the standard map with a notice"
                    }
                Text(key, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // All map types
        Card(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp).width(LIST_WIDTH).fillMaxHeight()) {
            LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                items(MAP_TYPES) { type ->
                    Row(
                        modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = type == mapType, onClick = { mapType = type }, role = Role.RadioButton)
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = type == mapType, onClick = null)
                        val name = TileSource.getMapTypeName(type)
                        Text(
                            if (needsKey(type)) "$name (needs a key)" else name,
                            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }
        }
    }
}
