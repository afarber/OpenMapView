/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Point

/**
 * A projection is used to translate between on-screen location and geographic coordinates.
 *
 * Screen locations are in screen pixels (not display pixels) with respect to the top left
 * corner of the map (not necessarily of the whole screen).
 *
 * This class provides methods for converting between screen coordinates and geographic coordinates,
 * as well as querying the visible region of the map.
 *
 * When map padding is set, the projection accounts for the logical viewport shift, ensuring that
 * coordinate conversions reflect the visible (non-padded) area of the map.
 */
class Projection
    internal constructor(
        private val center: LatLng,
        private val zoom: Double,
        private val viewWidth: Int,
        private val viewHeight: Int,
        private val panOffsetX: Float,
        private val panOffsetY: Float,
        private val paddingLeft: Int = 0,
        private val paddingTop: Int = 0,
        private val paddingRight: Int = 0,
        private val paddingBottom: Int = 0,
    ) {
        // Calculate the padding offset for the logical center
        private val paddingOffsetX = (paddingLeft - paddingRight) / 2f
        private val paddingOffsetY = (paddingTop - paddingBottom) / 2f

        /**
         * Returns the geographic location that corresponds to a screen location.
         *
         * The screen location is specified in screen pixels (not display pixels)
         * relative to the top left of the map.
         *
         * If map padding is set, the conversion accounts for the logical viewport offset,
         * ensuring accurate coordinate mapping in the visible (non-padded) area.
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
            val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

            // Adjust screen coordinates to account for padding offset
            val adjustedScreenX = screenX - paddingOffsetX
            val adjustedScreenY = screenY - paddingOffsetY

            val pixelX = (centerPixelX + (adjustedScreenX - viewWidth / 2 + panOffsetX).toDouble()).toInt()
            val pixelY = (centerPixelY + (adjustedScreenY - viewHeight / 2 + panOffsetY).toDouble()).toInt()

            return ProjectionUtils.pixelToLatLng(pixelX, pixelY, zoom.toInt())
        }

        /**
         * Returns a screen location that corresponds to a geographic coordinate.
         *
         * The screen location is specified in screen pixels (not display pixels)
         * relative to the top left of the map.
         *
         * If map padding is set, the returned screen coordinates account for the logical
         * viewport offset, reflecting where the location appears in the padded view.
         *
         * @param location The geographic location (LatLng)
         * @return The screen location in pixels
         */
        fun toScreenLocation(location: LatLng): Point {
            val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())
            val (locationPixelX, locationPixelY) = ProjectionUtils.latLngToPixel(location, zoom.toInt())

            val screenX = (locationPixelX - centerPixelX + viewWidth / 2 - panOffsetX + paddingOffsetX).toInt()
            val screenY = (locationPixelY - centerPixelY + viewHeight / 2 - panOffsetY + paddingOffsetY).toInt()

            return Point(screenX, screenY)
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

            val bounds =
                LatLngBounds
                    .builder()
                    .include(nearLeft)
                    .include(nearRight)
                    .include(farLeft)
                    .include(farRight)
                    .build()

            return VisibleRegion(
                nearLeft = nearLeft,
                nearRight = nearRight,
                farLeft = farLeft,
                farRight = farRight,
                latLngBounds = bounds,
            )
        }
    }
