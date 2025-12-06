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
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
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
 * A horizontal toolbar with buttons to navigate between markers.
 *
 * Both buttons use OsmParkGreen for visual consistency with OSM styling.
 *
 * @param onPrevClick Callback invoked when the previous marker button is clicked.
 * @param onNextClick Callback invoked when the next marker button is clicked.
 * @param modifier Modifier to be applied to the toolbar.
 */
@Composable
fun MarkerToolbar(
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
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
                onClick = onPrevClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topStart = ToolbarCornerRadius, bottomStart = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmParkGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = "Previous Marker")
            }
            FilledIconButton(
                onClick = onNextClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topEnd = ToolbarCornerRadius, bottomEnd = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmParkGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = "Next Marker")
            }
        }
    }
}
