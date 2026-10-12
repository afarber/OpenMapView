/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example01pan

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
 * A status overlay displaying current map state information.
 *
 * Shows the camera state (Idle/Moving), center position coordinates, and bounds status.
 *
 * @param cameraState Current camera state description (e.g., "Idle", "Moving (gesture)").
 * @param centerPosition Formatted string of the current map center coordinates.
 * @param boundsEnabled Whether camera bounds constraint is currently enabled.
 * @param modifier Modifier to be applied to the status overlay.
 */
@Composable
fun StatusToolbar(
    cameraState: String,
    centerPosition: String,
    boundsEnabled: Boolean,
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
            text = "Camera: $cameraState",
            color = Color.Black,
        )
        Text(
            text = "Center: $centerPosition",
            color = Color.Black,
        )
        Text(
            text = "Bounds: ${if (boundsEnabled) "On" else "Off"}",
            color = if (boundsEnabled) {
                Color.Black
            } else {
                Color.Red
            },
        )
    }
}
