/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.location.Location

/**
 * Called when the my-location dot is tapped.
 */
fun interface OnMyLocationClickListener {
    /**
     * Called when the my-location dot is tapped.
     *
     * @param location The location shown by the dot
     */
    fun onMyLocationClick(location: Location)
}
