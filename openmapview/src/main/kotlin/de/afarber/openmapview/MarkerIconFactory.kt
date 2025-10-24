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
 * Factory for creating default marker icons.
 */
internal object MarkerIconFactory {
    private const val DEFAULT_WIDTH = 48
    private const val DEFAULT_HEIGHT = 72
    private var cachedDefaultIcon: Bitmap? = null

    /**
     * Creates or returns a cached default marker icon.
     * The icon is a red teardrop shape with a white circle in the center.
     */
    fun getDefaultIcon(): Bitmap {
        cachedDefaultIcon?.let { return it }

        val bitmap = Bitmap.createBitmap(DEFAULT_WIDTH, DEFAULT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val markerPaint =
            Paint().apply {
                style = Paint.Style.FILL
                color = Color.RED
                isAntiAlias = true
            }

        val borderPaint =
            Paint().apply {
                style = Paint.Style.STROKE
                color = Color.parseColor("#8B0000") // Dark red
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

        cachedDefaultIcon = bitmap
        return bitmap
    }

    /**
     * Clears the cached default icon to free memory.
     */
    fun clearCache() {
        cachedDefaultIcon?.recycle()
        cachedDefaultIcon = null
    }
}
