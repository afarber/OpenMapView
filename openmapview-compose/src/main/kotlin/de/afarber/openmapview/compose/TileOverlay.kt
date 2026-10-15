/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import de.afarber.openmapview.TileProvider
import de.afarber.openmapview.TileOverlay as MapTileOverlay

/**
 * A layer of tiles drawn over or under the base map of an [OpenMap], declared in its
 * content, e.g. sea marks or hiking trails.
 *
 * The overlay is added to the map when it enters the composition and removed when it
 * leaves. Changing any parameter replaces it; keep [tileProvider] in a `remember` so it is
 * not replaced on every recomposition. Outside of [OpenMap]'s content it does nothing.
 *
 * Example:
 * ```kotlin
 * OpenMap {
 *     val seaMarks = remember { PredefinedTileProviders.openSeaMap() }
 *     TileOverlay(tileProvider = seaMarks, transparency = 0.2f)
 * }
 * ```
 *
 * @param tileProvider Provides the tiles, e.g. a [de.afarber.openmapview.UrlTileProvider]
 * @param transparency From 0 (opaque) to 1 (invisible)
 * @param zIndex Drawing order; negative values are drawn under the base map
 * @param visible Whether the overlay is drawn
 * @param fadeIn Whether tiles fade in when they load
 * @param tag Any object to associate with the overlay
 */
@Composable
fun TileOverlay(
    tileProvider: TileProvider,
    transparency: Float = 0f,
    zIndex: Float = 0f,
    visible: Boolean = true,
    fadeIn: Boolean = false,
    tag: Any? = null,
) {
    MapElement(
        tileProvider, transparency, zIndex, visible, fadeIn, tag,
        create = {
            MapTileOverlay(
                tileProvider = tileProvider,
                transparency = transparency,
                zIndex = zIndex,
                visible = visible,
                fadeIn = fadeIn,
                tag = tag,
            )
        },
        add = { addTileOverlay(it) },
        remove = { removeTileOverlay(it) },
    )
}
