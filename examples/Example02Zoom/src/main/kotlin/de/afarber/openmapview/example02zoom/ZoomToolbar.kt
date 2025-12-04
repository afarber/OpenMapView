/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example02zoom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
 * A horizontal toolbar with zoom in (+) and zoom out (-) buttons.
 *
 * Uses OSM park green background color for visual consistency.
 *
 * @param onZoomInClick Callback invoked when the zoom in button is clicked.
 * @param onZoomOutClick Callback invoked when the zoom out button is clicked.
 * @param modifier Modifier to be applied to the toolbar.
 */
@Composable
fun ZoomToolbar(
    onZoomInClick: () -> Unit,
    onZoomOutClick: () -> Unit,
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
                onClick = onZoomInClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topStart = ToolbarCornerRadius, bottomStart = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmWaterBlue,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In")
            }
            FilledIconButton(
                onClick = onZoomOutClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topEnd = ToolbarCornerRadius, bottomEnd = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmWaterBlue,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out")
            }
        }
    }
}
