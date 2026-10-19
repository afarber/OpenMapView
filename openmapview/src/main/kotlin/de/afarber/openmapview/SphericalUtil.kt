/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Distances, headings, offsets, interpolation and areas on the Earth as a sphere, e.g. to
 * move a vehicle marker along its route and turn it the way it travels.
 *
 * The methods have the names, parameters and units of `SphericalUtil` in android-maps-utils,
 * so code using it only needs another import. Distances are in meters, headings in degrees
 * clockwise from north, between -180 and 180.
 *
 * Example:
 * ```kotlin
 * val heading = SphericalUtil.computeHeading(from, to)
 * marker.position = SphericalUtil.interpolate(from, to, fraction)
 * marker.rotation = heading.toFloat()
 * ```
 */
object SphericalUtil {
    /** The mean radius of the Earth in meters, as in android-maps-utils. */
    const val EARTH_RADIUS = 6371009.0

    /**
     * Returns the heading from one location to another, in degrees clockwise from north,
     * between -180 and 180: 0 is north, 90 east, -90 west and -180 south.
     */
    @JvmStatic
    fun computeHeading(
        from: LatLng,
        to: LatLng,
    ): Double {
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val deltaLng = Math.toRadians(to.longitude - from.longitude)
        val heading =
            atan2(
                sin(deltaLng) * cos(toLat),
                cos(fromLat) * sin(toLat) - sin(fromLat) * cos(toLat) * cos(deltaLng),
            )
        return wrap(Math.toDegrees(heading), -180.0, 180.0)
    }

    /**
     * Returns the location reached by going [distance] meters from [from] at [heading]
     * degrees clockwise from north, along the great circle.
     */
    @JvmStatic
    fun computeOffset(
        from: LatLng,
        distance: Double,
        heading: Double,
    ): LatLng {
        val angle = distance / EARTH_RADIUS
        val headingRadians = Math.toRadians(heading)
        val fromLat = Math.toRadians(from.latitude)
        val fromLng = Math.toRadians(from.longitude)
        val sinLat = cos(angle) * sin(fromLat) + sin(angle) * cos(fromLat) * cos(headingRadians)
        val deltaLng = atan2(sin(angle) * cos(fromLat) * sin(headingRadians), cos(angle) - sin(fromLat) * sinLat)
        return LatLng(Math.toDegrees(asin(sinLat)), Math.toDegrees(fromLng + deltaLng))
    }

    /**
     * Returns the location from which going [distance] meters at [heading] degrees reaches
     * [to], the inverse of [computeOffset], or null if there is none.
     */
    @JvmStatic
    fun computeOffsetOrigin(
        to: LatLng,
        distance: Double,
        heading: Double,
    ): LatLng? {
        val headingRadians = Math.toRadians(heading)
        val angle = distance / EARTH_RADIUS
        // The start latitude solves a quadratic equation, see android-maps-utils
        val n1 = cos(angle)
        val n2 = sin(angle) * cos(headingRadians)
        val n3 = sin(angle) * sin(headingRadians)
        val n4 = sin(Math.toRadians(to.latitude))
        val n12 = n1 * n1
        val discriminant = n2 * n2 * n12 + n12 * n12 - n12 * n4 * n4
        if (discriminant < 0) return null

        var b = (n2 * n4 + sqrt(discriminant)) / (n1 * n1 + n2 * n2)
        val a = (n4 - n2 * b) / n1
        var fromLat = atan2(a, b)
        if (fromLat < -PI / 2 || fromLat > PI / 2) {
            b = (n2 * n4 - sqrt(discriminant)) / (n1 * n1 + n2 * n2)
            fromLat = atan2(a, b)
        }
        if (fromLat < -PI / 2 || fromLat > PI / 2) return null

        val fromLng = Math.toRadians(to.longitude) - atan2(n3, n1 * cos(fromLat) - n2 * sin(fromLat))
        return LatLng(Math.toDegrees(fromLat), Math.toDegrees(fromLng))
    }

    /**
     * Returns the location at [fraction] of the way from [from] to [to] along the great
     * circle: 0 is [from], 1 is [to]. Crossing the 180th meridian goes the short way.
     */
    @JvmStatic
    fun interpolate(
        from: LatLng,
        to: LatLng,
        fraction: Double,
    ): LatLng {
        val angle = computeAngleBetween(from, to)
        val sinAngle = sin(angle)
        if (sinAngle < 1e-6) {
            // Too close for the spherical formula: linear, over the shorter longitude difference
            val deltaLng = wrap(to.longitude - from.longitude, -180.0, 180.0)
            return LatLng(
                from.latitude + fraction * (to.latitude - from.latitude),
                from.longitude + fraction * deltaLng,
            )
        }

        val fromLat = Math.toRadians(from.latitude)
        val fromLng = Math.toRadians(from.longitude)
        val toLat = Math.toRadians(to.latitude)
        val toLng = Math.toRadians(to.longitude)
        val a = sin((1 - fraction) * angle) / sinAngle
        val b = sin(fraction * angle) / sinAngle
        val x = a * cos(fromLat) * cos(fromLng) + b * cos(toLat) * cos(toLng)
        val y = a * cos(fromLat) * sin(fromLng) + b * cos(toLat) * sin(toLng)
        val z = a * sin(fromLat) + b * sin(toLat)
        return LatLng(Math.toDegrees(atan2(z, sqrt(x * x + y * y))), Math.toDegrees(atan2(y, x)))
    }

    /** Returns the great-circle distance between two locations in meters. */
    @JvmStatic
    fun computeDistanceBetween(
        from: LatLng,
        to: LatLng,
    ): Double = computeAngleBetween(from, to) * EARTH_RADIUS

    /** Returns the length of a path in meters, 0 for fewer than 2 points. */
    @JvmStatic
    fun computeLength(path: List<LatLng>): Double = path.zipWithNext { from, to -> computeDistanceBetween(from, to) }.sum()

    /** Returns the area of a closed path in square meters, 0 for fewer than 3 points. */
    @JvmStatic
    fun computeArea(path: List<LatLng>): Double = abs(computeSignedArea(path))

    /**
     * Returns the signed area of a closed path in square meters: positive if the points go
     * counter-clockwise, negative if clockwise, 0 for fewer than 3 points.
     */
    @JvmStatic
    fun computeSignedArea(path: List<LatLng>): Double {
        if (path.size < 3) return 0.0
        // The sum of the areas of the triangles between each edge and the south pole
        var total = 0.0
        var previous = path.last()
        for (point in path) {
            total +=
                polarTriangleArea(
                    tan((PI / 2 - Math.toRadians(point.latitude)) / 2),
                    Math.toRadians(point.longitude),
                    tan((PI / 2 - Math.toRadians(previous.latitude)) / 2),
                    Math.toRadians(previous.longitude),
                )
            previous = point
        }
        return total * EARTH_RADIUS * EARTH_RADIUS
    }

    // The angle between two locations at the center of the Earth, in radians (haversine)
    internal fun computeAngleBetween(
        from: LatLng,
        to: LatLng,
    ): Double {
        val fromLat = Math.toRadians(from.latitude)
        val toLat = Math.toRadians(to.latitude)
        val sinHalfLat = sin((toLat - fromLat) / 2)
        val sinHalfLng = sin(Math.toRadians(to.longitude - from.longitude) / 2)
        val h = sinHalfLat * sinHalfLat + cos(fromLat) * cos(toLat) * sinHalfLng * sinHalfLng
        return 2 * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }

    // The signed area of the triangle between an edge and the south pole, on the unit sphere
    private fun polarTriangleArea(
        tan1: Double,
        lng1: Double,
        tan2: Double,
        lng2: Double,
    ): Double {
        val deltaLng = lng1 - lng2
        val t = tan1 * tan2
        return 2 * atan2(t * sin(deltaLng), 1 + t * cos(deltaLng))
    }

    // Wraps a value into [min, max)
    private fun wrap(
        value: Double,
        min: Double,
        max: Double,
    ): Double = if (value >= min && value < max) value else ((value - min) % (max - min) + (max - min)) % (max - min) + min
}
