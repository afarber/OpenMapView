/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example03markers

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A horizontal toolbar with buttons to add, remove, and clear markers.
 *
 * Uses OSM-inspired colors for visual consistency:
 * - Add button: OsmParkGreen
 * - Remove button: OsmHighwayPink
 * - Clear button: OsmWaterBlue
 *
 * @param onAddClick Callback invoked when the add marker button is clicked.
 * @param onRemoveClick Callback invoked when the remove marker button is clicked.
 * @param onClearClick Callback invoked when the clear all markers button is clicked.
 * @param modifier Modifier to be applied to the toolbar.
 */
@Composable
fun MarkerToolbar(
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ToolbarCornerRadius),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row {
            FilledIconButton(
                onClick = onAddClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topStart = ToolbarCornerRadius, bottomStart = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmParkGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Marker")
            }
            FilledIconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(0.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmHighwayPink,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Remove Marker")
            }
            FilledIconButton(
                onClick = onClearClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topEnd = ToolbarCornerRadius, bottomEnd = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmWaterBlue,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear All Markers")
            }
        }
    }
}
