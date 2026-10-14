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
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.graphics.withTranslation
import kotlin.math.ceil

/**
 * Attribution overlay displaying tile source copyright notice.
 *
 * Required by tile provider usage policies to show attribution on all maps.
 * Renders a semi-transparent background with clickable attribution text in the bottom-right corner.
 * Text wider than the view wraps onto more lines, so no part of the attribution is cut off.
 */
class AttributionOverlay(
    context: Context,
) {
    private var attributionText = "© OpenStreetMap contributors"
    private var attributionUrl = "https://www.openstreetmap.org/copyright"

    private val textPaint =
        TextPaint().apply {
            color = Color.BLACK
            textSize = 12f * context.resources.displayMetrics.density
            isAntiAlias = true
        }

    private val backgroundPaint =
        Paint().apply {
            color = Color.argb(180, 255, 255, 255)
            style = Paint.Style.FILL
        }

    private val padding = (4 * context.resources.displayMetrics.density).toInt()

    // Rebuilt only when the text or the view width changes
    private var layout: StaticLayout? = null
    private var layoutViewWidth = -1

    // Reused by bounds() to avoid allocating on every draw
    private val box = Rect()

    var onAttributionClickListener: (() -> Unit)? = null

    /**
     * Sets the attribution text to be displayed.
     *
     * @param text The attribution text (e.g., tile source copyright notice)
     */
    fun setAttributionText(text: String) {
        attributionText = text
        layout = null
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

    /**
     * Returns the text laid out right-aligned, at most as wide as the view minus padding.
     */
    private fun layoutFor(viewWidth: Int): StaticLayout {
        layout?.let { if (layoutViewWidth == viewWidth) return it }

        val maxWidth = (viewWidth - padding * 2).coerceAtLeast(1)
        val desiredWidth = ceil(Layout.getDesiredWidth(attributionText, textPaint)).toInt()
        val width = desiredWidth.coerceIn(1, maxWidth)
        return StaticLayout.Builder
            .obtain(attributionText, 0, attributionText.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setIncludePad(false)
            .build()
            .also {
                layout = it
                layoutViewWidth = viewWidth
            }
    }

    /**
     * Returns the background box of the attribution in the bottom-right corner of the view.
     * The returned [Rect] is reused, so it is only valid until the next call.
     */
    internal fun bounds(
        viewWidth: Int,
        viewHeight: Int,
    ): Rect {
        val textLayout = layoutFor(viewWidth)
        box.set(
            viewWidth - textLayout.width - padding * 2,
            viewHeight - textLayout.height - padding * 2,
            viewWidth,
            viewHeight,
        )
        return box
    }

    /**
     * Returns the number of lines the attribution takes in a view of the given width.
     */
    internal fun lineCount(viewWidth: Int): Int = layoutFor(viewWidth).lineCount

    fun draw(
        canvas: Canvas,
        viewWidth: Int,
        viewHeight: Int,
    ) {
        val box = bounds(viewWidth, viewHeight)
        canvas.drawRect(box, backgroundPaint)

        canvas.withTranslation((box.left + padding).toFloat(), (box.top + padding).toFloat()) {
            layoutFor(viewWidth).draw(this)
        }
    }

    fun handleTouch(
        x: Float,
        y: Float,
        viewWidth: Int,
        viewHeight: Int,
    ): Boolean {
        val box = bounds(viewWidth, viewHeight)

        if (x >= box.left && x <= box.right && y >= box.top && y <= box.bottom) {
            onAttributionClickListener?.invoke()
            return true
        }
        return false
    }
}
