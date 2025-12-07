/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color

/**
 * Defines a style span for a portion of a polyline.
 *
 * Style spans allow different segments of a polyline to have different colors,
 * enabling multi-colored polylines (e.g., traffic conditions, elevation gradients).
 *
 * @property color The color for this span's segments.
 * @property segments The number of consecutive segments this style applies to. Default is 1.
 */
data class StyleSpan(
    val color: Color,
    val segments: Int = 1,
) {
    init {
        require(segments >= 1) { "StyleSpan must cover at least 1 segment" }
    }
}
