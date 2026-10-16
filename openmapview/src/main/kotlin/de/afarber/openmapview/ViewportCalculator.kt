/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Utility for calculating which map tiles are visible in the viewport.
 *
 * Determines tile coordinates based on map center, zoom level, and view dimensions.
 * Includes buffering for smooth panning experience.
 */
object ViewportCalculator {
    private const val TILE_SIZE = 256

    /**
     * The zoom level of the tiles drawn at a fractional zoom: the nearest whole level, so
     * the tiles are scaled by a factor between 0.71 and 1.41.
     */
    internal fun tileZoom(zoom: Float): Int = zoom.roundToInt().coerceAtLeast(0)

    /**
     * Calculates which tiles are visible at a fractional zoom level.
     *
     * The tiles are of the zoom level [tileZoom] and drawn scaled to the fractional zoom.
     *
     * @param center The map center location
     * @param zoom The current, possibly fractional zoom level
     * @param viewWidth The viewport width in pixels
     * @param viewHeight The viewport height in pixels
     * @param panOffsetX The horizontal pan offset in screen pixels (default 0)
     * @param panOffsetY The vertical pan offset in screen pixels (default 0)
     * @return A list of tile coordinates that should be rendered
     */
    fun getVisibleTiles(
        center: LatLng,
        zoom: Float,
        viewWidth: Int,
        viewHeight: Int,
        panOffsetX: Float = 0f,
        panOffsetY: Float = 0f,
    ): List<TileCoordinate> {
        val tileZoom = tileZoom(zoom)
        // Screen pixels per pixel of the tile zoom level
        val scale = 2.0.pow((zoom - tileZoom).toDouble()).toFloat()
        return getVisibleTiles(
            center,
            tileZoom,
            ceil(viewWidth / scale).toInt(),
            ceil(viewHeight / scale).toInt(),
            panOffsetX / scale,
            panOffsetY / scale,
        )
    }

    /**
     * Calculates which tiles are visible in the current viewport.
     *
     * Includes a 1-tile buffer around the edges for smooth panning.
     *
     * @param center The map center location
     * @param zoom The current zoom level
     * @param viewWidth The viewport width in pixels
     * @param viewHeight The viewport height in pixels
     * @param panOffsetX The horizontal pan offset in pixels (default 0)
     * @param panOffsetY The vertical pan offset in pixels (default 0)
     * @return A list of tile coordinates that should be rendered
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
        val (centerX, centerY) = ProjectionUtils.latLngToPixel(center, zoom)

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
