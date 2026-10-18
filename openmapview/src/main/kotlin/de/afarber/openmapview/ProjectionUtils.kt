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

internal object ProjectionUtils {
    /** The width of a tile in pixels of the tile image, and of the world at zoom 0. */
    const val TILE_SIZE = 256.0

    /**
     * Converts GPS coordinates to tile coordinates at a given zoom level.
     */
    fun latLngToTile(
        latLng: LatLng,
        zoom: Int,
    ): TileCoordinate {
        val n = 2.0.pow(zoom)
        val xTile = ((latLng.longitude + 180.0) / 360.0 * n).toInt()
        val latRad = latLng.latitude * PI / 180.0
        val yTile = ((1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * n).toInt()
        return TileCoordinate(xTile, yTile, zoom)
    }

    /**
     * Converts tile coordinates to pixel position (top-left corner).
     */
    fun tileToPixel(tile: TileCoordinate): Pair<Int, Int> = Pair(tile.x * TILE_SIZE.toInt(), tile.y * TILE_SIZE.toInt())

    /**
     * Converts pixel coordinates to GPS coordinates at a given zoom level.
     */
    fun pixelToLatLng(
        x: Int,
        y: Int,
        zoom: Int,
    ): LatLng = pixelToLatLng(x.toDouble(), y.toDouble(), zoom.toFloat())

    /**
     * Converts fractional pixel coordinates to GPS coordinates at a given zoom level.
     */
    fun pixelToLatLng(
        x: Double,
        y: Double,
        zoom: Int,
    ): LatLng = pixelToLatLng(x, y, zoom.toFloat())

    /**
     * Converts pixel coordinates to GPS coordinates at a fractional zoom level, where the
     * world is tileSize * 2^zoom pixels wide.
     */
    fun pixelToLatLng(
        x: Double,
        y: Double,
        zoom: Float,
        tileSize: Double = TILE_SIZE,
    ): LatLng {
        val worldSize = worldSize(zoom, tileSize)
        val lng = x / worldSize * 360.0 - 180.0
        val latRad = atan(sinh(PI * (1.0 - 2.0 * y / worldSize)))
        val lat = latRad * 180.0 / PI

        return LatLng(lat, lng)
    }

    /**
     * Converts GPS coordinates to pixel position at a given zoom level.
     */
    fun latLngToPixel(
        latLng: LatLng,
        zoom: Int,
    ): Pair<Float, Float> = latLngToPixel(latLng, zoom.toFloat())

    /**
     * Converts GPS coordinates to pixel position at a fractional zoom level, where the
     * world is tileSize * 2^zoom pixels wide.
     */
    fun latLngToPixel(
        latLng: LatLng,
        zoom: Float,
        tileSize: Double = TILE_SIZE,
    ): Pair<Float, Float> {
        val (xPixel, yPixel) = latLngToPixelExact(latLng, zoom, tileSize)
        return Pair(xPixel.toFloat(), yPixel.toFloat())
    }

    /**
     * Like [latLngToPixel], but with double precision, e.g. for converting a location back
     * and forth without drift.
     */
    fun latLngToPixelExact(
        latLng: LatLng,
        zoom: Float,
        tileSize: Double = TILE_SIZE,
    ): Pair<Double, Double> {
        val worldSize = worldSize(zoom, tileSize)
        val xPixel = (latLng.longitude + 180.0) / 360.0 * worldSize
        val latRad = latLng.latitude * PI / 180.0
        val yPixel = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * worldSize
        return Pair(xPixel, yPixel)
    }

    /**
     * The width and height of the whole world in pixels at a fractional zoom level, where
     * a tile at a whole zoom level is [tileSize] pixels wide.
     */
    fun worldSize(
        zoom: Float,
        tileSize: Double = TILE_SIZE,
    ): Double = tileSize * 2.0.pow(zoom.toDouble())

    /**
     * Calculates meters per pixel at a given latitude and zoom level.
     * Uses Web Mercator projection where scale varies with latitude.
     *
     * @param latitude The latitude in degrees
     * @param zoom The zoom level
     * @return Meters per pixel at this latitude and zoom
     */
    fun metersPerPixelAtLatitude(
        latitude: Double,
        zoom: Int,
    ): Double = metersPerPixelAtLatitude(latitude, zoom.toFloat())

    /**
     * Calculates meters per pixel at a given latitude and fractional zoom level.
     */
    fun metersPerPixelAtLatitude(
        latitude: Double,
        zoom: Float,
        tileSize: Double = TILE_SIZE,
    ): Double {
        val earthCircumference = 40075017.0
        val latitudeRadians = latitude * PI / 180.0
        return earthCircumference * cos(latitudeRadians) / worldSize(zoom, tileSize)
    }
}
