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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MapController(
    private val context: Context,
) {
    private var zoom = 10.0
    private var center = LatLng(0.0, 0.0)
    private var viewWidth = 0
    private var viewHeight = 0
    private var panOffsetX = 0f
    private var panOffsetY = 0f

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val tileDownloader = TileDownloader()
    private val tileCache = TileCache()
    private var tileSource = TileSource.STANDARD
    private val downloadingTiles = mutableSetOf<TileCoordinate>()
    private var onTileLoadedCallback: (() -> Unit)? = null

    private val tileBorderPaint =
        Paint().apply {
            style = Paint.Style.STROKE
            color = Color.GRAY
            strokeWidth = 2f
        }

    private val tileTextPaint =
        Paint().apply {
            color = Color.BLACK
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }

    private val tilePlaceholderPaint =
        Paint().apply {
            style = Paint.Style.FILL
            color = Color.LTGRAY
        }

    fun setZoom(z: Double) {
        zoom = z
    }

    fun getZoom(): Double = zoom

    fun setCenter(latLng: LatLng) {
        center = latLng
    }

    fun setViewSize(
        width: Int,
        height: Int,
    ) {
        viewWidth = width
        viewHeight = height
    }

    fun setOnTileLoadedCallback(callback: () -> Unit) {
        onTileLoadedCallback = callback
    }

    fun updatePanOffset(
        dx: Float,
        dy: Float,
    ) {
        panOffsetX -= dx
        panOffsetY -= dy
    }

    fun commitPan() {
        if (panOffsetX == 0f && panOffsetY == 0f) return

        // Convert accumulated pan offset to new center
        val (centerPixelX, centerPixelY) = Projection.latLngToPixel(center, zoom.toInt())
        val newCenterPixelX = (centerPixelX + panOffsetX).toInt()
        val newCenterPixelY = (centerPixelY + panOffsetY).toInt()

        center = Projection.pixelToLatLng(newCenterPixelX, newCenterPixelY, zoom.toInt())

        // Reset pan offset
        panOffsetX = 0f
        panOffsetY = 0f
    }

    fun draw(canvas: Canvas?) {
        canvas ?: return
        if (viewWidth <= 0 || viewHeight <= 0) return

        // Get visible tiles
        val visibleTiles =
            ViewportCalculator.getVisibleTiles(
                center,
                zoom.toInt(),
                viewWidth,
                viewHeight,
                panOffsetX,
                panOffsetY,
            )

        // Calculate center pixel position
        val (centerPixelX, centerPixelY) = Projection.latLngToPixel(center, zoom.toInt())

        // Draw each tile
        for (tile in visibleTiles) {
            val (tilePixelX, tilePixelY) = Projection.tileToPixel(tile)

            // Calculate screen position
            val screenX = (tilePixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (tilePixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            // Check if tile is in cache
            val cachedBitmap = tileCache.get(tile)
            if (cachedBitmap != null) {
                // Draw cached bitmap
                canvas.drawBitmap(cachedBitmap, screenX, screenY, null)
            } else {
                // Draw placeholder
                canvas.drawRect(screenX, screenY, screenX + 256, screenY + 256, tilePlaceholderPaint)
                canvas.drawRect(screenX, screenY, screenX + 256, screenY + 256, tileBorderPaint)

                // Start downloading if not already in progress
                if (!downloadingTiles.contains(tile)) {
                    downloadingTiles.add(tile)
                    downloadTile(tile)
                }
            }
        }
    }

    private fun downloadTile(tile: TileCoordinate) {
        scope.launch(Dispatchers.IO) {
            val url = tileSource.getTileUrl(tile)
            val bitmap = tileDownloader.downloadTile(url)
            if (bitmap != null) {
                tileCache.put(tile, bitmap)
                downloadingTiles.remove(tile)
                // Trigger redraw on main thread
                launch(Dispatchers.Main) {
                    onTileLoadedCallback?.invoke()
                }
            } else {
                downloadingTiles.remove(tile)
            }
        }
    }

    fun onResume() {}

    fun onPause() {}

    fun onDestroy() {
        scope.cancel()
        tileDownloader.close()
        tileCache.clear()
    }
}
