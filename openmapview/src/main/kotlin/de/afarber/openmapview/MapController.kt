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

/**
 * Core controller managing map state, rendering, and interactions.
 *
 * Handles map positioning (zoom, center), tile management, marker rendering,
 * shape drawing, camera animations, and touch event processing.
 *
 * This class is used internally by [OpenMapView] and is not part of the public API.
 */
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
        private const val DEFAULT_MIN_ZOOM = 2.0
        private const val DEFAULT_MAX_ZOOM = 19.0
        private const val TILE_SIZE = 256f
    }

    private var minZoomPreference = DEFAULT_MIN_ZOOM
    private var maxZoomPreference = DEFAULT_MAX_ZOOM

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

    /**
     * Sets the zoom level, clamping to valid range.
     *
     * @param z The desired zoom level (will be clamped to current min/max zoom preferences)
     */
    fun setZoom(z: Double) {
        zoom = z.coerceIn(minZoomPreference, maxZoomPreference)
    }

    /**
     * Returns the current zoom level.
     *
     * @return The current zoom level
     */
    fun getZoom(): Double = zoom

    /**
     * Sets the minimum zoom level preference.
     *
     * @param minZoom The minimum zoom level (will be clamped to 2.0-19.0)
     */
    fun setMinZoomPreference(minZoom: Float) {
        minZoomPreference = minZoom.toDouble().coerceIn(DEFAULT_MIN_ZOOM, DEFAULT_MAX_ZOOM)
        zoom = zoom.coerceIn(minZoomPreference, maxZoomPreference)
    }

    /**
     * Sets the maximum zoom level preference.
     *
     * @param maxZoom The maximum zoom level (will be clamped to 2.0-19.0)
     */
    fun setMaxZoomPreference(maxZoom: Float) {
        maxZoomPreference = maxZoom.toDouble().coerceIn(DEFAULT_MIN_ZOOM, DEFAULT_MAX_ZOOM)
        zoom = zoom.coerceIn(minZoomPreference, maxZoomPreference)
    }

    /**
     * Returns the current minimum zoom level preference.
     *
     * @return The minimum zoom level
     */
    fun getMinZoomLevel(): Float = minZoomPreference.toFloat()

    /**
     * Returns the current maximum zoom level preference.
     *
     * @return The maximum zoom level
     */
    fun getMaxZoomLevel(): Float = maxZoomPreference.toFloat()

    /**
     * Resets the min/max zoom preferences to their defaults (2.0 - 19.0).
     */
    fun resetMinMaxZoomPreference() {
        minZoomPreference = DEFAULT_MIN_ZOOM
        maxZoomPreference = DEFAULT_MAX_ZOOM
        zoom = zoom.coerceIn(minZoomPreference, maxZoomPreference)
    }

    /**
     * Applies a zoom gesture centered on a specific screen point.
     *
     * Adjusts the zoom level and map center so that the content under
     * the focus point remains stationary during the zoom.
     *
     * @param scaleFactor The zoom scale factor (>1 zooms in, <1 zooms out)
     * @param focusX The screen X coordinate of the zoom center
     * @param focusY The screen Y coordinate of the zoom center
     */
    fun zoom(
        scaleFactor: Float,
        focusX: Float,
        focusY: Float,
    ) {
        val oldZoom = zoom
        val newZoom = (zoom * scaleFactor).coerceIn(minZoomPreference, maxZoomPreference)

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

    /**
     * Sets the map center to the specified location.
     *
     * @param latLng The new center location
     */
    fun setCenter(latLng: LatLng) {
        center = latLng
    }

    /**
     * Returns the current map center.
     *
     * @return The current center location
     */
    fun getCenter(): LatLng = center

    /**
     * Returns the current camera position.
     *
     * @return A CameraPosition containing current target and zoom
     */
    fun getCameraPosition(): CameraPosition =
        CameraPosition(
            target = center,
            zoom = zoom,
        )

    /**
     * Creates a Projection instance for coordinate conversions.
     *
     * The projection captures the current map state (center, zoom, view size, pan offset)
     * and provides methods for converting between screen and geographic coordinates.
     *
     * @return A Projection instance for the current map state
     */
    fun createProjection(): Projection =
        Projection(
            center = center,
            zoom = zoom,
            viewWidth = viewWidth,
            viewHeight = viewHeight,
            panOffsetX = panOffsetX,
            panOffsetY = panOffsetY,
        )

    /**
     * Converts screen coordinates to geographic coordinates.
     *
     * Takes into account the current camera position, zoom level, and pan offset.
     *
     * @param screenX The X coordinate in screen pixels
     * @param screenY The Y coordinate in screen pixels
     * @return The geographic location (LatLng) at that screen position
     */
    fun screenToLatLng(
        screenX: Float,
        screenY: Float,
    ): LatLng {
        // Get center pixel coordinates at current zoom
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        // Convert screen coordinates to pixel coordinates
        // Account for view center offset and pan offset
        val pixelX = (centerPixelX + (screenX - viewWidth / 2 + panOffsetX).toDouble()).toInt()
        val pixelY = (centerPixelY + (screenY - viewHeight / 2 + panOffsetY).toDouble()).toInt()

        // Convert pixel coordinates to LatLng
        return ProjectionUtils.pixelToLatLng(pixelX, pixelY, zoom.toInt())
    }

    /**
     * Moves the camera instantly to a new position.
     *
     * Stops any ongoing animation and commits any pending pan offsets
     * before applying the camera update.
     *
     * @param cameraUpdate The camera update to apply
     */
    fun moveCamera(cameraUpdate: CameraUpdate) {
        stopAnimation()
        commitPan()
        applyCameraUpdate(cameraUpdate)
    }

    /**
     * Animates the camera to a new position.
     *
     * Stops any ongoing animation and commits any pending pan offsets before starting.
     * The animation runs on the main thread and triggers redraws at ~60fps.
     *
     * @param cameraUpdate The camera update to animate to
     * @param durationMs Duration of the animation in milliseconds (default 250ms)
     * @param listener Optional listener for animation completion callbacks
     */
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

    /**
     * Stops any ongoing camera animation.
     *
     * If an animation is running, it will be cancelled and the listener's
     * onCancel() callback will be invoked. The camera remains at its current position.
     */
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
                    zoom = cameraUpdate.zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            is CameraUpdate.NewCameraPosition ->
                cameraUpdate.position.copy(
                    zoom = cameraUpdate.position.zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            is CameraUpdate.ZoomIn ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom + 1.0).coerceIn(minZoomPreference, maxZoomPreference),
                )
            is CameraUpdate.ZoomOut ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom - 1.0).coerceIn(minZoomPreference, maxZoomPreference),
                )
            is CameraUpdate.ZoomTo ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = cameraUpdate.zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            is CameraUpdate.ZoomBy ->
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom + cameraUpdate.amount).coerceIn(minZoomPreference, maxZoomPreference),
                )
        }

    private fun interpolate(
        start: Double,
        end: Double,
        progress: Float,
    ): Double = start + (end - start) * progress

    /**
     * Sets the view dimensions for rendering calculations.
     *
     * Called automatically when the view is resized.
     *
     * @param width The view width in pixels
     * @param height The view height in pixels
     */
    fun setViewSize(
        width: Int,
        height: Int,
    ) {
        viewWidth = width
        viewHeight = height
    }

    /**
     * Sets a callback to be invoked when tiles finish loading.
     *
     * @param callback The callback to invoke when tiles are loaded
     */
    fun setOnTileLoadedCallback(callback: () -> Unit) {
        onTileLoadedCallback = callback
    }

    /**
     * Updates the temporary pan offset during a drag gesture.
     *
     * The offset accumulates until committed via [commitPan].
     *
     * @param dx The horizontal movement in pixels
     * @param dy The vertical movement in pixels
     */
    fun updatePanOffset(
        dx: Float,
        dy: Float,
    ) {
        panOffsetX -= dx
        panOffsetY -= dy
    }

    /**
     * Commits accumulated pan offsets to the map center.
     *
     * Converts the temporary pan offset to a new center location and resets the offset.
     * Called when a pan gesture ends or before camera operations.
     */
    fun commitPan() {
        if (panOffsetX == 0f && panOffsetY == 0f) return

        // Convert accumulated pan offset to new center
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())
        val newCenterPixelX = (centerPixelX + panOffsetX).toInt()
        val newCenterPixelY = (centerPixelY + panOffsetY).toInt()

        center = ProjectionUtils.pixelToLatLng(newCenterPixelX, newCenterPixelY, zoom.toInt())

        // Reset pan offset
        panOffsetX = 0f
        panOffsetY = 0f
    }

    /**
     * Renders the map to the provided canvas.
     *
     * Draws visible tiles, prefetches adjacent tiles, and renders polygons,
     * polylines, and markers in correct z-order.
     *
     * @param canvas The canvas to draw on
     */
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

        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        for (tile in visibleTiles) {
            val (tilePixelX, tilePixelY) = ProjectionUtils.tileToPixel(tile)

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

        // Draw shapes in zIndex order: polygons first, then polylines, then markers
        // Within each type, sort by zIndex (lower zIndex drawn first, higher on top)
        val sortedPolygons = polygons.sortedBy { it.zIndex }
        val sortedPolylines = polylines.sortedBy { it.zIndex }
        val sortedMarkers = markers.sortedBy { it.zIndex }

        // Group all shapes by zIndex and draw in order
        val allZIndices =
            (
                sortedPolygons.map {
                    it.zIndex
                } + sortedPolylines.map { it.zIndex } + sortedMarkers.map { it.zIndex }
            ).distinct().sorted()

        for (z in allZIndices) {
            // Draw polygons at this zIndex
            drawPolygonsByZIndex(canvas, centerPixelX, centerPixelY, z, sortedPolygons)
            // Draw polylines at this zIndex
            drawPolylinesByZIndex(canvas, centerPixelX, centerPixelY, z, sortedPolylines)
            // Draw markers at this zIndex
            drawMarkersByZIndex(canvas, centerPixelX, centerPixelY, z, sortedMarkers)
        }
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
            if (!polyline.visible || polyline.points.size < 2) continue

            paint.color = polyline.strokeColor
            paint.strokeWidth = polyline.strokeWidth

            val path = android.graphics.Path()
            var isFirst = true

            for (point in polyline.points) {
                val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom.toInt())
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
            if (!polygon.visible || polygon.points.size < 3) continue

            fillPaint.color = polygon.fillColor
            strokePaint.color = polygon.strokeColor
            strokePaint.strokeWidth = polygon.strokeWidth

            val path = android.graphics.Path()
            path.fillType = android.graphics.Path.FillType.EVEN_ODD

            // Draw main polygon outline
            var isFirst = true
            for (point in polygon.points) {
                val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom.toInt())
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
                    val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom.toInt())
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
            // Skip invisible markers
            if (!marker.visible) continue

            // Convert marker position to pixel coordinates
            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom.toInt())

            // Calculate screen position
            val screenX = (markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            // Get marker icon
            val icon = marker.icon ?: defaultMarkerIcon

            // Apply anchor point
            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            // Create paint with alpha if needed
            val paint =
                if (marker.alpha < 1.0f) {
                    Paint().apply {
                        alpha = (marker.alpha * 255).toInt()
                    }
                } else {
                    null
                }

            // Draw the marker
            canvas.drawBitmap(icon, screenX - anchorX, screenY - anchorY, paint)
        }
    }

    private fun drawPolygonsByZIndex(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
        zIndex: Float,
        sortedPolygons: List<Polygon>,
    ) {
        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.isAntiAlias = true

        val strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.strokeJoin = Paint.Join.ROUND
        strokePaint.isAntiAlias = true

        for (polygon in sortedPolygons) {
            if (polygon.zIndex != zIndex || !polygon.visible || polygon.points.size < 3) continue

            fillPaint.color = polygon.fillColor
            strokePaint.color = polygon.strokeColor
            strokePaint.strokeWidth = polygon.strokeWidth

            val path = android.graphics.Path()
            var isFirst = true

            for (point in polygon.points) {
                val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom.toInt())
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

            for (hole in polygon.holes) {
                if (hole.size < 3) continue
                var isFirstHole = true
                for (point in hole) {
                    val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom.toInt())
                    val screenX = (pixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                    val screenY = (pixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

                    if (isFirstHole) {
                        path.moveTo(screenX, screenY)
                        isFirstHole = false
                    } else {
                        path.lineTo(screenX, screenY)
                    }
                }
                path.close()
            }

            path.fillType = android.graphics.Path.FillType.EVEN_ODD
            canvas.drawPath(path, fillPaint)
            canvas.drawPath(path, strokePaint)
        }
    }

    private fun drawPolylinesByZIndex(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
        zIndex: Float,
        sortedPolylines: List<Polyline>,
    ) {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.isAntiAlias = true

        for (polyline in sortedPolylines) {
            if (polyline.zIndex != zIndex || !polyline.visible || polyline.points.size < 2) continue

            paint.color = polyline.strokeColor
            paint.strokeWidth = polyline.strokeWidth

            val path = android.graphics.Path()
            var isFirst = true

            for (point in polyline.points) {
                val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom.toInt())
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

    private fun drawMarkersByZIndex(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
        zIndex: Float,
        sortedMarkers: List<Marker>,
    ) {
        val paint = Paint()
        paint.isAntiAlias = true

        for (marker in sortedMarkers) {
            if (marker.zIndex != zIndex || !marker.visible) continue

            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom.toInt())

            val screenX = (markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val icon = marker.icon ?: defaultMarkerIcon

            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            paint.alpha = (marker.alpha * 255).toInt().coerceIn(0, 255)

            canvas.drawBitmap(icon, screenX - anchorX, screenY - anchorY, paint)
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

    /**
     * Adds a marker to the map.
     *
     * @param marker The marker to add
     * @return The added marker instance
     */
    fun addMarker(marker: Marker): Marker {
        markers.add(marker)
        return marker
    }

    /**
     * Removes a marker from the map.
     *
     * @param marker The marker to remove
     * @return true if removed, false if not found
     */
    fun removeMarker(marker: Marker): Boolean = markers.remove(marker)

    /**
     * Removes all markers from the map.
     */
    fun clearMarkers() {
        markers.clear()
    }

    /**
     * Returns a copy of all markers on the map.
     *
     * @return A list of all markers
     */
    fun getMarkers(): List<Marker> = markers.toList()

    /**
     * Adds a polyline to the map.
     *
     * @param polyline The polyline to add
     * @return The added polyline instance
     */
    fun addPolyline(polyline: Polyline): Polyline {
        polylines.add(polyline)
        return polyline
    }

    /**
     * Removes a polyline from the map.
     *
     * @param polyline The polyline to remove
     * @return true if removed, false if not found
     */
    fun removePolyline(polyline: Polyline): Boolean = polylines.remove(polyline)

    /**
     * Removes all polylines from the map.
     */
    fun clearPolylines() {
        polylines.clear()
    }

    /**
     * Returns a copy of all polylines on the map.
     *
     * @return A list of all polylines
     */
    fun getPolylines(): List<Polyline> = polylines.toList()

    /**
     * Adds a polygon to the map.
     *
     * @param polygon The polygon to add
     * @return The added polygon instance
     */
    fun addPolygon(polygon: Polygon): Polygon {
        polygons.add(polygon)
        return polygon
    }

    /**
     * Removes a polygon from the map.
     *
     * @param polygon The polygon to remove
     * @return true if removed, false if not found
     */
    fun removePolygon(polygon: Polygon): Boolean = polygons.remove(polygon)

    /**
     * Removes all polygons from the map.
     */
    fun clearPolygons() {
        polygons.clear()
    }

    /**
     * Returns a copy of all polygons on the map.
     *
     * @return A list of all polygons
     */
    fun getPolygons(): List<Polygon> = polygons.toList()

    /**
     * Parses GeoJSON and adds all features to the map.
     *
     * @param geoJsonString The GeoJSON string to parse
     * @return A GeoJsonResult containing all added features
     */
    fun addGeoJson(geoJsonString: String): GeoJsonResult {
        val result = GeoJsonParser.parse(geoJsonString)
        result.markers.forEach { addMarker(it) }
        result.polylines.forEach { addPolyline(it) }
        result.polygons.forEach { addPolygon(it) }
        return result
    }

    /**
     * Finds the topmost marker at the specified screen coordinates.
     *
     * Checks markers in reverse order (top to bottom) for correct z-ordering.
     *
     * @param x The screen X coordinate
     * @param y The screen Y coordinate
     * @return The touched marker, or null if no marker was found
     */
    fun handleMarkerTouch(
        x: Float,
        y: Float,
    ): Marker? {
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        // Check markers in reverse order (top to bottom) for correct z-ordering
        for (marker in markers.reversed()) {
            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom.toInt())

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

    var onPolylineClickListener: OnPolylineClickListener? = null
    var onPolygonClickListener: OnPolygonClickListener? = null

    /**
     * Checks if a touch at screen coordinates hits a clickable polyline.
     *
     * Uses point-to-line-segment distance calculation with a touch tolerance.
     *
     * @param x The x coordinate in screen pixels
     * @param y The y coordinate in screen pixels
     * @return The clicked polyline or null if none was hit
     */
    fun handlePolylineTouch(
        x: Float,
        y: Float,
    ): Polyline? {
        val touchTolerance = 20f
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        // Check polylines in reverse order (top to bottom) for correct z-ordering
        for (polyline in polylines.reversed()) {
            if (!polyline.clickable) continue

            // Convert all points to screen coordinates
            val screenPoints =
                polyline.points.map { latLng ->
                    val (px, py) = ProjectionUtils.latLngToPixel(latLng, zoom.toInt())
                    val sx = (px - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                    val sy = (py - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()
                    Pair(sx, sy)
                }

            // Check each line segment
            for (i in 0 until screenPoints.size - 1) {
                val p1 = screenPoints[i]
                val p2 = screenPoints[i + 1]

                val distance = distanceToLineSegment(x, y, p1.first, p1.second, p2.first, p2.second)
                if (distance <= touchTolerance + polyline.strokeWidth / 2) {
                    return polyline
                }
            }
        }
        return null
    }

    /**
     * Checks if a touch at screen coordinates hits a clickable polygon.
     *
     * Uses point-in-polygon ray casting algorithm.
     *
     * @param x The x coordinate in screen pixels
     * @param y The y coordinate in screen pixels
     * @return The clicked polygon or null if none was hit
     */
    fun handlePolygonTouch(
        x: Float,
        y: Float,
    ): Polygon? {
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        // Check polygons in reverse order (top to bottom) for correct z-ordering
        for (polygon in polygons.reversed()) {
            if (!polygon.clickable) continue

            // Convert all points to screen coordinates
            val screenPoints =
                polygon.points.map { latLng ->
                    val (px, py) = ProjectionUtils.latLngToPixel(latLng, zoom.toInt())
                    val sx = (px - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                    val sy = (py - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()
                    Pair(sx, sy)
                }

            // Check if point is inside the main polygon
            if (isPointInPolygon(x, y, screenPoints)) {
                // Check if point is inside any holes
                var inHole = false
                for (hole in polygon.holes) {
                    val holeScreenPoints =
                        hole.map { latLng ->
                            val (px, py) = ProjectionUtils.latLngToPixel(latLng, zoom.toInt())
                            val sx = (px - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                            val sy = (py - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()
                            Pair(sx, sy)
                        }
                    if (isPointInPolygon(x, y, holeScreenPoints)) {
                        inHole = true
                        break
                    }
                }

                if (!inHole) {
                    return polygon
                }
            }
        }
        return null
    }

    /**
     * Calculates the shortest distance from a point to a line segment.
     *
     * @param px Point x coordinate
     * @param py Point y coordinate
     * @param x1 Line segment start x
     * @param y1 Line segment start y
     * @param x2 Line segment end x
     * @param y2 Line segment end y
     * @return The distance in pixels
     */
    private fun distanceToLineSegment(
        px: Float,
        py: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSquared = dx * dx + dy * dy

        if (lengthSquared == 0f) {
            // Line segment is a point
            val dpx = px - x1
            val dpy = py - y1
            return kotlin.math.sqrt(dpx * dpx + dpy * dpy)
        }

        // Calculate projection parameter t
        val t = ((px - x1) * dx + (py - y1) * dy) / lengthSquared
        val clampedT = t.coerceIn(0f, 1f)

        // Find closest point on line segment
        val closestX = x1 + clampedT * dx
        val closestY = y1 + clampedT * dy

        // Calculate distance
        val distX = px - closestX
        val distY = py - closestY
        return kotlin.math.sqrt(distX * distX + distY * distY)
    }

    /**
     * Determines if a point is inside a polygon using ray casting algorithm.
     *
     * @param x Point x coordinate
     * @param y Point y coordinate
     * @param points Polygon vertices in screen coordinates
     * @return True if point is inside polygon
     */
    private fun isPointInPolygon(
        x: Float,
        y: Float,
        points: List<Pair<Float, Float>>,
    ): Boolean {
        var inside = false
        var j = points.size - 1

        for (i in points.indices) {
            val xi = points[i].first
            val yi = points[i].second
            val xj = points[j].first
            val yj = points[j].second

            val intersect = ((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi)
            if (intersect) inside = !inside

            j = i
        }

        return inside
    }

    /**
     * Called when the app comes to the foreground.
     *
     * Reserved for future use (e.g., resuming tile downloads).
     */
    fun onResume() {
        // Called when app comes to foreground
        // Could be used to resume tile downloads if paused
    }

    /**
     * Called when the app goes to the background.
     *
     * Reserved for future use (e.g., pausing tile downloads to save battery).
     */
    fun onPause() {
        // Called when app goes to background
        // Could be used to pause tile downloads to save battery
    }

    /**
     * Cleans up resources to prevent memory leaks.
     *
     * Cancels coroutines, closes network connections, disk caches, and clears icon caches.
     * Called automatically when the view is destroyed.
     */
    fun onDestroy() {
        // Clean up resources to prevent memory leaks
        scope.cancel()
        tileDownloader.close()
        tileCache.close()
        MarkerIconFactory.clearCache()
    }
}
