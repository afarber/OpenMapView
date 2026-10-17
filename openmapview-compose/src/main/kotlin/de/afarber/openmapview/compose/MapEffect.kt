/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import de.afarber.openmapview.OpenMapView
import kotlinx.coroutines.CoroutineScope

/**
 * Runs [block] with the [OpenMapView] of the enclosing [OpenMap], for map calls that have no
 * composable, e.g. [OpenMapView.snapshot]. Like `LaunchedEffect`, it runs when it enters the
 * composition and again when one of [keys] changes, and its coroutine is canceled when it
 * leaves the composition.
 *
 * Outside of an [OpenMap] it does nothing.
 *
 * ```kotlin
 * OpenMap(cameraPositionState = cameraPositionState) {
 *     MapEffect(isRunFinished) { map ->
 *         if (isRunFinished) map.snapshot { bitmap -> bitmap?.let(onSnapshot) }
 *     }
 * }
 * ```
 *
 * @param keys Values that restart [block] when they change
 * @param block Called on the main thread with the map
 */
@Composable
fun MapEffect(
    vararg keys: Any?,
    block: suspend CoroutineScope.(OpenMapView) -> Unit,
) {
    val context = LocalMapContext.current ?: return
    LaunchedEffect(context.map, *keys) { block(context.map) }
}
