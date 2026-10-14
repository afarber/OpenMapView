/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Callback for when the map has finished rendering.
 *
 * Set it using [OpenMapView.setOnMapLoadedCallback]. It is called once, after all visible
 * base map tiles have been drawn and the camera is not moving. It is not called if the
 * tiles can't be loaded (e.g. offline) or while the camera keeps moving.
 */
fun interface OnMapLoadedCallback {
    /**
     * Called when the map has finished rendering.
     */
    fun onMapLoaded()
}
