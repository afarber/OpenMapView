/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.StyleSpan
import de.afarber.openmapview.Polyline as MapPolyline

/**
 * A polyline on an [OpenMap], declared in its content.
 *
 * The polyline is added to the map when it enters the composition and removed when it
 * leaves. Changing any parameter replaces it. Outside of [OpenMap]'s content it does nothing.
 *
 * Example:
 * ```kotlin
 * OpenMap {
 *     Polyline(
 *         points = listOf(LatLng(51.4661, 7.2491), LatLng(51.5136, 7.4653)),
 *         strokeColor = Color.Blue,
 *         clickable = true,
 *         onClick = { Log.d("Map", "Route tapped") },
 *     )
 * }
 * ```
 *
 * @param points The vertices, at least 2
 * @param strokeColor Line color
 * @param strokeWidth Line width in pixels
 * @param strokePattern Dash pattern, or null for a solid line
 * @param startCap Cap at the first vertex
 * @param endCap Cap at the last vertex
 * @param strokeJoin Joint type at the vertices
 * @param geodesic Whether segments follow great circles
 * @param spans Per-segment colors, see [StyleSpan]
 * @param visible Whether the polyline is drawn
 * @param clickable Whether taps on the polyline call [onClick]
 * @param zIndex Drawing order relative to other shapes and markers
 * @param tag Any object to associate with the polyline
 * @param onClick Called when a clickable polyline is tapped
 */
@Composable
fun Polyline(
    points: List<LatLng>,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 10f,
    strokePattern: PathEffect? = null,
    startCap: StrokeCap = StrokeCap.Butt,
    endCap: StrokeCap = StrokeCap.Butt,
    strokeJoin: StrokeJoin = StrokeJoin.Round,
    geodesic: Boolean = false,
    spans: List<StyleSpan> = emptyList(),
    visible: Boolean = true,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapPolyline) -> Unit = {},
) {
    MapElement(
        points, strokeColor, strokeWidth, strokePattern, startCap, endCap,
        strokeJoin, geodesic, spans, visible, clickable, zIndex, tag,
        create = {
            MapPolyline(
                points = points,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                strokePattern = strokePattern,
                startCap = startCap,
                endCap = endCap,
                strokeJoin = strokeJoin,
                geodesic = geodesic,
                spans = spans,
                visible = visible,
                clickable = clickable,
                zIndex = zIndex,
                tag = tag,
            )
        },
        add = { addPolyline(it) },
        remove = { removePolyline(it) },
        onClick = onClick,
    )
}
