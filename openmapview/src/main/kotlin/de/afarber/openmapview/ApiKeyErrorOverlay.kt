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
import android.graphics.Rect
import android.text.TextPaint

/**
 * Overlay displayed when a map type requires an API key that is not configured.
 *
 * Shows a translucent gray overlay covering the map with an error message
 * positioned at the view center (screen center). The message remains fixed
 * at screen center regardless of map panning.
 *
 * Touch events pass through this overlay, allowing users to pan and zoom
 * the map even when the error is displayed.
 *
 * @param context Android context for accessing resources
 * @param providerName Human-readable name of the tile provider (e.g., "Thunderforest", "Tracestrack")
 * @param mapTypeName Human-readable name of the map type (e.g., "Cycle Map", "Transport Map")
 */
class ApiKeyErrorOverlay(
    context: Context,
    private val providerName: String,
    private val mapTypeName: String,
) {
    private val density = context.resources.displayMetrics.density

    // Semi-transparent gray overlay (30% opacity)
    private val overlayPaint =
        Paint().apply {
            color = Color.argb((255 * 0.3).toInt(), 128, 128, 128)
            style = Paint.Style.FILL
        }

    // Background for text (slightly darker, more opaque)
    private val textBackgroundPaint =
        Paint().apply {
            color = Color.argb((255 * 0.8).toInt(), 80, 80, 80)
            style = Paint.Style.FILL
            setShadowLayer(4f * density, 0f, 2f * density, Color.argb(128, 0, 0, 0))
        }

    // White text for readability
    private val titleTextPaint =
        TextPaint().apply {
            color = Color.WHITE
            textSize = 18f * density
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }

    private val bodyTextPaint =
        TextPaint().apply {
            color = Color.WHITE
            textSize = 14f * density
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

    private val linkTextPaint =
        TextPaint().apply {
            color = Color.argb(255, 100, 180, 255) // Light blue for link
            textSize = 13f * density
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            isUnderlineText = true
        }

    private val padding = 16f * density
    private val lineSpacing = 8f * density
    private val cornerRadius = 8f * density

    // Text content
    private val title = "API Key Required"
    private val message = "$mapTypeName requires an API key from $providerName."
    private val instruction = "Configure key in AndroidManifest.xml or via ApiKeyManager."
    private val learnMore = "See docs/API_KEYS.md for details"

    // Text bounds for measuring
    private val titleBounds = Rect()
    private val messageBounds = Rect()
    private val instructionBounds = Rect()
    private val learnMoreBounds = Rect()

    init {
        // Measure text dimensions
        titleTextPaint.getTextBounds(title, 0, title.length, titleBounds)
        bodyTextPaint.getTextBounds(message, 0, message.length, messageBounds)
        bodyTextPaint.getTextBounds(instruction, 0, instruction.length, instructionBounds)
        linkTextPaint.getTextBounds(learnMore, 0, learnMore.length, learnMoreBounds)
    }

    /**
     * Draws the error overlay on the canvas.
     *
     * The overlay covers the entire view with semi-transparent gray, and displays
     * an error message box centered at the view center (screen center).
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
        // Draw semi-transparent overlay over entire view
        canvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), overlayPaint)

        // Calculate message box dimensions
        val maxTextWidth =
            maxOf(
                titleBounds.width(),
                messageBounds.width(),
                instructionBounds.width(),
                learnMoreBounds.width(),
            )
        val boxWidth = maxTextWidth + padding * 2
        val boxHeight =
            titleBounds.height() + lineSpacing +
                messageBounds.height() + lineSpacing +
                instructionBounds.height() + lineSpacing +
                learnMoreBounds.height() + padding * 2

        // Center the message box at view center
        val centerX = viewWidth / 2f
        val centerY = viewHeight / 2f
        val boxLeft = centerX - boxWidth / 2
        val boxTop = centerY - boxHeight / 2
        val boxRight = centerX + boxWidth / 2
        val boxBottom = centerY + boxHeight / 2

        // Draw message box background with rounded corners
        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, cornerRadius, cornerRadius, textBackgroundPaint)

        // Draw text lines
        var currentY = boxTop + padding + titleBounds.height()

        // Title
        canvas.drawText(title, centerX, currentY, titleTextPaint)
        currentY += lineSpacing + messageBounds.height()

        // Message
        canvas.drawText(message, centerX, currentY, bodyTextPaint)
        currentY += lineSpacing + instructionBounds.height()

        // Instruction
        canvas.drawText(instruction, centerX, currentY, bodyTextPaint)
        currentY += lineSpacing + learnMoreBounds.height()

        // Learn more link
        canvas.drawText(learnMore, centerX, currentY, linkTextPaint)
    }

    /**
     * Returns true if this overlay should handle touch events.
     *
     * Always returns false to allow touch events to pass through to the map,
     * enabling pan and zoom even when the error is displayed.
     */
    fun handleTouch(): Boolean = false
}
