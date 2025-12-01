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
import androidx.compose.ui.unit.dp

@Composable
fun ControlToolbar(
    boundsEnabled: Boolean,
    onBoundsClick: () -> Unit,
    onResetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(ToolbarCornerRadius),
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column {
            FilledTonalButton(
                onClick = onBoundsClick,
                modifier = Modifier.size(width = 80.dp, height = 48.dp),
                shape = RoundedCornerShape(topStart = ToolbarCornerRadius, topEnd = ToolbarCornerRadius),
                contentPadding = PaddingValues(4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = OsmWaterBlue,
                    contentColor = Color.Black,
                ),
            ) {
                Text(
                    text = if (boundsEnabled) "Bounds On" else "Bounds Off",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FilledTonalButton(
                onClick = onResetClick,
                modifier = Modifier.size(width = 80.dp, height = 48.dp),
                shape = RoundedCornerShape(bottomStart = ToolbarCornerRadius, bottomEnd = ToolbarCornerRadius),
                contentPadding = PaddingValues(4.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = OsmWaterBlue,
                    contentColor = Color.Black,
                ),
            ) {
                Text("Reset", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
