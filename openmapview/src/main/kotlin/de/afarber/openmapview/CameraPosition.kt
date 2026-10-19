/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.os.Parcel
import android.os.Parcelable

/**
 * Represents the camera position for a map.
 *
 * Defines where the camera is positioned (target location), how close the camera is to
 * the earth's surface (zoom level) and which way is up on the screen (bearing).
 *
 * Parcelable, e.g. for a [android.os.Bundle] or `rememberSaveable`.
 *
 * @param target The location that the camera is pointing at
 * @param zoom The zoom level, ranging from 2.0 (world view) to 19.0 (street level)
 * @param bearing The direction at the top of the screen, in degrees clockwise from north,
 *   e.g. 90 for east up; normalized to 0 (inclusive) to 360 (exclusive)
 * @throws IllegalArgumentException if zoom is outside the valid range or bearing is not finite
 */
class CameraPosition
    @JvmOverloads
    constructor(
        target: LatLng,
        zoom: Float,
        bearing: Float = 0f,
    ) : Parcelable {
        init {
            require(zoom >= MIN_ZOOM) { "Zoom level must be at least $MIN_ZOOM" }
            require(zoom <= MAX_ZOOM) { "Zoom level must be at most $MAX_ZOOM" }
            require(bearing.isFinite()) { "Bearing must be a finite number, but was $bearing" }
        }

        /** The location that the camera is pointing at. */
        @JvmField
        val target: LatLng = target

        /** The zoom level, ranging from 2.0 (world view) to 19.0 (street level). */
        @JvmField
        val zoom: Float = zoom

        /** The direction at the top of the screen, in degrees clockwise from north, 0 to 360. */
        @JvmField
        val bearing: Float = normalizeBearing(bearing)

        operator fun component1(): LatLng = target

        operator fun component2(): Float = zoom

        operator fun component3(): Float = bearing

        fun copy(
            target: LatLng = this.target,
            zoom: Float = this.zoom,
            bearing: Float = this.bearing,
        ): CameraPosition = CameraPosition(target, zoom, bearing)

        override fun equals(other: Any?): Boolean =
            this === other ||
                (
                    other is CameraPosition &&
                        target == other.target &&
                        zoom.compareTo(other.zoom) == 0 &&
                        bearing.compareTo(other.bearing) == 0
                )

        override fun hashCode(): Int = (31 * target.hashCode() + zoom.hashCode()) * 31 + bearing.hashCode()

        override fun toString(): String = "CameraPosition(target=$target, zoom=$zoom, bearing=$bearing)"

        override fun describeContents(): Int = 0

        override fun writeToParcel(
            parcel: Parcel,
            flags: Int,
        ) {
            parcel.writeDouble(target.latitude)
            parcel.writeDouble(target.longitude)
            parcel.writeFloat(zoom)
            parcel.writeFloat(bearing)
        }

        /**
         * Builds a [CameraPosition] step by step, as in the Maps SDK, e.g.
         * `CameraPosition.builder().target(latLng).zoom(15f).bearing(90f).build()`.
         */
        class Builder() {
            private var target: LatLng? = null
            private var zoom: Float = MIN_ZOOM
            private var bearing: Float = 0f

            /** Starts with the values of [position]. */
            constructor(position: CameraPosition) : this() {
                target = position.target
                zoom = position.zoom
                bearing = position.bearing
            }

            /** Sets the location that the camera is pointing at. */
            fun target(target: LatLng): Builder = apply { this.target = target }

            /** Sets the zoom level. */
            fun zoom(zoom: Float): Builder = apply { this.zoom = zoom }

            /** Sets the direction at the top of the screen, in degrees clockwise from north. */
            fun bearing(bearing: Float): Builder = apply { this.bearing = bearing }

            /**
             * Returns the camera position.
             *
             * @throws IllegalStateException if no target was set
             */
            fun build(): CameraPosition = CameraPosition(checkNotNull(target) { "Camera target must be set" }, zoom, bearing)
        }

        companion object {
            private const val MIN_ZOOM = 2.0f
            private const val MAX_ZOOM = 19.0f

            /** Returns a new [Builder]. */
            @JvmStatic
            fun builder(): Builder = Builder()

            /** Returns a new [Builder] that starts with the values of [position]. */
            @JvmStatic
            fun builder(position: CameraPosition): Builder = Builder(position)

            /** Returns a camera position at [target] and [zoom], facing north. */
            @JvmStatic
            fun fromLatLngZoom(
                target: LatLng,
                zoom: Float,
            ): CameraPosition = CameraPosition(target, zoom)

            internal fun normalizeBearing(bearing: Float): Float {
                if (bearing >= 0f && bearing < 360f) return bearing
                val wrapped = ((bearing % 360f) + 360f) % 360f
                // A tiny negative bearing can round up to 360
                return if (wrapped >= 360f) 0f else wrapped
            }

            @JvmField
            val CREATOR: Parcelable.Creator<CameraPosition> =
                object : Parcelable.Creator<CameraPosition> {
                    override fun createFromParcel(parcel: Parcel): CameraPosition =
                        CameraPosition(LatLng(parcel.readDouble(), parcel.readDouble()), parcel.readFloat(), parcel.readFloat())

                    override fun newArray(size: Int): Array<CameraPosition?> = arrayOfNulls(size)
                }
        }
    }
