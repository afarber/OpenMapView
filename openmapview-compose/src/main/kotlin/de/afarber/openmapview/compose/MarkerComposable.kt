/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.platform.ComposeView
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Marker as MapMarker

/**
 * A [Marker] whose icon is drawn by [content].
 *
 * The content is drawn once into an image and is not live:
 * it is drawn again when one of [keys] changes. Pass the values the content shows, e.g. a
 * count or a selection state. Taps on the icon call [onClick], not clickables inside it.
 *
 * Example:
 * ```kotlin
 * MarkerComposable(count, state = rememberMarkerState(LatLng(51.4661, 7.2491))) {
 *     Box(Modifier.size(40.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
 *         Text("$count", color = Color.White)
 *     }
 * }
 * ```
 *
 * The other parameters are the same as for [Marker].
 *
 * @param keys Values that make the content draw again when they change
 * @param content The marker icon
 */
@Composable
fun MarkerComposable(
    vararg keys: Any?,
    state: MarkerState,
    title: String? = null,
    snippet: String? = null,
    anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    visible: Boolean = true,
    alpha: Float = 1.0f,
    draggable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapMarker) -> Boolean = { false },
    onInfoWindowClick: (MapMarker) -> Unit = {},
    onInfoWindowClose: (MapMarker) -> Unit = {},
    content: @Composable () -> Unit,
) {
    val mapContext = LocalMapContext.current ?: return
    // Lets the content use the composition locals of the screen, e.g. MaterialTheme
    val compositionContext = rememberCompositionContext()
    val icon =
        remember(mapContext, *keys) {
            BitmapDescriptorFactory.fromBitmap(drawComposable(mapContext.map, compositionContext, content))
        }

    Marker(
        state = state,
        title = title,
        snippet = snippet,
        icon = icon,
        anchor = anchor,
        visible = visible,
        alpha = alpha,
        draggable = draggable,
        zIndex = zIndex,
        tag = tag,
        onClick = onClick,
        onInfoWindowClick = onInfoWindowClick,
        onInfoWindowClose = onInfoWindowClose,
    )
}

// Draws the content into a bitmap at its natural size. The ComposeView is briefly added to
// the map, because it only composes while attached to a window.
internal fun drawComposable(
    map: OpenMapView,
    compositionContext: CompositionContext,
    content: @Composable () -> Unit,
): Bitmap {
    val view =
        ComposeView(map.context).apply {
            setParentCompositionContext(compositionContext)
            setContent(content)
        }
    map.addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    try {
        val unspecified = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(unspecified, unspecified)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        val bitmap =
            Bitmap.createBitmap(
                view.measuredWidth.coerceAtLeast(1),
                view.measuredHeight.coerceAtLeast(1),
                Bitmap.Config.ARGB_8888,
            )
        view.draw(Canvas(bitmap))
        return bitmap
    } finally {
        map.removeView(view)
    }
}
