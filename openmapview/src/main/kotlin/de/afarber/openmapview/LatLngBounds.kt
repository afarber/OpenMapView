/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.math.max
import kotlin.math.min

/**
 * An immutable class representing a latitude/longitude aligned rectangle.
 *
 * The bounds are defined by the southwest and northeast corners of the rectangle.
 *
 * @property southwest The southwest corner of the bounds
 * @property northeast The northeast corner of the bounds
 */
data class LatLngBounds(
    val southwest: LatLng,
    val northeast: LatLng,
) {
    /**
     * Returns whether this bounds contains the given point.
     *
     * @param point The point to test
     * @return true if the point is contained within the bounds, false otherwise
     */
    fun contains(point: LatLng): Boolean {
        val lat = point.latitude
        val lng = point.longitude

        val latContained = lat >= southwest.latitude && lat <= northeast.latitude

        val lngContained =
            if (southwest.longitude <= northeast.longitude) {
                lng >= southwest.longitude && lng <= northeast.longitude
            } else {
                lng >= southwest.longitude || lng <= northeast.longitude
            }

        return latContained && lngContained
    }

    /**
     * Returns the center of the bounds.
     *
     * @return The geographic center point
     */
    fun getCenter(): LatLng {
        val lat = (southwest.latitude + northeast.latitude) / 2.0

        val lng =
            if (southwest.longitude <= northeast.longitude) {
                (southwest.longitude + northeast.longitude) / 2.0
            } else {
                var center = (southwest.longitude + northeast.longitude + 360.0) / 2.0
                if (center >= 180.0) center -= 360.0
                center
            }

        return LatLng(lat, lng)
    }

    /**
     * Returns a new bounds that extends this bounds to include the given point.
     *
     * @param point The point to include
     * @return A new LatLngBounds that includes both the original bounds and the point
     */
    fun including(point: LatLng): LatLngBounds {
        val minLat = min(southwest.latitude, point.latitude)
        val maxLat = max(northeast.latitude, point.latitude)

        val minLng: Double
        val maxLng: Double

        if (southwest.longitude <= northeast.longitude) {
            minLng = min(southwest.longitude, point.longitude)
            maxLng = max(northeast.longitude, point.longitude)
        } else {
            minLng = southwest.longitude
            maxLng = northeast.longitude
        }

        return LatLngBounds(
            southwest = LatLng(minLat, minLng),
            northeast = LatLng(maxLat, maxLng),
        )
    }

    companion object {
        /**
         * Creates a new builder for constructing bounds.
         *
         * @return A new Builder instance
         */
        fun builder(): Builder = Builder()
    }

    /**
     * Builder for creating LatLngBounds by including multiple points.
     *
     * The builder calculates the minimum bounding box that contains all
     * included points.
     */
    class Builder {
        private var minLat: Double? = null
        private var maxLat: Double? = null
        private var minLng: Double? = null
        private var maxLng: Double? = null

        /**
         * Includes a point in the bounds being built.
         *
         * The bounds will be extended to include this point.
         *
         * @param point The point to include
         * @return This builder for method chaining
         */
        fun include(point: LatLng): Builder {
            val lat = point.latitude
            val lng = point.longitude

            minLat =
                if (minLat == null) {
                    lat
                } else {
                    min(minLat!!, lat)
                }
            maxLat =
                if (maxLat == null) {
                    lat
                } else {
                    max(maxLat!!, lat)
                }
            minLng =
                if (minLng == null) {
                    lng
                } else {
                    min(minLng!!, lng)
                }
            maxLng =
                if (maxLng == null) {
                    lng
                } else {
                    max(maxLng!!, lng)
                }

            return this
        }

        /**
         * Creates the LatLngBounds from the included points.
         *
         * @return The constructed LatLngBounds
         * @throws IllegalStateException if no points have been included
         */
        fun build(): LatLngBounds {
            require(minLat != null && maxLat != null && minLng != null && maxLng != null) {
                "Cannot build LatLngBounds: no points have been included"
            }

            return LatLngBounds(
                southwest = LatLng(minLat!!, minLng!!),
                northeast = LatLng(maxLat!!, maxLng!!),
            )
        }
    }
}
