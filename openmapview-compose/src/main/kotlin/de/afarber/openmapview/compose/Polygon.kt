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
import de.afarber.openmapview.Polygon as MapPolygon

/**
 * A polygon on an [OpenMap], declared in its content.
 *
 * The polygon is added to the map when it enters the composition and removed when it
 * leaves. Changing a parameter updates it on the map. Outside of [OpenMap]'s content it does nothing.
 *
 * Example:
 * ```kotlin
 * OpenMap {
 *     Polygon(
 *         points = listOf(LatLng(51.47, 7.24), LatLng(51.47, 7.26), LatLng(51.46, 7.25)),
 *         fillColor = Color.Green.copy(alpha = 0.3f),
 *         clickable = true,
 *         onClick = { Log.d("Map", "Area tapped") },
 *     )
 * }
 * ```
 *
 * @param points The outline vertices, at least 3
 * @param holes Areas cut out of the polygon, each with at least 3 vertices
 * @param strokeColor Outline color
 * @param strokeWidth Outline width in pixels
 * @param strokePattern Dash pattern, or null for a solid outline
 * @param strokeCap Cap of the outline segments
 * @param strokeJoin Joint type at the vertices
 * @param fillColor Fill color
 * @param geodesic Whether edges follow great circles
 * @param visible Whether the polygon is drawn
 * @param clickable Whether taps inside the polygon call [onClick]
 * @param zIndex Drawing order relative to other shapes and markers
 * @param tag Any object to associate with the polygon
 * @param onClick Called when a clickable polygon is tapped
 */
@Composable
fun Polygon(
    points: List<LatLng>,
    holes: List<List<LatLng>> = emptyList(),
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 10f,
    strokePattern: PathEffect? = null,
    strokeCap: StrokeCap = StrokeCap.Butt,
    strokeJoin: StrokeJoin = StrokeJoin.Round,
    fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128),
    geodesic: Boolean = false,
    visible: Boolean = true,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapPolygon) -> Unit = {},
) {
    LiveMapElement(
        create = {
            MapPolygon(
                points = points,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                strokePattern = strokePattern,
                strokeCap = strokeCap,
                strokeJoin = strokeJoin,
                fillColor = fillColor,
                holes = holes,
                geodesic = geodesic,
                visible = visible,
                clickable = clickable,
                zIndex = zIndex,
                tag = tag,
            )
        },
        update = {
            this.points = points
            this.strokeColor = strokeColor
            this.strokeWidth = strokeWidth
            this.strokePattern = strokePattern
            this.strokeCap = strokeCap
            this.strokeJoin = strokeJoin
            this.fillColor = fillColor
            this.holes = holes
            this.geodesic = geodesic
            this.visible = visible
            this.clickable = clickable
            this.zIndex = zIndex
            this.tag = tag
        },
        add = { addPolygon(it) },
        remove = { removePolygon(it) },
        onClick = onClick,
    )
}
