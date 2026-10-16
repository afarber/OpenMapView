/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.GroundOverlay as MapGroundOverlay

/**
 * An image placed on an [OpenMap], declared in its content.
 *
 * Place it either by [bounds], or by [position] with [width] (and optionally [height]).
 * The overlay is added to the map when it enters the composition and removed when it
 * leaves. Changing a parameter updates it on the map. Outside of [OpenMap]'s content it does nothing.
 *
 * Example:
 * ```kotlin
 * OpenMap {
 *     GroundOverlay(
 *         image = BitmapDescriptorFactory.fromResource(R.drawable.floor_plan),
 *         bounds = LatLngBounds(LatLng(51.46, 7.24), LatLng(51.47, 7.26)),
 *         transparency = 0.3f,
 *     )
 * }
 * ```
 *
 * @param image The image to show
 * @param position Where the [anchor] of the image is placed, used with [width]
 * @param width Width of the image in meters, used with [position]
 * @param height Height in meters, or null to keep the image's aspect ratio
 * @param bounds The area the image covers, instead of [position] and [width]
 * @param anchor Point of the image placed at [position], as fractions of its size
 * @param bearing Clockwise rotation in degrees
 * @param transparency From 0 (opaque) to 1 (invisible)
 * @param visible Whether the overlay is drawn
 * @param zIndex Drawing order relative to other overlays
 * @param clickable Whether taps on the overlay call [onClick]
 * @param tag Any object to associate with the overlay
 * @param onClick Called when a clickable overlay is tapped
 */
@Composable
fun GroundOverlay(
    image: BitmapDescriptor,
    position: LatLng? = null,
    width: Float? = null,
    height: Float? = null,
    bounds: LatLngBounds? = null,
    anchor: Pair<Float, Float> = Pair(0.5f, 0.5f),
    bearing: Float = 0f,
    transparency: Float = 0f,
    visible: Boolean = true,
    zIndex: Float = 0f,
    clickable: Boolean = false,
    tag: Any? = null,
    onClick: (MapGroundOverlay) -> Unit = {},
) {
    // A different anchor needs a new overlay; other changes update it in place
    key(anchor) {
        LiveMapElement(
            create = {
                MapGroundOverlay(
                    image = image,
                    position = position,
                    width = width,
                    height = height,
                    bounds = bounds,
                    anchor = anchor,
                    bearing = bearing,
                    transparency = transparency,
                    visible = visible,
                    zIndex = zIndex,
                    clickable = clickable,
                    tag = tag,
                )
            },
            update = {
                this.image = image
                // The placement changes only when the parameters describe a new one
                if (bounds != null) {
                    if (bounds != this.bounds) setPositionFromBounds(bounds)
                } else if (position != null && width != null) {
                    if (position != this.position || width != this.width || height != this.height) {
                        setDimensions(width, height)
                        setPosition(position)
                    }
                }
                this.bearing = bearing
                this.transparency = transparency
                this.visible = visible
                this.zIndex = zIndex
                this.clickable = clickable
                this.tag = tag
            },
            add = { addGroundOverlay(it) },
            remove = { removeGroundOverlay(it) },
            onClick = onClick,
        )
    }
}
