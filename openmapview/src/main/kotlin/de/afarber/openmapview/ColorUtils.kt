/*
 * Copyright (c) 2025-2026 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

/**
 * Converts a CSS color string to a Compose Color.
 *
 * Supported formats:
 * - Hex colors: "#RGB", "#ARGB", "#RRGGBB", "#AARRGGBB"
 * - Named colors: "red", "blue", "green", etc.
 *
 * @return Compose Color, or null if the string cannot be parsed
 *
 * Example usage:
 * ```
 * val color1 = "#FF0000".toComposeColor() // Red
 * val color2 = "blue".toComposeColor()    // Blue
 * val color3 = "#80FF0000".toComposeColor() // Semi-transparent red
 * ```
 */
fun String.toComposeColor(): Color? =
    try {
        val androidColor = this.toColorInt()
        Color(androidColor)
    } catch (e: IllegalArgumentException) {
        null
    }
