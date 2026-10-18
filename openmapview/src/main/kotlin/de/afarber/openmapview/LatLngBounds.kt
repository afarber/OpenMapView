/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
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
@Parcelize
data class LatLngBounds(
    @JvmField val southwest: LatLng,
    @JvmField val northeast: LatLng,
) : Parcelable {
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
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * Builder for creating LatLngBounds by including multiple points.
     *
     * The builder calculates the smallest bounding box that contains all
     * included points. It crosses the 180th meridian when that is narrower,
     * e.g. for points in Fiji and Samoa.
     */
    class Builder {
        private var minLat = Double.NaN
        private var maxLat = Double.NaN
        private val longitudes = mutableListOf<Double>()

        /**
         * Includes a point in the bounds being built.
         *
         * The bounds will be extended to include this point.
         *
         * @param point The point to include
         * @return This builder for method chaining
         */
        fun include(point: LatLng): Builder {
            minLat = if (minLat.isNaN()) point.latitude else min(minLat, point.latitude)
            maxLat = if (maxLat.isNaN()) point.latitude else max(maxLat, point.latitude)
            longitudes.add(point.longitude)
            return this
        }

        /**
         * Creates the LatLngBounds from the included points.
         *
         * @return The constructed LatLngBounds
         * @throws IllegalArgumentException if no points have been included
         */
        fun build(): LatLngBounds {
            require(longitudes.isNotEmpty()) {
                "Cannot build LatLngBounds: no points have been included"
            }

            // The bounds leave out the widest gap between neighboring longitudes. The gap
            // around the back of the globe, from the east end to the west end, is the usual one.
            val sorted = longitudes.sorted()
            var west = sorted.first()
            var east = sorted.last()
            var widestGap = west + 360.0 - east
            for (i in 0 until sorted.size - 1) {
                val gap = sorted[i + 1] - sorted[i]
                if (gap > widestGap) {
                    widestGap = gap
                    west = sorted[i + 1]
                    east = sorted[i]
                }
            }

            return LatLngBounds(
                southwest = LatLng(minLat, west),
                northeast = LatLng(maxLat, east),
            )
        }
    }
}
