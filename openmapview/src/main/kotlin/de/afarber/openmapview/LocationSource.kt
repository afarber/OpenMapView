/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.location.Location

/**
 * Provides locations for the my-location layer.
 *
 * By default the map uses the platform `LocationManager` with the GPS and network
 * providers. Set your own source with [OpenMapView.setLocationSource], e.g. to use the
 * fused location provider of Play Services or to replay a recorded track.
 */
public interface LocationSource {
    /**
     * Receives new locations from a [LocationSource].
     */
    public fun interface OnLocationChangedListener {
        /**
         * Called with each new location of the device.
         *
         * @param location The new location
         */
        public fun onLocationChanged(location: Location)
    }

    /**
     * Starts sending locations to [listener]. Called when the my-location layer is enabled
     * and the map is shown.
     *
     * @param listener The listener to send locations to
     */
    public fun activate(listener: OnLocationChangedListener)

    /**
     * Stops sending locations. Called when the my-location layer is disabled or the map is
     * paused or detached.
     */
    public fun deactivate()
}
