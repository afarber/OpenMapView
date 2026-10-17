/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.math.pow

/**
 * Returns the camera at [progress] (0 to 1) of an animation from [start] to [end].
 *
 * The zoom changes linearly. The target moves on the Web Mercator map so that the screen
 * moves smoothly: when zooming in, the end target slides toward the view center at constant
 * screen speed, instead of first leaving the screen at the higher zoom levels and jumping
 * back at the end. When zooming out, the start target slides away from the center the
 * same way. Without a zoom change, the target moves at constant speed.
 */
internal fun interpolateCamera(
    start: CameraPosition,
    end: CameraPosition,
    progress: Float,
): CameraPosition {
    val zoom = start.zoom + (end.zoom - start.zoom) * progress
    // The share of the way from the start to the end target, on the map at zoom 0
    val share =
        when {
            end.zoom > start.zoom -> 1.0 - (1.0 - progress) * 2.0.pow((start.zoom - zoom).toDouble())
            end.zoom < start.zoom -> progress * 2.0.pow((end.zoom - zoom).toDouble())
            else -> progress.toDouble()
        }
    val (startX, startY) = ProjectionUtils.latLngToPixelExact(start.target, 0f)
    val (endX, endY) = ProjectionUtils.latLngToPixelExact(end.target, 0f)
    val target = ProjectionUtils.pixelToLatLng(startX + (endX - startX) * share, startY + (endY - startY) * share, 0f)
    return CameraPosition(target, zoom)
}
