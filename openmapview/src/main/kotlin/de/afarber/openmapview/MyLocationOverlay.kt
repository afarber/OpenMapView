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
import android.graphics.RectF
import kotlin.math.hypot

/**
 * Draws the my-location layer: a blue dot with its accuracy circle, and the my-location
 * button in the top right corner.
 */
internal class MyLocationOverlay(
    context: Context,
) {
    private val density = context.resources.displayMetrics.density
    private val dotRadius = 7f * density
    private val dotBorder = 2f * density
    private val buttonRadius = 20f * density
    private val margin = 16f * density

    // Taps this close to the dot count as taps on it
    private val dotTouchRadius = 16f * density

    private val accuracyFillPaint = paint(Color.argb(40, 66, 133, 244), Paint.Style.FILL)
    private val accuracyStrokePaint = paint(Color.argb(90, 66, 133, 244), Paint.Style.STROKE, density)
    private val dotBorderPaint = paint(Color.WHITE, Paint.Style.FILL)
    private val dotPaint = paint(Color.rgb(66, 133, 244), Paint.Style.FILL)
    private val buttonPaint = paint(Color.argb(200, 255, 255, 255), Paint.Style.FILL)
    private val buttonBorderPaint = paint(Color.argb(100, 0, 0, 0), Paint.Style.STROKE, 1f)
    private val iconPaint = paint(Color.DKGRAY, Paint.Style.STROKE, 2f * density)

    private val buttonBounds = RectF()
    private var dotX = Float.NaN
    private var dotY = Float.NaN

    private fun paint(
        color: Int,
        style: Paint.Style,
        strokeWidth: Float = 0f,
    ) = Paint().apply {
        this.color = color
        this.style = style
        this.strokeWidth = strokeWidth
        isAntiAlias = true
    }

    /**
     * Draws the location dot at the given screen position.
     *
     * @param accuracyRadius The accuracy circle radius in pixels, 0 for none
     */
    fun drawLocation(
        canvas: Canvas,
        x: Float,
        y: Float,
        accuracyRadius: Float,
    ) {
        dotX = x
        dotY = y
        if (accuracyRadius > dotRadius) {
            canvas.drawCircle(x, y, accuracyRadius, accuracyFillPaint)
            canvas.drawCircle(x, y, accuracyRadius, accuracyStrokePaint)
        }
        canvas.drawCircle(x, y, dotRadius + dotBorder, dotBorderPaint)
        canvas.drawCircle(x, y, dotRadius, dotPaint)
    }

    /** Forgets the dot position, e.g. when the location is not known or not drawn. */
    fun clearLocation() {
        dotX = Float.NaN
        dotY = Float.NaN
    }

    /** Draws the my-location button: a crosshair in a circle in the top right corner. */
    fun drawButton(
        canvas: Canvas,
        viewWidth: Int,
    ) {
        val centerX = viewWidth - margin - buttonRadius
        val centerY = margin + buttonRadius
        buttonBounds.set(centerX - buttonRadius, centerY - buttonRadius, centerX + buttonRadius, centerY + buttonRadius)

        canvas.drawCircle(centerX, centerY, buttonRadius, buttonPaint)
        canvas.drawCircle(centerX, centerY, buttonRadius, buttonBorderPaint)

        val ring = buttonRadius * 0.4f
        val tick = buttonRadius * 0.65f
        canvas.drawCircle(centerX, centerY, ring, iconPaint)
        canvas.drawLine(centerX, centerY - tick, centerX, centerY - ring, iconPaint)
        canvas.drawLine(centerX, centerY + ring, centerX, centerY + tick, iconPaint)
        canvas.drawLine(centerX - tick, centerY, centerX - ring, centerY, iconPaint)
        canvas.drawLine(centerX + ring, centerY, centerX + tick, centerY, iconPaint)
    }

    /** Whether a touch at the given position hits the button, as last drawn. */
    fun isOnButton(
        x: Float,
        y: Float,
    ): Boolean = buttonBounds.contains(x, y)

    /** Whether a touch at the given position hits the location dot, as last drawn. */
    fun isOnDot(
        x: Float,
        y: Float,
    ): Boolean = !dotX.isNaN() && hypot(x - dotX, y - dotY) <= dotTouchRadius
}
