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

    private var lastDrawnTiles = mutableSetOf<TileCoordinate>()

    companion object {
        private const val MIN_ZOOM = 2.0
        private const val MAX_ZOOM = 19.0
        private const val TILE_SIZE = 256f
    }

    private val markers = mutableListOf<Marker>()
    private val defaultMarkerIcon by lazy { MarkerIconFactory.getDefaultIcon() }
    var onMarkerClickListener: ((Marker) -> Boolean)? = null

    private val polylines = mutableListOf<Polyline>()
    private val polygons = mutableListOf<Polygon>()

    private var animationJob: Job? = null
    private var animationListener: OnCameraAnimationListener? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val tileDownloader = TileDownloader()
    private val tileCache = TileCache(context)
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

    fun getCenter(): LatLng = center

    fun getCameraPosition(): CameraPosition =
        CameraPosition(
            target = center,
            zoom = zoom,
        )

    fun moveCamera(cameraUpdate: CameraUpdate) {
        stopAnimation()
        commitPan()
        applyCameraUpdate(cameraUpdate)
    }

    fun animateCamera(
        cameraUpdate: CameraUpdate,
        durationMs: Int = 250,
        listener: OnCameraAnimationListener? = null,
    ) {
        stopAnimation()
        commitPan()

        val startPosition = getCameraPosition()
        val targetPosition = calculateTargetPosition(cameraUpdate, startPosition)

        animationListener = listener
        animationJob =
            scope.launch {
                try {
                    val startTime = System.currentTimeMillis()
                    val endTime = startTime + durationMs

                    while (System.currentTimeMillis() < endTime) {
                        val currentTime = System.currentTimeMillis()
                        val progress = ((currentTime - startTime).toFloat() / durationMs).coerceIn(0f, 1f)

                        val interpolatedLat =
                            interpolate(
                                startPosition.target.latitude,
                                targetPosition.target.latitude,
                                progress,
                            )
                        val interpolatedLng =
                            interpolate(
                                startPosition.target.longitude,
                                targetPosition.target.longitude,
                                progress,
                            )
                        val interpolatedZoom =
                            interpolate(
                                startPosition.zoom,
                                targetPosition.zoom,
                                progress,
                            )

                        center = LatLng(interpolatedLat, interpolatedLng)
                        zoom = interpolatedZoom

                        onTileLoadedCallback?.invoke()

                        kotlinx.coroutines.delay(16)
                    }

                    center = targetPosition.target
                    zoom = targetPosition.zoom
                    onTileLoadedCallback?.invoke()

                    animationListener?.onFinish()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    animationListener?.onCancel()
                    throw e
                } finally {
                    animationJob = null
                    animationListener = null
                }
            }
    }

    fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        animationListener = null
    }

    private fun applyCameraUpdate(cameraUpdate: CameraUpdate) {
        val targetPosition = calculateTargetPosition(cameraUpdate, getCameraPosition())
        center = targetPosition.target
        zoom = targetPosition.zoom
    }

    private fun calculateTargetPosition(
        cameraUpdate: CameraUpdate,
        currentPosition: CameraPosition,
    ): CameraPosition =
        when (cameraUpdate) {
            is CameraUpdate.NewLatLng ->
                CameraPosition(
                    target = cameraUpdate.target,
                    zoom = currentPosition.zoom,
                )
            is CameraUpdate.NewLatLngZoom ->
                CameraPosition(
                    target = cameraUpdate.target,
                    zoom = cameraUpdate.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM),
                )
            is CameraUpdate.NewCameraPosition ->
                cameraUpdate.position.copy(
                    zoom = cameraUpdate.position.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM),
                )
            is CameraUpdate.ZoomIn ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom + 1.0).coerceIn(MIN_ZOOM, MAX_ZOOM),
                )
            is CameraUpdate.ZoomOut ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom - 1.0).coerceIn(MIN_ZOOM, MAX_ZOOM),
                )
            is CameraUpdate.ZoomTo ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = cameraUpdate.zoom.coerceIn(MIN_ZOOM, MAX_ZOOM),
                )
            is CameraUpdate.ZoomBy ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom + cameraUpdate.amount).coerceIn(MIN_ZOOM, MAX_ZOOM),
                )
        }

    private fun interpolate(
        start: Double,
        end: Double,
        progress: Float,
    ): Double = start + (end - start) * progress

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
        if (viewWidth <= 0 || viewHeight <= 0) {
            return
        }

        val visibleTiles =
            ViewportCalculator.getVisibleTiles(
                center,
                zoom.toInt(),
                viewWidth,
                viewHeight,
                panOffsetX,
                panOffsetY,
            )

        val (centerPixelX, centerPixelY) = Projection.latLngToPixel(center, zoom.toInt())

        for (tile in visibleTiles) {
            val (tilePixelX, tilePixelY) = Projection.tileToPixel(tile)

            val screenX = (tilePixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (tilePixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val cachedBitmap = tileCache.get(tile)
            if (cachedBitmap != null) {
                canvas.drawBitmap(cachedBitmap, screenX, screenY, null)
            } else {
                canvas.drawRect(screenX, screenY, screenX + TILE_SIZE, screenY + TILE_SIZE, tilePlaceholderPaint)
                canvas.drawRect(screenX, screenY, screenX + TILE_SIZE, screenY + TILE_SIZE, tileBorderPaint)

                if (!downloadingTiles.contains(tile)) {
                    downloadingTiles.add(tile)
                    downloadTile(tile)
                }
            }
        }

        // Prefetch adjacent tiles only when viewport changes to avoid excessive downloads
        if (lastDrawnTiles != visibleTiles.toSet()) {
            prefetchAdjacentTiles(visibleTiles)
            lastDrawnTiles = visibleTiles.toMutableSet()
        }

        drawPolygons(canvas, centerPixelX, centerPixelY)
        drawPolylines(canvas, centerPixelX, centerPixelY)
        drawMarkers(canvas, centerPixelX, centerPixelY)
    }

    /**
     * Prefetch tiles adjacent to the visible viewport for smoother panning.
     *
     * Downloads a 2-tile buffer (512px) around the visible area. These low-priority
     * downloads run in the background and don't trigger redraws to avoid performance impact.
     * Only prefetches when the viewport changes to prevent excessive downloads during
     * continuous panning.
     */
    private fun prefetchAdjacentTiles(visibleTiles: List<TileCoordinate>) {
        // Calculate tiles in a buffer zone: add 2 tiles (512px) in each direction
        val prefetchTiles =
            ViewportCalculator.getVisibleTiles(
                center,
                zoom.toInt(),
                viewWidth + 512,
                viewHeight + 512,
                panOffsetX,
                panOffsetY,
            )

        // Download tiles that are adjacent but not yet visible
        for (tile in prefetchTiles) {
            if (!visibleTiles.contains(tile) && !downloadingTiles.contains(tile) && tileCache.get(tile) == null) {
                downloadingTiles.add(tile)
                downloadTile(tile, lowPriority = true)
            }
        }
    }

    private fun drawPolylines(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
    ) {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.isAntiAlias = true

        for (polyline in polylines) {
            if (polyline.points.size < 2) continue

            paint.color = polyline.strokeColor
            paint.strokeWidth = polyline.strokeWidth

            val path = android.graphics.Path()
            var isFirst = true

            for (point in polyline.points) {
                val (pixelX, pixelY) = Projection.latLngToPixel(point, zoom.toInt())
                val screenX = (pixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                val screenY = (pixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

                if (isFirst) {
                    path.moveTo(screenX, screenY)
                    isFirst = false
                } else {
                    path.lineTo(screenX, screenY)
                }
            }

            canvas.drawPath(path, paint)
        }
    }

    private fun drawPolygons(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
    ) {
        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.isAntiAlias = true

        val strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokePaint.isAntiAlias = true

        for (polygon in polygons) {
            if (polygon.points.size < 3) continue

            fillPaint.color = polygon.fillColor
            strokePaint.color = polygon.strokeColor
            strokePaint.strokeWidth = polygon.strokeWidth

            val path = android.graphics.Path()
            path.fillType = android.graphics.Path.FillType.EVEN_ODD

            // Draw main polygon outline
            var isFirst = true
            for (point in polygon.points) {
                val (pixelX, pixelY) = Projection.latLngToPixel(point, zoom.toInt())
                val screenX = (pixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                val screenY = (pixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

                if (isFirst) {
                    path.moveTo(screenX, screenY)
                    isFirst = false
                } else {
                    path.lineTo(screenX, screenY)
                }
            }
            path.close()

            // Draw holes
            for (hole in polygon.holes) {
                if (hole.size < 3) continue
                isFirst = true
                for (point in hole) {
                    val (pixelX, pixelY) = Projection.latLngToPixel(point, zoom.toInt())
                    val screenX = (pixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                    val screenY = (pixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

                    if (isFirst) {
                        path.moveTo(screenX, screenY)
                        isFirst = false
                    } else {
                        path.lineTo(screenX, screenY)
                    }
                }
                path.close()
            }

            // Draw fill first, then stroke
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
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

    private fun downloadTile(
        tile: TileCoordinate,
        lowPriority: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            val url = tileSource.getTileUrl(tile)
            val bitmap = tileDownloader.downloadTile(url)
            if (bitmap != null) {
                tileCache.put(tile, bitmap)
                downloadingTiles.remove(tile)
                // Only trigger redraw for visible tiles to avoid excessive invalidations
                // during prefetching
                if (!lowPriority) {
                    launch(Dispatchers.Main) {
                        onTileLoadedCallback?.invoke()
                    }
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

    fun addPolyline(polyline: Polyline): Polyline {
        polylines.add(polyline)
        return polyline
    }

    fun removePolyline(polyline: Polyline): Boolean = polylines.remove(polyline)

    fun clearPolylines() {
        polylines.clear()
    }

    fun getPolylines(): List<Polyline> = polylines.toList()

    fun addPolygon(polygon: Polygon): Polygon {
        polygons.add(polygon)
        return polygon
    }

    fun removePolygon(polygon: Polygon): Boolean = polygons.remove(polygon)

    fun clearPolygons() {
        polygons.clear()
    }

    fun getPolygons(): List<Polygon> = polygons.toList()

    fun addGeoJson(geoJsonString: String): GeoJsonResult {
        val result = GeoJsonParser.parse(geoJsonString)
        result.markers.forEach { addMarker(it) }
        result.polylines.forEach { addPolyline(it) }
        result.polygons.forEach { addPolygon(it) }
        return result
    }

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

    fun onResume() {
        // Called when app comes to foreground
        // Could be used to resume tile downloads if paused
    }

    fun onPause() {
        // Called when app goes to background
        // Could be used to pause tile downloads to save battery
    }

    fun onDestroy() {
        // Clean up resources to prevent memory leaks
        scope.cancel()
        tileDownloader.close()
        tileCache.close()
        MarkerIconFactory.clearCache()
    }
}
