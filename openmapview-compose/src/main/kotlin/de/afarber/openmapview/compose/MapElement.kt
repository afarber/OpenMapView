/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import de.afarber.openmapview.OpenMapView

/**
 * Adds an element (shape or overlay) to the map of the enclosing [OpenMap] while it is in
 * the composition, and removes it when it leaves.
 *
 * A change of any of [keys] removes the element and adds a new one, created by [create],
 * for elements the map can't update in place. Does nothing outside of [OpenMap]'s content.
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

/**
 * Adds a shape that the map draws from its current properties, such as a polyline, to the
 * map of the enclosing [OpenMap]. The shape is created once; on recomposition [update] sets
 * the current parameters on it, and the shape redraws the map when a value changed.
 * Does nothing outside of [OpenMap]'s content.
 *
 * @param create Creates the shape from the current parameters
 * @param update Sets the current parameters on the shape
 * @param add Adds the shape to the map and returns the added instance
 * @param remove Removes the shape from the map
 * @param onClick Called when the shape is tapped
 */
@Composable
internal fun <T : Any> LiveMapElement(
    create: () -> T,
    update: T.() -> Unit,
    add: OpenMapView.(T) -> T,
    remove: OpenMapView.(T) -> Unit,
    onClick: (T) -> Unit,
) {
    val context = LocalMapContext.current ?: return
    val currentOnClick by rememberUpdatedState(onClick)
    val shape = remember(context) { create() }

    DisposableEffect(context, shape) {
        val added = context.map.add(shape)
        context.shapeClickHandlers[added] = { currentOnClick(added) }
        onDispose {
            context.shapeClickHandlers.remove(added)
            context.map.remove(added)
        }
    }
    // After every recomposition: unchanged values don't redraw the map
    SideEffect { shape.update() }
}
