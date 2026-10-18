/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter

/**
 * Ready-made color filters for [OpenMapView.setTileColorFilter].
 *
 * ```kotlin
 * mapView.setTileColorFilter(TileColorFilters.invertedDark())
 * ```
 *
 * ```java
 * mapView.setTileColorFilter(TileColorFilters.invertedDark());
 * ```
 */
object TileColorFilters {
    private val invertedDark: ColorFilter by lazy {
        // Light colors become dark: white turns black, black turns white
        val matrix =
            rgbMatrix(
                floatArrayOf(-1f, 0f, 0f, 0f, 255f),
                floatArrayOf(0f, -1f, 0f, 0f, 255f),
                floatArrayOf(0f, 0f, -1f, 0f, 255f),
            )
        // Rotating the hue by 180 degrees brings the inverted hues back,
        // so water stays blue and parks stay green
        matrix.postConcat(
            rgbMatrix(
                floatArrayOf(-0.574f, 1.430f, 0.144f, 0f, 0f),
                floatArrayOf(0.426f, 0.430f, 0.144f, 0f, 0f),
                floatArrayOf(0.426f, 1.430f, -0.856f, 0f, 0f),
            ),
        )
        // A dark gray background and light gray labels are easier on the eyes than
        // black and white
        matrix.postConcat(
            rgbMatrix(
                floatArrayOf(0.8f, 0f, 0f, 0f, 20f),
                floatArrayOf(0f, 0.8f, 0f, 0f, 20f),
                floatArrayOf(0f, 0f, 0.8f, 0f, 20f),
            ),
        )
        ColorMatrixColorFilter(matrix)
    }

    // A color matrix from its red, green and blue rows, which keeps the alpha
    private fun rgbMatrix(
        red: FloatArray,
        green: FloatArray,
        blue: FloatArray,
    ) = ColorMatrix(red + green + blue + floatArrayOf(0f, 0f, 0f, 1f, 0f))

    /**
     * Turns the light tiles of [MapType.STANDARD] into a dark map, for a dark mode
     * without an API key.
     *
     * This is an approximation: the colors are inverted and their hues kept, so the
     * background is dark gray, the labels light and water still blue, but the map was not
     * designed for it. [MapType.CARTO_DARK] is a real dark map, but needs an API key.
     *
     * @return The same filter on every call
     */
    @JvmStatic
    fun invertedDark(): ColorFilter = invertedDark
}
