/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example01pan

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.KeyboardDoubleArrowUp
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp

/**
 * A horizontal toolbar with four arrow buttons for panning the map.
 *
 * Displays left, up, down, and right arrow buttons in a row with OSM park green background.
 *
 * @param onLeftClick Callback invoked when the left arrow button is clicked.
 * @param onUpClick Callback invoked when the up arrow button is clicked.
 * @param onDownClick Callback invoked when the down arrow button is clicked.
 * @param onRightClick Callback invoked when the right arrow button is clicked.
 * @param modifier Modifier to be applied to the toolbar.
 */
@Composable
fun ArrowToolbar(
    onLeftClick: () -> Unit,
    onUpClick: () -> Unit,
    onDownClick: () -> Unit,
    onRightClick: () -> Unit,
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
                onClick = onLeftClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topStart = ToolbarCornerRadius, bottomStart = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmParkGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowLeft, contentDescription = "Left")
            }
            FilledIconButton(
                onClick = onUpClick,
                modifier = Modifier.size(56.dp),
                shape = RectangleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmParkGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowUp, contentDescription = "Up")
            }
            FilledIconButton(
                onClick = onDownClick,
                modifier = Modifier.size(56.dp),
                shape = RectangleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmParkGreen,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowDown, contentDescription = "Down")
            }
            FilledIconButton(
                onClick = onRightClick,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(topEnd = ToolbarCornerRadius, bottomEnd = ToolbarCornerRadius),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = OsmHighwayPink,
                    contentColor = Color.Black,
                ),
            ) {
                Icon(Icons.Default.KeyboardDoubleArrowRight, contentDescription = "Right")
            }
        }
    }
}
