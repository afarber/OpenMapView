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
import de.afarber.openmapview.PatternItem
import de.afarber.openmapview.Circle as MapCircle

/**
 * A circle on an [OpenMap], declared in its content.
 *
 * The circle is added to the map when it enters the composition and removed when it leaves.
 * Changing a parameter updates it on the map, so it can follow e.g. a [MarkerState]'s position.
 * Outside of [OpenMap]'s content it does nothing.
 *
 * Example:
 * ```kotlin
 * OpenMap {
 *     Circle(center = LatLng(51.4661, 7.2491), radius = 500f, fillColor = Color.Blue.copy(alpha = 0.2f))
 * }
 * ```
 *
 * @param center The center of the circle
 * @param radius The radius in meters, greater than 0
 * @param strokeColor Outline color
 * @param strokeWidth Outline width in pixels
 * @param strokePattern Dash pattern, or null for a solid outline
 * @param pattern Dots, dashes and gaps, used instead of [strokePattern] if not null
 * @param strokeCap Cap of the outline dashes
 * @param strokeJoin Joint type of the outline
 * @param fillColor Fill color
 * @param visible Whether the circle is drawn
 * @param clickable Whether taps inside the circle call [onClick]
 * @param zIndex Drawing order relative to other shapes and markers
 * @param tag Any object to associate with the circle
 * @param onClick Called when a clickable circle is tapped
 */
@Composable
fun Circle(
    center: LatLng,
    radius: Float,
    strokeColor: Color = Color.Black,
    strokeWidth: Float = 10f,
    strokePattern: PathEffect? = null,
    pattern: List<PatternItem>? = null,
    strokeCap: StrokeCap = StrokeCap.Round,
    strokeJoin: StrokeJoin = StrokeJoin.Round,
    fillColor: Color = Color(red = 128, green = 128, blue = 128, alpha = 128),
    visible: Boolean = true,
    clickable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
    onClick: (MapCircle) -> Unit = {},
) {
    LiveMapElement(
        create = {
            MapCircle(
                center = center,
                radius = radius,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                strokePattern = strokePattern,
                pattern = pattern,
                strokeCap = strokeCap,
                strokeJoin = strokeJoin,
                fillColor = fillColor,
                visible = visible,
                clickable = clickable,
                zIndex = zIndex,
                tag = tag,
            )
        },
        update = {
            this.center = center
            this.radius = radius
            this.strokeColor = strokeColor
            this.strokeWidth = strokeWidth
            this.strokePattern = strokePattern
            this.pattern = pattern
            this.strokeCap = strokeCap
            this.strokeJoin = strokeJoin
            this.fillColor = fillColor
            this.visible = visible
            this.clickable = clickable
            this.zIndex = zIndex
            this.tag = tag
        },
        add = { addCircle(it) },
        remove = { removeCircle(it) },
        onClick = onClick,
    )
}
