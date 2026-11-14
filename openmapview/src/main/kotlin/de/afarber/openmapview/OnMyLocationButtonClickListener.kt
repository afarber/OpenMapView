/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Called when the my-location button is tapped.
 */
public fun interface OnMyLocationButtonClickListener {
    /**
     * Called when the my-location button is tapped.
     *
     * @return true to consume the tap, false to also move the camera to the location
     */
    public fun onMyLocationButtonClick(): Boolean
}
