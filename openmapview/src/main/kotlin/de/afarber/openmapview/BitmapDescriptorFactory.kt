/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap

/**
 * Factory for creating marker icons, compatible with Google Maps API.
 *
 * Provides predefined color constants and methods to generate colored marker icons.
 * Colors are specified using HSV hue values (0-360 degrees on the color wheel).
 */
object BitmapDescriptorFactory {
    /**
     * HUE constants matching Google Maps BitmapDescriptorFactory.
     * Values represent degrees on the HSV color wheel.
     */
    const val HUE_RED = 0f
    const val HUE_ORANGE = 30f
    const val HUE_YELLOW = 60f
    const val HUE_GREEN = 120f
    const val HUE_CYAN = 180f
    const val HUE_AZURE = 210f
    const val HUE_BLUE = 240f
    const val HUE_VIOLET = 270f
    const val HUE_MAGENTA = 300f
    const val HUE_ROSE = 330f

    /**
     * Creates a marker icon with the specified hue.
     *
     * @param hue The hue value (0-360) on the color wheel. Defaults to red (0).
     *            0=red, 120=green, 240=blue, etc.
     * @return A bitmap of the colored marker icon
     */
    fun defaultMarker(hue: Float = HUE_RED): Bitmap = MarkerIconFactory.getDefaultIcon(hue)
}
