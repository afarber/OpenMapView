/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap

/**
 * Factory for creating marker icons.
 *
 * Provides predefined color constants and methods to generate marker icon descriptors
 * from various sources (default colors, resources, assets, bitmaps).
 * Colors are specified using HSV hue values (0-360 degrees on the color wheel).
 */
object BitmapDescriptorFactory {
    /**
     * Predefined HUE constants for common marker colors.
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
     * Creates a marker icon descriptor with the specified hue.
     *
     * @param hue The hue value (0-360) on the color wheel. Defaults to red (0).
     *            0=red, 120=green, 240=blue, etc.
     * @return A BitmapDescriptor for the default colored marker
     */
    fun defaultMarker(hue: Float = HUE_RED): BitmapDescriptor = BitmapDescriptor.DefaultMarker(hue)

    /**
     * Creates a marker icon descriptor from a drawable resource.
     *
     * @param resourceId The drawable resource ID (e.g., R.drawable.marker_icon)
     * @return A BitmapDescriptor for the resource marker
     */
    fun fromResource(resourceId: Int): BitmapDescriptor = BitmapDescriptor.ResourceMarker(resourceId)

    /**
     * Creates a marker icon descriptor from an asset file.
     *
     * @param assetName The name of the asset file (e.g., "markers/custom_marker.png")
     * @return A BitmapDescriptor for the asset marker
     */
    fun fromAsset(assetName: String): BitmapDescriptor = BitmapDescriptor.AssetMarker(assetName)

    /**
     * Creates a marker icon descriptor from a bitmap object.
     *
     * @param bitmap The bitmap to use as marker icon
     * @return A BitmapDescriptor for the bitmap marker
     */
    fun fromBitmap(bitmap: Bitmap): BitmapDescriptor = BitmapDescriptor.BitmapMarker(bitmap)
}
