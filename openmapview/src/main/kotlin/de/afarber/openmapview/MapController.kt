/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

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
    companion object {
        private const val DEFAULT_MIN_ZOOM = 2.0
        private const val DEFAULT_MAX_ZOOM = 19.0
        private const val TILE_SIZE = 256f
    }

    private var minZoomPreference = DEFAULT_MIN_ZOOM
    private var maxZoomPreference = DEFAULT_MAX_ZOOM
    private var cameraTargetBounds: LatLngBounds? = null

    private var zoom = 10.0
    private var center = LatLng(0.0, 0.0)

    /**
     * Clamps a LatLng coordinate to remain within the camera target bounds.
     *
     * If no bounds are set, returns the input unchanged.
     *
     * @param latLng The coordinate to clamp
     * @return The clamped coordinate
     */
    private fun clampToTargetBounds(latLng: LatLng): LatLng {
        val bounds = cameraTargetBounds ?: return latLng

        val clampedLat = latLng.latitude.coerceIn(bounds.southwest.latitude, bounds.northeast.latitude)
        val clampedLng = latLng.longitude.coerceIn(bounds.southwest.longitude, bounds.northeast.longitude)

        return LatLng(clampedLat, clampedLng)
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var panOffsetX = 0f
    private var panOffsetY = 0f
    private var paddingLeft = 0
    private var paddingTop = 0
    private var paddingRight = 0
    private var paddingBottom = 0

    private var lastDrawnTiles = mutableSetOf<TileCoordinate>()

    private val markers = mutableListOf<Marker>()
    private val defaultMarkerIcon by lazy { MarkerIconFactory.getDefaultIcon() }
    var onMarkerClickListener: OnMarkerClickListener? = null
    var onInfoWindowClickListener: OnInfoWindowClickListener? = null

    private val polylines = mutableListOf<Polyline>()
    private val polygons = mutableListOf<Polygon>()
    private val circles = mutableListOf<Circle>()
    private val groundOverlays = mutableListOf<GroundOverlay>()
    private val tileOverlays = mutableListOf<TileOverlay>()
    private val overlayTileCaches = mutableMapOf<String, TileCache>()
    private val overlayDownloadingTiles = mutableMapOf<String, MutableSet<TileCoordinate>>()

    private var animationJob: Job? = null
    private var animationListener: CancelableCallback? = null

    var onCameraMoveStartedListener: OnCameraMoveStartedListener? = null
    var onCameraMoveListener: OnCameraMoveListener? = null
    var onCameraIdleListener: OnCameraIdleListener? = null
    var onCameraMoveCanceledListener: OnCameraMoveCanceledListener? = null

    internal var isCameraMoving = false
    internal var currentMoveReason: Int? = null

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private val tileDownloader = TileDownloader()
    private val tileCache = TileCache(context)
    private var tileSource: TileSource? = TileSource.STANDARD
    private val downloadingTiles = mutableSetOf<TileCoordinate>()
    private var onTileLoadedCallback: (() -> Unit)? = null

    // API key error overlay (null if no error)
    private var apiKeyErrorOverlay: ApiKeyErrorOverlay? = null
    private var requestedMapType: Int = MapType.STANDARD
    private var actualTileSource: TileSource? = TileSource.STANDARD

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
     * Loads a bitmap from a BitmapDescriptor.
     *
     * @param descriptor The descriptor specifying the bitmap source
     * @return The loaded bitmap, or the default marker icon if loading fails
     */
    private fun loadBitmap(descriptor: BitmapDescriptor?): Bitmap {
        if (descriptor == null) {
            return defaultMarkerIcon
        }

        return when (descriptor) {
            is BitmapDescriptor.DefaultMarker -> {
                MarkerIconFactory.getDefaultIcon(descriptor.hue)
            }
            is BitmapDescriptor.BitmapMarker -> {
                descriptor.bitmap
            }
            is BitmapDescriptor.ResourceMarker -> {
                try {
                    BitmapFactory.decodeResource(context.resources, descriptor.resourceId)
                        ?: defaultMarkerIcon
                } catch (e: Exception) {
                    defaultMarkerIcon
                }
            }
            is BitmapDescriptor.AssetMarker -> {
                try {
                    context.assets.open(descriptor.assetName).use { stream ->
                        BitmapFactory.decodeStream(stream) ?: defaultMarkerIcon
                    }
                } catch (e: IOException) {
                    defaultMarkerIcon
                }
            }
        }
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
     * Sets a LatLngBounds to constrain the camera target.
     *
     * When bounds are set, the camera target (center) is constrained to remain within
     * these bounds for both user gestures (pan/scroll) and programmatic camera movements.
     *
     * @param bounds The bounds to constrain the camera target, or null to remove constraints
     */
    fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) {
        cameraTargetBounds = bounds
        // Apply constraint immediately if camera is currently outside bounds
        bounds?.let { setCenter(center) }
    }

    /**
     * Returns the current camera target bounds constraint.
     *
     * @return The bounds constraining the camera target, or null if no constraint is set
     */
    fun getLatLngBoundsForCameraTarget(): LatLngBounds? = cameraTargetBounds

    /**
     * Sets the map type with API key handling.
     *
     * If the requested map type requires an API key that is not configured:
     * - Falls back to STANDARD tile source
     * - Creates an overlay indicating the missing API key
     * - The map remains interactive (touch events pass through)
     *
     * @param mapType The MapType constant
     */
    fun setMapType(mapType: Int) {
        requestedMapType = mapType

        // Initialize ApiKeyManager if not already done
        ApiKeyManager.initialize(context)

        // Get the tile source for this map type
        val source = TileSource.fromMapType(mapType)

        // Check if an API key is required but not configured
        if (source.requiresApiKey && !source.hasApiKey()) {
            // API key required but not configured - use STANDARD and show overlay
            actualTileSource = TileSource.STANDARD
            apiKeyErrorOverlay =
                ApiKeyErrorOverlay(
                    context,
                    source.getProviderName(),
                    TileSource.getMapTypeName(mapType),
                )
            android.util.Log.w(
                "OpenMapView",
                "${TileSource.getMapTypeName(mapType)} requires an API key from ${source.getProviderName()}. " +
                    "Configure key in AndroidManifest.xml or via ApiKeyManager. " +
                    "Displaying STANDARD map instead.",
            )
        } else {
            // All good - use the requested tile source
            actualTileSource = source
            apiKeyErrorOverlay = null
        }

        // Apply the tile source
        setTileSource(if (mapType == MapType.NONE) null else actualTileSource)
    }

    /**
     * Sets the tile source for rendering the base map.
     *
     * When the tile source changes, the tile cache is cleared to prevent
     * displaying incorrect tiles from the previous source.
     *
     * @param source The new tile source, or null to display no base map tiles
     */
    private fun setTileSource(source: TileSource?) {
        if (tileSource == source) return
        tileSource = source
        tileCache.clear()
        downloadingTiles.clear()
    }

    /**
     * Gets the current tile source.
     *
     * @return The current tile source, or null if no base map tiles are displayed
     */
    fun getTileSource(): TileSource? = tileSource

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
     * If camera target bounds are set, the center will be clamped to stay within those bounds.
     *
     * @param latLng The new center location
     */
    fun setCenter(latLng: LatLng) {
        center = clampToTargetBounds(latLng)
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
     * The projection captures the current map state (center, zoom, view size, pan offset, padding)
     * and provides methods for converting between screen and geographic coordinates.
     *
     * If map padding is set, the projection accounts for the logical viewport offset in all
     * coordinate conversions and visible region calculations.
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
            paddingLeft = paddingLeft,
            paddingTop = paddingTop,
            paddingRight = paddingRight,
            paddingBottom = paddingBottom,
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

        // Fire camera move started event
        if (!isCameraMoving) {
            isCameraMoving = true
            currentMoveReason = OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION
            onCameraMoveStartedListener?.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION)
        }

        applyCameraUpdate(cameraUpdate)

        // Fire camera idle event immediately since moveCamera is instant
        isCameraMoving = false
        currentMoveReason = null
        onCameraIdleListener?.onCameraIdle()
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
        listener: CancelableCallback? = null,
    ) {
        stopAnimation()
        commitPan()

        val startPosition = getCameraPosition()
        val targetPosition = calculateTargetPosition(cameraUpdate, startPosition)

        // Fire camera move started event
        if (!isCameraMoving) {
            isCameraMoving = true
            currentMoveReason = OnCameraMoveStartedListener.REASON_API_ANIMATION
            onCameraMoveStartedListener?.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_API_ANIMATION)
        }

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

                        setCenter(LatLng(interpolatedLat, interpolatedLng))
                        setZoom(interpolatedZoom)

                        // Fire camera move event during animation
                        onCameraMoveListener?.onCameraMove()
                        onTileLoadedCallback?.invoke()

                        kotlinx.coroutines.delay(16)
                    }

                    setCenter(targetPosition.target)
                    setZoom(targetPosition.zoom)
                    onTileLoadedCallback?.invoke()

                    animationListener?.onFinish()

                    // Fire camera idle event after animation completes
                    isCameraMoving = false
                    currentMoveReason = null
                    onCameraIdleListener?.onCameraIdle()
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
        if (animationJob != null) {
            animationJob?.cancel()
            animationJob = null
            animationListener = null

            // Fire camera move canceled event
            if (isCameraMoving) {
                onCameraMoveCanceledListener?.onCameraMoveCanceled()
                isCameraMoving = false
                currentMoveReason = null
            }
        }
    }

    private fun applyCameraUpdate(cameraUpdate: CameraUpdate) {
        val targetPosition = calculateTargetPosition(cameraUpdate, getCameraPosition())
        setCenter(applyPaddingOffset(targetPosition.target, targetPosition.zoom))
        setZoom(targetPosition.zoom)
    }

    /**
     * Applies padding offset to a target LatLng position.
     *
     * Converts the pixel padding offset to a LatLng offset at the given zoom level,
     * then adjusts the target position to compensate for the padding.
     *
     * @param target The original target position
     * @param targetZoom The zoom level at which to calculate the offset
     * @return The adjusted LatLng position accounting for padding
     */
    private fun applyPaddingOffset(
        target: LatLng,
        targetZoom: Double,
    ): LatLng {
        // If no padding, return original target
        if (paddingLeft == 0 && paddingTop == 0 && paddingRight == 0 && paddingBottom == 0) {
            return target
        }

        // Calculate pixel offset from padding (positive values shift the "logical center")
        val xOffsetPixels = (paddingLeft - paddingRight) / 2.0
        val yOffsetPixels = (paddingTop - paddingBottom) / 2.0

        // Convert the offset to lat/lng degrees
        // At the target location, calculate the degrees per pixel
        val scale = 256.0 * 2.0.pow(targetZoom)

        // Convert pixel offsets to world coordinate offsets
        val worldXOffset = xOffsetPixels * (1.0 / scale) * 360.0
        val worldYOffset = yOffsetPixels * (1.0 / scale) * 360.0

        // Apply the offset to the target (subtract because padding pushes content away)
        return LatLng(
            target.latitude + worldYOffset,
            target.longitude - worldXOffset,
        )
    }

    /**
     * Calculates the appropriate zoom level to fit bounds within the specified viewport dimensions.
     *
     * Iterates from maximum zoom down to minimum zoom, finding the largest zoom level
     * where the bounds fit entirely within the viewport.
     *
     * @param bounds The geographic bounds to fit
     * @param viewWidth The viewport width in pixels
     * @param viewHeight The viewport height in pixels
     * @return The calculated zoom level (between 2.0 and 19.0)
     */
    private fun calculateZoomForBounds(
        bounds: LatLngBounds,
        viewWidth: Int,
        viewHeight: Int,
    ): Double {
        // Ensure we have valid viewport dimensions
        if (viewWidth <= 0 || viewHeight <= 0) {
            return DEFAULT_MIN_ZOOM
        }

        // Iterate from max zoom down to find the largest zoom that fits
        for (zoom in 19 downTo 2) {
            val (swX, swY) = ProjectionUtils.latLngToPixel(bounds.southwest, zoom)
            val (neX, neY) = ProjectionUtils.latLngToPixel(bounds.northeast, zoom)

            val boundsWidth = kotlin.math.abs(neX - swX)
            val boundsHeight = kotlin.math.abs(swY - neY) // Y increases downward

            if (boundsWidth <= viewWidth && boundsHeight <= viewHeight) {
                return zoom.toDouble()
            }
        }

        // Fallback to minimum zoom
        return DEFAULT_MIN_ZOOM
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
            is CameraUpdate.ScrollBy -> {
                // Convert current center to pixel coordinates
                val (centerPixelX, centerPixelY) =
                    ProjectionUtils.latLngToPixel(
                        currentPosition.target,
                        currentPosition.zoom.toInt(),
                    )

                // Apply pixel scroll offset
                val newPixelX = (centerPixelX + cameraUpdate.xPixels).toInt()
                val newPixelY = (centerPixelY + cameraUpdate.yPixels).toInt()

                // Convert back to LatLng
                val newTarget = ProjectionUtils.pixelToLatLng(newPixelX, newPixelY, currentPosition.zoom.toInt())

                CameraPosition(
                    target = newTarget,
                    zoom = currentPosition.zoom,
                )
            }
            is CameraUpdate.NewLatLngBounds -> {
                val zoom =
                    calculateZoomForBounds(
                        bounds = cameraUpdate.bounds,
                        viewWidth = viewWidth - cameraUpdate.padding * 2,
                        viewHeight = viewHeight - cameraUpdate.padding * 2,
                    )
                CameraPosition(
                    target = cameraUpdate.bounds.getCenter(),
                    zoom = zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            }
            is CameraUpdate.NewLatLngBoundsWithSize -> {
                val zoom =
                    calculateZoomForBounds(
                        bounds = cameraUpdate.bounds,
                        viewWidth = cameraUpdate.width - cameraUpdate.padding * 2,
                        viewHeight = cameraUpdate.height - cameraUpdate.padding * 2,
                    )
                CameraPosition(
                    target = cameraUpdate.bounds.getCenter(),
                    zoom = zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            }
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
     * Sets map padding that affects the logical viewport.
     *
     * Padding adjusts where camera operations consider the "center" of the map
     * without changing the physical view size.
     *
     * @param left Left padding in pixels
     * @param top Top padding in pixels
     * @param right Right padding in pixels
     * @param bottom Bottom padding in pixels
     */
    fun setMapPadding(
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        paddingLeft = left
        paddingTop = top
        paddingRight = right
        paddingBottom = bottom
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

        setCenter(ProjectionUtils.pixelToLatLng(newCenterPixelX, newCenterPixelY, zoom.toInt()))

        // Reset pan offset
        panOffsetX = 0f
        panOffsetY = 0f

        // Fire camera idle event after pan gesture completes
        if (isCameraMoving && currentMoveReason == OnCameraMoveStartedListener.REASON_GESTURE) {
            isCameraMoving = false
            currentMoveReason = null
            onCameraIdleListener?.onCameraIdle()
        }
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

        // Sort tile overlays by z-index
        val sortedTileOverlays = tileOverlays.filter { it.visible }.sortedBy { it.zIndex }

        // Draw tile overlays with negative z-index (below base tiles)
        for (overlay in sortedTileOverlays.filter { it.zIndex < 0 }) {
            drawTileOverlay(canvas, visibleTiles, centerPixelX, centerPixelY, overlay)
        }

        // Draw base map tiles (z-index 0)
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

        // Draw tile overlays with positive z-index (above base tiles)
        for (overlay in sortedTileOverlays.filter { it.zIndex >= 0 }) {
            drawTileOverlay(canvas, visibleTiles, centerPixelX, centerPixelY, overlay)
        }

        // Prefetch adjacent tiles only when viewport changes to avoid excessive downloads
        if (lastDrawnTiles != visibleTiles.toSet()) {
            prefetchAdjacentTiles(visibleTiles)
            lastDrawnTiles = visibleTiles.toMutableSet()
        }

        // Draw ground overlays (after tile overlays, before shapes)
        val sortedGroundOverlays = groundOverlays.filter { it.visible }.sortedBy { it.zIndex }
        for (groundOverlay in sortedGroundOverlays) {
            drawGroundOverlay(canvas, centerPixelX, centerPixelY, groundOverlay)
        }

        // Draw shapes in zIndex order: polygons first, then circles, then polylines, then markers
        // Within each type, sort by zIndex (lower zIndex drawn first, higher on top)
        val sortedPolygons = polygons.sortedBy { it.zIndex }
        val sortedCircles = circles.sortedBy { it.zIndex }
        val sortedPolylines = polylines.sortedBy { it.zIndex }
        val sortedMarkers = markers.sortedBy { it.zIndex }

        // Group all shapes by zIndex and draw in order
        val allZIndices =
            (
                sortedPolygons.map {
                    it.zIndex
                } + sortedCircles.map { it.zIndex } + sortedPolylines.map { it.zIndex } + sortedMarkers.map { it.zIndex }
            ).distinct().sorted()

        for (z in allZIndices) {
            // Draw polygons at this zIndex
            drawPolygonsByZIndex(canvas, centerPixelX, centerPixelY, z, sortedPolygons)
            // Draw circles at this zIndex
            drawCirclesByZIndex(canvas, centerPixelX, centerPixelY, z, sortedCircles)
            // Draw polylines at this zIndex
            drawPolylinesByZIndex(canvas, centerPixelX, centerPixelY, z, sortedPolylines)
            // Draw markers at this zIndex
            drawMarkersByZIndex(canvas, centerPixelX, centerPixelY, z, sortedMarkers)
        }

        // Draw info windows after all markers (so they appear on top)
        drawInfoWindows(canvas, centerPixelX, centerPixelY)

        // Draw API key error overlay if present (on top of everything, but touch events pass through)
        apiKeyErrorOverlay?.draw(canvas, viewWidth, viewHeight)
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

            paint.color = polyline.strokeColor.toArgb()
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

            fillPaint.color = polygon.fillColor.toArgb()
            strokePaint.color = polygon.strokeColor.toArgb()
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

    private fun drawCircles(
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
        strokePaint.isAntiAlias = true

        for (circle in circles) {
            if (!circle.visible || circle.radius <= 0) continue

            fillPaint.color = circle.fillColor.toArgb()
            strokePaint.color = circle.strokeColor.toArgb()
            strokePaint.strokeWidth = circle.strokeWidth

            // Convert center to screen coordinates
            val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(circle.center, zoom.toInt())
            val screenX = (pixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (pixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            // Convert radius from meters to pixels
            val radiusInPixels = metersToPixels(circle.radius, circle.center.latitude, zoom)

            // Draw fill first, then stroke
            canvas.drawCircle(screenX, screenY, radiusInPixels, fillPaint)
            canvas.drawCircle(screenX, screenY, radiusInPixels, strokePaint)
        }
    }

    /**
     * Converts a distance in meters to screen pixels at a given latitude and zoom level.
     * Uses the Mercator projection formula.
     *
     * @param meters Distance in meters
     * @param latitude Latitude at which to calculate (affects scale in Mercator projection)
     * @param zoom Current zoom level
     * @return Distance in screen pixels
     */
    private fun metersToPixels(
        meters: Float,
        latitude: Double,
        zoom: Double,
    ): Float {
        val metersPerPixel = 156543.03392 * cos(latitude * PI / 180.0) / 2.0.pow(zoom)
        return (meters / metersPerPixel).toFloat()
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
            val icon = loadBitmap(marker.icon)

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

    private fun drawGroundOverlay(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
        groundOverlay: GroundOverlay,
    ) {
        val bitmap = loadBitmap(groundOverlay.image)

        val paint =
            Paint().apply {
                alpha = ((1f - groundOverlay.transparency) * 255).toInt()
            }

        canvas.save()

        if (groundOverlay.bounds != null) {
            val bounds = groundOverlay.bounds
            val (nwPixelX, nwPixelY) =
                ProjectionUtils.latLngToPixel(
                    LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
                    zoom.toInt(),
                )
            val (sePixelX, sePixelY) =
                ProjectionUtils.latLngToPixel(
                    LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                    zoom.toInt(),
                )

            val left = (nwPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val top = (nwPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()
            val right = (sePixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val bottom = (sePixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val centerX = (left + right) / 2
            val centerY = (top + bottom) / 2
            val anchorX = left + (right - left) * groundOverlay.anchor.first
            val anchorY = top + (bottom - top) * groundOverlay.anchor.second

            if (groundOverlay.bearing != 0f) {
                canvas.rotate(groundOverlay.bearing, anchorX, anchorY)
            }

            val destRect = android.graphics.RectF(left, top, right, bottom)
            canvas.drawBitmap(bitmap, null, destRect, paint)
        } else if (groundOverlay.position != null && groundOverlay.width != null) {
            val (posPixelX, posPixelY) = ProjectionUtils.latLngToPixel(groundOverlay.position, zoom.toInt())

            val metersPerPixel = ProjectionUtils.metersPerPixelAtLatitude(groundOverlay.position.latitude, zoom.toInt())
            val widthPixels = groundOverlay.width / metersPerPixel

            val heightPixels =
                if (groundOverlay.height != null) {
                    groundOverlay.height / metersPerPixel
                } else {
                    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                    widthPixels * aspectRatio
                }

            val centerX = (posPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val centerY = (posPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val left = (centerX - widthPixels / 2).toFloat()
            val top = (centerY - heightPixels / 2).toFloat()
            val right = (centerX + widthPixels / 2).toFloat()
            val bottom = (centerY + heightPixels / 2).toFloat()

            val anchorX = left + (right - left) * groundOverlay.anchor.first
            val anchorY = top + (bottom - top) * groundOverlay.anchor.second

            if (groundOverlay.bearing != 0f) {
                canvas.rotate(groundOverlay.bearing, anchorX, anchorY)
            }

            val destRect = android.graphics.RectF(left, top, right, bottom)
            canvas.drawBitmap(bitmap, null, destRect, paint)
        }

        canvas.restore()
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

            fillPaint.color = polygon.fillColor.toArgb()
            strokePaint.color = polygon.strokeColor.toArgb()
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

            paint.color = polyline.strokeColor.toArgb()
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

    private fun drawCirclesByZIndex(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
        zIndex: Float,
        sortedCircles: List<Circle>,
    ) {
        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.isAntiAlias = true

        val strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeCap = Paint.Cap.ROUND
        strokePaint.isAntiAlias = true

        for (circle in sortedCircles) {
            if (circle.zIndex != zIndex || !circle.visible || circle.radius <= 0) continue

            fillPaint.color = circle.fillColor.toArgb()
            strokePaint.color = circle.strokeColor.toArgb()
            strokePaint.strokeWidth = circle.strokeWidth

            val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(circle.center, zoom.toInt())
            val screenX = (pixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (pixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val radiusInPixels = metersToPixels(circle.radius, circle.center.latitude, zoom)

            canvas.drawCircle(screenX, screenY, radiusInPixels, fillPaint)
            canvas.drawCircle(screenX, screenY, radiusInPixels, strokePaint)
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

            val icon = loadBitmap(marker.icon)

            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            paint.alpha = (marker.alpha * 255).toInt().coerceIn(0, 255)

            canvas.drawBitmap(icon, screenX - anchorX, screenY - anchorY, paint)
        }
    }

    private fun drawInfoWindows(
        canvas: Canvas,
        centerPixelX: Double,
        centerPixelY: Double,
    ) {
        val backgroundPaint =
            Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }

        val borderPaint =
            Paint().apply {
                color = Color.DKGRAY
                style = Paint.Style.STROKE
                strokeWidth = 2f
                isAntiAlias = true
            }

        val titlePaint =
            Paint().apply {
                color = Color.BLACK
                textSize = 36f
                isAntiAlias = true
                isFakeBoldText = true
            }

        val snippetPaint =
            Paint().apply {
                color = Color.DKGRAY
                textSize = 28f
                isAntiAlias = true
            }

        for (marker in markers) {
            if (!marker.isInfoWindowShown || !marker.visible) continue

            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom.toInt())
            val screenX = (markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val title = marker.title
            val snippet = marker.snippet

            if (title == null && snippet == null) continue

            val padding = 20f
            val lineSpacing = 10f

            var maxWidth = 0f
            var totalHeight = 0f

            if (title != null) {
                val titleWidth = titlePaint.measureText(title)
                maxWidth = maxOf(maxWidth, titleWidth)
                totalHeight += titlePaint.textSize
            }

            if (snippet != null) {
                val snippetWidth = snippetPaint.measureText(snippet)
                maxWidth = maxOf(maxWidth, snippetWidth)
                if (title != null) totalHeight += lineSpacing
                totalHeight += snippetPaint.textSize
            }

            val boxWidth = maxWidth + padding * 2
            val boxHeight = totalHeight + padding * 2

            val icon = loadBitmap(marker.icon)
            val markerHeight = icon.height * marker.anchor.second

            val infoWindowX = screenX - boxWidth / 2
            val infoWindowY = screenY - markerHeight - boxHeight - 10f

            val rect =
                android.graphics.RectF(
                    infoWindowX,
                    infoWindowY,
                    infoWindowX + boxWidth,
                    infoWindowY + boxHeight,
                )

            canvas.drawRoundRect(rect, 8f, 8f, backgroundPaint)
            canvas.drawRoundRect(rect, 8f, 8f, borderPaint)

            var textY = infoWindowY + padding + titlePaint.textSize

            if (title != null) {
                canvas.drawText(title, infoWindowX + padding, textY, titlePaint)
                textY += lineSpacing
            }

            if (snippet != null) {
                textY += snippetPaint.textSize
                canvas.drawText(snippet, infoWindowX + padding, textY, snippetPaint)
            }
        }
    }

    private fun downloadTile(
        tile: TileCoordinate,
        lowPriority: Boolean = false,
    ) {
        scope.launch(Dispatchers.IO) {
            val source = tileSource ?: return@launch
            val url = source.getTileUrl(tile)
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
     * Draws tiles for a tile overlay layer.
     *
     * @param canvas The canvas to draw on
     * @param visibleTiles The list of visible tile coordinates
     * @param centerPixelX Center pixel X coordinate
     * @param centerPixelY Center pixel Y coordinate
     * @param overlay The tile overlay to render
     */
    private fun drawTileOverlay(
        canvas: Canvas,
        visibleTiles: List<TileCoordinate>,
        centerPixelX: Double,
        centerPixelY: Double,
        overlay: TileOverlay,
    ) {
        val cache = overlayTileCaches[overlay.id] ?: return
        val downloadingSet = overlayDownloadingTiles[overlay.id] ?: return

        // Create paint for transparency if needed
        val paint =
            if (overlay.transparency > 0f) {
                Paint().apply {
                    alpha = ((1f - overlay.transparency) * 255).toInt()
                }
            } else {
                null
            }

        for (tile in visibleTiles) {
            val (tilePixelX, tilePixelY) = ProjectionUtils.tileToPixel(tile)

            val screenX = (tilePixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (tilePixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val cachedBitmap = cache.get(tile)
            if (cachedBitmap != null) {
                canvas.drawBitmap(cachedBitmap, screenX, screenY, paint)
            } else {
                if (!downloadingSet.contains(tile)) {
                    downloadingSet.add(tile)
                    downloadOverlayTile(tile, overlay)
                }
            }
        }
    }

    /**
     * Downloads a tile for an overlay layer.
     *
     * @param tile The tile coordinate to download
     * @param overlay The tile overlay requesting the tile
     */
    private fun downloadOverlayTile(
        tile: TileCoordinate,
        overlay: TileOverlay,
    ) {
        scope.launch(Dispatchers.IO) {
            val tileData = overlay.tileProvider.getTile(tile.x, tile.y, tile.zoom)
            if (tileData != null) {
                // Decode tile data to bitmap
                val options =
                    BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888 // Support transparency
                        inScaled = false
                    }
                val bitmap = BitmapFactory.decodeByteArray(tileData.data, 0, tileData.data.size, options)

                if (bitmap != null) {
                    val cache = overlayTileCaches[overlay.id]
                    cache?.put(tile, bitmap)

                    overlayDownloadingTiles[overlay.id]?.remove(tile)

                    launch(Dispatchers.Main) {
                        onTileLoadedCallback?.invoke()
                    }
                } else {
                    overlayDownloadingTiles[overlay.id]?.remove(tile)
                }
            } else {
                overlayDownloadingTiles[overlay.id]?.remove(tile)
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
     * Adds a circle to the map.
     *
     * @param circle The circle to add
     * @return The added circle instance
     */
    fun addCircle(circle: Circle): Circle {
        circles.add(circle)
        return circle
    }

    /**
     * Removes a circle from the map.
     *
     * @param circle The circle to remove
     * @return true if removed, false if not found
     */
    fun removeCircle(circle: Circle): Boolean = circles.remove(circle)

    /**
     * Removes all circles from the map.
     */
    fun clearCircles() {
        circles.clear()
    }

    /**
     * Returns a copy of all circles on the map.
     *
     * @return A list of all circles
     */
    fun getCircles(): List<Circle> = circles.toList()

    /**
     * Adds a ground overlay to the map.
     *
     * @param groundOverlay The ground overlay to add
     * @return The added ground overlay instance
     */
    fun addGroundOverlay(groundOverlay: GroundOverlay): GroundOverlay {
        groundOverlays.add(groundOverlay)
        return groundOverlay
    }

    /**
     * Removes a ground overlay from the map.
     *
     * @param groundOverlay The ground overlay to remove
     * @return true if removed, false if not found
     */
    fun removeGroundOverlay(groundOverlay: GroundOverlay): Boolean = groundOverlays.remove(groundOverlay)

    /**
     * Removes all ground overlays from the map.
     */
    fun clearGroundOverlays() {
        groundOverlays.clear()
    }

    /**
     * Returns a copy of all ground overlays on the map.
     *
     * @return A list of all ground overlays
     */
    fun getGroundOverlays(): List<GroundOverlay> = groundOverlays.toList()

    /**
     * Adds a tile overlay to the map.
     *
     * @param tileOverlay The tile overlay to add
     * @return The added tile overlay instance
     */
    fun addTileOverlay(tileOverlay: TileOverlay): TileOverlay {
        tileOverlays.add(tileOverlay)
        overlayTileCaches[tileOverlay.id] = TileCache(context)
        overlayDownloadingTiles[tileOverlay.id] = mutableSetOf()
        return tileOverlay
    }

    /**
     * Removes a tile overlay from the map.
     *
     * @param tileOverlay The tile overlay to remove
     * @return true if removed, false if not found
     */
    fun removeTileOverlay(tileOverlay: TileOverlay): Boolean {
        val removed = tileOverlays.remove(tileOverlay)
        if (removed) {
            overlayTileCaches.remove(tileOverlay.id)
            overlayDownloadingTiles.remove(tileOverlay.id)
        }
        return removed
    }

    /**
     * Removes all tile overlays from the map.
     */
    fun clearTileOverlays() {
        tileOverlays.clear()
        overlayTileCaches.clear()
        overlayDownloadingTiles.clear()
    }

    /**
     * Returns a copy of all tile overlays on the map.
     *
     * @return A list of all tile overlays
     */
    fun getTileOverlays(): List<TileOverlay> = tileOverlays.toList()

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

            val icon = loadBitmap(marker.icon)
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

    /**
     * Finds the info window at the specified screen coordinates.
     *
     * Checks info windows for visible markers and returns the marker whose
     * info window was clicked, if any.
     *
     * @param x The screen X coordinate
     * @param y The screen Y coordinate
     * @return The marker whose info window was touched, or null if none was found
     */
    fun handleInfoWindowTouch(
        x: Float,
        y: Float,
    ): Marker? {
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        val titlePaint =
            Paint().apply {
                textSize = 36f
            }

        val snippetPaint =
            Paint().apply {
                textSize = 28f
            }

        for (marker in markers.reversed()) {
            if (!marker.isInfoWindowShown || !marker.visible) continue

            val title = marker.title
            val snippet = marker.snippet

            if (title == null && snippet == null) continue

            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom.toInt())
            val screenX = (markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val screenY = (markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            val padding = 20f
            val lineSpacing = 10f

            var maxWidth = 0f
            var totalHeight = 0f

            if (title != null) {
                val titleWidth = titlePaint.measureText(title)
                maxWidth = maxOf(maxWidth, titleWidth)
                totalHeight += titlePaint.textSize
            }

            if (snippet != null) {
                val snippetWidth = snippetPaint.measureText(snippet)
                maxWidth = maxOf(maxWidth, snippetWidth)
                if (title != null) totalHeight += lineSpacing
                totalHeight += snippetPaint.textSize
            }

            val boxWidth = maxWidth + padding * 2
            val boxHeight = totalHeight + padding * 2

            val icon = loadBitmap(marker.icon)
            val markerHeight = icon.height * marker.anchor.second

            val infoWindowX = screenX - boxWidth / 2
            val infoWindowY = screenY - markerHeight - boxHeight - 10f

            if (x >= infoWindowX &&
                x <= infoWindowX + boxWidth &&
                y >= infoWindowY &&
                y <= infoWindowY + boxHeight
            ) {
                return marker
            }
        }
        return null
    }

    var onPolylineClickListener: OnPolylineClickListener? = null
    var onPolygonClickListener: OnPolygonClickListener? = null
    var onCircleClickListener: OnCircleClickListener? = null
    var onGroundOverlayClickListener: OnGroundOverlayClickListener? = null

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
     * Checks if a touch at screen coordinates hits a clickable circle.
     *
     * Detects circle hits by calculating the distance from the touch point to the circle center.
     * Returns the topmost (highest zIndex) circle that was touched.
     *
     * @param x Screen X coordinate in pixels
     * @param y Screen Y coordinate in pixels
     * @return The touched circle, or null if none was hit
     */
    fun handleCircleTouch(
        x: Float,
        y: Float,
    ): Circle? {
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        // Check circles in reverse order (top to bottom) for correct z-ordering
        for (circle in circles.reversed()) {
            if (!circle.clickable) continue

            // Convert circle center to screen coordinates
            val (px, py) = ProjectionUtils.latLngToPixel(circle.center, zoom.toInt())
            val sx = (px - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
            val sy = (py - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

            // Convert radius from meters to pixels
            val radiusInPixels = metersToPixels(circle.radius, circle.center.latitude, zoom)

            // Calculate distance from touch point to circle center
            val dx = x - sx
            val dy = y - sy
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            // Check if touch is within the circle (including stroke width)
            if (distance <= radiusInPixels + circle.strokeWidth / 2) {
                return circle
            }
        }
        return null
    }

    fun handleGroundOverlayTouch(
        x: Float,
        y: Float,
    ): GroundOverlay? {
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom.toInt())

        for (groundOverlay in groundOverlays.reversed()) {
            if (!groundOverlay.clickable) continue

            if (groundOverlay.bounds != null) {
                val bounds = groundOverlay.bounds
                val (nwPixelX, nwPixelY) =
                    ProjectionUtils.latLngToPixel(
                        LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
                        zoom.toInt(),
                    )
                val (sePixelX, sePixelY) =
                    ProjectionUtils.latLngToPixel(
                        LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                        zoom.toInt(),
                    )

                val left = (nwPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                val top = (nwPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()
                val right = (sePixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                val bottom = (sePixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

                if (groundOverlay.bearing != 0f) {
                    val anchorX = left + (right - left) * groundOverlay.anchor.first
                    val anchorY = top + (bottom - top) * groundOverlay.anchor.second
                    val rotatedPoint = rotatePointAround(x, y, anchorX, anchorY, -groundOverlay.bearing)
                    if (rotatedPoint.first >= left &&
                        rotatedPoint.first <= right &&
                        rotatedPoint.second >= top &&
                        rotatedPoint.second <= bottom
                    ) {
                        return groundOverlay
                    }
                } else {
                    if (x >= left && x <= right && y >= top && y <= bottom) {
                        return groundOverlay
                    }
                }
            } else if (groundOverlay.position != null && groundOverlay.width != null) {
                val (posPixelX, posPixelY) = ProjectionUtils.latLngToPixel(groundOverlay.position, zoom.toInt())
                val metersPerPixel = ProjectionUtils.metersPerPixelAtLatitude(groundOverlay.position.latitude, zoom.toInt())
                val widthPixels = groundOverlay.width / metersPerPixel

                val bitmap = loadBitmap(groundOverlay.image)
                val heightPixels =
                    if (groundOverlay.height != null) {
                        groundOverlay.height / metersPerPixel
                    } else {
                        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                        widthPixels * aspectRatio
                    }

                val centerX = (posPixelX - centerPixelX + viewWidth / 2 - panOffsetX).toFloat()
                val centerY = (posPixelY - centerPixelY + viewHeight / 2 - panOffsetY).toFloat()

                val left = (centerX - widthPixels / 2).toFloat()
                val top = (centerY - heightPixels / 2).toFloat()
                val right = (centerX + widthPixels / 2).toFloat()
                val bottom = (centerY + heightPixels / 2).toFloat()

                if (groundOverlay.bearing != 0f) {
                    val anchorX = left + (right - left) * groundOverlay.anchor.first
                    val anchorY = top + (bottom - top) * groundOverlay.anchor.second
                    val rotatedPoint = rotatePointAround(x, y, anchorX, anchorY, -groundOverlay.bearing)
                    if (rotatedPoint.first >= left &&
                        rotatedPoint.first <= right &&
                        rotatedPoint.second >= top &&
                        rotatedPoint.second <= bottom
                    ) {
                        return groundOverlay
                    }
                } else {
                    if (x >= left && x <= right && y >= top && y <= bottom) {
                        return groundOverlay
                    }
                }
            }
        }
        return null
    }

    private fun rotatePointAround(
        x: Float,
        y: Float,
        centerX: Float,
        centerY: Float,
        angleDegrees: Float,
    ): Pair<Float, Float> {
        val angleRadians = angleDegrees.toDouble() * PI / 180.0
        val cosValue = cos(angleRadians).toFloat()
        val sinValue = sin(angleRadians).toFloat()

        val dx = x - centerX
        val dy = y - centerY

        val rotatedX = dx * cosValue - dy * sinValue + centerX
        val rotatedY = dx * sinValue + dy * cosValue + centerY

        return Pair(rotatedX, rotatedY)
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
