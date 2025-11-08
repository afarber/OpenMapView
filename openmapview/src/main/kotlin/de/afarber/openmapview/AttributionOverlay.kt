/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.TextPaint

/**
 * Attribution overlay displaying tile source copyright notice.
 *
 * Required by tile provider usage policies to show attribution on all maps.
 * Renders a semi-transparent background with clickable attribution text in the bottom-right corner.
 */
class AttributionOverlay(
    context: Context,
) {
    private var attributionText = "© OpenStreetMap contributors"
    private var attributionUrl = "https://www.openstreetmap.org/copyright"

    private val density = context.resources.displayMetrics.density

    private val textPaint =
        TextPaint().apply {
            color = Color.BLACK
            textSize = 12f * density
            isAntiAlias = true
        }

    private val glowPaint =
        TextPaint().apply {
            color = Color.WHITE
            textSize = 12f * density
            isAntiAlias = true
            maskFilter = BlurMaskFilter(5f * density, BlurMaskFilter.Blur.NORMAL)
        }

    private val backgroundPaint =
        Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            style = Paint.Style.FILL
        }

    private var textBounds = Rect()
    private val padding = (4 * density).toInt()

    var isGlowEnabled: Boolean = true

    var onAttributionClickListener: (() -> Unit)? = null

    init {
        updateTextBounds()
    }

    /**
     * Sets the attribution text to be displayed.
     *
     * @param text The attribution text (e.g., tile source copyright notice)
     */
    fun setAttributionText(text: String) {
        attributionText = text
        updateTextBounds()
    }

    /**
     * Sets the attribution URL.
     *
     * @param url The URL to the attribution/copyright page
     */
    fun setAttributionUrl(url: String) {
        attributionUrl = url
    }

    /**
     * Gets the current attribution URL.
     *
     * @return The URL to the attribution/copyright page
     */
    fun getAttributionUrl(): String = attributionUrl

    private fun updateTextBounds() {
        textPaint.getTextBounds(attributionText, 0, attributionText.length, textBounds)
    }

    fun draw(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
    ) {
        val textWidth = textBounds.width()
        val textHeight = textBounds.height()

        val bgLeft = viewWidth - textWidth - padding * 2
        val bgTop = viewHeight - textHeight - padding * 2
        val bgRight = viewWidth
        val bgBottom = viewHeight

        canvas.drawRect(bgLeft.toFloat(), bgTop.toFloat(), bgRight.toFloat(), bgBottom.toFloat(), backgroundPaint)

        val textX = viewWidth - textWidth - padding
        val textY = viewHeight - padding

        if (isGlowEnabled) {
            canvas.drawText(attributionText, textX.toFloat(), textY.toFloat(), glowPaint)
        }
        canvas.drawText(attributionText, textX.toFloat(), textY.toFloat(), textPaint)
    }

    fun handleTouch(
        x: Float,
        y: Float,
        viewWidth: Int,
        viewHeight: Int,
    ): Boolean {
        val textWidth = textBounds.width()
        val textHeight = textBounds.height()

        val bgLeft = viewWidth - textWidth - padding * 2
        val bgTop = viewHeight - textHeight - padding * 2
        val bgRight = viewWidth
        val bgBottom = viewHeight

        if (x >= bgLeft && x <= bgRight && y >= bgTop && y <= bgBottom) {
            onAttributionClickListener?.invoke()
            return true
        }
        return false
    }
}
