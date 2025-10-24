/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sinh
import kotlin.math.tan

object Projection {
    private const val TILE_SIZE = 256

    /**
     * Converts GPS coordinates to tile coordinates at a given zoom level.
     */
    fun latLngToTile(latLng: LatLng, zoom: Int): TileCoordinate {
        val n = 2.0.pow(zoom)
        val xTile = ((latLng.longitude + 180.0) / 360.0 * n).toInt()
        val latRad = Math.toRadians(latLng.latitude)
        val yTile = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
        return TileCoordinate(xTile, yTile, zoom)
    }

    /**
     * Converts tile coordinates to pixel position (top-left corner).
     */
    fun tileToPixel(tile: TileCoordinate): Pair<Int, Int> {
        return Pair(tile.x * TILE_SIZE, tile.y * TILE_SIZE)
    }

    /**
     * Converts pixel coordinates to GPS coordinates at a given zoom level.
     */
    fun pixelToLatLng(x: Int, y: Int, zoom: Int): LatLng {
        val n = 2.0.pow(zoom)
        val xTile = x.toDouble() / TILE_SIZE
        val yTile = y.toDouble() / TILE_SIZE

        val lng = xTile / n * 360.0 - 180.0
        val latRad = atan(sinh(PI * (1.0 - 2.0 * yTile / n)))
        val lat = Math.toDegrees(latRad)

        return LatLng(lat, lng)
    }

    /**
     * Converts GPS coordinates to pixel position at a given zoom level.
     */
    fun latLngToPixel(latLng: LatLng, zoom: Int): Pair<Double, Double> {
        val n = 2.0.pow(zoom)
        val xPixel = (latLng.longitude + 180.0) / 360.0 * n * TILE_SIZE
        val latRad = Math.toRadians(latLng.latitude)
        val yPixel = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n * TILE_SIZE
        return Pair(xPixel, yPixel)
    }
}
