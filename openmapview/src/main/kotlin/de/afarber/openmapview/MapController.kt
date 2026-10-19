/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.withRotation
import androidx.core.graphics.withSave
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.IOException
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

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
        private const val MAX_CONCURRENT_TILE_DOWNLOADS = 4

        // Per tile overlay, so a slow overlay server only delays its own tiles
        private const val MAX_CONCURRENT_OVERLAY_DOWNLOADS = 2

        // How many converted stroke patterns strokeEffect() keeps
        private const val MAX_PATTERN_EFFECTS = 64

        // How many zoom levels up drawFallbackTile() looks for a cached tile to scale up
        private const val MAX_FALLBACK_LEVELS = 4

        // How many zoom levels above the target of a camera animation its preview tiles are
        // loaded, see requestTargetTiles(). Within MAX_FALLBACK_LEVELS, so they get drawn.
        private const val TARGET_PREVIEW_LEVELS = 3

        // Default info window layout, in dp
        private const val INFO_WINDOW_PADDING_DP = 8f
        private const val INFO_WINDOW_LINE_SPACING_DP = 4f
        private const val INFO_WINDOW_CORNER_DP = 8f

        // The triangle below the default frame, pointing at the marker
        private const val INFO_WINDOW_POINTER_WIDTH_DP = 16f
        private const val INFO_WINDOW_POINTER_HEIGHT_DP = 8f

        // Between the info window and the top of its marker
        private const val INFO_WINDOW_GAP_DP = 4f

        // Longest side of a vector ground overlay image rasterized by loadGroundOverlayBitmap()
        internal const val MAX_GROUND_OVERLAY_BITMAP_SIZE = 1024
    }

    private val density = context.resources.displayMetrics.density

    // Screen pixels per tile at a whole zoom level, see setTileScale()
    private var tileSize = ProjectionUtils.TILE_SIZE * density

    private val infoWindowPadding = INFO_WINDOW_PADDING_DP * density
    private val infoWindowLineSpacing = INFO_WINDOW_LINE_SPACING_DP * density
    private val infoWindowGap = INFO_WINDOW_GAP_DP * density
    private val infoWindowCorner = INFO_WINDOW_CORNER_DP * density
    private val infoWindowPointerWidth = INFO_WINDOW_POINTER_WIDTH_DP * density
    private val infoWindowPointerHeight = INFO_WINDOW_POINTER_HEIGHT_DP * density

    // The outline of the default frame and one of its corners, reused for each info window
    private val infoWindowFramePath = Path()
    private val infoWindowCornerRect = RectF()

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
            strokeWidth = density
            isAntiAlias = true
        }

    private val infoWindowTitlePaint =
        Paint().apply {
            color = Color.BLACK
            textSize = 14f * density
            isAntiAlias = true
            isFakeBoldText = true
        }

    private val infoWindowSnippetPaint =
        Paint().apply {
            color = Color.DKGRAY
            textSize = 12f * density
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
    private val defaultMarkerIcon by lazy { MarkerIconFactory.getDefaultIcon(density = density) }

    // Decoded resource and asset icons, keyed by descriptor (failed loads are not cached)
    private val descriptorBitmapCache = HashMap<BitmapDescriptor, Bitmap>()

    // Ground overlay resource images keyed by resource id and rasterized width
    private val groundOverlayBitmapCache = HashMap<Pair<Int, Int>, Bitmap>()
    var onMarkerClickListener: OnMarkerClickListener? = null
    var onInfoWindowClickListener: OnInfoWindowClickListener? = null
    var onInfoWindowCloseListener: OnInfoWindowCloseListener? = null
    var onInfoWindowLongClickListener: OnInfoWindowLongClickListener? = null

    private val polylines = mutableListOf<Polyline>()
    private val polygons = mutableListOf<Polygon>()
    private val circles = mutableListOf<Circle>()
    private val groundOverlays = mutableListOf<GroundOverlay>()
    private val tileOverlays = mutableListOf<TileOverlay>()
    private val overlayTileCaches = mutableMapOf<String, TileCache>()
    private val overlayDownloadingTiles = mutableMapOf<String, MutableSet<TileCoordinate>>()
    private val overlayTileRetries = mutableMapOf<String, TileRetryTracker>()
    private val overlayDownloadSemaphores = mutableMapOf<String, Semaphore>()

    private var animationJob: Job? = null

    // A newLatLngBounds() camera update made before the view had a size, applied by
    // setViewSize(), with the listener of animateCamera(), if it was animated
    private var pendingBoundsUpdate: CameraUpdate.NewLatLngBounds? = null
    private var pendingBoundsListener: CancelableCallback? = null

    var onCameraMoveStartedListener: OnCameraMoveStartedListener? = null
    var onCameraMoveListener: OnCameraMoveListener? = null
    var onCameraIdleListener: OnCameraIdleListener? = null
    var onCameraMoveCanceledListener: OnCameraMoveCanceledListener? = null

    internal var isCameraMoving = false

    // moveCamera() reports its move later on the main thread, like the camera listeners of
    // the Maps SDK, so a listener set right after it still hears about it. Before the view
    // has a size, the report waits for setViewSize(), so the visible region is known by then.
    private val cameraEventHandler = Handler(Looper.getMainLooper())
    private var hasPendingMoveEvents = false
    private val pendingMoveEvents = Runnable { flushPendingCameraEvents() }
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

    // For the base map only: tile overlays have their own limits in overlayDownloadSemaphores
    // The downloads of the current base map source and their permits. A new source cancels
    // them and gets its own permits, so slow downloads of the old source can't hold up its tiles
    private var sourceDownloads: Job = SupervisorJob(scope.coroutineContext[Job])
    private var tileDownloadSemaphore = Semaphore(MAX_CONCURRENT_TILE_DOWNLOADS)
    private var tileSource: TileSource? = TileSource.STANDARD
    private var downloadingTiles = mutableSetOf<TileCoordinate>()

    // The base tiles the map still needs: the visible ones, the prefetched ones around them
    // and those at the target of a camera animation. Queued downloads of other tiles are
    // skipped, so the tiles a camera animation flew over don't hold up the ones on screen.
    // Written on the main thread, read by the downloads. Null until the first draw.
    @Volatile
    private var wantedTiles: Set<TileCoordinate>? = null
    private var prefetchedTiles = emptySet<TileCoordinate>()
    private var animationTargetTiles = emptySet<TileCoordinate>()
    private var tileRetries = TileRetryTracker()
    private var onTileLoadedCallback: (() -> Unit)? = null

    // One-shot callback for setOnMapLoadedCallback, checked at the end of every draw
    private var onMapLoadedCallback: OnMapLoadedCallback? = null

    // API key error overlay (null if no error)
    private var apiKeyErrorOverlay: ApiKeyErrorOverlay? = null
    private var requestedMapType: Int = MapType.STANDARD
    private var actualTileSource: TileSource? = TileSource.STANDARD

    // See setMapColorScheme() and setTileColorFilter()
    private var mapColorScheme = MapColorScheme.LIGHT
    private var customTileColorFilter: ColorFilter? = null
    private var isNightMode =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

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

    // Tile overlays are drawn without the tile color filter
    private val overlayTilePaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // Shared by all ground overlays, which only differ in alpha; filtered as they are scaled
    private val groundOverlayPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    // Reused by drawPathWithCaps() for lines with different start and end caps
    private val capOutline = Path()
    private val capPath = Path()
    private val capRect = RectF()
    private val capMatrix = Matrix()
    private val capFillPaint = Paint()

    // Path effects of dot, dash and gap patterns by pattern and stroke width, see strokeEffect()
    private val patternEffects = HashMap<Pair<List<PatternItem>, Float>, android.graphics.PathEffect>()

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
                MarkerIconFactory.getDefaultIcon(descriptor.hue, density)
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

    /**
     * Loads the image of a ground overlay, which is stretched to its size in meters.
     *
     * A vector or other XML drawable is rasterized at the width the overlay reaches on screen
     * at the maximum zoom, rounded up to a power of two and capped at
     * [MAX_GROUND_OVERLAY_BITMAP_SIZE], so it stays sharp when zoomed in.
     * Other images are the same as [loadBitmap].
     *
     * @param groundOverlay The ground overlay to load the image of
     * @return The image to draw
     */
    internal fun loadGroundOverlayBitmap(groundOverlay: GroundOverlay): Bitmap {
        val image = groundOverlay.image
        val bitmap = loadBitmap(image)
        if (image !is BitmapDescriptor.ResourceMarker || bitmap === defaultMarkerIcon) return bitmap

        val maxZoomWidth = groundOverlayWidthAtMaxZoom(groundOverlay) ?: return bitmap
        val width =
            Integer
                .highestOneBit(maxZoomWidth.toInt().coerceAtLeast(1))
                .let { if (it < maxZoomWidth) it * 2 else it }
                .coerceAtMost(MAX_GROUND_OVERLAY_BITMAP_SIZE)
        if (width <= bitmap.width) return bitmap

        val key = image.resourceId to width
        groundOverlayBitmapCache[key]?.let { return it }

        val rasterized =
            try {
                ContextCompat
                    .getDrawable(context, image.resourceId)
                    ?.takeUnless { it is BitmapDrawable }
                    ?.let { drawable ->
                        var height = width * bitmap.height / bitmap.width
                        var scaledWidth = width
                        if (height > MAX_GROUND_OVERLAY_BITMAP_SIZE) {
                            scaledWidth = MAX_GROUND_OVERLAY_BITMAP_SIZE * bitmap.width / bitmap.height
                            height = MAX_GROUND_OVERLAY_BITMAP_SIZE
                        }
                        drawable.toBitmap(scaledWidth.coerceAtLeast(1), height.coerceAtLeast(1))
                    }
            } catch (e: Exception) {
                null
            }

        // A bitmap drawable is cached as is, so it is not looked up again
        return (rasterized ?: bitmap).also { groundOverlayBitmapCache[key] = it }
    }

    // Width in pixels of a ground overlay at the maximum zoom, or null when it has no placement
    private fun groundOverlayWidthAtMaxZoom(groundOverlay: GroundOverlay): Float? {
        val bounds = groundOverlay.bounds
        val position = groundOverlay.position
        val width = groundOverlay.width
        if (bounds != null) {
            val (westPixelX, _) = ProjectionUtils.latLngToPixel(bounds.southwest, maxZoomPreference, tileSize)
            val (eastPixelX, _) = ProjectionUtils.latLngToPixel(bounds.northeast, maxZoomPreference, tileSize)
            return eastPixelX - westPixelX
        }
        if (position == null || width == null) return null
        val metersPerPixel =
            ProjectionUtils.metersPerPixelAtLatitude(position.latitude, maxZoomPreference, tileSize)
        return (width / metersPerPixel).toFloat()
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
        val target = takePanOffset()
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
    // must hear about, e.g. to update a status or the Compose camera state. Like a move by
    // moveCamera(), it is reported later on the main thread; a running gesture or animation
    // reports its own idle event when it ends.
    private fun clampZoomToPreferences() {
        val clamped = zoom.coerceIn(minZoomPreference, maxZoomPreference)
        if (clamped == zoom) return
        setZoom(clamped)
        if (!isCameraMoving) postInstantMoveEvents()
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
        sourceDownloads.cancel()
        sourceDownloads = SupervisorJob(scope.coroutineContext[Job])
        tileDownloadSemaphore = Semaphore(MAX_CONCURRENT_TILE_DOWNLOADS)
        // A dark source is not darkened by the color scheme
        applyTileColorFilter()
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
        cancelPendingCameraUpdate()
        flushPendingCameraEvents()
        if (!isCameraMoving) {
            isCameraMoving = true
            currentMoveReason = OnCameraMoveStartedListener.REASON_GESTURE
            onCameraMoveStartedListener?.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_GESTURE)
        }
        onCameraMoveListener?.onCameraMove()
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
     * During a pan or pinch gesture this includes the not yet committed pan offset, so
     * camera move listeners see where the camera is now.
     *
     * @return The current camera target
     */
    fun getCenter(): LatLng =
        if (panOffsetX != 0f || panOffsetY != 0f) {
            targetOf(pannedCenter(panOffsetX, panOffsetY), zoom)
        } else {
            targetOf(center, zoom)
        }

    // Moves `center` to the current camera target, which then no longer depends on the pan
    // offset, before the zoom or padding changes how the offset maps to a location
    private fun takePanOffset(): LatLng {
        val target = getCenter()
        panOffsetX = 0f
        panOffsetY = 0f
        return target
    }

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
        val (x, y) = ProjectionUtils.latLngToPixelExact(latLng, atZoom, tileSize)
        return ProjectionUtils.pixelToLatLng(x + dx, y + dy, atZoom, tileSize)
    }

    /**
     * Sets how many screen pixels a pixel of a 256 px tile covers.
     *
     * The default is the screen density, so the world is 256 * 2^zoom dp wide, as with the
     * Maps SDK, and the labels on the tiles have the same size on every screen. 1 draws one
     * tile pixel per screen pixel: the map shows more area, with smaller labels.
     *
     * The camera target and zoom are kept.
     *
     * @param scale Screen pixels per tile pixel, greater than 0
     */
    fun setTileScale(scale: Float) {
        require(scale > 0f) { "Tile scale must be greater than 0, but was $scale" }
        tileSize = ProjectionUtils.TILE_SIZE * scale
    }

    /**
     * Returns how many screen pixels a pixel of a 256 px tile covers, see [setTileScale].
     */
    fun getTileScale(): Float = (tileSize / ProjectionUtils.TILE_SIZE).toFloat()

    /**
     * Sets a color filter for the base map tiles and their placeholders, e.g.
     * [TileColorFilters.invertedDark] for a dark map. Markers, shapes, ground and tile
     * overlays and info windows keep their colors.
     *
     * @param filter The filter, or null to draw the tiles unchanged
     */
    fun setTileColorFilter(filter: ColorFilter?) {
        customTileColorFilter = filter
        applyTileColorFilter()
    }

    /**
     * Returns the color filter of the base map tiles, see [setTileColorFilter].
     */
    fun getTileColorFilter(): ColorFilter? = customTileColorFilter

    /**
     * Sets the color scheme of the map, one of the [MapColorScheme] constants.
     *
     * The dark scheme draws the tiles with [TileColorFilters.invertedDark], unless they are
     * dark already or a filter was set with [setTileColorFilter].
     *
     * @param scheme [MapColorScheme.LIGHT], [MapColorScheme.DARK] or
     *   [MapColorScheme.FOLLOW_SYSTEM]
     */
    fun setMapColorScheme(scheme: Int) {
        require(scheme == MapColorScheme.LIGHT || scheme == MapColorScheme.DARK || scheme == MapColorScheme.FOLLOW_SYSTEM) {
            "Unknown map color scheme $scheme"
        }
        mapColorScheme = scheme
        applyTileColorFilter()
    }

    /**
     * Returns the color scheme of the map, see [setMapColorScheme].
     */
    fun getMapColorScheme(): Int = mapColorScheme

    /**
     * Sets whether the device is in night mode, for [MapColorScheme.FOLLOW_SYSTEM].
     */
    fun setNightMode(night: Boolean) {
        isNightMode = night
        applyTileColorFilter()
    }

    // The filter set by the app wins; otherwise a dark scheme darkens light tiles
    private fun applyTileColorFilter() {
        val dark =
            mapColorScheme == MapColorScheme.DARK ||
                (mapColorScheme == MapColorScheme.FOLLOW_SYSTEM && isNightMode)
        val filter =
            customTileColorFilter
                ?: TileColorFilters.invertedDark().takeIf { dark && tileSource?.isDark != true }
        tilePaint.colorFilter = filter
        fallbackTilePaint.colorFilter = filter
        tilePlaceholderPaint.colorFilter = filter
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
            tileSize = tileSize,
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)

        // Convert screen coordinates to pixel coordinates
        // Account for view center offset and pan offset
        val pixelX = centerPixelX + (screenX - viewWidth / 2 + panOffsetX).toDouble()
        val pixelY = centerPixelY + (screenY - viewHeight / 2 + panOffsetY).toDouble()

        // Convert pixel coordinates to LatLng
        return ProjectionUtils.pixelToLatLng(pixelX, pixelY, zoom, tileSize)
    }

    /**
     * Moves the camera instantly to a new position.
     *
     * Stops any ongoing animation and commits any pending pan offsets
     * before applying the camera update. If the camera already is at the target,
     * nothing else happens and no camera listeners are called.
     *
     * The camera moves right away, but onCameraMoveStarted and onCameraIdle are called
     * later on the main thread, after the view has a size. Another camera change reports
     * them first, so the events always arrive in order.
     *
     * @param cameraUpdate The camera update to apply
     */
    fun moveCamera(cameraUpdate: CameraUpdate) {
        if (deferBoundsUpdate(cameraUpdate, null)) return
        stopAnimation()
        commitPan()

        val targetPosition = calculateTargetPosition(cameraUpdate, getCameraPosition())
        if (targetPosition == getCameraPosition()) return

        setZoom(targetPosition.zoom)
        setCenter(targetPosition.target)

        // The started and idle events of the previous move were reported by stopAnimation()
        postInstantMoveEvents()
    }

    /**
     * Has [flushPendingCameraEvents] report an instant move later on the main thread,
     * or after the view gets a size. Several moves before then are reported once.
     */
    private fun postInstantMoveEvents() {
        if (hasPendingMoveEvents) return
        hasPendingMoveEvents = true
        if (viewWidth > 0 && viewHeight > 0) cameraEventHandler.post(pendingMoveEvents)
    }

    /**
     * Reports a move by [moveCamera] that was not reported yet: onCameraMoveStarted with
     * REASON_DEVELOPER_ANIMATION, then onCameraIdle. Called on the main thread after the
     * move, and before any other camera change, so the events keep their order.
     */
    internal fun flushPendingCameraEvents() {
        if (!hasPendingMoveEvents) return
        hasPendingMoveEvents = false
        cameraEventHandler.removeCallbacks(pendingMoveEvents)
        onCameraMoveStartedListener?.onCameraMoveStarted(OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION)
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
        if (deferBoundsUpdate(cameraUpdate, listener)) return
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

        requestTargetTiles(targetPosition)

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

                        val camera = interpolateCamera(startPosition, targetPosition, progress)
                        setZoom(camera.zoom)
                        setCenter(camera.target)

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
                    if (animationJob === coroutineContext[Job]) {
                        animationJob = null
                        animationTargetTiles = emptySet()
                    }
                }
            }
    }

    /**
     * Keeps a [CameraUpdate.NewLatLngBounds] made before the view has a size, because the
     * zoom that fits the bounds depends on it. [setViewSize] applies it once the view is laid
     * out, so it can be used right after creating the map. Any other camera update replaces
     * it, like a later camera update replaces a running animation.
     *
     * @return true if the update was kept for later
     */
    private fun deferBoundsUpdate(
        cameraUpdate: CameraUpdate,
        listener: CancelableCallback?,
    ): Boolean {
        cancelPendingCameraUpdate()
        if (cameraUpdate !is CameraUpdate.NewLatLngBounds || (viewWidth > 0 && viewHeight > 0)) return false
        pendingBoundsUpdate = cameraUpdate
        pendingBoundsListener = listener
        return true
    }

    /**
     * Drops a camera update kept by [deferBoundsUpdate], e.g. when the camera is set
     * directly, calling onCancel() of its listener.
     */
    fun cancelPendingCameraUpdate() {
        if (pendingBoundsUpdate == null) return
        val listener = pendingBoundsListener
        pendingBoundsUpdate = null
        pendingBoundsListener = null
        listener?.onCancel()
    }

    /**
     * Starts loading the base tiles at the target of a camera animation before the camera
     * gets there, ahead of the tiles it flies over:
     * - a few tiles [TARGET_PREVIEW_LEVELS] zoom levels up, which drawFallbackTile() scales
     *   up while the camera zooms in
     * - the tiles of the target itself, so the map is complete when the animation ends
     */
    private fun requestTargetTiles(target: CameraPosition) {
        if (tileSource == null || viewWidth <= 0 || viewHeight <= 0) return
        val viewCenter = viewCenterOf(clampToTargetBounds(target.target), target.zoom)
        val previewZoom = (target.zoom - TARGET_PREVIEW_LEVELS).coerceAtLeast(0f)
        val tiles =
            (
                ViewportCalculator.getVisibleTiles(viewCenter, previewZoom, viewWidth, viewHeight, tileSize = tileSize) +
                    ViewportCalculator.getVisibleTiles(viewCenter, target.zoom, viewWidth, viewHeight, tileSize = tileSize)
            ).mapTo(LinkedHashSet()) { it.wrapped() }
        animationTargetTiles = tiles
        wantedTiles = wantedTiles?.plus(animationTargetTiles)

        val now = SystemClock.elapsedRealtime()
        for (tile in tiles) {
            if (needsDownload(tile, now)) {
                downloadingTiles.add(tile)
                downloadTile(tile)
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
        flushPendingCameraEvents()
        if (animationJob != null) {
            animationJob?.cancel()
            animationJob = null
            animationTargetTiles = emptySet()

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
        // Web Mercator coordinates at zoom 0, where the world is tileSize pixels wide
        val (swX, swY) = ProjectionUtils.latLngToPixel(bounds.southwest, 0f, tileSize)
        val (neX, neY) = ProjectionUtils.latLngToPixel(bounds.northeast, 0f, tileSize)
        val width = if (neX >= swX) neX - swX else tileSize.toFloat() - swX + neX
        val height = swY - neY // Y increases downward

        var centerX = swX + width / 2f
        if (centerX >= tileSize) centerX -= tileSize.toFloat()
        val center = ProjectionUtils.pixelToLatLng(centerX.toDouble(), (swY + neY) / 2.0, 0f, tileSize)

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
                        tileSize,
                    )

                // Apply pixel scroll offset
                val newPixelX = (centerPixelX + cameraUpdate.xPixels).toDouble()
                val newPixelY = (centerPixelY + cameraUpdate.yPixels).toDouble()

                // Convert back to LatLng
                val newTarget = ProjectionUtils.pixelToLatLng(newPixelX, newPixelY, currentPosition.zoom, tileSize)

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

        // A move made before the layout is reported now that the visible region is known
        if (hasPendingMoveEvents && width > 0 && height > 0) {
            cameraEventHandler.removeCallbacks(pendingMoveEvents)
            cameraEventHandler.post(pendingMoveEvents)
        }

        // The camera jumps to the bounds kept before the layout, also when they were animated
        val update = pendingBoundsUpdate
        if (update != null && width > 0 && height > 0) {
            val listener = pendingBoundsListener
            pendingBoundsUpdate = null
            pendingBoundsListener = null
            moveCamera(update)
            listener?.onFinish()
        }
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
        val target = takePanOffset()
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
     * The world repeats horizontally, so panning past longitude 180 goes on at -180;
     * the returned longitude is always between -180 and 180.
     */
    private fun pannedCenter(
        offsetX: Float,
        offsetY: Float,
    ): LatLng {
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)
        val newCenterPixelX = (centerPixelX + offsetX).toDouble()
        val newCenterPixelY = (centerPixelY + offsetY).toDouble()
        return ProjectionUtils.pixelToLatLng(newCenterPixelX, newCenterPixelY, zoom, tileSize)
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
                tileSize = tileSize,
            )

        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)

        // Sort tile overlays by z-index
        val sortedTileOverlays = tileOverlays.filter { it.visible }.sortedBy { it.zIndex }

        // Draw tile overlays with negative z-index (below base tiles)
        for (overlay in sortedTileOverlays.filter { it.zIndex < 0 }) {
            drawTileOverlay(canvas, visibleTiles, centerPixelX, centerPixelY, overlay)
        }

        // Tiles of repeated copies of the world are loaded as the tiles they show
        wantedTiles = visibleTiles.mapTo(HashSet()) { it.wrapped() } + prefetchedTiles + animationTargetTiles

        // Draw base map tiles (z-index 0). Without a tile source (MapType.NONE) there is
        // nothing to wait for, a stale offline fallback tile counts as drawn.
        var allTilesDrawn = true
        for (placed in visibleTiles) {
            tileScreenRect(placed, centerPixelX, centerPixelY, tileDst)
            val tile = placed.wrapped()

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
        // Centered in the area inside the map padding, like the map's own controls
        apiKeyErrorOverlay?.draw(
            canvas,
            viewWidth,
            viewHeight,
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (viewWidth - paddingRight).toFloat(),
            (viewHeight - paddingBottom).toFloat(),
        )

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
        // Calculate tiles in a buffer zone: add a tile in each direction
        val buffer = (2 * tileSize).toInt()
        val prefetchTiles =
            ViewportCalculator.getVisibleTiles(
                center,
                zoom,
                viewWidth + buffer,
                viewHeight + buffer,
                panOffsetX,
                panOffsetY,
                tileSize = tileSize,
            )

        prefetchedTiles = prefetchTiles.mapTo(HashSet()) { it.wrapped() }
        wantedTiles = wantedTiles?.plus(prefetchedTiles)

        // Download tiles that are adjacent but not yet visible
        val visible = visibleTiles.mapTo(HashSet()) { it.wrapped() }
        val now = SystemClock.elapsedRealtime()
        for (tile in prefetchedTiles) {
            if (!visible.contains(tile) && needsDownload(tile, now)) {
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
     * Returns the screen X of a pixel X at the current zoom, on the copy of the repeated
     * world nearest to [nearX]: by default the view center, so a location is drawn where
     * the camera is, or the previous point of a shape, so its segments take the short way,
     * also across the antimeridian.
     */
    private fun wrappedScreenX(
        pixelX: Float,
        centerPixelX: Float,
        nearX: Float = viewWidth / 2f,
    ): Float {
        val x = pixelX - centerPixelX + viewWidth / 2 - panOffsetX
        val world = ProjectionUtils.worldSize(zoom, tileSize).toFloat()
        return x - world * ((x - nearX) / world).roundToInt()
    }

    /**
     * Returns the screen coordinates of the points of a shape, as x and y pairs. Each point
     * is on the copy of the world nearest to the previous one, see [wrappedScreenX], and the
     * first one nearest to [nearX].
     */
    private fun shapeScreenPoints(
        points: List<LatLng>,
        centerPixelX: Float,
        centerPixelY: Float,
        nearX: Float = viewWidth / 2f,
    ): FloatArray {
        val out = FloatArray(points.size * 2)
        var previousX = nearX
        for ((i, point) in points.withIndex()) {
            val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(point, zoom, tileSize)
            previousX = wrappedScreenX(pixelX, centerPixelX, previousX)
            out[2 * i] = previousX
            out[2 * i + 1] = pixelY - centerPixelY + viewHeight / 2 - panOffsetY
        }
        return out
    }

    // Adds the points from shapeScreenPoints() to the path as a closed ring
    private fun addRing(
        path: android.graphics.Path,
        screenPoints: FloatArray,
    ) {
        path.moveTo(screenPoints[0], screenPoints[1])
        for (i in 2 until screenPoints.size step 2) {
            path.lineTo(screenPoints[i], screenPoints[i + 1])
        }
        path.close()
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
        zoom: Float,
    ): Float {
        val metersPerPixel = ProjectionUtils.metersPerPixelAtLatitude(latitude, zoom, tileSize)
        return (meters / metersPerPixel).toFloat()
    }

    private fun drawGroundOverlay(
        canvas: Canvas,
        centerPixelX: Float,
        centerPixelY: Float,
        groundOverlay: GroundOverlay,
    ) {
        val bitmap = loadGroundOverlayBitmap(groundOverlay)
        val rect = groundOverlayScreenRect(groundOverlay, bitmap, centerPixelX, centerPixelY) ?: return

        groundOverlayPaint.alpha = ((1f - groundOverlay.transparency) * 255).toInt()

        canvas.withSave {
            if (groundOverlay.bearing != 0f) {
                val anchorX = rect.left + rect.width() * groundOverlay.anchor.first
                val anchorY = rect.top + rect.height() * groundOverlay.anchor.second
                canvas.rotate(groundOverlay.bearing, anchorX, anchorY)
            }
            canvas.drawBitmap(bitmap, null, rect, groundOverlayPaint)
        }
    }

    /**
     * Returns where a ground overlay is drawn on screen, before rotation by its bearing.
     *
     * An overlay placed by bounds covers them, and its anchor is only the rotation pivot.
     * An overlay placed by position has the anchor point of the image at that position.
     *
     * @param bitmap The overlay image, which gives the aspect ratio when no height is set
     * @return The rectangle in view pixels, or null when the overlay has no placement
     */
    private fun groundOverlayScreenRect(
        groundOverlay: GroundOverlay,
        bitmap: Bitmap,
        centerPixelX: Float,
        centerPixelY: Float,
    ): RectF? {
        // Read once: the overlay is a live handle, so its properties can change
        val bounds = groundOverlay.bounds
        val position = groundOverlay.position
        val width = groundOverlay.width
        val height = groundOverlay.height
        val offsetY = viewHeight / 2 - panOffsetY - centerPixelY

        if (bounds != null) {
            val (nwPixelX, nwPixelY) =
                ProjectionUtils.latLngToPixel(
                    LatLng(bounds.northeast.latitude, bounds.southwest.longitude),
                    zoom,
                    tileSize,
                )
            val (sePixelX, sePixelY) =
                ProjectionUtils.latLngToPixel(
                    LatLng(bounds.southwest.latitude, bounds.northeast.longitude),
                    zoom,
                    tileSize,
                )
            // Bounds crossing the antimeridian end in the next copy of the world
            val world = ProjectionUtils.worldSize(zoom, tileSize).toFloat()
            val widthPixels = if (sePixelX >= nwPixelX) sePixelX - nwPixelX else sePixelX + world - nwPixelX
            // The copy whose middle is nearest to the view center
            val left = wrappedScreenX(nwPixelX + widthPixels / 2, centerPixelX) - widthPixels / 2
            return RectF(left, nwPixelY + offsetY, left + widthPixels, sePixelY + offsetY)
        }

        if (position == null || width == null) return null

        val (posPixelX, posPixelY) = ProjectionUtils.latLngToPixel(position, zoom, tileSize)
        val posScreenX = wrappedScreenX(posPixelX, centerPixelX)
        val metersPerPixel = ProjectionUtils.metersPerPixelAtLatitude(position.latitude, zoom, tileSize)
        val widthPixels = (width / metersPerPixel).toFloat()
        val heightPixels =
            if (height != null) {
                (height / metersPerPixel).toFloat()
            } else {
                widthPixels * bitmap.height / bitmap.width
            }

        val left = posScreenX - widthPixels * groundOverlay.anchor.first
        val top = posPixelY + offsetY - heightPixels * groundOverlay.anchor.second
        return RectF(left, top, left + widthPixels, top + heightPixels)
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
            strokePaint.pathEffect = strokeEffect(polygon.pattern, polygon.strokePattern, polygon.strokeWidth)
            strokePaint.strokeCap = polygon.strokeCap.toAndroidCap()
            strokePaint.strokeJoin = polygon.strokeJoin.toAndroidJoin()

            val path = android.graphics.Path()
            val outline = shapeScreenPoints(points, centerPixelX, centerPixelY)
            addRing(path, outline)

            for (hole in polygon.holes) {
                if (hole.size < 3) continue
                // Expand hole points with geodesic interpolation if needed
                val holePoints =
                    if (polygon.geodesic) {
                        GeodesicUtils.expandWithGeodesicPoints(hole)
                    } else {
                        hole
                    }
                // On the same copy of the world as the outline
                addRing(path, shapeScreenPoints(holePoints, centerPixelX, centerPixelY, outline[0]))
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
                paint.pathEffect = strokeEffect(polyline.pattern, polyline.strokePattern, polyline.strokeWidth)
                paint.strokeJoin = polyline.strokeJoin.toAndroidJoin()

                val screenPoints = shapeScreenPoints(points, centerPixelX, centerPixelY)
                val path = android.graphics.Path()
                path.moveTo(screenPoints[0], screenPoints[1])
                for (i in 2 until screenPoints.size step 2) {
                    path.lineTo(screenPoints[i], screenPoints[i + 1])
                }

                if (polyline.startCap == polyline.endCap) {
                    paint.strokeCap = polyline.startCap.toAndroidCap()
                    canvas.drawPath(path, paint)
                } else {
                    drawPathWithCaps(canvas, path, screenPoints, polyline.startCap, polyline.endCap, paint)
                }
            }
        }
    }

    /**
     * Draws a stroked [path] with a different cap at its start and its end.
     *
     * A Paint has one cap for both ends of a path. So the path is turned into the outline of
     * its stroke with butt ends, the cap shapes are added to the ends of that outline as a
     * union, and the result is filled once. Nothing is drawn before that single fill: drawing
     * the caps separately would make a translucent line darker where they overlap the line,
     * or leave a lighter seam where their anti-aliased edges touch it.
     *
     * @param screenPoints The x, y pairs of the path, to find the direction at each end
     * @param paint The stroke paint of the line, with its color, width, join and path effect
     */
    private fun drawPathWithCaps(
        canvas: Canvas,
        path: android.graphics.Path,
        screenPoints: FloatArray,
        startCap: StrokeCap,
        endCap: StrokeCap,
        paint: Paint,
    ) {
        paint.strokeCap = Paint.Cap.BUTT
        capOutline.rewind()
        paint.getFillPath(path, capOutline)

        val last = screenPoints.size - 2
        addCap(startCap, screenPoints, 0, 2, paint.strokeWidth)
        addCap(endCap, screenPoints, last, -2, paint.strokeWidth)

        capFillPaint.set(paint)
        capFillPaint.style = Paint.Style.FILL
        // The outline already has the dashes of a pattern
        capFillPaint.pathEffect = null
        canvas.drawPath(capOutline, capFillPaint)
    }

    /**
     * Adds a cap to [capOutline] at the end point at [endIndex] of [screenPoints], pointing away
     * from the nearest other point found by stepping [step] through the x, y pairs.
     */
    private fun addCap(
        cap: StrokeCap,
        screenPoints: FloatArray,
        endIndex: Int,
        step: Int,
        strokeWidth: Float,
    ) {
        if (cap != StrokeCap.Round && cap != StrokeCap.Square) return
        val x = screenPoints[endIndex]
        val y = screenPoints[endIndex + 1]

        // The end segment gives the direction; skip points at the same place as the end
        var i = endIndex + step
        while (i in 0..screenPoints.size - 2 && screenPoints[i] == x && screenPoints[i + 1] == y) i += step
        if (i !in 0..screenPoints.size - 2) return
        val dx = x - screenPoints[i]
        val dy = y - screenPoints[i + 1]

        // Built pointing along +x from the end point, then turned and moved into place. The
        // shape also covers up to half a width of the line inside the end, so the union
        // leaves no seam, but not more than the end segment, so it can't stick out at a corner.
        val half = strokeWidth / 2
        val inside = minOf(half, hypot(dx, dy))
        capPath.rewind()
        if (cap == StrokeCap.Round) {
            capRect.set(-half, -half, half, half)
            capPath.moveTo(-inside, -half)
            capPath.arcTo(capRect, -90f, 180f)
            capPath.lineTo(-inside, half)
            capPath.close()
        } else {
            capPath.addRect(-inside, -half, half, half, android.graphics.Path.Direction.CW)
        }
        capMatrix.setRotate(Math.toDegrees(atan2(dy, dx).toDouble()).toFloat())
        capMatrix.postTranslate(x, y)
        capPath.transform(capMatrix)
        capOutline.op(capPath, android.graphics.Path.Op.UNION)
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
        paint.pathEffect = strokeEffect(polyline.pattern, polyline.strokePattern, polyline.strokeWidth)
        paint.strokeJoin = polyline.strokeJoin.toAndroidJoin()

        var segmentIndex = 0
        val totalSegments = points.size - 1
        val screenPoints = shapeScreenPoints(points, centerPixelX, centerPixelY)

        // A single segment is both first and last, so it needs both caps
        if (totalSegments == 1 && polyline.startCap != polyline.endCap) {
            paint.color =
                polyline.spans
                    .first()
                    .color
                    .toArgb()
            val path = android.graphics.Path()
            path.moveTo(screenPoints[0], screenPoints[1])
            path.lineTo(screenPoints[2], screenPoints[3])
            drawPathWithCaps(canvas, path, screenPoints, polyline.startCap, polyline.endCap, paint)
            return
        }

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

                val i = segmentIndex * 2
                canvas.drawLine(screenPoints[i], screenPoints[i + 1], screenPoints[i + 2], screenPoints[i + 3], paint)
                segmentIndex++
            }
        }

        // Draw remaining segments with default color
        if (segmentIndex < totalSegments) {
            paint.color = polyline.strokeColor.toArgb()

            while (segmentIndex < totalSegments) {
                val isLastSegment = segmentIndex == totalSegments - 1
                paint.strokeCap = if (isLastSegment) polyline.endCap.toAndroidCap() else Paint.Cap.BUTT

                val i = segmentIndex * 2
                canvas.drawLine(screenPoints[i], screenPoints[i + 1], screenPoints[i + 2], screenPoints[i + 3], paint)
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
            strokePaint.pathEffect = strokeEffect(circle.pattern, circle.strokePattern, circle.strokeWidth)
            strokePaint.strokeCap = circle.strokeCap.toAndroidCap()
            strokePaint.strokeJoin = circle.strokeJoin.toAndroidJoin()

            val (pixelX, pixelY) = ProjectionUtils.latLngToPixel(circle.center, zoom, tileSize)
            val screenX = wrappedScreenX(pixelX, centerPixelX)
            val screenY = pixelY - centerPixelY + viewHeight / 2 - panOffsetY

            val radiusInPixels = metersToPixels(circle.radius, circle.center.latitude, zoom)

            canvas.drawCircle(screenX, screenY, radiusInPixels, fillPaint)
            canvas.drawCircle(screenX, screenY, radiusInPixels, strokePaint)
        }
    }

    /**
     * Returns the path effect of a stroke: its [pattern] of dots, dashes and gaps if set,
     * else its [strokePattern]. Converted patterns are kept, as they depend only on the width.
     */
    private fun strokeEffect(
        pattern: List<PatternItem>?,
        strokePattern: PathEffect?,
        strokeWidth: Float,
    ): android.graphics.PathEffect? {
        if (pattern == null) return strokePattern?.asAndroidPathEffect()
        patternEffects[pattern to strokeWidth]?.let { return it }
        if (patternEffects.size >= MAX_PATTERN_EFFECTS) patternEffects.clear()
        val effect = pattern.toAndroidPathEffect(strokeWidth) ?: return null
        // A copy as the key, so changing the caller's list later doesn't break the map
        patternEffects[pattern.toList() to strokeWidth] = effect
        return effect
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

            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom, tileSize)

            val screenX = wrappedScreenX(markerPixelX, centerPixelX)
            val screenY = markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY

            val icon = loadBitmap(marker.icon)

            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            paint.alpha = (marker.alpha * 255).toInt().coerceIn(0, 255)

            if (marker.rotation == 0f) {
                canvas.drawBitmap(icon, screenX - anchorX, screenY - anchorY, paint)
            } else {
                canvas.withRotation(marker.rotation, screenX, screenY) {
                    drawBitmap(icon, screenX - anchorX, screenY - anchorY, paint)
                }
            }
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

            buildInfoWindowFrame(rect)
            canvas.drawPath(infoWindowFramePath, infoWindowBackgroundPaint)
            canvas.drawPath(infoWindowFramePath, infoWindowBorderPaint)

            if (image != null) {
                // A view from InfoWindowAdapter.getInfoContents, inside the default frame
                canvas.drawBitmap(image.bitmap, rect.left + infoWindowPadding, rect.top + infoWindowPadding, null)
                continue
            }

            val title = marker.title
            val snippet = marker.snippet
            var textY = rect.top + infoWindowPadding + infoWindowTitlePaint.textSize

            if (title != null) {
                canvas.drawText(title, rect.left + infoWindowPadding, textY, infoWindowTitlePaint)
                textY += infoWindowLineSpacing
            }

            if (snippet != null) {
                textY += infoWindowSnippetPaint.textSize
                canvas.drawText(snippet, rect.left + infoWindowPadding, textY, infoWindowSnippetPaint)
            }
        }
    }

    /**
     * Sets [infoWindowFramePath] to the outline of the default frame: the rounded [rect]
     * with a triangle below its bottom center, pointing at the marker. One outline, so the
     * border has no seam between the two.
     */
    private fun buildInfoWindowFrame(rect: RectF) {
        val r = infoWindowCorner.coerceAtMost(minOf(rect.width(), rect.height()) / 2)
        val halfPointer = (infoWindowPointerWidth / 2).coerceAtMost(rect.width() / 2 - r)
        val centerX = rect.centerX()
        val corner = infoWindowCornerRect
        infoWindowFramePath.apply {
            rewind()
            moveTo(rect.left + r, rect.top)
            lineTo(rect.right - r, rect.top)
            corner.set(rect.right - 2 * r, rect.top, rect.right, rect.top + 2 * r)
            arcTo(corner, 270f, 90f)
            lineTo(rect.right, rect.bottom - r)
            corner.set(rect.right - 2 * r, rect.bottom - 2 * r, rect.right, rect.bottom)
            arcTo(corner, 0f, 90f)
            lineTo(centerX + halfPointer, rect.bottom)
            lineTo(centerX, rect.bottom + infoWindowPointerHeight)
            lineTo(centerX - halfPointer, rect.bottom)
            lineTo(rect.left + r, rect.bottom)
            corner.set(rect.left, rect.bottom - 2 * r, rect.left + 2 * r, rect.bottom)
            arcTo(corner, 90f, 90f)
            lineTo(rect.left, rect.top + r)
            corner.set(rect.left, rect.top, rect.left + 2 * r, rect.top + 2 * r)
            arcTo(corner, 180f, 90f)
            close()
        }
    }

    /**
     * Returns the screen area of the marker's info window, above its info window anchor,
     * or null if the window is not shown. The pointer of the default frame
     * is below this area, between it and the icon.
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
            val frame = if (image.framed) infoWindowPadding * 2 else 0f
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
                if (title != null) totalHeight += infoWindowLineSpacing
                totalHeight += infoWindowSnippetPaint.textSize
            }

            boxWidth = maxWidth + infoWindowPadding * 2
            boxHeight = totalHeight + infoWindowPadding * 2
        }

        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)
        val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom, tileSize)
        val screenX = wrappedScreenX(markerPixelX, centerPixelX)
        val screenY = markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY

        // The window points at the info window anchor of the icon, which turns with the marker
        val icon = loadBitmap(marker.icon)
        val iconLeft = screenX - icon.width * marker.anchor.first
        val iconTop = screenY - icon.height * marker.anchor.second
        var pointX = iconLeft + icon.width * marker.infoWindowAnchor.first
        var pointY = iconTop + icon.height * marker.infoWindowAnchor.second
        if (marker.rotation != 0f) {
            val rotated = rotatePointAround(pointX, pointY, screenX, screenY, marker.rotation)
            pointX = rotated.first
            pointY = rotated.second
        }

        // A view from InfoWindowAdapter.getInfoWindow has no pointer
        val pointer = if (image != null && !image.framed) 0f else infoWindowPointerHeight
        val left = pointX - boxWidth / 2
        val top = pointY - boxHeight - pointer - infoWindowGap
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
        val semaphore = tileDownloadSemaphore
        val downloader = tileDownloader ?: TileDownloader().also { tileDownloader = it }
        scope.launch(sourceDownloads + Dispatchers.IO) {
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
                        semaphore.withPermit {
                            // The source changed, or the camera moved on while this
                            // request was queued
                            if (cache !== tileCache || wantedTiles?.contains(tile) == false) {
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
        val size = tileSize * 2.0.pow((zoom - tile.zoom).toDouble())
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
                overlayTilePaint
            }

        for (placed in visibleTiles) {
            tileScreenRect(placed, centerPixelX, centerPixelY, tileDst)
            val tile = placed.wrapped()

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
        val semaphore = overlayDownloadSemaphores[overlay.id] ?: return
        scope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var noTile = false
            try {
                bitmap =
                    semaphore.withPermit {
                        val cachedBitmap = cache.get(tile)
                        if (cachedBitmap != null) {
                            cachedBitmap
                        } else {
                            // The provider may block: this runs on an IO thread
                            val tileData = overlay.tileProvider.getTile(tile.x, tile.y, tile.zoom)
                            if (tileData === TileProvider.NO_TILE) {
                                noTile = true
                                null
                            } else if (tileData != null) {
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
                    } else if (noTile) {
                        // Nothing to draw there, now or later
                        retries.onNoTile(tile)
                    } else {
                        // Not loaded now: asked again after a delay, not on every frame
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
        overlayDownloadSemaphores[tileOverlay.id] = Semaphore(MAX_CONCURRENT_OVERLAY_DOWNLOADS)
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
            overlayDownloadSemaphores.remove(tileOverlay.id)
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
        overlayDownloadSemaphores.clear()
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)

        // Check markers in reverse order (top to bottom) for correct z-ordering
        for (marker in markers.reversed()) {
            val (markerPixelX, markerPixelY) = ProjectionUtils.latLngToPixel(marker.position, zoom, tileSize)

            val screenX = wrappedScreenX(markerPixelX, centerPixelX)
            val screenY = markerPixelY - centerPixelY + viewHeight / 2 - panOffsetY

            val icon = loadBitmap(marker.icon)
            val anchorX = icon.width * marker.anchor.first
            val anchorY = icon.height * marker.anchor.second

            // A rotated icon is hit where the touch, rotated back around the anchor, hits the upright icon
            val (touchX, touchY) =
                if (marker.rotation == 0f) {
                    x to y
                } else {
                    rotatePointAround(x, y, screenX, screenY, -marker.rotation)
                }

            val markerLeft = screenX - anchorX
            val markerTop = screenY - anchorY
            val markerRight = markerLeft + icon.width
            val markerBottom = markerTop + icon.height

            if (touchX >= markerLeft && touchX <= markerRight && touchY >= markerTop && touchY <= markerBottom) {
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)

        // Check polylines in reverse order (top to bottom) for correct z-ordering
        for (polyline in polylines.reversed()) {
            if (!polyline.clickable) continue

            // Convert all points to screen coordinates, as they are drawn
            val screenPoints = shapeScreenPoints(polyline.points, centerPixelX, centerPixelY)

            // Check each line segment
            for (i in 0 until screenPoints.size - 2 step 2) {
                val distance =
                    distanceToLineSegment(x, y, screenPoints[i], screenPoints[i + 1], screenPoints[i + 2], screenPoints[i + 3])
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)

        // Check polygons in reverse order (top to bottom) for correct z-ordering
        for (polygon in polygons.reversed()) {
            if (!polygon.clickable) continue

            // Convert all points to screen coordinates, as they are drawn
            val screenPoints = shapeScreenPoints(polygon.points, centerPixelX, centerPixelY)

            // Check if point is inside the main polygon
            if (isPointInPolygon(x, y, screenPoints)) {
                // Check if point is inside any holes
                var inHole = false
                for (hole in polygon.holes) {
                    val holeScreenPoints = shapeScreenPoints(hole, centerPixelX, centerPixelY, screenPoints[0])
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)

        // Check circles in reverse order (top to bottom) for correct z-ordering
        for (circle in circles.reversed()) {
            if (!circle.clickable) continue

            // Convert circle center to screen coordinates
            val (px, py) = ProjectionUtils.latLngToPixel(circle.center, zoom, tileSize)
            val sx = wrappedScreenX(px, centerPixelX)
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
        val (centerPixelX, centerPixelY) = ProjectionUtils.latLngToPixel(center, zoom, tileSize)

        for (groundOverlay in groundOverlays.reversed()) {
            if (!groundOverlay.clickable) continue

            val bitmap = loadGroundOverlayBitmap(groundOverlay)
            val rect = groundOverlayScreenRect(groundOverlay, bitmap, centerPixelX, centerPixelY) ?: continue

            if (groundOverlay.bearing != 0f) {
                val anchorX = rect.left + rect.width() * groundOverlay.anchor.first
                val anchorY = rect.top + rect.height() * groundOverlay.anchor.second
                val rotatedPoint = rotatePointAround(x, y, anchorX, anchorY, -groundOverlay.bearing)
                if (rect.contains(rotatedPoint.first, rotatedPoint.second)) {
                    return groundOverlay
                }
            } else if (rect.contains(x, y)) {
                return groundOverlay
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
     * @param points Polygon vertices in screen coordinates, as x and y pairs, see [shapeScreenPoints]
     * @return True if point is inside polygon
     */
    private fun isPointInPolygon(
        x: Float,
        y: Float,
        points: FloatArray,
    ): Boolean {
        var inside = false
        var j = points.size / 2 - 1

        for (i in 0 until points.size / 2) {
            val xi = points[2 * i]
            val yi = points[2 * i + 1]
            val xj = points[2 * j]
            val yj = points[2 * j + 1]

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
     * Stops camera animations and tile downloads and closes the HTTP client. Called when
     * the view is detached from its window or its lifecycle owner is destroyed. Calling it
     * more than once is safe, and the map keeps working afterwards: tiles that were still
     * loading are requested again on the next draw.
     */
    fun onDestroy() {
        // A destroyed map reports no more camera events
        hasPendingMoveEvents = false
        cameraEventHandler.removeCallbacks(pendingMoveEvents)
        stopAnimation()
        scope.coroutineContext.cancelChildren()
        // cancelChildren() also canceled the downloads job, which could not start new ones
        sourceDownloads = SupervisorJob(scope.coroutineContext[Job])
        tileDownloader?.close()
        tileDownloader = null
        // Cancelled downloads never remove their tiles, so start with empty sets
        downloadingTiles = mutableSetOf()
        overlayDownloadingTiles.values.forEach { it.clear() }
        // The default marker icons are kept: other maps may be drawing them, and the
        // factory keeps at most 10 small ones
    }
}
