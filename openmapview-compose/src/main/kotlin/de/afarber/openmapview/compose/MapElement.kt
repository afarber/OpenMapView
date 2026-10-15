/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import de.afarber.openmapview.OpenMapView

/**
 * Adds an element (shape or overlay) to the map of the enclosing [OpenMap] while it is in
 * the composition, and removes it when it leaves.
 *
 * The map's elements are immutable, so a change of any of [keys] removes the element and
 * adds a new one, created by [create]. Does nothing outside of [OpenMap]'s content.
 *
 * @param keys All parameters the element is created from
 * @param create Creates the element from the current parameters
 * @param add Adds the element to the map and returns the added instance
 * @param remove Removes the element from the map
 * @param onClick Called when the element is tapped, if the map reports taps for it; null for
 *   elements that can't be tapped, such as tile overlays
 */
@Composable
internal fun <T : Any> MapElement(
    vararg keys: Any?,
    create: () -> T,
    add: OpenMapView.(T) -> T,
    remove: OpenMapView.(T) -> Unit,
    onClick: ((T) -> Unit)? = null,
) {
    val context = LocalMapContext.current ?: return
    val currentOnClick by rememberUpdatedState(onClick)

    DisposableEffect(context, *keys) {
        val element = context.map.add(create())
        if (onClick != null) {
            context.shapeClickHandlers[element] = { currentOnClick?.invoke(element) }
        }
        onDispose {
            context.shapeClickHandlers.remove(element)
            context.map.remove(element)
        }
    }
}
