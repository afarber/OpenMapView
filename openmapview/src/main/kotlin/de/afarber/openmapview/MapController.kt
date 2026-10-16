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
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withSave
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Converts Compose PathEffect to Android PathEffect.
 * PathEffect is already backed by android.graphics.PathEffect internally.
 * We access it through reflection since it's not directly exposed.
 */
private fun PathEffect.asAndroidPathEffect(): android.graphics.PathEffect {
    // PathEffect in Compose is a wrapper around android.graphics.PathEffect
    // Access the underlying native instance via reflection
    val nativeField = this::class.java.getDeclaredField("nativePathEffect")
    nativeField.isAccessible = true
    return nativeField.get(this) as android.graphics.PathEffect
}

/**
 * Converts Compose StrokeCap to Android Paint.Cap.
 */
private fun StrokeCap.toAndroidCap(): Paint.Cap =
    when (this) {
        StrokeCap.Butt -> Paint.Cap.BUTT
        StrokeCap.Round -> Paint.Cap.ROUND
        StrokeCap.Square -> Paint.Cap.SQUARE
        else -> Paint.Cap.ROUND // Default fallback
    }

/**
 * Converts Compose StrokeJoin to Android Paint.Join.
 */
private fun StrokeJoin.toAndroidJoin(): Paint.Join =
    when (this) {
        StrokeJoin.Miter -> Paint.Join.MITER
        StrokeJoin.Round -> Paint.Join.ROUND
        StrokeJoin.Bevel -> Paint.Join.BEVEL
        else -> Paint.Join.ROUND // Default fallback
    }

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
        // Allow internal access for testing
        internal const val DEFAULT_MIN_ZOOM = 2.0f
        internal const val DEFAULT_MAX_ZOOM = 19.0f
        private const val TILE_SIZE = 256f
        private const val MAX_CONCURRENT_TILE_DOWNLOADS = 4

        // How many zoom levels up drawFallbackTile() looks for a cached tile to scale up
        private const val MAX_FALLBACK_LEVELS = 4

        // Default info window layout, in pixels
        private const val INFO_WINDOW_PADDING = 20f
        private const val INFO_WINDOW_LINE_SPACING = 10f
    }

    private val infoWindowBackgroundPaint =
        Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

    private val infoWindowBorderPaint =
        Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }

    private val infoWindowTitlePaint =
        Paint().apply {
            color = Color.BLACK
            textSize = 36f
            isAntiAlias = true
            isFakeBoldText = true
        }

    private val infoWindowSnippetPaint =
        Paint().apply {
            color = Color.DKGRAY
            textSize = 28f
            isAntiAlias = true
        }

    private var minZoomPreference = DEFAULT_MIN_ZOOM
    private var maxZoomPreference = DEFAULT_MAX_ZOOM
    private var cameraTargetBounds: LatLngBounds? = null

    private var zoom = 10.0f
    private var center = LatLng(0.0, 0.0)

    /**
     * Clamps a LatLng coordinate to remain within the camera target bounds.
     *
     * If no bounds are set, returns the input unchanged.
     *
     * @param latLng The coordinate to clamp
     * @return The clamped coordinate
     */
    private fun clampToTargetBounds(latLng: LatLng): LatLng = clampToTargetBoundsWithEdges(latLng).first

    /**
     * Clamps a LatLng coordinate to remain within the camera target bounds,
     * and returns which edges were hit during clamping.
     *
     * If no bounds are set, returns the input unchanged with an empty edge set.
     *
     * @param latLng The coordinate to clamp
     * @return A pair of the clamped coordinate and the set of edges that were hit
     */
    private fun clampToTargetBoundsWithEdges(latLng: LatLng): Pair<LatLng, Set<Edge>> {
        val bounds = cameraTargetBounds ?: return Pair(latLng, emptySet())

        val hitEdges = mutableSetOf<Edge>()

        val clampedLat =
            when {
                latLng.latitude < bounds.southwest.latitude -> {
                    hitEdges.add(Edge.BOTTOM)
                    bounds.southwest.latitude
                }

                latLng.latitude > bounds.northeast.latitude -> {
                    hitEdges.add(Edge.TOP)
                    bounds.northeast.latitude
                }

                else -> {
                    latLng.latitude
                }
            }

        val clampedLng =
            when {
                latLng.longitude < bounds.southwest.longitude -> {
                    hitEdges.add(Edge.LEFT)
                    bounds.southwest.longitude
                }

                latLng.longitude > bounds.northeast.longitude -> {
                    hitEdges.add(Edge.RIGHT)
                    bounds.northeast.longitude
                }

                else -> {
                    latLng.longitude
                }
            }

        return Pair(LatLng(clampedLat, clampedLng), hitEdges)
    }

    private var viewWidth = 0
    private var viewHeight = 0
    private var panOffsetX = 0f
    private var panOffsetY = 0f
    private var paddingLeft = 0
    private var paddingTop = 0
    private var paddingRight = 0
    private var paddingBottom = 0

    // Custom edge glow for overzoom visual feedback
    // Stores glow intensity for each edge (0.0 = none, 1.0 = full)
    private var edgeGlowTop = 0f
    private var edgeGlowBottom = 0f
    private var edgeGlowLeft = 0f
    private var edgeGlowRight = 0f

    // Glow parameters
    private val edgeGlowMaxHeight = 150f // Maximum glow height in pixels
    private val edgeGlowDecayRate = 0.92f // Decay multiplier per frame (lower = faster fade)
    private val edgeGlowColor = Color.argb(180, 33, 150, 243) // Semi-transparent blue

    private var uiSettings: UiSettings? = null

    private var lastDrawnTiles = mutableSetOf<TileCoordinate>()

    private val markers = mutableListOf<Marker>()
    private val defaultMarkerIcon by lazy { MarkerIconFactory.getDefaultIcon() }

    // Decoded resource and asset icons, keyed by descriptor (failed loads are not cached)
    private val descriptorBitmapCache = HashMap<BitmapDescriptor, Bitmap>()
    var onMarkerClickListener: OnMarkerClickListener? = null
    var onInfoWindowClickListener: OnInfoWindowClickListener? = null
    var onInfoWindowCloseListener: OnInfoWindowCloseListener? = null

    private val polylines = mutableListOf<Polyline>()
    private val polygons = mutableListOf<Polygon>()
    private val circles = mutableListOf<Circle>()
    private val groundOverlays = mutableListOf<GroundOverlay>()
    private val tileOverlays = mutableListOf<TileOverlay>()
    private val overlayTileCaches = mutableMapOf<String, TileCache>()
    private val overlayDownloadingTiles = mutableMapOf<String, MutableSet<TileCoordinate>>()
    private val overlayTileRetries = mutableMapOf<String, TileRetryTracker>()

    private var animationJob: Job? = null

    var onCameraMoveStartedListener: OnCameraMoveStartedListener? = null
    var onCameraMoveListener: OnCameraMoveListener? = null
    var onCameraIdleListener: OnCameraIdleListener? = null
    var onCameraMoveCanceledListener: OnCameraMoveCanceledListener? = null

    internal var isCameraMoving = false
    internal var currentMoveReason: Int? = null

    // Never cancelled itself, only its children (see onDestroy), so the map keeps working
    // when its view is attached to a window again
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Created on the first download and closed by onDestroy
    private var tileDownloader: TileDownloader? = null

    init {
        UserAgent.init(context)
    }

    // Replaced on every tile source change, so downloads still running for the old
    // source can't put their tiles into the cache of the new one
    @Volatile
    private var tileCache = createTileCache(TileSource.STANDARD)
    private val tileDownloadSemaphore = Semaphore(MAX_CONCURRENT_TILE_DOWNLOADS)
    private var tileSource: TileSource? = TileSource.STANDARD
    private var downloadingTiles = mutableSetOf<TileCoordinate>()
    private var tileRetries = TileRetryTracker()
    private var onTileLoadedCallback: (() -> Unit)? = null

    // One-shot callback for setOnMapLoadedCallback, checked at the end of every draw
    private var onMapLoadedCallback: OnMapLoadedCallback? = null

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

    private val tilePlaceholderPaint =
        Paint().apply {
            style = Paint.Style.FILL
            color = Color.LTGRAY
        }

    // Smooths tiles of other zoom levels that are scaled as fallback
    // Tiles are scaled to fractional zoom levels, so they are drawn with bitmap filtering
    private val tilePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fallbackTilePaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fallbackSrc = Rect()
    private val fallbackDst = RectF()
    private val tileDst = RectF()

    /**
     * Loads a bitmap from a BitmapDescriptor.
     *
     * Resource and asset bitmaps are decoded once and cached, because this runs on every draw.
     *
     * @param descriptor The descriptor specifying the bitmap source
     * @return The loaded bitmap, or the default marker icon if loading fails
     */
    internal fun loadBitmap(descriptor: BitmapDescriptor?): Bitmap {
        if (descriptor == null) {
            return defaultMarkerIcon
        }

        return when (descriptor) {
            is BitmapDescriptor.DefaultMarker -> {
                MarkerIconFactory.getDefaultIcon(descriptor.hue)
            }

            is BitmapDescriptor.BitmapMarker -> {
                descriptor.bitmap.takeUnless { it.isRecycled } ?: defaultMarkerIcon
            }

            is BitmapDescriptor.ResourceMarker -> {
                descriptorBitmapCache[descriptor]
                    ?: decodeResource(descriptor.resourceId)?.also { descriptorBitmapCache[descriptor] = it }
                    ?: defaultMarkerIcon
            }

            is BitmapDescriptor.AssetMarker -> {
                descriptorBitmapCache[descriptor]
                    ?: decodeAsset(descriptor.assetName)?.also { descriptorBitmapCache[descriptor] = it }
                    ?: defaultMarkerIcon
            }
        }
    }

    // Goes through Drawable rather than BitmapFactory so vector and other XML drawables work too
    private fun decodeResource(resourceId: Int): Bitmap? =
        try {
            ContextCompat.getDrawable(context, resourceId)?.toBitmap()
        } catch (e: Exception) {
            null
        }

    private fun decodeAsset(assetName: String): Bitmap? =
        try {
            context.assets.open(assetName).use { stream -> BitmapFactory.decodeStream(stream) }
        } catch (e: IOException) {
            null
        }

    /**
     * Sets the zoom level, clamping to valid range.
     *
     * @param z The desired zoom level (will be clamped to current min/max zoom preferences)
     */
    fun setZoom(z: Float) {
        val target = getCenter()
        zoom = z.coerceIn(minZoomPreference, maxZoomPreference)
        center = viewCenterOf(target, zoom)
    }

    /**
     * Returns the current zoom level.
     *
     * @return The current zoom level
     */
    fun getZoom(): Float = zoom

    /**
     * Sets the minimum zoom level preference.
     *
     * If the camera is zoomed out further, it zooms in to the new minimum and reports the
     * move to the camera listeners.
     *
     * @param minZoom The minimum zoom level (will be clamped to 2.0-19.0)
     */
    fun setMinZoomPreference(minZoom: Float) {
        minZoomPreference = minZoom.coerceIn(DEFAULT_MIN_ZOOM, DEFAULT_MAX_ZOOM)
        clampZoomToPreferences()
    }

    /**
     * Sets the maximum zoom level preference.
     *
     * If the camera is zoomed in further, it zooms out to the new maximum and reports the
     * move to the camera listeners.
     *
     * @param maxZoom The maximum zoom level (will be clamped to 2.0-19.0)
     */
    fun setMaxZoomPreference(maxZoom: Float) {
        maxZoomPreference = maxZoom.coerceIn(DEFAULT_MIN_ZOOM, DEFAULT_MAX_ZOOM)
        clampZoomToPreferences()
    }

    // Keeps the zoom within the preferences. A changed zoom is a camera move the listeners
    // must hear about, e.g. to update a status or the Compose camera state; a running gesture
    // or animation reports its own idle event when it ends.
    private fun clampZoomToPreferences() {
        val clamped = zoom.coerceIn(minZoomPreference, maxZoomPreference)
        if (clamped == zoom) return
        setZoom(clamped)
        if (!isCameraMoving) {
            reportInstantMove(OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION)
        }
    }

    /**
     * Returns the current minimum zoom level preference.
     *
     * @return The minimum zoom level
     */
    fun getMinZoomLevel(): Float = minZoomPreference

    /**
     * Returns the current maximum zoom level preference.
     *
     * @return The maximum zoom level
     */
    fun getMaxZoomLevel(): Float = maxZoomPreference

    /**
     * Resets the min/max zoom preferences to their defaults (2.0 - 19.0).
     */
    fun resetMinMaxZoomPreference() {
        minZoomPreference = DEFAULT_MIN_ZOOM
        maxZoomPreference = DEFAULT_MAX_ZOOM
    }

    /**
     * Sets the UiSettings instance for controlling UI behavior.
     *
     * @param settings The UiSettings instance
     */
    fun setUiSettings(settings: UiSettings) {
        uiSettings = settings
    }

    /**
     * Triggers the edge glow effect when an overzoom attempt occurs.
     *
     * @param overzoomAmount The amount of overzoom attempted (positive for zoom in, negative for zoom out)
     */
    private fun triggerOverzoomEffect(overzoomAmount: Float) {
        if (uiSettings?.isEdgeEffectEnabled != true) return
        if (viewWidth == 0 || viewHeight == 0) return

        // Calculate glow strength based on overzoom magnitude
        val glowStrength = (kotlin.math.abs(overzoomAmount) * 2.0f).coerceIn(0.3f, 1.0f)

        // Trigger edge effect on all edges
        triggerEdgeEffect(setOf(Edge.TOP, Edge.BOTTOM, Edge.LEFT, Edge.RIGHT), glowStrength)
    }

    /**
     * Triggers the edge glow effect on specified edges.
     *
     * This method can be called by app developers from button handlers or other UI controls
     * to provide visual feedback when the camera cannot move further in a direction.
     *
     * @param edges The set of edges to trigger the effect on
     * @param intensity The glow intensity from 0.0 (none) to 1.0 (full), default 0.8
     */
    fun triggerEdgeEffect(
        edges: Set<Edge>,
        intensity: Float = 0.8f,
    ) {
        if (uiSettings?.isEdgeEffectEnabled != true) return
        if (viewWidth == 0 || viewHeight == 0) return

        val clampedIntensity = intensity.coerceIn(0.0f, 1.0f)

        if (Edge.TOP in edges) edgeGlowTop = clampedIntensity
        if (Edge.BOTTOM in edges) edgeGlowBottom = clampedIntensity
        if (Edge.LEFT in edges) edgeGlowLeft = clampedIntensity
        if (Edge.RIGHT in edges) edgeGlowRight = clampedIntensity
    }

    /**
     * Checks if any edge glow is currently active.
     *
     * @return true if any edge glow is visible
     */
    fun hasActiveEdgeEffect(): Boolean =
        edgeGlowTop > 0.01f ||
            edgeGlowBottom > 0.01f ||
            edgeGlowLeft > 0.01f ||
            edgeGlowRight > 0.01f

    /**
     * Draws the custom edge glow effects on the canvas.
     *
     * @param canvas The canvas to draw on
     * @param width The view width
     * @param height The view height
     */
    fun drawEdgeEffects(
        canvas: Canvas,
        width: Int,
        height: Int,
    ) {
        if (width == 0 || height == 0) return

        if (!hasActiveEdgeEffect()) return

        // Draw top edge glow
        if (edgeGlowTop > 0.01f) {
            val glowHeight = edgeGlowMaxHeight * edgeGlowTop
            val gradient =
                LinearGradient(
                    0f,
                    0f,
                    0f,
                    glowHeight,
                    intArrayOf(
                        Color.argb(
                            (Color.alpha(edgeGlowColor) * edgeGlowTop).toInt(),
                            Color.red(edgeGlowColor),
                            Color.green(edgeGlowColor),
                            Color.blue(edgeGlowColor),
                        ),
                        Color.TRANSPARENT,
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            val paint =
                Paint().apply {
                    shader = gradient
                    isAntiAlias = true
                }
            canvas.drawRect(0f, 0f, width.toFloat(), glowHeight, paint)

            // Decay the glow
            edgeGlowTop *= edgeGlowDecayRate
        }

        // Draw bottom edge glow
        if (edgeGlowBottom > 0.01f) {
            val glowHeight = edgeGlowMaxHeight * edgeGlowBottom
            val gradient =
                LinearGradient(
                    0f,
                    height.toFloat(),
                    0f,
                    height - glowHeight,
                    intArrayOf(
                        Color.argb(
                            (Color.alpha(edgeGlowColor) * edgeGlowBottom).toInt(),
                            Color.red(edgeGlowColor),
                            Color.green(edgeGlowColor),
                            Color.blue(edgeGlowColor),
                        ),
                        Color.TRANSPARENT,
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            val paint =
                Paint().apply {
                    shader = gradient
                    isAntiAlias = true
                }
            canvas.drawRect(0f, height - glowHeight, width.toFloat(), height.toFloat(), paint)

            // Decay the glow
            edgeGlowBottom *= edgeGlowDecayRate
        }

        // Draw left edge glow
        if (edgeGlowLeft > 0.01f) {
            val glowWidth = edgeGlowMaxHeight * edgeGlowLeft
            val gradient =
                LinearGradient(
                    0f,
                    0f,
                    glowWidth,
                    0f,
                    intArrayOf(
                        Color.argb(
                            (Color.alpha(edgeGlowColor) * edgeGlowLeft).toInt(),
                            Color.red(edgeGlowColor),
                            Color.green(edgeGlowColor),
                            Color.blue(edgeGlowColor),
                        ),
                        Color.TRANSPARENT,
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            val paint =
                Paint().apply {
                    shader = gradient
                    isAntiAlias = true
                }
            canvas.drawRect(0f, 0f, glowWidth, height.toFloat(), paint)

            // Decay the glow
            edgeGlowLeft *= edgeGlowDecayRate
        }

        // Draw right edge glow
        if (edgeGlowRight > 0.01f) {
            val glowWidth = edgeGlowMaxHeight * edgeGlowRight
            val gradient =
                LinearGradient(
                    width.toFloat(),
                    0f,
                    width - glowWidth,
                    0f,
                    intArrayOf(
                        Color.argb(
                            (Color.alpha(edgeGlowColor) * edgeGlowRight).toInt(),
                            Color.red(edgeGlowColor),
                            Color.green(edgeGlowColor),
                            Color.blue(edgeGlowColor),
                        ),
                        Color.TRANSPARENT,
                    ),
                    null,
                    Shader.TileMode.CLAMP,
                )
            val paint =
                Paint().apply {
                    shader = gradient
                    isAntiAlias = true
                }
            canvas.drawRect(width - glowWidth, 0f, width.toFloat(), height.toFloat(), paint)

            // Decay the glow
            edgeGlowRight *= edgeGlowDecayRate
        }
    }

    /**
     * Releases all edge glows, allowing them to decay naturally.
     */
    fun releaseEdgeEffects() {
        // Edge glows will decay automatically in drawEdgeEffects()
        // No immediate action needed
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
     * When the tile source changes, a new tile cache is used, so tiles from the previous
     * source are never displayed. The disk cache keeps tiles per source, so switching
     * back to a previous source can reuse its tiles.
     *
     * @param source The new tile source, or null to display no base map tiles
     */
    private fun setTileSource(source: TileSource?) {
        if (tileSource == source) return
        tileSource = source
        tileCache = createTileCache(source)
        downloadingTiles = mutableSetOf()
        tileRetries = TileRetryTracker()
    }

    private fun createTileCache(source: TileSource?): TileCache = TileCache(context, source?.name ?: DiskTileCache.DEFAULT_NAMESPACE)

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
     * @param scaleFactor The zoom scale factor (>1 zooms in, <1 zooms out). Doubling the
     *   distance between the fingers zooms in by one level, as the map doubles in size.
     * @param focusX The screen X coordinate of the zoom center
     * @param focusY The screen Y coordinate of the zoom center
     */
    fun zoom(
        scaleFactor: Float,
        focusX: Float,
        focusY: Float,
    ) {
        val oldZoom = zoom
        val requestedZoom = zoom + log2(scaleFactor)
        val newZoom = requestedZoom.coerceIn(minZoomPreference, maxZoomPreference)

        // Trigger edge effect only when actually trying to exceed zoom limits
        if (requestedZoom < minZoomPreference || requestedZoom > maxZoomPreference) {
            val overzoomAmount = requestedZoom - newZoom
            triggerOverzoomEffect(overzoomAmount)
        }

        if (oldZoom == newZoom) {
            return
        }

        zoom = newZoom

        // The map grows by this ratio around its center. Scale the pan offset with it and
        // shift by the focus point's distance from the view center, so the content under
        // the fingers stays put.
        val ratio = 2.0.pow((newZoom - oldZoom).toDouble()).toFloat()
        val focusOffsetX = focusX - viewWidth / 2f
        val focusOffsetY = focusY - viewHeight / 2f
        panOffsetX = panOffsetX * ratio + focusOffsetX * (ratio - 1)
        panOffsetY = panOffsetY * ratio + focusOffsetY * (ratio - 1)
    }

    /**
     * Reports a camera move caused by a gesture (pan or pinch zoom): fires
     * onCameraMoveStarted with REASON_GESTURE when the move begins, then onCameraMove.
     * [commitPan] fires onCameraIdle when the gesture ends.
     */
    internal fun reportGestureMove() {
        if (!isCameraMoving) {
            isCameraMoving = true
            currentMoveReason = OnCameraMoveStartedListener.REASON_GESTURE
            onCameraMoveStartedListener?.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE)
        }
        onCameraMoveListener?.onCameraMove()
    }

    /**
     * Reports an instant camera change that is not a gesture, e.g. by the zoom buttons:
     * fires onCameraMoveStarted, onCameraMove and onCameraIdle.
     */
    internal fun reportInstantMove(reason: Int) {
        if (!isCameraMoving) {
            onCameraMoveStartedListener?.onCameraMoveStarted(reason)
        }
        onCameraMoveListener?.onCameraMove()
        isCameraMoving = false
        currentMoveReason = null
        onCameraIdleListener?.onCameraIdle()
    }

    /**
     * Sets the camera target: the location shown at the center of the area inside the map
     * padding, or at the view center without padding.
     *
     * If camera target bounds are set, the target will be clamped to stay within those bounds.
     *
     * @param latLng The new camera target
     */
    fun setCenter(latLng: LatLng) {
        center = viewCenterOf(clampToTargetBounds(latLng), zoom)
    }

    /**
     * Returns the camera target: the location shown at the center of the area inside the map
     * padding, or at the view center without padding.
     *
     * @return The current camera target
     */
    fun getCenter(): LatLng = targetOf(center, zoom)

    // The map is drawn around `center`, the location at the view center. The camera target is
    // the location at the center of the area inside the map padding, half the difference of
    // the opposite paddings away from it.
    private fun viewCenterOf(
        target: LatLng,
        atZoom: Float,
    ): LatLng = shiftedByPixels(target, (paddingRight - paddingLeft) / 2.0, (paddingBottom - paddingTop) / 2.0, atZoom)

    private fun targetOf(
        viewCenter: LatLng,
        atZoom: Float,
    ): LatLng = shiftedByPixels(viewCenter, (paddingLeft - paddingRight) / 2.0, (paddingTop - paddingBottom) / 2.0, atZoom)

    private fun shiftedByPixels(
        latLng: LatLng,
        dx: Double,
        dy: Double,
        atZoom: Float,
    ): LatLng {
        if (dx == 0.0 && dy == 0.0) return latLng
        val (x, y) = ProjectionUtils.latLngToPixelExact(latLng, atZoom)
        return ProjectionUtils.pixelToLatLng(x + dx, y + dy, atZoom)
    }

    /**
     * Returns the current camera position.
     *
     * @return A CameraPosition containing current target and zoom
     */
    fun getCameraPosition(): CameraPosition =
        CameraPosition(
            target = getCenter(),
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)

        // Convert screen coordinates to pixel coordinates
        // Account for view center offset and pan offset
        val pixelX = centerPixelX + (screenX - viewWidth / 2 + panOffsetX).toDouble()
        val pixelY = centerPixelY + (screenY - viewHeight / 2 + panOffsetY).toDouble()

        // Convert pixel coordinates to LatLng
        return ProjectionUtils.pixelToLatLng(pixelX, pixelY, zoom)
    }

    /**
     * Moves the camera instantly to a new position.
     *
     * Stops any ongoing animation and commits any pending pan offsets
     * before applying the camera update. If the camera already is at the target,
     * nothing else happens and no camera listeners are called.
     *
     * @param cameraUpdate The camera update to apply
     */
    fun moveCamera(cameraUpdate: CameraUpdate) {
        stopAnimation()
        commitPan()

        val targetPosition = calculateTargetPosition(cameraUpdate, getCameraPosition())
        if (targetPosition == getCameraPosition()) return

        // Fire camera move started event
        if (!isCameraMoving) {
            isCameraMoving = true
            currentMoveReason = OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION
            onCameraMoveStartedListener?.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION)
        }

        setZoom(targetPosition.zoom)
        setCenter(targetPosition.target)

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
     * If the camera already is at the target, the listener's onFinish() is called right
     * away and no camera listeners are called.
     *
     * The move is reported to [OnCameraMoveStartedListener] with
     * [OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION].
     *
     * @param cameraUpdate The camera update to animate to
     * @param durationMs Duration of the animation in milliseconds (default 250ms)
     * @param listener Optional listener for animation completion callbacks
     */
    fun animateCamera(
        cameraUpdate: CameraUpdate,
        durationMs: Int = 250,
        listener: CancelableCallback? = null,
    ) = animateCamera(cameraUpdate, durationMs, listener, OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION)

    // Animates the camera, reporting the given reason, e.g. REASON_API_ANIMATION for the
    // map's own buttons
    internal fun animateCamera(
        cameraUpdate: CameraUpdate,
        durationMs: Int,
        listener: CancelableCallback?,
        reason: Int,
    ) {
        stopAnimation()
        commitPan()

        val startPosition = getCameraPosition()
        val targetPosition = calculateTargetPosition(cameraUpdate, startPosition)

        // Nothing to animate, e.g. a Compose update block moving to the current position
        if (targetPosition == startPosition) {
            listener?.onFinish()
            return
        }

        // Fire camera move started event
        if (!isCameraMoving) {
            isCameraMoving = true
            currentMoveReason = reason
            onCameraMoveStartedListener?.onCameraMoveStarted(reason)
        }

        // The listener is captured by the coroutine: a cancelled animation finishes only after
        // stopAnimation() returned, possibly when the next animation has already started
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

                        setZoom(interpolatedZoom)
                        setCenter(LatLng(interpolatedLat, interpolatedLng))

                        // Fire camera move event during animation
                        onCameraMoveListener?.onCameraMove()
                        onTileLoadedCallback?.invoke()

                        kotlinx.coroutines.delay(16)
                    }

                    setZoom(targetPosition.zoom)
                    setCenter(targetPosition.target)
                    onTileLoadedCallback?.invoke()

                    listener?.onFinish()

                    // Fire camera idle event after animation completes
                    isCameraMoving = false
                    currentMoveReason = null
                    onCameraIdleListener?.onCameraIdle()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    listener?.onCancel()
                    throw e
                } finally {
                    if (animationJob === coroutineContext[Job]) animationJob = null
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

            // Fire camera move canceled event
            if (isCameraMoving) {
                onCameraMoveCanceledListener?.onCameraMoveCanceled()
                isCameraMoving = false
                currentMoveReason = null
            }
        }
    }

    /**
     * Calculates the camera position that shows the bounds as large as possible in a viewport.
     *
     * The zoom is fractional, so the bounds touch the viewport edges on one axis. The target
     * is the middle of the bounds on the Web Mercator map, which is a bit north of the average
     * latitude, so the bounds are centered on screen. Bounds crossing the antimeridian
     * (southwest longitude greater than northeast longitude) are supported.
     *
     * @param bounds The geographic bounds to fit
     * @param viewWidth The viewport width in pixels, without padding
     * @param viewHeight The viewport height in pixels, without padding
     * @return The camera position, its zoom clamped to the min and max zoom preferences
     */
    private fun cameraPositionForBounds(
        bounds: LatLngBounds,
        viewWidth: Int,
        viewHeight: Int,
    ): CameraPosition {
        // Web Mercator coordinates at zoom 0, where the world is TILE_SIZE pixels wide
        val (swX, swY) = ProjectionUtils.latLngToPixel(bounds.southwest, 0)
        val (neX, neY) = ProjectionUtils.latLngToPixel(bounds.northeast, 0)
        val width = if (neX >= swX) neX - swX else TILE_SIZE - swX + neX
        val height = swY - neY // Y increases downward

        var centerX = swX + width / 2f
        if (centerX >= TILE_SIZE) centerX -= TILE_SIZE
        val center = ProjectionUtils.pixelToLatLng(centerX.toDouble(), (swY + neY) / 2.0, 0)

        // Each zoom level doubles the size, so the zoom that fits is log2 of the scale
        val zoom =
            when {
                viewWidth <= 0 || viewHeight <= 0 -> {
                    DEFAULT_MIN_ZOOM
                }

                width <= 0f && height <= 0f -> {
                    DEFAULT_MAX_ZOOM
                }

                else -> {
                    val scaleX = if (width > 0f) viewWidth / width else Float.MAX_VALUE
                    val scaleY = if (height > 0f) viewHeight / height else Float.MAX_VALUE
                    log2(minOf(scaleX, scaleY))
                }
            }
        return CameraPosition(center, zoom.coerceIn(minZoomPreference, maxZoomPreference))
    }

    private fun calculateTargetPosition(
        cameraUpdate: CameraUpdate,
        currentPosition: CameraPosition,
    ): CameraPosition =
        when (cameraUpdate) {
            is CameraUpdate.NewLatLng -> {
                CameraPosition(
                    target = cameraUpdate.target,
                    zoom = currentPosition.zoom,
                )
            }

            is CameraUpdate.NewLatLngZoom -> {
                CameraPosition(
                    target = cameraUpdate.target,
                    zoom = cameraUpdate.zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            }

            is CameraUpdate.NewCameraPosition -> {
                cameraUpdate.position.copy(
                    zoom = cameraUpdate.position.zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            }

            is CameraUpdate.ZoomIn -> {
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom + 1.0f).coerceIn(minZoomPreference, maxZoomPreference),
                )
            }

            is CameraUpdate.ZoomOut -> {
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom - 1.0f).coerceIn(minZoomPreference, maxZoomPreference),
                )
            }

            is CameraUpdate.ZoomTo -> {
                CameraPosition(
                    target = currentPosition.target,
                    zoom = cameraUpdate.zoom.coerceIn(minZoomPreference, maxZoomPreference),
                )
            }

            is CameraUpdate.ZoomBy -> {
                CameraPosition(
                    target = currentPosition.target,
                    zoom = (currentPosition.zoom + cameraUpdate.amount).coerceIn(minZoomPreference, maxZoomPreference),
                )
            }

            is CameraUpdate.ScrollBy -> {
                // Convert current center to pixel coordinates
                val (centerPixelX, centerPixelY) =
                    ProjectionUtils.latLngToPixel(
                        currentPosition.target,
                        currentPosition.zoom,
                    )

                // Apply pixel scroll offset
                val newPixelX = (centerPixelX + cameraUpdate.xPixels).toDouble()
                val newPixelY = (centerPixelY + cameraUpdate.yPixels).toDouble()

                // Convert back to LatLng
                val newTarget = ProjectionUtils.pixelToLatLng(newPixelX, newPixelY, currentPosition.zoom)

                CameraPosition(
                    target = newTarget,
                    zoom = currentPosition.zoom,
                )
            }

            is CameraUpdate.NewLatLngBounds -> {
                cameraPositionForBounds(
                    bounds = cameraUpdate.bounds,
                    viewWidth = viewWidth - paddingLeft - paddingRight - cameraUpdate.padding * 2,
                    viewHeight = viewHeight - paddingTop - paddingBottom - cameraUpdate.padding * 2,
                )
            }

            is CameraUpdate.NewLatLngBoundsWithSize -> {
                cameraPositionForBounds(
                    bounds = cameraUpdate.bounds,
                    viewWidth = cameraUpdate.width - cameraUpdate.padding * 2,
                    viewHeight = cameraUpdate.height - cameraUpdate.padding * 2,
                )
            }
        }

    private fun interpolate(
        start: Float,
        end: Float,
        progress: Float,
    ): Float = start + (end - start) * progress

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
        // The camera target stays where it is, so the map moves into the new padded area
        val target = getCenter()
        paddingLeft = left
        paddingTop = top
        paddingRight = right
        paddingBottom = bottom
        center = viewCenterOf(target, zoom)
    }

    /**
     * Sets a callback that is invoked once, after a draw in which all visible base map
     * tiles were shown and the camera was not moving. If the map is already fully drawn,
     * the callback is invoked after the next frame. Tile overlays are not waited for,
     * because sparse overlays have no tiles for many areas.
     *
     * The callback runs on the main thread, outside of the draw pass.
     *
     * @param callback The callback, or null to remove a pending one
     */
    fun setOnMapLoadedCallback(callback: OnMapLoadedCallback?) {
        onMapLoadedCallback = callback
        // Redraw, so an already loaded map reports it without waiting for a camera change
        if (callback != null) onTileLoadedCallback?.invoke()
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
     * If camera target bounds are set and the pan would exceed them,
     * triggers the edge effect on the appropriate edges.
     *
     * @param dx The horizontal movement in pixels
     * @param dy The vertical movement in pixels
     */
    fun updatePanOffset(
        dx: Float,
        dy: Float,
    ) {
        val newPanOffsetX = panOffsetX - dx
        val newPanOffsetY = panOffsetY - dy

        // Check if bounds are set and detect edge hits during panning
        if (cameraTargetBounds != null) {
            val newCenter = pannedCenter(newPanOffsetX, newPanOffsetY)

            val (_, hitEdges) = clampToTargetBoundsWithEdges(targetOf(newCenter, zoom))
            if (hitEdges.isNotEmpty()) {
                triggerEdgeEffect(hitEdges, 0.6f)
            }
        }

        panOffsetX = newPanOffsetX
        panOffsetY = newPanOffsetY
    }

    /**
     * Returns the map center shifted by the given pan offset.
     *
     * The horizontal position is kept inside the world, so panning past the
     * left or right edge stops at longitude -180 or 180 instead of wrapping
     * around to the other side of the map.
     */
    private fun pannedCenter(
        offsetX: Float,
        offsetY: Float,
    ): LatLng {
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)
        val worldSize = ProjectionUtils.worldSize(zoom)
        val newCenterPixelX = (centerPixelX + offsetX).toDouble().coerceIn(0.0, worldSize)
        val newCenterPixelY = (centerPixelY + offsetY).toDouble()
        return ProjectionUtils.pixelToLatLng(newCenterPixelX, newCenterPixelY, zoom)
    }

    /**
     * Commits accumulated pan offsets to the map center.
     *
     * Converts the temporary pan offset to a new center location and resets the offset.
     * Called when a pan gesture ends or before camera operations.
     */
    fun commitPan() {
        if (panOffsetX != 0f || panOffsetY != 0f) {
            // Convert accumulated pan offset to new center
            setCenter(targetOf(pannedCenter(panOffsetX, panOffsetY), zoom))

            // Reset pan offset
            panOffsetX = 0f
            panOffsetY = 0f
        }

        // Fire camera idle event after a gesture completes, also after a pinch around
        // the view center, which leaves no pan offset
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
                zoom,
                viewWidth,
                viewHeight,
                panOffsetX,
                panOffsetY,
            )

        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)

        // Sort tile overlays by z-index
        val sortedTileOverlays = tileOverlays.filter { it.visible }.sortedBy { it.zIndex }

        // Draw tile overlays with negative z-index (below base tiles)
        for (overlay in sortedTileOverlays.filter { it.zIndex < 0 }) {
            drawTileOverlay(canvas, visibleTiles, centerPixelX, centerPixelY, overlay)
        }

        // Draw base map tiles (z-index 0). Without a tile source (MapType.NONE) there is
        // nothing to wait for, a stale offline fallback tile counts as drawn.
        var allTilesDrawn = true
        for (tile in visibleTiles) {
            tileScreenRect(tile, centerPixelX, centerPixelY, tileDst)

            val cachedBitmap = tileCache.getMemory(tile)
            if (cachedBitmap != null) {
                canvas.drawBitmap(cachedBitmap, null, tileDst, tilePaint)
            } else {
                canvas.drawRect(tileDst, tilePlaceholderPaint)
                if (!drawFallbackTile(canvas, tile, tileDst)) {
                    canvas.drawRect(tileDst, tileBorderPaint)
                }
                // A fallback is only a preview, the map is not loaded until the real tile is drawn
                if (tileSource != null) allTilesDrawn = false
            }

            if (needsDownload(tile, SystemClock.elapsedRealtime())) {
                downloadingTiles.add(tile)
                downloadTile(tile)
            }
        }

        // Draw tile overlays with positive z-index (above base tiles)
        for (overlay in sortedTileOverlays.filter { it.zIndex >= 0 }) {
            drawTileOverlay(canvas, visibleTiles, centerPixelX, centerPixelY, overlay)
        }

        // Prefetch adjacent tiles only when viewport changes to avoid excessive downloads
        if (!isCameraMoving && lastDrawnTiles != visibleTiles.toSet()) {
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
        drawInfoWindows(canvas)

        // Draw API key error overlay if present (on top of everything, but touch events pass through)
        apiKeyErrorOverlay?.draw(canvas, viewWidth, viewHeight)

        if (allTilesDrawn && !isCameraMoving) {
            onMapLoadedCallback?.let { callback ->
                onMapLoadedCallback = null
                // Run outside of the draw pass, so the callback may e.g. take a snapshot
                scope.launch { callback.onMapLoaded() }
            }
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
                zoom,
                viewWidth + 512,
                viewHeight + 512,
                panOffsetX,
                panOffsetY,
            )

        // Download tiles that are adjacent but not yet visible
        val now = SystemClock.elapsedRealtime()
        for (tile in prefetchTiles) {
            if (!visibleTiles.contains(tile) && needsDownload(tile, now)) {
                downloadingTiles.add(tile)
                downloadTile(tile, lowPriority = true)
            }
        }
    }

    /**
     * Returns true if a base tile should be downloaded: it is missing from memory, or it is
     * an expired tile shown as offline fallback, and its retry delay has passed.
     */
    internal fun needsDownload(
        tile: TileCoordinate,
        now: Long,
    ): Boolean =
        (tileCache.getMemory(tile) == null || tileCache.isStale(tile)) &&
            !downloadingTiles.contains(tile) &&
            tileRetries.canRequest(tile, now)

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
        zoom: Float,
    ): Float {
        val metersPerPixel = 156543.03392 * cos(latitude * PI / 180.0) / 2.0.pow(zoom.toDouble())
        return (meters / metersPerPixel).toFloat()
    }

    private fun drawGroundOverlay(
        canvas: Canvas,
        centerPixelX: Float,
        centerPixelY: Float,
        groundOverlay: GroundOverlay,
    ) {
        val bitmap = loadBitmap(groundOverlay.image)

        val paint =
            Paint().apply {
                alpha = ((1f - groundOverlay.transparency) * 255).toInt()
            }

        canvas.withSave {
            if (groundOverlay.bounds != null) {
                val bounds = groundOverlay.bounds
                val (nwPixelX, nwPixelY) =
                    ProjectionUtils.latLngToPixel(
                        LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
                        zoom,
                    )
                val (sePixelX, sePixelY) =
                    ProjectionUtils.latLngToPixel(
                        LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                        zoom,
                    )

                val left = nwPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val top = nwPixelY - centerPixelY + viewHeight / 2 - panOffsetY
                val right = sePixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val bottom = sePixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
                val (posPixelX, posPixelY) = ProjectionUtils.latLngToPixel(groundOverlay.position, zoom)

                val metersPerPixel = ProjectionUtils.metersPerPixelAtLatitude(groundOverlay.position.latitude, zoom)
                val widthPixels = groundOverlay.width / metersPerPixel

                val heightPixels =
                    if (groundOverlay.height != null) {
                        groundOverlay.height / metersPerPixel
                    } else {
                        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                        widthPixels * aspectRatio
                    }

                val centerX = posPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val centerY = posPixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
        }
    }

    private fun drawPolygonsByZIndex(
        canvas: Canvas,
        centerPixelX: Float,
        centerPixelY: Float,
        zIndex: Float,
        sortedPolygons: List<Polygon>,
    ) {
        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.isAntiAlias = true

        val strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.isAntiAlias = true

        for (polygon in sortedPolygons) {
            if (polygon.zIndex != zIndex || !polygon.visible || polygon.points.size < 3) continue

            // Expand points with geodesic interpolation if needed
            val points =
                if (polygon.geodesic) {
                    GeodesicUtils.expandWithGeodesicPoints(polygon.points)
                } else {
                    polygon.points
                }

            fillPaint.color = polygon.fillColor.toArgb()
            strokePaint.color = polygon.strokeColor.toArgb()
            strokePaint.strokeWidth = polygon.strokeWidth
            strokePaint.pathEffect = polygon.strokePattern?.asAndroidPathEffect()
            strokePaint.strokeCap = polygon.strokeCap.toAndroidCap()
            strokePaint.strokeJoin = polygon.strokeJoin.toAndroidJoin()

            val path = android.graphics.Path()
            var isFirst = true

            for (point in points) {
                val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom)
                val screenX = pixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val screenY = pixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
                // Expand hole points with geodesic interpolation if needed
                val holePoints =
                    if (polygon.geodesic) {
                        GeodesicUtils.expandWithGeodesicPoints(hole)
                    } else {
                        hole
                    }
                var isFirstHole = true
                for (point in holePoints) {
                    val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom)
                    val screenX = pixelX - centerPixelX + viewWidth / 2 - panOffsetX
                    val screenY = pixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
        centerPixelX: Float,
        centerPixelY: Float,
        zIndex: Float,
        sortedPolylines: List<Polyline>,
    ) {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true

        for (polyline in sortedPolylines) {
            if (polyline.zIndex != zIndex || !polyline.visible || polyline.points.size < 2) continue

            // Expand points with geodesic interpolation if needed
            val points =
                if (polyline.geodesic) {
                    GeodesicUtils.expandWithGeodesicPoints(polyline.points)
                } else {
                    polyline.points
                }

            // Handle spans (multi-color segments) or single color
            if (polyline.spans.isNotEmpty()) {
                drawPolylineWithSpansZIndex(canvas, polyline, points, centerPixelX, centerPixelY)
            } else {
                paint.color = polyline.strokeColor.toArgb()
                paint.strokeWidth = polyline.strokeWidth
                paint.pathEffect = polyline.strokePattern?.asAndroidPathEffect()
                paint.strokeCap = polyline.startCap.toAndroidCap() // Use startCap for full path
                paint.strokeJoin = polyline.strokeJoin.toAndroidJoin()

                val path = android.graphics.Path()
                var isFirst = true

                for (point in points) {
                    val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom)
                    val screenX = pixelX - centerPixelX + viewWidth / 2 - panOffsetX
                    val screenY = pixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
    }

    private fun drawPolylineWithSpansZIndex(
        canvas: Canvas,
        polyline: Polyline,
        points: List<LatLng>,
        centerPixelX: Float,
        centerPixelY: Float,
    ) {
        val paint = Paint()
        paint.style = Paint.Style.STROKE
        paint.isAntiAlias = true
        paint.strokeWidth = polyline.strokeWidth
        paint.pathEffect = polyline.strokePattern?.asAndroidPathEffect()
        paint.strokeJoin = polyline.strokeJoin.toAndroidJoin()

        var segmentIndex = 0
        val totalSegments = points.size - 1

        for (span in polyline.spans) {
            if (segmentIndex >= totalSegments) break

            paint.color = span.color.toArgb()

            for (i in 0 until span.segments) {
                if (segmentIndex >= totalSegments) break

                val isFirstSegment = segmentIndex == 0
                val isLastSegment = segmentIndex == totalSegments - 1

                // Apply appropriate cap
                paint.strokeCap =
                    when {
                        isFirstSegment -> polyline.startCap.toAndroidCap()
                        isLastSegment -> polyline.endCap.toAndroidCap()
                        else -> Paint.Cap.BUTT
                    }

                val from = points[segmentIndex]
                val to = points[segmentIndex + 1]

                val (fromPixelX, fromPixelY) = ProjectionUtils.latLngToPixel(from, zoom)
                val fromScreenX = fromPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val fromScreenY = fromPixelY - centerPixelY + viewHeight / 2 - panOffsetY

                val (toPixelX, toPixelY) = ProjectionUtils.latLngToPixel(to, zoom)
                val toScreenX = toPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val toScreenY = toPixelY - centerPixelY + viewHeight / 2 - panOffsetY

                canvas.drawLine(fromScreenX, fromScreenY, toScreenX, toScreenY, paint)
                segmentIndex++
            }
        }

        // Draw remaining segments with default color
        if (segmentIndex < totalSegments) {
            paint.color = polyline.strokeColor.toArgb()

            while (segmentIndex < totalSegments) {
                val isLastSegment = segmentIndex == totalSegments - 1
                paint.strokeCap = if (isLastSegment) polyline.endCap.toAndroidCap() else Paint.Cap.BUTT

                val from = points[segmentIndex]
                val to = points[segmentIndex + 1]

                val (fromPixelX, fromPixelY) = ProjectionUtils.latLngToPixel(from, zoom)
                val fromScreenX = fromPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val fromScreenY = fromPixelY - centerPixelY + viewHeight / 2 - panOffsetY

                val (toPixelX, toPixelY) = ProjectionUtils.latLngToPixel(to, zoom)
                val toScreenX = toPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val toScreenY = toPixelY - centerPixelY + viewHeight / 2 - panOffsetY

                canvas.drawLine(fromScreenX, fromScreenY, toScreenX, toScreenY, paint)
                segmentIndex++
            }
        }
    }

    private fun drawCirclesByZIndex(
        canvas: Canvas,
        centerPixelX: Float,
        centerPixelY: Float,
        zIndex: Float,
        sortedCircles: List<Circle>,
    ) {
        val fillPaint = Paint()
        fillPaint.style = Paint.Style.FILL
        fillPaint.isAntiAlias = true

        val strokePaint = Paint()
        strokePaint.style = Paint.Style.STROKE
        strokePaint.isAntiAlias = true

        for (circle in sortedCircles) {
            if (circle.zIndex != zIndex || !circle.visible || circle.radius <= 0) continue

            fillPaint.color = circle.fillColor.toArgb()
            strokePaint.color = circle.strokeColor.toArgb()
            strokePaint.strokeWidth = circle.strokeWidth
            strokePaint.pathEffect = circle.strokePattern?.asAndroidPathEffect()
            strokePaint.strokeCap = circle.strokeCap.toAndroidCap()
            strokePaint.strokeJoin = circle.strokeJoin.toAndroidJoin()

            val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(circle.center, zoom)
            val screenX = pixelX - centerPixelX + viewWidth / 2 - panOffsetX
            val screenY = pixelY - centerPixelY + viewHeight / 2 - panOffsetY

            val radiusInPixels = metersToPixels(circle.radius, circle.center.latitude, zoom)

            canvas.drawCircle(screenX, screenY, radiusInPixels, fillPaint)
            canvas.drawCircle(screenX, screenY, radiusInPixels, strokePaint)
        }
    }

    private fun drawMarkersByZIndex(
        canvas: Canvas,
        centerPixelX: Float,
        centerPixelY: Float,
        zIndex: Float,
        sortedMarkers: List<Marker>,
    ) {
        val paint = Paint()
        paint.isAntiAlias = true

        for (marker in sortedMarkers) {
            if (marker.zIndex != zIndex || !marker.visible) continue

            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom)

            val screenX = markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX
            val screenY = markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY

            val icon = loadBitmap(marker.icon)

            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            paint.alpha = (marker.alpha * 255).toInt().coerceIn(0, 255)

            canvas.drawBitmap(icon, screenX - anchorX, screenY - anchorY, paint)
        }
    }

    private fun drawInfoWindows(canvas: Canvas) {
        for (marker in markers) {
            val rect = infoWindowRect(marker) ?: continue
            val image = marker.infoWindowImage

            // A view from InfoWindowAdapter.getInfoWindow is the whole window
            if (image != null && !image.framed) {
                canvas.drawBitmap(image.bitmap, rect.left, rect.top, null)
                continue
            }

            canvas.drawRoundRect(rect, 8f, 8f, infoWindowBackgroundPaint)
            canvas.drawRoundRect(rect, 8f, 8f, infoWindowBorderPaint)

            if (image != null) {
                // A view from InfoWindowAdapter.getInfoContents, inside the default frame
                canvas.drawBitmap(image.bitmap, rect.left + INFO_WINDOW_PADDING, rect.top + INFO_WINDOW_PADDING, null)
                continue
            }

            val title = marker.title
            val snippet = marker.snippet
            var textY = rect.top + INFO_WINDOW_PADDING + infoWindowTitlePaint.textSize

            if (title != null) {
                canvas.drawText(title, rect.left + INFO_WINDOW_PADDING, textY, infoWindowTitlePaint)
                textY += INFO_WINDOW_LINE_SPACING
            }

            if (snippet != null) {
                textY += infoWindowSnippetPaint.textSize
                canvas.drawText(snippet, rect.left + INFO_WINDOW_PADDING, textY, infoWindowSnippetPaint)
            }
        }
    }

    /**
     * Returns the screen area of the marker's info window, above its icon,
     * or null if the window is not shown.
     */
    private fun infoWindowRect(marker: Marker): RectF? {
        if (!marker.isInfoWindowShown || !marker.visible) return null

        val image = marker.infoWindowImage
        val title = marker.title
        val snippet = marker.snippet
        val boxWidth: Float
        val boxHeight: Float

        if (image != null) {
            // The default frame adds padding around a getInfoContents view
            val frame = if (image.framed) INFO_WINDOW_PADDING * 2 else 0f
            boxWidth = image.bitmap.width + frame
            boxHeight = image.bitmap.height + frame
        } else {
            if (title == null && snippet == null) return null

            var maxWidth = 0f
            var totalHeight = 0f

            if (title != null) {
                maxWidth = maxOf(maxWidth, infoWindowTitlePaint.measureText(title))
                totalHeight += infoWindowTitlePaint.textSize
            }

            if (snippet != null) {
                maxWidth = maxOf(maxWidth, infoWindowSnippetPaint.measureText(snippet))
                if (title != null) totalHeight += INFO_WINDOW_LINE_SPACING
                totalHeight += infoWindowSnippetPaint.textSize
            }

            boxWidth = maxWidth + INFO_WINDOW_PADDING * 2
            boxHeight = totalHeight + INFO_WINDOW_PADDING * 2
        }

        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)
        val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom)
        val screenX = markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX
        val screenY = markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY

        val icon = loadBitmap(marker.icon)
        val markerHeight = icon.height * marker.anchor.second

        val left = screenX - boxWidth / 2
        val top = screenY - markerHeight - boxHeight - 10f
        return RectF(left, top, left + boxWidth, top + boxHeight)
    }

    private fun downloadTile(
        tile: TileCoordinate,
        lowPriority: Boolean = false,
    ) {
        // Capture the current source and its cache: if the source changes while this
        // download is queued or running, the result must not end up in the new cache
        val source = tileSource ?: return
        val cache = tileCache
        val downloading = downloadingTiles
        val retries = tileRetries
        val downloader = tileDownloader ?: TileDownloader().also { tileDownloader = it }
        scope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var skipped = false
            var stale = false
            try {
                // The source changed while this request was queued
                if (cache !== tileCache) {
                    skipped = true
                    return@launch
                }
                // Disk hits don't need a download permit, a stale tile in memory is downloaded again
                if (!cache.isStale(tile)) {
                    bitmap = cache.get(tile)
                }
                if (bitmap == null) {
                    bitmap =
                        tileDownloadSemaphore.withPermit {
                            if (cache !== tileCache) {
                                skipped = true
                                return@withPermit null
                            }
                            downloader.downloadTileBytes(source.getTileUrl(tile))?.let { bytes ->
                                decodeTile(bytes)?.also { cache.put(tile, it, bytes) }
                            }
                        }
                }
                // Offline or server error: show an expired tile from disk, if there is one
                if (bitmap == null && !skipped) {
                    bitmap = cache.getStale(tile)
                    stale = bitmap != null && cache.isStale(tile)
                }
            } finally {
                launch(Dispatchers.Main) {
                    downloading.remove(tile)
                    if (skipped || cache !== tileCache) return@launch
                    if (bitmap != null && !stale) {
                        retries.onSuccess(tile)
                        if (!lowPriority) onTileLoadedCallback?.invoke()
                    } else {
                        // A stale fallback counts as a failure, so the tile is retried with back-off
                        val retryDelay = retries.onFailure(tile, SystemClock.elapsedRealtime())
                        if (!lowPriority) {
                            if (stale) onTileLoadedCallback?.invoke()
                            scheduleRedraw(retryDelay)
                        }
                    }
                }
            }
        }
    }

    /**
     * Draws cached tiles of other zoom levels in place of a tile that isn't loaded yet, so
     * the map shows a preview instead of a gray square while the tile downloads, e.g.
     * during a camera animation:
     * - the nearest cached ancestor (up to [MAX_FALLBACK_LEVELS] levels up), its matching
     *   part scaled up, for zooming in
     * - any cached children one level down, scaled down on top, for zooming out
     *
     * Only the memory cache is used, as this runs on every frame.
     *
     * @return true if anything was drawn
     */
    private fun drawFallbackTile(
        canvas: Canvas,
        tile: TileCoordinate,
        dst: RectF,
    ): Boolean {
        var drawn = false
        for (levels in 1..MAX_FALLBACK_LEVELS) {
            val ancestor = TileCoordinate(tile.x shr levels, tile.y shr levels, tile.zoom - levels)
            if (ancestor.zoom < 0) break
            val bitmap = tileCache.getMemory(ancestor) ?: continue
            // The part of the ancestor covered by this tile
            val size = bitmap.width shr levels
            val left = (tile.x - (ancestor.x shl levels)) * size
            val top = (tile.y - (ancestor.y shl levels)) * size
            fallbackSrc.set(left, top, left + size, top + size)
            canvas.drawBitmap(bitmap, fallbackSrc, dst, fallbackTilePaint)
            drawn = true
            break
        }

        val halfWidth = dst.width() / 2
        val halfHeight = dst.height() / 2
        for (dy in 0..1) {
            for (dx in 0..1) {
                val child = TileCoordinate(tile.x * 2 + dx, tile.y * 2 + dy, tile.zoom + 1)
                val bitmap = tileCache.getMemory(child) ?: continue
                val left = dst.left + dx * halfWidth
                val top = dst.top + dy * halfHeight
                fallbackDst.set(left, top, left + halfWidth, top + halfHeight)
                canvas.drawBitmap(bitmap, null, fallbackDst, fallbackTilePaint)
                drawn = true
            }
        }
        return drawn
    }

    /**
     * Sets [out] to the screen rectangle of a tile, scaled from the tile's zoom level to the
     * current fractional zoom. The edges are rounded to whole pixels, so neighboring tiles
     * meet without gaps or overlaps.
     *
     * @param centerPixelX Center pixel X coordinate at the current zoom
     * @param centerPixelY Center pixel Y coordinate at the current zoom
     */
    private fun tileScreenRect(
        tile: TileCoordinate,
        centerPixelX: Float,
        centerPixelY: Float,
        out: RectF,
    ) {
        val size = TILE_SIZE * 2.0.pow((zoom - tile.zoom).toDouble())
        val originX = viewWidth / 2.0 - panOffsetX - centerPixelX
        val originY = viewHeight / 2.0 - panOffsetY - centerPixelY
        out.set(
            (originX + tile.x * size).roundToInt().toFloat(),
            (originY + tile.y * size).roundToInt().toFloat(),
            (originX + (tile.x + 1) * size).roundToInt().toFloat(),
            (originY + (tile.y + 1) * size).roundToInt().toFloat(),
        )
    }

    // Redraws once a failed tile may be requested again, so it is retried
    // even when the map isn't moving
    private fun scheduleRedraw(delayMillis: Long) {
        scope.launch {
            delay(delayMillis)
            onTileLoadedCallback?.invoke()
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
        centerPixelX: Float,
        centerPixelY: Float,
        overlay: TileOverlay,
    ) {
        val cache = overlayTileCaches[overlay.id] ?: return
        val downloadingSet = overlayDownloadingTiles[overlay.id] ?: return
        val retries = overlayTileRetries[overlay.id] ?: return
        val now = SystemClock.elapsedRealtime()

        // Create paint for transparency if needed
        val paint =
            if (overlay.transparency > 0f) {
                Paint(Paint.FILTER_BITMAP_FLAG).apply {
                    alpha = ((1f - overlay.transparency) * 255).toInt()
                }
            } else {
                tilePaint
            }

        for (tile in visibleTiles) {
            tileScreenRect(tile, centerPixelX, centerPixelY, tileDst)

            val cachedBitmap = cache.getMemory(tile)
            if (cachedBitmap != null) {
                canvas.drawBitmap(cachedBitmap, null, tileDst, paint)
            } else {
                if (!downloadingSet.contains(tile) && retries.canRequest(tile, now)) {
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
        val cache = overlayTileCaches[overlay.id] ?: return
        scope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            try {
                bitmap =
                    tileDownloadSemaphore.withPermit {
                        val cachedBitmap = cache.get(tile)
                        if (cachedBitmap != null) {
                            cachedBitmap
                        } else {
                            val tileData = overlay.tileProvider.getTile(tile.x, tile.y, tile.zoom)
                            if (tileData != null) {
                                val options =
                                    BitmapFactory.Options().apply {
                                        inPreferredConfig = Bitmap.Config.ARGB_8888 // Support transparency
                                        inScaled = false
                                    }
                                BitmapFactory
                                    .decodeByteArray(tileData.data, 0, tileData.data.size, options)
                                    ?.also { cache.put(tile, it) }
                            } else {
                                null
                            }
                        }
                    }
            } finally {
                launch(Dispatchers.Main) {
                    overlayDownloadingTiles[overlay.id]?.remove(tile)
                    // The overlay may have been removed meanwhile
                    val retries = overlayTileRetries[overlay.id] ?: return@launch
                    if (bitmap != null) {
                        retries.onSuccess(tile)
                        onTileLoadedCallback?.invoke()
                    } else {
                        // Also covers providers without a tile for this area, so they aren't asked every frame
                        scheduleRedraw(retries.onFailure(tile, SystemClock.elapsedRealtime()))
                    }
                }
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
        // Memory only: overlay tiles come from arbitrary TileProviders and must not
        // end up in the disk cache of the base map
        overlayTileCaches[tileOverlay.id] = TileCache()
        overlayDownloadingTiles[tileOverlay.id] = mutableSetOf()
        overlayTileRetries[tileOverlay.id] = TileRetryTracker()
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
            overlayTileRetries.remove(tileOverlay.id)
        }
        return removed
    }

    /**
     * Drops the downloaded tiles of a tile overlay, so they are requested again.
     *
     * @param tileOverlay The tile overlay whose tiles to drop
     */
    fun clearTileOverlayCache(tileOverlay: TileOverlay) {
        if (tileOverlay !in tileOverlays) return
        overlayTileCaches[tileOverlay.id] = TileCache()
        overlayDownloadingTiles[tileOverlay.id] = mutableSetOf()
        overlayTileRetries[tileOverlay.id] = TileRetryTracker()
    }

    /**
     * Removes all tile overlays from the map.
     */
    fun clearTileOverlays() {
        tileOverlays.clear()
        overlayTileCaches.clear()
        overlayDownloadingTiles.clear()
        overlayTileRetries.clear()
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)

        // Check markers in reverse order (top to bottom) for correct z-ordering
        for (marker in markers.reversed()) {
            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom)

            val screenX = markerPixelX - centerPixelX + viewWidth / 2 - panOffsetX
            val screenY = markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
    ): Marker? = markers.reversed().firstOrNull { infoWindowRect(it)?.contains(x, y) == true }

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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)

        // Check polylines in reverse order (top to bottom) for correct z-ordering
        for (polyline in polylines.reversed()) {
            if (!polyline.clickable) continue

            // Convert all points to screen coordinates
            val screenPoints =
                polyline.points.map { latLng ->
                    val (px, py) = ProjectionUtils.latLngToPixel(latLng, zoom)
                    val sx = px - centerPixelX + viewWidth / 2 - panOffsetX
                    val sy = py - centerPixelY + viewHeight / 2 - panOffsetY
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)

        // Check polygons in reverse order (top to bottom) for correct z-ordering
        for (polygon in polygons.reversed()) {
            if (!polygon.clickable) continue

            // Convert all points to screen coordinates
            val screenPoints =
                polygon.points.map { latLng ->
                    val (px, py) = ProjectionUtils.latLngToPixel(latLng, zoom)
                    val sx = px - centerPixelX + viewWidth / 2 - panOffsetX
                    val sy = py - centerPixelY + viewHeight / 2 - panOffsetY
                    Pair(sx, sy)
                }

            // Check if point is inside the main polygon
            if (isPointInPolygon(x, y, screenPoints)) {
                // Check if point is inside any holes
                var inHole = false
                for (hole in polygon.holes) {
                    val holeScreenPoints =
                        hole.map { latLng ->
                            val (px, py) = ProjectionUtils.latLngToPixel(latLng, zoom)
                            val sx = px - centerPixelX + viewWidth / 2 - panOffsetX
                            val sy = py - centerPixelY + viewHeight / 2 - panOffsetY
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)

        // Check circles in reverse order (top to bottom) for correct z-ordering
        for (circle in circles.reversed()) {
            if (!circle.clickable) continue

            // Convert circle center to screen coordinates
            val (px, py) = ProjectionUtils.latLngToPixel(circle.center, zoom)
            val sx = px - centerPixelX + viewWidth / 2 - panOffsetX
            val sy = py - centerPixelY + viewHeight / 2 - panOffsetY

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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom)

        for (groundOverlay in groundOverlays.reversed()) {
            if (!groundOverlay.clickable) continue

            if (groundOverlay.bounds != null) {
                val bounds = groundOverlay.bounds
                val (nwPixelX, nwPixelY) =
                    ProjectionUtils.latLngToPixel(
                        LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
                        zoom,
                    )
                val (sePixelX, sePixelY) =
                    ProjectionUtils.latLngToPixel(
                        LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                        zoom,
                    )

                val left = nwPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val top = nwPixelY - centerPixelY + viewHeight / 2 - panOffsetY
                val right = sePixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val bottom = sePixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
                val (posPixelX, posPixelY) = ProjectionUtils.latLngToPixel(groundOverlay.position, zoom)
                val metersPerPixel = ProjectionUtils.metersPerPixelAtLatitude(groundOverlay.position.latitude, zoom)
                val widthPixels = groundOverlay.width / metersPerPixel

                val bitmap = loadBitmap(groundOverlay.image)
                val heightPixels =
                    if (groundOverlay.height != null) {
                        groundOverlay.height / metersPerPixel
                    } else {
                        val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                        widthPixels * aspectRatio
                    }

                val centerX = posPixelX - centerPixelX + viewWidth / 2 - panOffsetX
                val centerY = posPixelY - centerPixelY + viewHeight / 2 - panOffsetY

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
     * Does nothing: tiles are requested from [draw], so they load again as soon as the map is drawn.
     */
    fun onResume() {
    }

    /**
     * Called when the app goes to the background.
     *
     * Does nothing: a map in the background is not drawn, so it requests no new tiles.
     * Downloads already running finish, so their tiles are cached when the app comes back.
     */
    fun onPause() {
    }

    /**
     * Releases resources to prevent memory leaks.
     *
     * Stops camera animations and tile downloads, closes the HTTP client and clears the
     * marker icon cache. Called when the view is detached from its window or its lifecycle
     * owner is destroyed. Calling it more than once is safe, and the map keeps working
     * afterwards: tiles that were still loading are requested again on the next draw.
     */
    fun onDestroy() {
        stopAnimation()
        scope.coroutineContext.cancelChildren()
        tileDownloader?.close()
        tileDownloader = null
        // Cancelled downloads never remove their tiles, so start with empty sets
        downloadingTiles = mutableSetOf()
        overlayDownloadingTiles.values.forEach { it.clear() }
        MarkerIconFactory.clearCache()
    }
}
