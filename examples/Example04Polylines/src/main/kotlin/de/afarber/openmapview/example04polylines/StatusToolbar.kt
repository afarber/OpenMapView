/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example04polylines

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A status overlay displaying current overlay selection and camera state.
 *
 * Shows the currently selected overlay index, title, type, and camera state.
 * The index text turns red when the overlay is highlighted.
 *
 * @param totalCount Total number of overlays.
 * @param selectedIndex The index of the currently selected overlay.
 * @param overlayTitle Title of the currently selected overlay.
 * @param overlayType Type of overlay ("Polyline" or "Polygon").
 * @param cameraState Current camera state description (e.g., "Idle", "Moving (gesture)").
 * @param isHighlighted Whether the overlay is currently highlighted.
 * @param modifier Modifier to be applied to the status overlay.
 */
@Composable
fun StatusToolbar(
    totalCount: Int,
    selectedIndex: Int,
    overlayTitle: String,
    overlayType: String,
    cameraState: String,
    isHighlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(ToolbarCornerRadius))
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Overlay #${selectedIndex + 1} of $totalCount",
            color = if (isHighlighted) Color.Red else Color.Black,
        )
        Text(
            text = overlayTitle,
            color = Color.Black,
        )
        Text(
            text = overlayType,
            color = Color.Gray,
        )
        Text(
            text = "Camera: $cameraState",
            color = Color.Black,
        )
    }
}
