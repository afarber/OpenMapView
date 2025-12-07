/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example03markers

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
 * A status overlay displaying current marker selection and camera state.
 *
 * Shows the currently selected marker index, name, and camera state.
 * The marker index text turns red when an info window is shown.
 *
 * @param totalCount Total number of markers.
 * @param selectedIndex The index of the currently selected marker.
 * @param selectedMarkerTitle Title of the currently selected marker, or null if none selected.
 * @param cameraState Current camera state description (e.g., "Idle", "Moving (gesture)").
 * @param isInfoWindowShown Whether an info window is currently shown.
 * @param modifier Modifier to be applied to the status overlay.
 */
@Composable
fun StatusToolbar(
    totalCount: Int,
    selectedIndex: Int,
    selectedMarkerTitle: String?,
    cameraState: String,
    isInfoWindowShown: Boolean,
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
            text = "Marker #${selectedIndex + 1} of $totalCount",
            color = if (isInfoWindowShown) Color.Red else Color.Black,
        )
        Text(
            text = selectedMarkerTitle ?: "None",
            color = Color.Black,
        )
        Text(
            text = "Camera: $cameraState",
            color = Color.Black,
        )
    }
}
