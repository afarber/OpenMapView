/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.view.View

/**
 * Provides custom views for marker info windows, as in Google's `GoogleMap.InfoWindowAdapter`.
 *
 * When an info window opens, [getInfoWindow] is called first. If it returns null,
 * [getInfoContents] is called. If that returns null too, the default window with the
 * marker's title and snippet is shown.
 *
 * The returned view is drawn once into an image when the window opens, so it is not live:
 * call [Marker.showInfoWindow] again to show changes. Taps on the window are reported to
 * the [OnInfoWindowClickListener], not to views inside it.
 *
 * Example:
 * ```kotlin
 * mapView.setInfoWindowAdapter(object : InfoWindowAdapter {
 *     override fun getInfoWindow(marker: Marker): View? = null
 *
 *     override fun getInfoContents(marker: Marker): View =
 *         TextView(context).apply { text = "${marker.title} (custom)" }
 * })
 * ```
 */
interface InfoWindowAdapter {
    /**
     * Returns the view for the whole info window, drawn without the default frame, or
     * null to call [getInfoContents] instead.
     *
     * @param marker The marker whose info window opens
     */
    fun getInfoWindow(marker: Marker): View?

    /**
     * Returns the view drawn inside the default info window frame, or null for the
     * default title and snippet.
     *
     * @param marker The marker whose info window opens
     */
    fun getInfoContents(marker: Marker): View?
}
