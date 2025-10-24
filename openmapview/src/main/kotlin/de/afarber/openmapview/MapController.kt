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

    companion object {
        private const val MIN_ZOOM = 2.0
        private const val MAX_ZOOM = 19.0
    }

    private val markers = mutableListOf<Marker>()
    private val defaultMarkerIcon by lazy { MarkerIconFactory.getDefaultIcon() }
    var onMarkerClickListener: ((Marker) -> Boolean)? = null

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
        zoom = z.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    fun getZoom(): Double = zoom

    fun zoom(
        scaleFactor: Float,
        focusX: Float,
        focusY: Float,
    ) {
        val oldZoom = zoom
        val newZoom = (zoom * scaleFactor).coerceIn(MIN_ZOOM, MAX_ZOOM)

        if (oldZoom == newZoom) return // Already at limit

        zoom = newZoom

        // Adjust center to zoom towards focus point
        val zoomRatio = (newZoom / oldZoom).toFloat()
        val centerPixelX = viewWidth / 2f + panOffsetX
        val centerPixelY = viewHeight / 2f + panOffsetY

        val dx = (focusX - centerPixelX) * (1 - zoomRatio)
        val dy = (focusY - centerPixelY) * (1 - zoomRatio)

        panOffsetX += dx
        panOffsetY += dy
    }

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

    fun draw(canvas: Canvas) {
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

        // Draw markers on top of tiles
        drawMarkers(canvas, centerPixelX, centerPixelY)
    }

    private fun drawMarkers(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
    ) {
        for (marker in markers) {
            // Convert marker position to pixel coordinates
            val (markerPixelX, markerPixelY) = Projection.latLngToPixel(marker.position, zoom.toInt())

            // Calculate screen position
            val screenX = (markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            // Get marker icon
            val icon = marker.icon ?: defaultMarkerIcon

            // Apply anchor point
            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            // Draw the marker
            canvas.drawBitmap(icon, screenX - anchorX, screenY - anchorY, null)
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

    fun addMarker(marker: Marker): Marker {
        markers.add(marker)
        return marker
    }

    fun removeMarker(marker: Marker): Boolean = markers.remove(marker)

    fun clearMarkers() {
        markers.clear()
    }

    fun getMarkers(): List<Marker> = markers.toList()

    fun handleMarkerTouch(
        x: Float,
        y: Float,
    ): Marker? {
        val (centerPixelX, centerPixelY) = Projection.latLngToPixel(center, zoom.toInt())

        // Check markers in reverse order (top to bottom) for correct z-ordering
        for (marker in markers.reversed()) {
            val (markerPixelX, markerPixelY) = Projection.latLngToPixel(marker.position, zoom.toInt())

            val screenX = (markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val icon = marker.icon ?: defaultMarkerIcon
            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            val markerLeft = screenX - anchorX
            val markerTop = screenY - anchorY
            val markerRight = markerLeft + icon.width
            val markerBottom = markerTop + icon.height

            if (x >= markerLeft && x <= markerRight && y >= markerTop && y <= markerBottom) {
                return marker
            }
        }
        return null
    }

    fun onResume() {}

    fun onPause() {}

    fun onDestroy() {
        scope.cancel()
        tileDownloader.close()
        tileCache.clear()
        MarkerIconFactory.clearCache()
    }
}
