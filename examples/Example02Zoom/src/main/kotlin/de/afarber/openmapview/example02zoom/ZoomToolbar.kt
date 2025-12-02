/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example02zoom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A vertical toolbar with zoom in (+) and zoom out (-) buttons.
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
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(ToolbarCornerRadius))
            .background(OsmParkGreen),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Zoom In button
        Text(
            text = "+",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onZoomInClick)
                .padding(8.dp),
        )

        // Zoom Out button
        Text(
            text = "-",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .size(56.dp)
                .clickable(onClick = onZoomOutClick)
                .padding(8.dp),
        )
    }
}
