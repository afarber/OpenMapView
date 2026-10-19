/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.core.graphics.withRotation

/**
 * The compass button, shown in the top-left corner while the map is turned.
 *
 * A round button in the style of the zoom buttons with a needle whose red half points to
 * north on the map, so it turns with the map. Tapping it turns the map back to north up.
 */
internal class CompassOverlay(
    context: Context,
) {
    private val density = context.resources.displayMetrics.density
    private val buttonRadius = 20f * density
    private val margin = 16f * density
    private val needleLength = 13f * density
    private val needleHalfWidth = 4f * density

    private val buttonPaint =
        Paint().apply {
            color = Color.argb(200, 255, 255, 255)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val buttonBorderPaint =
        Paint().apply {
            color = Color.argb(100, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

    private val northPaint =
        Paint().apply {
            color = Color.rgb(0xE5, 0x39, 0x35)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val southPaint =
        Paint().apply {
            color = Color.rgb(0x75, 0x75, 0x75)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val northHalf = Path()
    private val southHalf = Path()

    // Empty while the compass is hidden, so it is not hit
    private val bounds = RectF()

    /**
     * Draws the compass for the given bearing, or nothing while the map faces north.
     *
     * @param canvas The canvas, translated to the top-left corner of the map padding
     * @param bearing The camera bearing in degrees clockwise from north
     */
    fun draw(
        canvas: Canvas,
        bearing: Float,
    ) {
        if (bearing == 0f) {
            bounds.setEmpty()
            return
        }
        val centerX = margin + buttonRadius
        val centerY = margin + buttonRadius
        bounds.set(centerX - buttonRadius, centerY - buttonRadius, centerX + buttonRadius, centerY + buttonRadius)

        canvas.drawCircle(centerX, centerY, buttonRadius, buttonPaint)
        canvas.drawCircle(centerX, centerY, buttonRadius, buttonBorderPaint)

        // North on the turned map is the bearing counter-clockwise from the top of the screen
        canvas.withRotation(-bearing, centerX, centerY) {
            northHalf.apply {
                rewind()
                moveTo(centerX, centerY - needleLength)
                lineTo(centerX + needleHalfWidth, centerY)
                lineTo(centerX - needleHalfWidth, centerY)
                close()
            }
            southHalf.apply {
                rewind()
                moveTo(centerX, centerY + needleLength)
                lineTo(centerX - needleHalfWidth, centerY)
                lineTo(centerX + needleHalfWidth, centerY)
                close()
            }
            drawPath(northHalf, northPaint)
            drawPath(southHalf, southPaint)
        }
    }

    /**
     * Returns whether the point is on the compass, relative to the top-left corner of the
     * map padding, as where it was last drawn.
     */
    fun contains(
        x: Float,
        y: Float,
    ): Boolean = bounds.contains(x, y)
}
