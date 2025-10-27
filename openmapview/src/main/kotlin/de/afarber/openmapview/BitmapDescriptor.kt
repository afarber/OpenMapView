/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap

/**
 * Represents a bitmap image used for marker icons.
 *
 * This sealed class provides different ways to specify marker icons:
 * - [DefaultMarker]: Default colored teardrop marker
 * - [BitmapMarker]: Custom bitmap object
 * - [ResourceMarker]: Bitmap from drawable resource
 * - [AssetMarker]: Bitmap from assets folder
 */
sealed class BitmapDescriptor {
    /**
     * Default teardrop marker with specified hue color.
     *
     * @property hue The hue value (0-360) on the HSV color wheel
     */
    data class DefaultMarker(
        val hue: Float,
    ) : BitmapDescriptor()

    /**
     * Custom marker from a bitmap object.
     *
     * @property bitmap The bitmap to use as marker icon
     */
    data class BitmapMarker(
        val bitmap: Bitmap,
    ) : BitmapDescriptor()

    /**
     * Marker from a drawable resource ID.
     *
     * @property resourceId The drawable resource ID (e.g., R.drawable.marker_icon)
     */
    data class ResourceMarker(
        val resourceId: Int,
    ) : BitmapDescriptor()

    /**
     * Marker from an asset file.
     *
     * @property assetName The name of the asset file (e.g., "markers/custom_marker.png")
     */
    data class AssetMarker(
        val assetName: String,
    ) : BitmapDescriptor()
}
