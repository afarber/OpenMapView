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
import android.text.TextPaint

/**
 * Zoom controls overlay displaying +/- buttons for zooming.
 *
 * Renders two circular buttons in the bottom-right corner (above attribution)
 * for zooming in and out.
 */
class ZoomControlsOverlay(
    context: Context,
) {
    private val buttonRadius = 20f * context.resources.displayMetrics.density
    private val buttonSpacing = 8f * context.resources.displayMetrics.density
    private val margin = 16f * context.resources.displayMetrics.density
    private val attributionHeight = 24f * context.resources.displayMetrics.density

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

    private val textPaint =
        TextPaint().apply {
            color = Color.BLACK
            textSize = 24f * context.resources.displayMetrics.density
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

    private var zoomInButtonBounds = RectF()
    private var zoomOutButtonBounds = RectF()

    /**
     * Draws the zoom controls on the canvas.
     *
     * @param canvas The canvas to draw on
     * @param viewWidth The view width in pixels
     * @param viewHeight The view height in pixels
     */
    fun draw(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
    ) {
        val centerX = viewWidth - margin - buttonRadius
        val zoomInCenterY = viewHeight - attributionHeight - margin - buttonRadius
        val zoomOutCenterY = zoomInCenterY - (buttonRadius * 2) - buttonSpacing

        // Update bounds for touch detection
        zoomInButtonBounds.set(
            centerX - buttonRadius,
            zoomInCenterY - buttonRadius,
            centerX + buttonRadius,
            zoomInCenterY + buttonRadius,
        )

        zoomOutButtonBounds.set(
            centerX - buttonRadius,
            zoomOutCenterY - buttonRadius,
            centerX + buttonRadius,
            zoomOutCenterY + buttonRadius,
        )

        // Draw zoom out button (-)
        canvas.drawCircle(centerX, zoomOutCenterY, buttonRadius, buttonPaint)
        canvas.drawCircle(centerX, zoomOutCenterY, buttonRadius, buttonBorderPaint)
        canvas.drawText("-", centerX, zoomOutCenterY + textPaint.textSize / 3, textPaint)

        // Draw zoom in button (+)
        canvas.drawCircle(centerX, zoomInCenterY, buttonRadius, buttonPaint)
        canvas.drawCircle(centerX, zoomInCenterY, buttonRadius, buttonBorderPaint)
        canvas.drawText("+", centerX, zoomInCenterY + textPaint.textSize / 3, textPaint)
    }

    /**
     * Handles touch events on the zoom controls.
     *
     * @param x Touch x coordinate
     * @param y Touch y coordinate
     * @return 1 for zoom in, -1 for zoom out, 0 for no action
     */
    fun handleTouch(
        x: Float,
        y: Float,
    ): Int {
        if (zoomInButtonBounds.contains(x, y)) {
            return 1 // Zoom in
        }
        if (zoomOutButtonBounds.contains(x, y)) {
            return -1 // Zoom out
        }
        return 0 // No action
    }
}
