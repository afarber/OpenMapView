/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example01pan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.LatLng

/**
 * A vertical toolbar displaying buttons for navigating to preset locations.
 *
 * Each button is labeled "Loc 1", "Loc 2", etc. and uses OSM highway pink background.
 *
 * @param locations List of [LatLng] positions to display as buttons.
 * @param onLocationClick Callback invoked when a location button is clicked, receiving the [LatLng].
 * @param modifier Modifier to be applied to the toolbar.
 */
@Composable
fun LocationToolbar(
    locations: List<LatLng>,
    onLocationClick: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ToolbarCornerRadius),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            locations.forEachIndexed { index, location ->
                val shape = when {
                    locations.size == 1 -> RoundedCornerShape(ToolbarCornerRadius)
                    index == 0 -> RoundedCornerShape(topStart = ToolbarCornerRadius, topEnd = ToolbarCornerRadius)
                    index == locations.lastIndex -> RoundedCornerShape(bottomStart = ToolbarCornerRadius, bottomEnd = ToolbarCornerRadius)
                    else -> RectangleShape
                }
                FilledTonalButton(
                    onClick = { onLocationClick(location) },
                    modifier = Modifier.size(width = 72.dp, height = 48.dp),
                    shape = shape,
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = OsmHighwayPink,
                        contentColor = Color.Black,
                    ),
                ) {
                    Text("Loc ${index + 1}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
