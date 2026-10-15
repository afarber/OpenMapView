/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/** The default width of [ScaleBar], which is also the longest bar it draws. */
val DefaultScaleBarWidth: Dp = 65.dp

/** The default height of [ScaleBar]. */
val DefaultScaleBarHeight: Dp = 50.dp

/**
 * A scale bar for an [OpenMap], as in Google's maps-compose-widgets: a metric distance
 * above a line and an imperial distance below it. The bars show round distances such as
 * 200 m or 500 ft, up to [width] long, at the latitude of the camera target.
 *
 * Place it over the map, e.g. in a `Box` around [OpenMap]:
 * ```kotlin
 * Box {
 *     OpenMap(cameraPositionState = cameraPositionState)
 *     ScaleBar(cameraPositionState, Modifier.align(Alignment.TopStart).padding(8.dp))
 * }
 * ```
 *
 * @param cameraPositionState The camera of the map
 * @param modifier Modifier for the scale bar
 * @param width The longest bar that is drawn
 * @param height The height of the scale bar, including both labels
 * @param textColor The color of the labels
 * @param lineColor The color of the lines
 * @param shadowColor The outline around the lines, for contrast on any map
 */
@Composable
fun ScaleBar(
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    width: Dp = DefaultScaleBarWidth,
    height: Dp = DefaultScaleBarHeight,
    textColor: Color = Color.DarkGray,
    lineColor: Color = Color.DarkGray,
    shadowColor: Color = Color.White,
) {
    val position = cameraPositionState.position
    val widthPx = with(LocalDensity.current) { width.toPx() }
    val metersPerPixel = metersPerPixel(position.target.latitude, position.zoom)
    val metric = metricScale(metersPerPixel * widthPx)
    val imperial = imperialScale(metersPerPixel * widthPx)
    val textStyle = TextStyle(color = textColor, fontSize = 12.sp)

    Box(modifier = modifier.size(width, height)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val y = size.height / 2
            val metricLength = (metric.meters / metersPerPixel).toFloat()
            val imperialLength = (imperial.meters / metersPerPixel).toFloat()
            // Draw the outline first, then the lines on top of it
            for ((color, extra) in listOf(shadowColor to 2.dp.toPx(), lineColor to 0f)) {
                drawScaleLines(color, 2.dp.toPx() + extra, y, metricLength, imperialLength)
            }
        }
        BasicText(text = metric.label, style = textStyle, modifier = Modifier.align(Alignment.TopStart))
        BasicText(text = imperial.label, style = textStyle, modifier = Modifier.align(Alignment.BottomStart))
    }
}

/**
 * A [ScaleBar] that appears while the camera moves and fades out [visibilityDurationMillis]
 * after it stops, as in Google's maps-compose-widgets.
 *
 * @param visibilityDurationMillis How long the scale bar stays after the last camera change
 * @param enterTransition How the scale bar appears
 * @param exitTransition How the scale bar disappears
 * @see ScaleBar for the other parameters
 */
@Composable
fun DisappearingScaleBar(
    cameraPositionState: CameraPositionState,
    modifier: Modifier = Modifier,
    width: Dp = DefaultScaleBarWidth,
    height: Dp = DefaultScaleBarHeight,
    textColor: Color = Color.DarkGray,
    lineColor: Color = Color.DarkGray,
    shadowColor: Color = Color.White,
    visibilityDurationMillis: Int = 3_000,
    enterTransition: EnterTransition = fadeIn(),
    exitTransition: ExitTransition = fadeOut(),
) {
    var visible by remember { mutableStateOf(false) }
    // Restarted on every camera change, so the bar hides only after the camera stops
    LaunchedEffect(cameraPositionState.position) {
        visible = true
        delay(visibilityDurationMillis.toLong())
        visible = false
    }

    AnimatedVisibility(visible = visible, modifier = modifier, enter = enterTransition, exit = exitTransition) {
        ScaleBar(cameraPositionState, Modifier, width, height, textColor, lineColor, shadowColor)
    }
}

// A horizontal line with a tick down at the end of the metric bar and up at the end of the
// imperial bar, starting at the left edge
private fun DrawScope.drawScaleLines(
    color: Color,
    strokeWidth: Float,
    y: Float,
    metricLength: Float,
    imperialLength: Float,
) {
    val tick = size.height / 6
    val lineLength = maxOf(metricLength, imperialLength)
    drawLine(color, Offset(0f, y), Offset(lineLength, y), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(0f, y - tick), Offset(0f, y + tick), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(metricLength, y), Offset(metricLength, y - tick), strokeWidth, StrokeCap.Round)
    drawLine(color, Offset(imperialLength, y), Offset(imperialLength, y + tick), strokeWidth, StrokeCap.Round)
}

/** A round distance for a scale bar, e.g. "500 m", and its length in meters. */
internal data class Scale(
    val meters: Double,
    val label: String,
)

private const val EARTH_CIRCUMFERENCE_METERS = 40_075_016.686
private const val METERS_PER_FOOT = 0.3048
private const val FEET_PER_MILE = 5280

/**
 * Meters per screen pixel at a latitude and zoom on 256 px Web Mercator tiles, as drawn by
 * OpenMapView: 156543 m at the equator at zoom 0, halved on each zoom level.
 */
internal fun metersPerPixel(
    latitude: Double,
    zoom: Float,
): Double = EARTH_CIRCUMFERENCE_METERS / 256 * cos(latitude * PI / 180) / 2.0.pow(zoom.toDouble())

/** The largest 1, 2 or 5 times a power of ten that is not above [max], e.g. 500 for 730. */
internal fun roundDistance(max: Double): Double {
    if (max <= 0) return 0.0
    val power = 10.0.pow(floor(log10(max)))
    val first = max / power
    val step =
        when {
            first >= 5 -> 5
            first >= 2 -> 2
            else -> 1
        }
    return step * power
}

/** The metric scale for a bar of at most [maxMeters], in meters or kilometers. */
internal fun metricScale(maxMeters: Double): Scale {
    val meters = roundDistance(maxMeters)
    val label = if (meters >= 1000) "${formatNumber(meters / 1000)} km" else "${formatNumber(meters)} m"
    return Scale(meters, label)
}

/** The imperial scale for a bar of at most [maxMeters], in feet or miles. */
internal fun imperialScale(maxMeters: Double): Scale {
    val maxFeet = maxMeters / METERS_PER_FOOT
    if (maxFeet < FEET_PER_MILE) {
        val feet = roundDistance(maxFeet)
        return Scale(feet * METERS_PER_FOOT, "${formatNumber(feet)} ft")
    }
    val miles = roundDistance(maxFeet / FEET_PER_MILE)
    return Scale(miles * FEET_PER_MILE * METERS_PER_FOOT, "${formatNumber(miles)} mi")
}

// Whole numbers without decimals, small distances like 0.5 with them
private fun formatNumber(value: Double): String = if (value >= 1 && value == floor(value)) value.roundToInt().toString() else value.toString()
