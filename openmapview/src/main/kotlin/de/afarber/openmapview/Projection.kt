/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Point
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A projection is used to translate between on-screen location and geographic coordinates.
 *
 * Screen locations are in screen pixels (not display pixels) with respect to the top left
 * corner of the map (not necessarily of the whole screen).
 *
 * This class provides methods for converting between screen coordinates and geographic coordinates,
 * as well as querying the visible region of the map.
 *
 * Screen locations are where the map draws a location, also when map padding is set. Only the
 * visible region is limited to the area inside the padding.
 *
 * The world repeats horizontally. A location is on the copy of the world nearest to the view
 * center, as the map draws it, and a screen location on any copy gives a longitude between
 * -180 and 180.
 */
class Projection
    internal constructor(
        private val center: LatLng,
        private val zoom: Float,
        private val viewWidth: Int,
        private val viewHeight: Int,
        private val panOffsetX: Float,
        private val panOffsetY: Float,
        private val paddingLeft: Int = 0,
        private val paddingTop: Int = 0,
        private val paddingRight: Int = 0,
        private val paddingBottom: Int = 0,
        private val tileSize: Double = ProjectionUtils.TILE_SIZE,
        private val bearing: Float = 0f,
    ) {
        // The map turns by -bearing around the camera target at the center of the padded area
        private val pivotX = viewWidth / 2f + (paddingLeft - paddingRight) / 2f
        private val pivotY = viewHeight / 2f + (paddingTop - paddingBottom) / 2f
        private val radians = Math.toRadians(bearing.toDouble())
        private val cos = cos(radians).toFloat()
        private val sin = sin(radians).toFloat()

        // Turns a point around the pivot by the bearing (to map space) or back (to the screen)
        private fun turned(
            x: Float,
            y: Float,
            toMapSpace: Boolean,
        ): Pair<Float, Float> {
            if (bearing == 0f) return x to y
            val s = if (toMapSpace) sin else -sin
            val dx = x - pivotX
            val dy = y - pivotY
            return (dx * cos - dy * s + pivotX) to (dx * s + dy * cos + pivotY)
        }

        /**
         * Returns the geographic location that corresponds to a screen location.
         *
         * The screen location is specified in screen pixels (not display pixels)
         * relative to the top left of the map.
         *
         * @param point The screen location in pixels
         * @return The geographic location (LatLng) corresponding to the screen point
         */
        fun fromScreenLocation(point: Point): LatLng = fromScreenLocation(point.x.toFloat(), point.y.toFloat())

        /**
         * Returns the geographic location that corresponds to a screen location.
         *
         * Internal helper that takes float coordinates.
         *
         * @param screenX The X coordinate in screen pixels
         * @param screenY The Y coordinate in screen pixels
         * @return The geographic location (LatLng) corresponding to the screen position
         */
        private fun fromScreenLocation(
            screenX: Float,
            screenY: Float,
        ): LatLng {
            val (mapX, mapY) = turned(screenX, screenY, toMapSpace = true)
            return mapSpaceToLatLng(mapX, mapY)
        }

        // The location at a point of the map laid out with north up
        private fun mapSpaceToLatLng(
            mapX: Float,
            mapY: Float,
        ): LatLng {
            val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)
            val pixelX = (centerPixelX + mapX - viewWidth / 2 + panOffsetX).toDouble()
            val pixelY = (centerPixelY + mapY - viewHeight / 2 + panOffsetY).toDouble()
            return ProjectionUtils.pixelToLatLng(pixelX, pixelY, zoom, tileSize)
        }

        /**
         * Returns a screen location that corresponds to a geographic coordinate.
         *
         * The screen location is specified in screen pixels (not display pixels)
         * relative to the top left of the map.
         *
         * @param location The geographic location (LatLng)
         * @return The screen location in pixels, on the copy of the world nearest to the view center
         */
        fun toScreenLocation(location: LatLng): Point {
            val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)
            val (locationPixelX, locationPixelY) = ProjectionUtils.latLngToPixel(location, zoom, tileSize)

            val x = locationPixelX - centerPixelX + viewWidth / 2 - panOffsetX
            val world = ProjectionUtils.worldSize(zoom, tileSize).toFloat()
            val mapX = x - world * ((x - viewWidth / 2f) / world).roundToInt()
            val mapY = locationPixelY - centerPixelY + viewHeight / 2 - panOffsetY
            val (screenX, screenY) = turned(mapX, mapY, toMapSpace = false)

            return Point(screenX.toInt(), screenY.toInt())
        }

        /**
         * Returns the visible region of the map.
         *
         * Gets a projection of the viewing frustum for converting between screen coordinates
         * and geo-latitude/longitude coordinates. The visible region includes the four corner
         * points and the smallest bounding box that contains them.
         *
         * If map padding is set, the visible region represents the logical (non-padded) viewport,
         * excluding the padded areas where UI elements may be overlaying the map.
         *
         * The bounding box crosses the antimeridian (southwest longitude greater than northeast
         * longitude) when the view does, and spans all longitudes when the view is at least as
         * wide as the world.
         *
         * @return A VisibleRegion containing the four corners and bounding box
         */
        fun getVisibleRegion(): VisibleRegion {
            // Calculate the logical (padded) viewport bounds
            val left = paddingLeft.toFloat()
            val top = paddingTop.toFloat()
            val right = (viewWidth - paddingRight).toFloat()
            val bottom = (viewHeight - paddingBottom).toFloat()

            val nearLeft = fromScreenLocation(left, bottom)
            val nearRight = fromScreenLocation(right, bottom)
            val farLeft = fromScreenLocation(left, top)
            val farRight = fromScreenLocation(right, top)

            // The bounding box of the corners on the map laid out with north up: for a
            // turned map the corners are not the box's corners. From the west to the east
            // edge, which may be on another copy of the world.
            val corners =
                listOf(left to bottom, right to bottom, left to top, right to top).map { (x, y) ->
                    turned(x, y, toMapSpace = true)
                }
            val west = corners.minOf { it.first }
            val east = corners.maxOf { it.first }
            val north = corners.minOf { it.second }
            val south = corners.maxOf { it.second }
            val wholeWorld = east - west >= ProjectionUtils.worldSize(zoom, tileSize)
            val southwest = mapSpaceToLatLng(west, south)
            val northeast = mapSpaceToLatLng(east, north)
            val bounds =
                LatLngBounds(
                    southwest = LatLng(southwest.latitude, if (wholeWorld) -180.0 else southwest.longitude),
                    northeast = LatLng(northeast.latitude, if (wholeWorld) 180.0 else northeast.longitude),
                )

            return VisibleRegion(
                nearLeft = nearLeft,
                nearRight = nearRight,
                farLeft = farLeft,
                farRight = farRight,
                latLngBounds = bounds,
            )
        }
    }
