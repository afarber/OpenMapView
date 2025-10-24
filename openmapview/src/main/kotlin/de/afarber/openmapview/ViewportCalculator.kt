/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ViewportCalculator {
    private const val TILE_SIZE = 256

    /**
     * Calculates which tiles are visible in the current viewport.
     * Includes a 1-tile buffer around the edges for smooth panning.
     */
    fun getVisibleTiles(
        center: LatLng,
        zoom: Int,
        viewWidth: Int,
        viewHeight: Int,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
    ): List<TileCoordinate> {
        if (viewWidth <= 0 || viewHeight <= 0) return emptyList()

        // Convert center to pixel coordinates
        val (centerX, centerY) = Projection.latLngToPixel(center, zoom)

        // Calculate viewport bounds in pixel space (with pan offset)
        val left = (centerX - viewWidth / 2 + panOffsetX).toInt()
        val top = (centerY - viewHeight / 2 + panOffsetY).toInt()
        val right = (centerX + viewWidth / 2 + panOffsetX).toInt()
        val bottom = (centerY + viewHeight / 2 + panOffsetY).toInt()

        // Convert to tile coordinates
        val minTileX = max(0, left / TILE_SIZE - 1)
        val minTileY = max(0, top / TILE_SIZE - 1)
        val maxTileX = min((2.0.pow(zoom) - 1).toInt(), right / TILE_SIZE + 1)
        val maxTileY = min((2.0.pow(zoom) - 1).toInt(), bottom / TILE_SIZE + 1)

        // Build list of all visible tiles
        val tiles = mutableListOf<TileCoordinate>()
        for (x in minTileX..maxTileX) {
            for (y in minTileY..maxTileY) {
                tiles.add(TileCoordinate(x, y, zoom))
            }
        }

        return tiles
    }
}
