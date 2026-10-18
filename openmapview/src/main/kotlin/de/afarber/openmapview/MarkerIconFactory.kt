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
import androidx.core.graphics.createBitmap
import kotlin.math.roundToInt

/**
 * Factory for creating default marker icons with customizable colors.
 */
internal object MarkerIconFactory {
    // Icon size in dp, close to the default marker of the Maps SDK
    private const val DEFAULT_WIDTH_DP = 28f
    private const val DEFAULT_HEIGHT_DP = 42f
    private const val MAX_CACHE_SIZE = 10

    // LRU cache for colored marker icons ((hue, density) -> bitmap)
    private val iconCache = LinkedHashMap<Pair<Float, Float>, Bitmap>(MAX_CACHE_SIZE + 1, 0.75f, true)

    /**
     * Creates or returns a cached marker icon with the specified hue.
     *
     * @param hue The hue value (0-360) on the HSV color wheel. Defaults to 0 (red).
     * @param density The screen density, the icon is 28x42 dp
     * @return A bitmap of the colored marker icon
     */
    fun getDefaultIcon(
        hue: Float = 0f,
        density: Float = 1f,
    ): Bitmap {
        // Normalize hue to 0-360 range, icons of each density are cached separately
        val key = Pair(hue % 360f, density)

        // Return cached icon if available. If a recycled bitmap somehow ended up in cache,
        // drop it and regenerate below.
        iconCache[key]?.let { cached ->
            if (!cached.isRecycled) {
                return cached
            }
            iconCache.remove(key)
        }

        // Create new icon
        val bitmap = createMarkerIcon(key.first, density)

        // Add to cache with LRU eviction.
        // Do not call recycle() here: returned icons may still be used by active map instances.
        if (iconCache.size >= MAX_CACHE_SIZE) {
            val firstKey = iconCache.keys.first()
            iconCache.remove(firstKey)
        }
        iconCache[key] = bitmap

        return bitmap
    }

    /**
     * Creates a marker icon bitmap with the specified hue.
     */
    private fun createMarkerIcon(
        hue: Float,
        density: Float,
    ): Bitmap {
        val width = (DEFAULT_WIDTH_DP * density).roundToInt().coerceAtLeast(1)
        val height = (DEFAULT_HEIGHT_DP * density).roundToInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
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
                strokeWidth = 1.5f * density
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
        val inset = 1.5f * density
        val centerX = width / 2f
        val circleRadius = width / 2f - 2 * inset
        val circleBottom = circleRadius * 2 + 2 * inset

        // Create teardrop using circle + triangle
        path.addCircle(centerX, circleRadius + inset, circleRadius, Path.Direction.CW)

        // Add the pointy bottom
        path.moveTo(centerX - circleRadius * 0.5f, circleBottom - inset)
        path.lineTo(centerX, height - inset)
        path.lineTo(centerX + circleRadius * 0.5f, circleBottom - inset)
        path.close()

        // Draw the marker
        canvas.drawPath(path, markerPaint)
        canvas.drawPath(path, borderPaint)

        // Draw white center circle
        canvas.drawCircle(centerX, circleRadius + inset, circleRadius * 0.4f, centerPaint)

        return bitmap
    }

    /**
     * Clears all cached icons by dropping references only.
     *
     * We intentionally avoid recycling cached bitmaps because references can still be held
     * by active map instances and recycled bitmaps would crash canvas drawing.
     */
    fun clearCache() {
        iconCache.clear()
    }
}
