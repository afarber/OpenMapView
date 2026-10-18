/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import de.afarber.openmapview.MapColorScheme

/**
 * The color scheme of an [OpenMap], see [de.afarber.openmapview.OpenMapView.setMapColorScheme].
 *
 * @property value The [MapColorScheme] constant
 */
enum class ComposeMapColorScheme(
    val value: Int,
) {
    /** Light map, whatever the device settings. */
    LIGHT(MapColorScheme.LIGHT),

    /** Dark map, whatever the device settings. */
    DARK(MapColorScheme.DARK),

    /** Light or dark map, following the night mode of the device. */
    FOLLOW_SYSTEM(MapColorScheme.FOLLOW_SYSTEM),
}
