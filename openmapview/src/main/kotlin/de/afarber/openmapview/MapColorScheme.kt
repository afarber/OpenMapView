/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Color scheme constants for [OpenMapView.setMapColorScheme], with the same values as in
 * the Maps SDK.
 *
 * The dark scheme is drawn from the light tiles with [TileColorFilters.invertedDark], an
 * approximation that needs no API key. Map types that are dark already, such as
 * [MapType.CARTO_DARK], are drawn unchanged.
 */
object MapColorScheme {
    /** An unknown color scheme, not accepted by [OpenMapView.setMapColorScheme]. */
    const val UNKNOWN: Int = -1

    /** Light map, whatever the device settings. The default of [OpenMapView]. */
    const val LIGHT: Int = 0

    /** Dark map, whatever the device settings. */
    const val DARK: Int = 1

    /** Light or dark map, following the night mode of the device. */
    const val FOLLOW_SYSTEM: Int = 2
}
