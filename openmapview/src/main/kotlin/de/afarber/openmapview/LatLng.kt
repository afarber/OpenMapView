/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * An immutable class representing a pair of latitude and longitude coordinates.
 *
 * Out-of-range values are normalized instead of rejected:
 * the latitude is clamped to the range -90 to 90 degrees, and a longitude outside
 * the range -180 to 180 degrees is wrapped into it (for example 190 becomes -170).
 *
 * @param latitude The latitude in degrees
 * @param longitude The longitude in degrees
 * @throws IllegalArgumentException if [latitude] or [longitude] is NaN or infinite
 */
class LatLng(
    latitude: Double,
    longitude: Double,
) {
    init {
        require(latitude.isFinite()) { "Latitude must be a finite number, but was $latitude" }
        require(longitude.isFinite()) { "Longitude must be a finite number, but was $longitude" }
    }

    /** The latitude in degrees, between -90 and 90. */
    val latitude: Double = latitude.coerceIn(-90.0, 90.0)

    /** The longitude in degrees, between -180 and 180. */
    val longitude: Double = normalizeLongitude(longitude)

    operator fun component1(): Double = latitude

    operator fun component2(): Double = longitude

    fun copy(
        latitude: Double = this.latitude,
        longitude: Double = this.longitude,
    ): LatLng = LatLng(latitude, longitude)

    override fun equals(other: Any?): Boolean =
        this === other ||
            (
                other is LatLng &&
                    latitude.compareTo(other.latitude) == 0 &&
                    longitude.compareTo(other.longitude) == 0
            )

    override fun hashCode(): Int = 31 * latitude.hashCode() + longitude.hashCode()

    override fun toString(): String = "LatLng(latitude=$latitude, longitude=$longitude)"

    private companion object {
        fun normalizeLongitude(longitude: Double): Double =
            if (longitude in -180.0..180.0) {
                longitude
            } else {
                ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
            }
    }
}
