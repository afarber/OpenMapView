/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

/**
 * Factory for creating default marker icons with customizable colors.
 */
internal object MarkerIconFactory {
    private const val DEFAULT_WIDTH = 48
    private const val DEFAULT_HEIGHT = 72
    private const val MAX_CACHE_SIZE = 10

    // LRU cache for colored marker icons (hue -> bitmap)
    private val iconCache = LinkedHashMap<Float, Bitmap>(MAX_CACHE_SIZE + 1, 0.75f, true)

    /**
     * Creates or returns a cached marker icon with the specified hue.
     *
     * @param hue The hue value (0-360) on the HSV color wheel. Defaults to 0 (red).
     * @return A bitmap of the colored marker icon
     */
    fun getDefaultIcon(hue: Float = 0f): Bitmap {
        // Normalize hue to 0-360 range
        val normalizedHue = hue % 360f

        // Return cached icon if available
        iconCache[normalizedHue]?.let { return it }

        // Create new icon
        val bitmap = createMarkerIcon(normalizedHue)

        // Add to cache with LRU eviction
        if (iconCache.size >= MAX_CACHE_SIZE) {
            val firstKey = iconCache.keys.first()
            iconCache.remove(firstKey)?.recycle()
        }
        iconCache[normalizedHue] = bitmap

        return bitmap
    }

    /**
     * Creates a marker icon bitmap with the specified hue.
     */
    private fun createMarkerIcon(hue: Float): Bitmap {
        val bitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Convert HSV to RGB
        val mainColor = Color.HSVToColor(floatArrayOf(hue, 1.0f, 1.0f))
        val borderColor = Color.HSVToColor(floatArrayOf(hue, 1.0f, 0.6f)) // Darker border

        val markerPaint =
            Paint().apply {
                style = Paint.Style.FILL
                color = mainColor
                isAntiAlias = true
            }

        val borderPaint =
            Paint().apply {
                style = Paint.Style.STROKE
                color = borderColor
                strokeWidth = 3f
                isAntiAlias = true
            }

        val centerPaint =
            Paint().apply {
                style = Paint.Style.FILL
                color = Color.WHITE
                isAntiAlias = true
            }

        // Draw teardrop shape
        val path = Path()
        val centerX = DEFAULT_WIDTH / 2f
        val circleRadius = DEFAULT_WIDTH / 2f - 4f
        val circleBottom = circleRadius * 2 + 4f

        // Create teardrop using circle + triangle
        path.addCircle(centerX, circleRadius + 2f, circleRadius, Path.Direction.CW)

        // Add the pointy bottom
        path.moveTo(centerX - circleRadius * 0.5f, circleBottom - 2f)
        path.lineTo(centerX, DEFAULT_HEIGHT.toFloat() - 2f)
        path.lineTo(centerX + circleRadius * 0.5f, circleBottom - 2f)
        path.close()

        // Draw the marker
        canvas.drawPath(path, markerPaint)
        canvas.drawPath(path, borderPaint)

        // Draw white center circle
        canvas.drawCircle(centerX, circleRadius + 2f, circleRadius * 0.4f, centerPaint)

        return bitmap
    }

    /**
     * Clears all cached icons to free memory.
     */
    fun clearCache() {
        iconCache.values.forEach { it.recycle() }
        iconCache.clear()
    }
}
