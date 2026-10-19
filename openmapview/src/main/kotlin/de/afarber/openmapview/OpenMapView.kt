/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.animation.TimeInterpolator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.withClip
import androidx.core.graphics.withTranslation
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import kotlin.math.cos
import kotlin.math.pow
import kotlin.time.Duration

// Finger movement in pixels below which a touch counts as a tap and a marker drag does not start
private const val TAP_SLOP_PX = 10f

// Duration of the camera animation to the device location for the my-location button
private const val MY_LOCATION_ANIMATION_MS = 250

// Duration of the zoom animation for the built-in zoom buttons
private const val ZOOM_BUTTON_ANIMATION_MS = 250

// Duration of the camera animation to a clicked marker
private const val MARKER_CLICK_ANIMATION_MS = 250

/**
 * A MapView powered by OpenStreetMap tiles.
 *
 * This view provides a modern MapView implementation supporting pan and zoom gestures,
 * markers, polylines, polygons, camera animations, and GeoJSON import. The view automatically
 * manages lifecycle events and tile caching for optimal performance.
 *
 * Usage example:
 * ```kotlin
 * val mapView = OpenMapView(context)
 * mapView.setCenter(LatLng(51.5074, -0.1278))
 * mapView.setZoom(12.0)
 * mapView.addMarker(Marker(position = LatLng(51.5074, -0.1278), title = "London"))
 * ```
 *
 * The view manages its lifecycle itself: while attached to a window, it observes the
 * lifecycle owner of its view tree (the Activity, Fragment or Compose screen). When the
 * view is detached or that owner is destroyed, tile downloads and animations stop and the
 * HTTP client is closed. A view that is attached again keeps working.
 *
 * ## Inherited View Methods
 *
 * As OpenMapView extends [FrameLayout], all standard Android View methods are available,
 * including accessibility support via [android.view.View.setContentDescription].
 * Use `setContentDescription(String)` to provide accessibility descriptions for screen readers.
 *
 * Example:
 * ```kotlin
 * mapView.setContentDescription("Map showing current location")
 * ```
 */
class OpenMapView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr),
        DefaultLifecycleObserver {
        private val controller = MapController(context)
        private val attributionOverlay = AttributionOverlay(context)
        private val zoomControlsOverlay = ZoomControlsOverlay(context)
        private val myLocationOverlay = MyLocationOverlay(context)
        private val uiSettings = UiSettings()
        private var currentMapType: Int = MapType.STANDARD

        // Set by setMapPadding(); the map's own controls stay inside it
        private var mapPaddingLeft = 0
        private var mapPaddingTop = 0
        private var mapPaddingRight = 0
        private var mapPaddingBottom = 0
        private var lastTouchX = 0f
        private var lastTouchY = 0f

        // The average position of all fingers on the map, which pans it
        private var focalX = 0f
        private var focalY = 0f
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var onMapClickListener: OnMapClickListener? = null
        private var onMapLongClickListener: OnMapLongClickListener? = null
        private var infoWindowAdapter: InfoWindowAdapter? = null

        // The my-location layer: the source set by the app or the platform default, the one
        // currently sending locations, and the last location received
        private var isMyLocationLayerEnabled = false
        private var customLocationSource: LocationSource? = null
        private val platformLocationSource by lazy { PlatformLocationSource(context) }
        private var activeLocationSource: LocationSource? = null
        private var myLocation: Location? = null
        private var isPaused = false
        private var onMyLocationButtonClickListener: OnMyLocationButtonClickListener? = null
        private var onMyLocationClickListener: OnMyLocationClickListener? = null

        private var onMarkerDragListener: OnMarkerDragListener? = null
        private var draggedMarker: Marker? = null
        private var isDragging = false

        // Where the finger grabbed the dragged marker, relative to its anchor point
        private var grabOffsetX = 0f
        private var grabOffsetY = 0f
        private val infoWindowHandler = Handler(Looper.getMainLooper())
        private var infoWindowDismissRunnable: Runnable? = null

        // The lifecycle observed while attached to a window
        private var observedLifecycle: Lifecycle? = null

        // Set when the current gesture became a long press, so lifting the finger is no click
        private var isLongPressed = false

        private val gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onLongPress(e: MotionEvent) {
                        isLongPressed = true
                        // A long press on an info window is not a long press on the map
                        val touchedInfoWindow = controller.handleInfoWindowTouch(e.x, e.y)
                        if (touchedInfoWindow != null) {
                            controller.onInfoWindowLongClickListener?.onInfoWindowLongClick(touchedInfoWindow)
                            return
                        }
                        val latLng = controller.screenToLatLng(e.x, e.y)
                        onMapLongClickListener?.onMapLongClick(latLng)
                    }
                },
            )

        private val scaleGestureDetector =
            ScaleGestureDetector(
                context,
                object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    override fun onScale(detector: ScaleGestureDetector): Boolean {
                        val scaleFactor = detector.scaleFactor
                        // When moving fingers also pan the map, zoom around their previous
                        // center; the pan in onTouchEvent then moves it to the new one, so
                        // the content stays under the fingers
                        val pans = uiSettings.isScrollGesturesEnabled && uiSettings.isScrollGesturesEnabledDuringRotateOrZoom
                        val focusX = if (pans) lastTouchX else detector.focusX
                        val focusY = if (pans) lastTouchY else detector.focusY
                        val oldZoom = controller.getZoom()
                        controller.zoom(scaleFactor, focusX, focusY)
                        // Nothing to report when the zoom is already at its limit
                        if (controller.getZoom() != oldZoom) {
                            controller.reportGestureMove()
                        }
                        invalidate()
                        return true
                    }
                },
            )

        init {
            setWillNotDraw(false)
            controller.setUiSettings(uiSettings)
            // E.g. the zoom buttons appear right away when they are enabled
            uiSettings.onDrawnSettingChanged = { invalidate() }
            controller.setOnTileLoadedCallback {
                invalidate()
            }
        }

        override fun dispatchDraw(canvas: Canvas) {
            // Tiles and shapes extend past the view edges. Not every parent clips its
            // children (e.g. Compose's AndroidView does not), so clip here.
            canvas.withClip(0, 0, width, height) {
                super.dispatchDraw(this)
                controller.draw(this)
                drawMyLocation(this)

                // The map's own controls are drawn inside the map padding
                withTranslation(mapPaddingLeft.toFloat(), mapPaddingTop.toFloat()) {
                    if (isMyLocationLayerEnabled && uiSettings.isMyLocationButtonEnabled) {
                        myLocationOverlay.drawButton(this, controlsWidth())
                    }
                    if (uiSettings.isZoomControlsEnabled) {
                        zoomControlsOverlay.draw(this, controlsWidth(), controlsHeight())
                    }
                    attributionOverlay.draw(this, controlsWidth(), controlsHeight())
                }

                // Draw custom edge glow effects LAST to ensure they're on top
                controller.drawEdgeEffects(this, width, height)
            }

            // Continue invalidating if edge glows are still active
            if (controller.hasActiveEdgeEffect()) {
                invalidate()
            }
        }

        // Sets focalX and focalY to the average position of the fingers that stay on the map
        private fun updateFocalPoint(event: MotionEvent) {
            val liftedIndex = if (event.actionMasked == MotionEvent.ACTION_POINTER_UP) event.actionIndex else -1
            var sumX = 0f
            var sumY = 0f
            var count = 0
            for (i in 0 until event.pointerCount) {
                if (i == liftedIndex) continue
                sumX += event.getX(i)
                sumY += event.getY(i)
                count++
            }
            if (count > 0) {
                focalX = sumX / count
                focalY = sumY / count
            }
        }

        private fun isOnDraggableMarker(event: MotionEvent): Boolean = controller.handleMarkerTouch(event.x, event.y)?.draggable == true

        // Called on a night mode switch when the activity handles uiMode changes itself;
        // otherwise the activity is recreated and the new map reads the configuration
        override fun onConfigurationChanged(newConfig: Configuration) {
            super.onConfigurationChanged(newConfig)
            controller.setNightMode(
                newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES,
            )
            invalidate()
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            controller.setViewSize(w, h)
            invalidate()
        }

        override fun performClick(): Boolean = super.performClick()

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Keep scroll containers (ScrollView, ViewPager, Compose verticalScroll) from taking
            // over drags the map handles itself. The parent resets this when the gesture ends.
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Touching the map stops a camera animation, which reports onCancel()
                    // and onCameraMoveCanceled
                    controller.stopAnimation()
                    if (uiSettings.isScrollGesturesEnabled || isOnDraggableMarker(event)) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    if (uiSettings.isZoomGesturesEnabled) {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }
            }

            // Let gesture detector handle long press (but not during drag)
            if (!isDragging) {
                gestureDetector.onTouchEvent(event)
            }

            // Let scale detector handle pinch gestures only if zoom gestures are enabled
            if (uiSettings.isZoomGesturesEnabled) {
                scaleGestureDetector.onTouchEvent(event)
            }

            // Pan by the average of all fingers, so a pinch, whose fingers move apart around
            // a fixed point, doesn't pan the map before the scale detector takes over
            updateFocalPoint(event)
            when (event.actionMasked) {
                // A finger was added or lifted: go on from the new focal point instead of jumping
                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP -> {
                    lastTouchX = focalX
                    lastTouchY = focalY
                    return true
                }
            }
            if (scaleGestureDetector.isInProgress) {
                // Moving both fingers while pinching also pans the map, unless disabled
                val allowScroll = uiSettings.isScrollGesturesEnabled && uiSettings.isScrollGesturesEnabledDuringRotateOrZoom
                if (event.actionMasked == MotionEvent.ACTION_MOVE && allowScroll) {
                    val dx = focalX - lastTouchX
                    val dy = focalY - lastTouchY
                    if (dx != 0f || dy != 0f) {
                        controller.updatePanOffset(dx, dy)
                        controller.reportGestureMove()
                        invalidate()
                    }
                }
                lastTouchX = focalX
                lastTouchY = focalY
            }

            // Handle dragging and panning only if not scaling
            if (!scaleGestureDetector.isInProgress) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        isLongPressed = false
                        lastTouchX = event.x
                        lastTouchY = event.y
                        initialTouchX = event.x
                        initialTouchY = event.y
                        // Check if touch is on a draggable marker
                        val touchedMarker = controller.handleMarkerTouch(event.x, event.y)
                        if (touchedMarker != null && touchedMarker.draggable) {
                            draggedMarker = touchedMarker
                            isDragging = false
                        }
                        return true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val dx = focalX - lastTouchX
                        val dy = focalY - lastTouchY
                        val movementDistance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

                        if (draggedMarker != null) {
                            // Start dragging once the finger has moved past the slop from the
                            // down point; until then the map must not pan under the marker
                            if (!isDragging) {
                                val fromDownX = event.x - initialTouchX
                                val fromDownY = event.y - initialTouchY
                                if (fromDownX * fromDownX + fromDownY * fromDownY <= TAP_SLOP_PX * TAP_SLOP_PX) {
                                    return true
                                }
                                isDragging = true
                                controller.commitPan()
                                // The marker keeps its place under the finger instead of
                                // jumping with its anchor point to the finger
                                val anchor = controller.createProjection().toScreenLocation(draggedMarker!!.position)
                                grabOffsetX = initialTouchX - anchor.x
                                grabOffsetY = initialTouchY - anchor.y
                                onMarkerDragListener?.onMarkerDragStart(draggedMarker!!)
                            }

                            // Continue dragging
                            if (isDragging) {
                                val latLng = controller.screenToLatLng(event.x - grabOffsetX, event.y - grabOffsetY)
                                draggedMarker!!.position = latLng
                                onMarkerDragListener?.onMarkerDrag(draggedMarker!!)
                                invalidate()
                                lastTouchX = event.x
                                lastTouchY = event.y
                                return true
                            }
                        }

                        // Pan the map if not dragging a marker and scroll gestures are enabled
                        val allowScroll = !isDragging && uiSettings.isScrollGesturesEnabled

                        if (allowScroll) {
                            controller.updatePanOffset(dx, dy)

                            // Fire camera move events during pan
                            if (movementDistance > 0) {
                                controller.reportGestureMove()
                            }

                            lastTouchX = focalX
                            lastTouchY = focalY
                            invalidate()
                        }
                        return true
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Release edge glows when touch ends
                        controller.releaseEdgeEffects()

                        // Handle drag end
                        if (isDragging && draggedMarker != null) {
                            onMarkerDragListener?.onMarkerDragEnd(draggedMarker!!)
                            draggedMarker = null
                            isDragging = false
                            invalidate()
                            return true
                        }

                        // Reset drag state
                        draggedMarker = null
                        isDragging = false

                        // Check if touch is on a marker (only if there was minimal movement)
                        val dx = event.x - initialTouchX
                        val dy = event.y - initialTouchY
                        val movementDistance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

                        // A tap ends with ACTION_UP; a canceled gesture or a long press is no click
                        if (event.action == MotionEvent.ACTION_UP && !isLongPressed && movementDistance < TAP_SLOP_PX) {
                            // Report the tap as a click, so accessibility services and any
                            // OnClickListener are notified like for other clickable views
                            performClick()

                            // Check zoom controls first (if enabled)
                            if (uiSettings.isZoomControlsEnabled) {
                                val zoomAction = zoomControlsOverlay.handleTouch(event.x - mapPaddingLeft, event.y - mapPaddingTop)
                                if (zoomAction != 0) {
                                    val update = if (zoomAction > 0) CameraUpdateFactory.zoomIn() else CameraUpdateFactory.zoomOut()
                                    // The map's own buttons animate and report an API animation
                                    controller.animateCamera(
                                        update,
                                        ZOOM_BUTTON_ANIMATION_MS,
                                        null,
                                        OnCameraMoveStartedListener.REASON_API_ANIMATION,
                                    )
                                    invalidate()
                                    return true
                                }
                            }

                            if (handleMyLocationTap(event.x, event.y)) {
                                controller.commitPan()
                                invalidate()
                                return true
                            }

                            // Check attribution overlay
                            if (attributionOverlay.handleTouch(
                                    event.x - mapPaddingLeft,
                                    event.y - mapPaddingTop,
                                    controlsWidth(),
                                    controlsHeight(),
                                )
                            ) {
                                controller.commitPan()
                                invalidate()
                                return true
                            }

                            // Check for info window touch (before marker touch)
                            val touchedInfoWindow = controller.handleInfoWindowTouch(event.x, event.y)
                            if (touchedInfoWindow != null) {
                                controller.onInfoWindowClickListener?.onInfoWindowClick(touchedInfoWindow)
                                controller.commitPan()
                                invalidate()
                                return true
                            }

                            // Check for marker touch
                            val touchedMarker = controller.handleMarkerTouch(event.x, event.y)
                            if (touchedMarker != null) {
                                controller.commitPan()
                                // The listener runs first. Unless it consumes the click, the marker's
                                // info window opens and the camera moves to the marker.
                                val consumed = controller.onMarkerClickListener?.onMarkerClick(touchedMarker) == true
                                if (!consumed) {
                                    if (!touchedMarker.isInfoWindowShown) {
                                        closeOtherInfoWindows(touchedMarker)
                                        openInfoWindow(touchedMarker)
                                    }
                                    controller.animateCamera(
                                        CameraUpdateFactory.newLatLng(touchedMarker.position),
                                        MARKER_CLICK_ANIMATION_MS,
                                        null,
                                        OnCameraMoveStartedListener.REASON_API_ANIMATION,
                                    )
                                }
                                invalidate()
                                return true
                            }

                            // Check for polyline touch
                            val touchedPolyline = controller.handlePolylineTouch(event.x, event.y)
                            if (touchedPolyline != null) {
                                controller.onPolylineClickListener?.onPolylineClick(touchedPolyline)
                                controller.commitPan()
                                invalidate()
                                return true
                            }

                            // Check for polygon touch
                            val touchedPolygon = controller.handlePolygonTouch(event.x, event.y)
                            if (touchedPolygon != null) {
                                controller.onPolygonClickListener?.onPolygonClick(touchedPolygon)
                                controller.commitPan()
                                invalidate()
                                return true
                            }

                            // Check for circle touch
                            val touchedCircle = controller.handleCircleTouch(event.x, event.y)
                            if (touchedCircle != null) {
                                controller.onCircleClickListener?.onCircleClick(touchedCircle)
                                controller.commitPan()
                                invalidate()
                                return true
                            }

                            // Check for ground overlay touch
                            val touchedGroundOverlay = controller.handleGroundOverlayTouch(event.x, event.y)
                            if (touchedGroundOverlay != null) {
                                controller.onGroundOverlayClickListener?.onGroundOverlayClick(touchedGroundOverlay)
                                controller.commitPan()
                                invalidate()
                                return true
                            }

                            // Fire map click event if nothing else was clicked
                            // Hide all info windows when clicking on empty map area
                            controller.getMarkers().forEach { it.hideInfoWindow() }
                            val latLng = controller.screenToLatLng(event.x, event.y)
                            onMapClickListener?.onMapClick(latLng)
                        }

                        controller.commitPan()
                        invalidate()
                        return true
                    }
                }
            }
            return true
        }

        /**
         * Sets the map center to the specified geographic location.
         *
         * @param latLng The target location for the map center
         */
        fun setCenter(latLng: LatLng) {
            controller.cancelPendingCameraUpdate()
            controller.setCenter(latLng)
            invalidate()
        }

        /**
         * Sets the zoom level of the map.
         *
         * Zoom levels range from 2.0 (world view) to 19.0 (street level).
         * Values outside this range will be clamped.
         *
         * @param zoom The target zoom level
         */
        fun setZoom(zoom: Float) {
            controller.cancelPendingCameraUpdate()
            controller.setZoom(zoom)
            invalidate()
        }

        /**
         * Returns the current zoom level of the map.
         *
         * @return The current zoom level (2.0 to 19.0)
         */
        fun getZoom(): Float = controller.getZoom()

        /**
         * Sets how many screen pixels a pixel of a 256 px tile covers.
         *
         * The default is the screen density, so tiles are 256 dp wide as with the Maps SDK,
         * and the same camera position shows the same area on every screen. 1 draws one tile
         * pixel per screen pixel: the map shows more area, with smaller labels.
         *
         * @param scale Screen pixels per tile pixel, greater than 0
         */
        fun setTileScale(scale: Float) {
            controller.setTileScale(scale)
            invalidate()
        }

        /**
         * Returns how many screen pixels a pixel of a 256 px tile covers, see [setTileScale].
         */
        fun getTileScale(): Float = controller.getTileScale()

        /**
         * Sets a color filter for the base map tiles, e.g. [TileColorFilters.invertedDark]
         * for a dark map without an API key.
         *
         * Works on every API level. Only the tiles and their placeholders are filtered:
         * markers, shapes, ground and tile overlays and info windows keep their colors.
         *
         * @param filter The filter, e.g. a `ColorMatrixColorFilter`, or null to draw the
         *   tiles unchanged
         */
        fun setTileColorFilter(filter: ColorFilter?) {
            if (filter === controller.getTileColorFilter()) return
            controller.setTileColorFilter(filter)
            invalidate()
        }

        /**
         * Returns the color filter of the base map tiles, see [setTileColorFilter].
         */
        fun getTileColorFilter(): ColorFilter? = controller.getTileColorFilter()

        /**
         * Sets the color scheme of the map, as with the Maps SDK.
         *
         * The dark scheme is an approximation: the light tiles are drawn with
         * [TileColorFilters.invertedDark], so it needs no API key and works on every API
         * level. Map types that are dark already, such as [MapType.CARTO_DARK], are drawn
         * unchanged, and a filter set with [setTileColorFilter] takes precedence. Markers,
         * shapes and info windows keep their colors.
         *
         * @param scheme [MapColorScheme.LIGHT] (the default), [MapColorScheme.DARK] or
         *   [MapColorScheme.FOLLOW_SYSTEM] for the night mode of the device
         */
        fun setMapColorScheme(scheme: Int) {
            controller.setMapColorScheme(scheme)
            invalidate()
        }

        /**
         * Returns the color scheme of the map, one of the [MapColorScheme] constants.
         */
        fun getMapColorScheme(): Int = controller.getMapColorScheme()

        /**
         * Sets the minimum zoom level preference.
         *
         * Constrains the camera zoom level to not go below this value.
         * If the current zoom is below the new minimum, it will be adjusted.
         *
         * @param minZoom The minimum zoom level (will be clamped to 2.0-19.0)
         */
        fun setMinZoomPreference(minZoom: Float) {
            controller.setMinZoomPreference(minZoom)
            invalidate()
        }

        /**
         * Sets the maximum zoom level preference.
         *
         * Constrains the camera zoom level to not go above this value.
         * If the current zoom is above the new maximum, it will be adjusted.
         *
         * @param maxZoom The maximum zoom level (will be clamped to 2.0-19.0)
         */
        fun setMaxZoomPreference(maxZoom: Float) {
            controller.setMaxZoomPreference(maxZoom)
            invalidate()
        }

        /**
         * Returns the current minimum zoom level preference.
         *
         * @return The minimum zoom level
         */
        fun getMinZoomLevel(): Float = controller.getMinZoomLevel()

        /**
         * Returns the current maximum zoom level preference.
         *
         * @return The maximum zoom level
         */
        fun getMaxZoomLevel(): Float = controller.getMaxZoomLevel()

        /**
         * Resets the minimum and maximum zoom preferences to their defaults (2.0 - 19.0).
         *
         * If the current zoom is outside the default range, it will be adjusted.
         */
        fun resetMinMaxZoomPreference() {
            controller.resetMinMaxZoomPreference()
            invalidate()
        }

        /**
         * Sets a LatLngBounds to constrain the camera target.
         *
         * When bounds are set, the camera target (center point) is constrained to remain within
         * these bounds for both user gestures (pan/scroll) and programmatic camera movements
         * (via [moveCamera] or [animateCamera]).
         *
         * The map viewport may show areas outside the bounds (since the camera target is just the
         * center point), but the center itself cannot move outside the specified region.
         *
         * If the camera is currently positioned outside the bounds when this method is called,
         * it will be immediately adjusted to the nearest point within the bounds.
         *
         * @param bounds The bounds to constrain the camera target, or null to remove constraints
         */
        fun setLatLngBoundsForCameraTarget(bounds: LatLngBounds?) {
            controller.setLatLngBoundsForCameraTarget(bounds)
            invalidate()
        }

        /**
         * Returns the current camera target bounds constraint.
         *
         * @return The bounds constraining the camera target, or null if no constraint is set
         */
        fun getLatLngBoundsForCameraTarget(): LatLngBounds? = controller.getLatLngBoundsForCameraTarget()

        /**
         * Sets the type of map tiles that should be displayed.
         *
         * OpenMapView provides free OpenStreetMap-based tile sources.
         *
         * **Supported types:**
         * - [MapType.NONE] - No base map tiles
         * - [MapType.STANDARD] - Standard road map (default)
         * - [MapType.CYCLOSM] - Cycling-focused map style
         * - [MapType.CYCLEMAP] - Thunderforest cycling map (requires API key)
         * - [MapType.TRANSPORT] - Public transport map (requires API key)
         * - [MapType.TRANSPORT_DARK] - Dark mode public transport map (requires API key)
         * - [MapType.TRACESTRACK_TOPO] - Topographic map with contour lines (requires API key)
         * - [MapType.HUMANITARIAN] - Humanitarian-focused style
         * - [MapType.OPNVKARTE] - German public transport
         *
         * When the map type changes, the tile cache is cleared and the map is redrawn.
         *
         * Example:
         * ```kotlin
         * mapView.setMapType(MapType.CYCLOSM)
         * mapView.setMapType(MapType.HUMANITARIAN)
         * ```
         *
         * @param type The map type constant from [MapType]
         * @throws IllegalArgumentException if type is not recognized
         * @see MapType
         * @see getMapType
         */
        fun setMapType(type: Int) {
            require(type in 0..14) { "Unknown map type: $type. Must be a valid MapType constant (0-14)." }

            currentMapType = type
            controller.setMapType(type)

            // Credit the tiles that are actually shown: without its API key, a map type
            // falls back to the standard map
            val source = controller.getTileSource() ?: TileSource.fromMapType(type)
            if (type != MapType.NONE) {
                attributionOverlay.setAttributionText(source.attributionText)
                attributionOverlay.setAttributionUrl(source.attributionUrl)
            }
            invalidate()
        }

        /**
         * Returns the current map type.
         *
         * @return The current map type constant from [MapType]
         * @see MapType
         * @see setMapType
         */
        fun getMapType(): Int = currentMapType

        /**
         * Returns whether indoor maps are enabled.
         *
         * @return false - indoor maps are not supported by OpenMapView.
         *         Standard OSM tiles do not include indoor map rendering.
         */
        fun isIndoorEnabled(): Boolean = false

        /**
         * Returns whether the traffic layer is enabled.
         *
         * @return false - traffic layer is not supported by OpenMapView.
         *         OSM tiles do not include real-time traffic data.
         *         Consider using custom tile overlays for third-party traffic data.
         */
        fun isTrafficEnabled(): Boolean = false

        /**
         * Returns whether buildings are enabled on the map.
         *
         * @return true - buildings are always rendered in OSM tiles and cannot be disabled.
         *         Building visibility is controlled by the tile server, not the client.
         */
        fun isBuildingsEnabled(): Boolean = true

        /**
         * Returns whether the my-location layer is enabled.
         *
         * @see setMyLocationEnabled
         */
        fun isMyLocationEnabled(): Boolean = isMyLocationLayerEnabled

        /**
         * Enables or disables the my-location layer: a blue dot at the
         * device location with an accuracy circle, and the my-location button (see
         * [UiSettings.isMyLocationButtonEnabled]).
         *
         * Locations come from the platform `LocationManager` (GPS and network), or from the
         * source set with [setLocationSource]. They are only requested while the map is
         * attached and not paused.
         *
         * The app must declare `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` in its
         * manifest and request it at runtime before enabling the layer.
         *
         * @param enabled true to show the device location
         * @throws SecurityException if enabled with the default source and no location permission
         */
        fun setMyLocationEnabled(enabled: Boolean) {
            if (enabled && customLocationSource == null && !PlatformLocationSource.hasLocationPermission(context)) {
                throw SecurityException("The my-location layer needs ACCESS_FINE_LOCATION or ACCESS_COARSE_LOCATION")
            }
            isMyLocationLayerEnabled = enabled
            if (!enabled) {
                stopLocationUpdates()
                myLocation = null
            }
            updateLocationUpdates()
            invalidate()
        }

        /**
         * Sets the source of locations for the my-location layer, or null for the default
         * platform `LocationManager`.
         *
         * Example, with the fused location provider of Play Services:
         * ```kotlin
         * mapView.setLocationSource(object : LocationSource {
         *     override fun activate(listener: LocationSource.OnLocationChangedListener) {
         *         // Request fused location updates and pass them to listener.onLocationChanged
         *     }
         *
         *     override fun deactivate() {
         *         // Remove the location updates
         *     }
         * })
         * ```
         *
         * @param source The location source, or null for the default one
         */
        fun setLocationSource(source: LocationSource?) {
            stopLocationUpdates()
            customLocationSource = source
            updateLocationUpdates()
        }

        /**
         * Sets a listener called when the my-location button is tapped.
         *
         * @param listener The listener, returning true to keep the camera where it is
         */
        fun setOnMyLocationButtonClickListener(listener: OnMyLocationButtonClickListener?) {
            onMyLocationButtonClickListener = listener
        }

        /**
         * Sets a listener called when the my-location dot is tapped.
         *
         * @param listener The listener
         */
        fun setOnMyLocationClickListener(listener: OnMyLocationClickListener?) {
            onMyLocationClickListener = listener
        }

        // Requests locations while the layer is enabled and the map is shown
        private fun updateLocationUpdates() {
            val wanted = isMyLocationLayerEnabled && isAttachedToWindow && !isPaused
            if (!wanted) {
                stopLocationUpdates()
                return
            }
            if (activeLocationSource != null) return

            val source = customLocationSource ?: platformLocationSource
            activeLocationSource = source
            source.activate { location ->
                // Ignore late locations from a source that was replaced or stopped
                if (activeLocationSource === source) {
                    myLocation = location
                    invalidate()
                }
            }
        }

        private fun stopLocationUpdates() {
            activeLocationSource?.deactivate()
            activeLocationSource = null
        }

        private fun drawMyLocation(canvas: Canvas) {
            if (!isMyLocationLayerEnabled) return

            val location = myLocation
            if (location == null) {
                myLocationOverlay.clearLocation()
            } else {
                val position = LatLng(location.latitude, location.longitude)
                val point = getProjection().toScreenLocation(position)
                // The accuracy in meters, converted with the Mercator scale at that latitude
                val metersPerPixel = 156543.03392 * cos(Math.toRadians(location.latitude)) / 2.0.pow(getZoom().toDouble())
                val accuracy = if (location.hasAccuracy()) (location.accuracy / metersPerPixel).toFloat() else 0f
                myLocationOverlay.drawLocation(canvas, point.x.toFloat(), point.y.toFloat(), accuracy)
            }
        }

        // The size of the area inside the map padding, where the map's own controls are drawn
        private fun controlsWidth(): Int = (width - mapPaddingLeft - mapPaddingRight).coerceAtLeast(0)

        private fun controlsHeight(): Int = (height - mapPaddingTop - mapPaddingBottom).coerceAtLeast(0)

        // Handles a tap on the my-location button or dot; returns whether it hit one of them
        private fun handleMyLocationTap(
            x: Float,
            y: Float,
        ): Boolean {
            if (!isMyLocationLayerEnabled) return false

            if (uiSettings.isMyLocationButtonEnabled && myLocationOverlay.isOnButton(x - mapPaddingLeft, y - mapPaddingTop)) {
                val consumed = onMyLocationButtonClickListener?.onMyLocationButtonClick() ?: false
                val location = myLocation
                if (!consumed && location != null) {
                    // Like the zoom buttons, the map's own button reports an API animation
                    controller.animateCamera(
                        CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)),
                        MY_LOCATION_ANIMATION_MS,
                        null,
                        OnCameraMoveStartedListener.REASON_API_ANIMATION,
                    )
                }
                return true
            }

            val location = myLocation
            val listener = onMyLocationClickListener
            if (location != null && listener != null && myLocationOverlay.isOnDot(x, y)) {
                listener.onMyLocationClick(location)
                return true
            }
            return false
        }

        /**
         * Returns the current camera position.
         *
         * @return A CameraPosition containing the current target location and zoom level
         */
        fun getCameraPosition(): CameraPosition = controller.getCameraPosition()

        /**
         * Returns a Projection object for coordinate transformations.
         *
         * The Projection allows you to convert between screen coordinates (in pixels)
         * and geographic coordinates (LatLng), as well as query the visible region.
         *
         * Example:
         * ```kotlin
         * val projection = mapView.getProjection()
         * val latLng = projection.fromScreenLocation(Point(100, 100))
         * val screenPoint = projection.toScreenLocation(LatLng(51.5074, -0.1278))
         * val visibleRegion = projection.getVisibleRegion()
         * ```
         *
         * @return A Projection instance for the current map state
         */
        fun getProjection(): Projection = controller.createProjection()

        /**
         * Opens the current map center in an external map application.
         *
         * Attempts to launch installed map apps (OsmAnd, Maps.me, HERE WeGo,
         * Waze, Organic Maps, etc.) using the geo: URI scheme. If no map app is available,
         * falls back to opening OpenStreetMap.org in the device browser.
         *
         * This provides functionality similar to a map toolbar, but opens OpenStreetMap in
         * the browser when no map app is installed.
         *
         * The geo: URI format follows RFC 5870 and is compatible with most map applications.
         * Android will either open the user's default map app or show an app picker if
         * multiple map apps are installed.
         *
         * Example usage:
         * ```kotlin
         * // Open current location
         * mapView.openInExternalApp()
         *
         * // Open with label
         * mapView.openInExternalApp("Coffee Shop")
         *
         * // Long-press to open
         * mapView.setOnMapLongClickListener { latLng ->
         *     mapView.moveCamera(CameraUpdateFactory.newLatLng(latLng))
         *     mapView.openInExternalApp("Selected Location")
         * }
         * ```
         *
         * @param label Optional label for the location (shown as pin title in some map apps)
         * @return true if successfully launched an intent, false if an error occurred
         * @see openLocationInExternalApp
         */
        @JvmOverloads
        fun openInExternalApp(label: String? = null): Boolean {
            val center = controller.getCenter()
            val zoom = controller.getZoom()
            return openLocationInExternalApp(center, zoom, context, label)
        }

        /**
         * Returns the UI settings for the map.
         *
         * Use this to configure which user interactions are enabled on the map,
         * such as zoom gestures and scroll gestures.
         *
         * Example:
         * ```kotlin
         * val uiSettings = mapView.getUiSettings()
         * uiSettings.isZoomGesturesEnabled = false  // Disable pinch-to-zoom
         * uiSettings.isScrollGesturesEnabled = false  // Disable panning
         * uiSettings.setAllGesturesEnabled(true)  // Enable all gestures
         * ```
         *
         * @return The UiSettings object for configuring user interactions
         */
        fun getUiSettings(): UiSettings = uiSettings

        /**
         * Triggers the edge glow effect on specified edges.
         *
         * Use this method to provide visual feedback when the camera cannot move further
         * in a direction, such as when a button press would exceed the camera bounds.
         *
         * The edge effect respects the [UiSettings.isEdgeEffectEnabled] setting.
         *
         * Example:
         * ```kotlin
         * // Trigger effect on the right edge when a "move right" button is pressed at bounds
         * mapView.triggerEdgeEffect(setOf(Edge.RIGHT))
         *
         * // Trigger effect on multiple edges with custom intensity
         * mapView.triggerEdgeEffect(setOf(Edge.TOP, Edge.RIGHT), 0.5f)
         * ```
         *
         * @param edges The set of edges to trigger the effect on
         * @param intensity The glow intensity from 0.0 (none) to 1.0 (full), default 0.8
         */
        @JvmOverloads
        fun triggerEdgeEffect(
            edges: Set<Edge>,
            intensity: Float = 0.8f,
        ) {
            controller.triggerEdgeEffect(edges, intensity)
            invalidate()
        }

        /**
         * Sets padding on the map.
         *
         * This padding affects the logical viewport of the map - it adjusts where the map
         * considers its "center" to be without changing the physical size of the view.
         * This is useful when you have UI elements (toolbars, FABs, etc.) overlaying parts
         * of the map and want camera operations to center content in the visible area.
         *
         * The map's own controls (zoom buttons, my-location button and attribution) are drawn
         * inside the padding, so the UI elements don't cover them.
         *
         * Note: This is different from View.setPadding(). View padding reduces the drawable
         * area, while map padding keeps the map full-size but adjusts camera positioning.
         *
         * Example:
         * ```kotlin
         * // Account for a 200px top toolbar
         * mapView.setMapPadding(0, 200, 0, 0)
         * // Now moveCamera will center 100px lower to account for the toolbar
         * ```
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
            mapPaddingLeft = left
            mapPaddingTop = top
            mapPaddingRight = right
            mapPaddingBottom = bottom
            controller.setMapPadding(left, top, right, bottom)
            invalidate()
        }

        /**
         * Moves the camera to a new position instantly, without animation.
         *
         * Use [CameraUpdateFactory] to create camera updates:
         * ```kotlin
         * moveCamera(CameraUpdateFactory.newLatLng(LatLng(51.5074, -0.1278)))
         * moveCamera(CameraUpdateFactory.zoomIn())
         * ```
         *
         * If the camera already is at the target, no camera listeners are called.
         *
         * @param cameraUpdate The camera update to apply
         */
        fun moveCamera(cameraUpdate: CameraUpdate) {
            controller.moveCamera(cameraUpdate)
            invalidate()
        }

        /**
         * Animates the camera to a new position with default duration (250ms).
         *
         * @param cameraUpdate The camera update to animate to
         */
        fun animateCamera(cameraUpdate: CameraUpdate) {
            controller.animateCamera(cameraUpdate)
        }

        /**
         * Animates the camera to a new position with custom duration.
         *
         * @param cameraUpdate The camera update to animate to
         * @param durationMs Duration of the animation in milliseconds
         */
        fun animateCamera(
            cameraUpdate: CameraUpdate,
            durationMs: Int,
        ) {
            controller.animateCamera(cameraUpdate, durationMs)
        }

        /**
         * Animates the camera to a new position with callbacks.
         *
         * The callback will receive notifications when the animation completes or is cancelled.
         * An animation is cancelled by touching the map, by calling [stopAnimation] or by starting another
         * camera move or animation. If the camera already is at the target, onFinish() is
         * called right away and no camera listeners are called.
         *
         * @param cameraUpdate The camera update to animate to
         * @param callback Optional callback for animation completion events
         */
        fun animateCamera(
            cameraUpdate: CameraUpdate,
            callback: CancelableCallback?,
        ) {
            controller.animateCamera(cameraUpdate, 250, callback)
        }

        /**
         * Animates the camera to a new position with custom duration and callbacks.
         *
         * The callback will receive notifications when the animation completes or is cancelled.
         * An animation is cancelled by touching the map, by calling [stopAnimation] or by starting another
         * camera move or animation. If the camera already is at the target, onFinish() is
         * called right away and no camera listeners are called.
         *
         * @param cameraUpdate The camera update to animate to
         * @param durationMs Duration of the animation in milliseconds
         * @param callback Optional callback for animation completion events
         */
        fun animateCamera(
            cameraUpdate: CameraUpdate,
            durationMs: Int,
            callback: CancelableCallback?,
        ) {
            controller.animateCamera(cameraUpdate, durationMs, callback)
        }

        /**
         * Animates the camera to a new position with a custom timing.
         *
         * Animations follow the display frames. By default they ease in and out like an
         * [android.view.animation.AccelerateDecelerateInterpolator]; pass e.g. a
         * [android.view.animation.LinearInterpolator] for constant speed. This overload is
         * an OpenMapView addition.
         *
         * @param cameraUpdate The camera update to animate to
         * @param durationMs Duration of the animation in milliseconds
         * @param interpolator Maps the elapsed share of the duration to the share of the way
         * @param callback Optional callback for animation completion events
         */
        fun animateCamera(
            cameraUpdate: CameraUpdate,
            durationMs: Int,
            interpolator: TimeInterpolator,
            callback: CancelableCallback?,
        ) {
            controller.animateCamera(cameraUpdate, durationMs, interpolator, callback)
        }

        /**
         * Stops the current camera animation, if any.
         *
         * The camera will remain at its current position when the animation is stopped.
         * If a callback was provided, its [CancelableCallback.onCancel] method will be invoked.
         */
        fun stopAnimation() {
            controller.stopAnimation()
            invalidate()
        }

        /**
         * Takes a snapshot of the map.
         *
         * The snapshot is captured asynchronously. When the snapshot is ready,
         * the callback is invoked on the Android UI thread with the resulting
         * bitmap. If the view has not been laid out yet (width or height is 0),
         * the callback receives null.
         *
         * The snapshot includes all visible map content:
         * - Base map tiles
         * - Tile overlays
         * - Markers, polylines, polygons, circles, ground overlays
         * - Zoom controls (if enabled)
         * - Attribution overlay
         *
         * Example usage:
         * ```kotlin
         * mapView.snapshot { bitmap ->
         *     bitmap?.let {
         *         // Save to file
         *         val file = File(context.filesDir, "map.png")
         *         it.compress(Bitmap.CompressFormat.PNG, 100, FileOutputStream(file))
         *     }
         * }
         * ```
         *
         * @param callback A [SnapshotReadyCallback] that will be invoked with the
         *                 snapshot bitmap or null if the snapshot failed.
         */
        fun snapshot(callback: SnapshotReadyCallback) {
            post {
                // Check if view is laid out
                if (width <= 0 || height <= 0) {
                    callback.onSnapshotReady(null)
                    return@post
                }

                try {
                    // Create bitmap matching view dimensions
                    val bitmap = createBitmap(width, height)
                    val canvas = Canvas(bitmap)

                    // Render the entire view to the bitmap
                    draw(canvas)

                    callback.onSnapshotReady(bitmap)
                } catch (e: Exception) {
                    // Handle OOM or other bitmap creation failures
                    callback.onSnapshotReady(null)
                }
            }
        }

        /**
         * Takes a snapshot of the map into a pre-allocated bitmap.
         *
         * This method is equivalent to [snapshot] but allows you to provide a
         * pre-allocated [Bitmap] object. If the bitmap's dimensions do not match
         * the current view dimensions, a new bitmap will be allocated at the
         * correct size.
         *
         * The snapshot is captured asynchronously and the callback is invoked on
         * the Android UI thread.
         *
         * Example usage:
         * ```kotlin
         * val bitmap = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)
         * mapView.snapshot({ result ->
         *     // result may be the same bitmap or a new one if size didn't match
         *     result?.let { saveToFile(it) }
         * }, bitmap)
         * ```
         *
         * @param callback A [SnapshotReadyCallback] that will be invoked with the
         *                 snapshot bitmap or null if the snapshot failed.
         * @param bitmap A pre-allocated [Bitmap] to render into. If the bitmap's
         *               dimensions do not match the view's dimensions, a new bitmap
         *               will be allocated.
         */
        fun snapshot(
            callback: SnapshotReadyCallback,
            bitmap: Bitmap,
        ) {
            post {
                // Check if view is laid out
                if (width <= 0 || height <= 0) {
                    callback.onSnapshotReady(null)
                    return@post
                }

                try {
                    // Check if provided bitmap matches view dimensions
                    val targetBitmap =
                        if (bitmap.width == width && bitmap.height == height) {
                            bitmap
                        } else {
                            // Allocate new bitmap at correct size
                            createBitmap(width, height)
                        }

                    val canvas = Canvas(targetBitmap)

                    // Render the entire view to the bitmap
                    draw(canvas)

                    callback.onSnapshotReady(targetBitmap)
                } catch (e: Exception) {
                    // Handle OOM or other bitmap creation failures
                    callback.onSnapshotReady(null)
                }
            }
        }

        /**
         * Adds a marker to the map.
         *
         * Example:
         * ```kotlin
         * val marker = Marker(
         *     position = LatLng(51.5074, -0.1278),
         *     title = "London",
         *     snippet = "Capital of the UK"
         * )
         * mapView.addMarker(marker)
         * ```
         *
         * @param marker The marker to add
         * @return The added marker instance
         */
        fun addMarker(marker: Marker): Marker {
            marker.mapView = this
            val result = controller.addMarker(marker)
            invalidate()
            return result
        }

        /**
         * Adds a marker to the map using MarkerOptions builder.
         *
         * Example:
         * ```kotlin
         * mapView.addMarker(
         *     MarkerOptions()
         *         .position(LatLng(51.5, 0.0))
         *         .title("London")
         *         .draggable(true)
         * )
         * ```
         *
         * @param markerOptions Configuration for the marker
         * @return The created Marker instance
         */
        fun addMarker(markerOptions: MarkerOptions): Marker {
            val marker = markerOptions.build()
            return addMarker(marker)
        }

        /**
         * Removes a marker from the map and closes its info window.
         *
         * @param marker The marker to remove
         * @return true if the marker was found and removed, false otherwise
         */
        fun removeMarker(marker: Marker): Boolean {
            val result = controller.removeMarker(marker)
            if (result) {
                // A marker added again later starts with its info window closed
                if (marker.isInfoWindowShown) hideInfoWindow(marker)
                marker.mapView = null
                invalidate()
            }
            return result
        }

        /**
         * Removes all markers from the map and closes the open info window.
         */
        fun clearMarkers() {
            val markers = controller.getMarkers()
            controller.clearMarkers()
            markers.forEach {
                if (it.isInfoWindowShown) hideInfoWindow(it)
                it.mapView = null
            }
            invalidate()
        }

        /**
         * Returns a list of all markers currently on the map.
         *
         * @return A list copy of all markers
         */
        fun getMarkers(): List<Marker> = controller.getMarkers()

        /**
         * Shows the info window for a marker and schedules auto-dismiss if configured.
         *
         * This method hides any currently shown info windows, shows the info window
         * for the specified marker, and triggers a redraw. If [UiSettings.infoWindowAutoDismiss]
         * is set to a positive duration, the info window will be automatically hidden
         * after that duration.
         *
         * Example:
         * ```kotlin
         * val marker = mapView.addMarker(Marker(position = LatLng(51.5, -0.1), title = "London"))
         * mapView.showInfoWindow(marker)  // Shows info window with auto-dismiss
         * ```
         *
         * @param marker The marker whose info window should be shown
         * @see hideInfoWindow
         * @see UiSettings.infoWindowAutoDismiss
         */
        fun showInfoWindow(marker: Marker) {
            closeOtherInfoWindows(marker)
            // Draws the window again, e.g. after the title changed
            marker.setInfoWindowShownInternal(false)
            openInfoWindow(marker)
            invalidate()
        }

        // Only one info window is shown at a time: closes the others and reports it
        private fun closeOtherInfoWindows(marker: Marker) {
            controller.getMarkers().forEach { other ->
                if (other !== marker && other.isInfoWindowShown) {
                    other.setInfoWindowShownInternal(false)
                    controller.onInfoWindowCloseListener?.onInfoWindowClose(other)
                }
            }
        }

        /**
         * Sets the adapter that provides custom views for info windows, or null for the
         * default window with the marker's title and snippet.
         *
         * Example:
         * ```kotlin
         * mapView.setInfoWindowAdapter(object : InfoWindowAdapter {
         *     override fun getInfoWindow(marker: Marker): View? = null
         *
         *     override fun getInfoContents(marker: Marker): View =
         *         layoutInflater.inflate(R.layout.info_contents, null)
         * })
         * ```
         *
         * @param adapter The adapter, or null for the default info windows
         * @see InfoWindowAdapter
         */
        fun setInfoWindowAdapter(adapter: InfoWindowAdapter?) {
            infoWindowAdapter = adapter
        }

        // Shows the info window of the marker: the adapter's view, or the title and snippet.
        // Without either there is nothing to show.
        private fun openInfoWindow(marker: Marker) {
            val adapter = infoWindowAdapter
            val image =
                adapter?.getInfoWindow(marker)?.let { InfoWindowImage(drawToBitmap(it), framed = false) }
                    ?: adapter?.getInfoContents(marker)?.let { InfoWindowImage(drawToBitmap(it), framed = true) }
            if (image == null && marker.title == null && marker.snippet == null) return

            marker.setInfoWindowShownInternal(true)
            marker.infoWindowImage = image
            scheduleInfoWindowAutoDismiss()
        }

        // Draws an info window view into a bitmap at its wrap_content size. The view is
        // briefly added to the map, so views that need a window, like ComposeView, are ready.
        private fun drawToBitmap(view: View): Bitmap {
            val added = view.parent == null
            if (added) addView(view, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            try {
                val unspecified = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                view.measure(unspecified, unspecified)
                view.layout(0, 0, view.measuredWidth, view.measuredHeight)
                val bitmap = createBitmap(view.measuredWidth.coerceAtLeast(1), view.measuredHeight.coerceAtLeast(1))
                view.draw(Canvas(bitmap))
                return bitmap
            } finally {
                if (added) removeView(view)
            }
        }

        /**
         * Hides the info window for a marker and cancels any pending auto-dismiss.
         *
         * Example:
         * ```kotlin
         * mapView.hideInfoWindow(marker)
         * ```
         *
         * @param marker The marker whose info window should be hidden
         * @see showInfoWindow
         */
        fun hideInfoWindow(marker: Marker) {
            val wasShown = marker.isInfoWindowShown
            marker.setInfoWindowShownInternal(false)
            cancelInfoWindowAutoDismiss()
            if (wasShown) {
                controller.onInfoWindowCloseListener?.onInfoWindowClose(marker)
            }
            invalidate()
        }

        /**
         * Sets a listener to handle marker click events.
         *
         * Example:
         * ```kotlin
         * mapView.setOnMarkerClickListener { marker ->
         *     Toast.makeText(context, marker.title, Toast.LENGTH_SHORT).show()
         *     true  // Return true to consume the event
         * }
         * ```
         *
         * @param listener Callback invoked when a marker is clicked, before the default behavior:
         *   unless it returns true, the marker's info window opens and the camera moves to the
         *   marker. Null clears the listener.
         */
        fun setOnMarkerClickListener(listener: OnMarkerClickListener?) {
            controller.onMarkerClickListener = listener
        }

        /**
         * Sets a listener to handle info window click events.
         *
         * Called when an info window is clicked. Info windows are shown above markers
         * when showInfoWindow() is called on a marker.
         *
         * Example:
         * ```kotlin
         * mapView.setOnInfoWindowClickListener { marker ->
         *     Toast.makeText(context, "Info window clicked: ${marker.title}", Toast.LENGTH_SHORT).show()
         * }
         * ```
         *
         * @param listener The listener to receive info window click events, or null to clear the listener
         */
        fun setOnInfoWindowClickListener(listener: OnInfoWindowClickListener?) {
            controller.onInfoWindowClickListener = listener
        }

        /**
         * Sets a listener to handle info window close events.
         *
         * Called when an info window is closed, either manually via hideInfoWindow()
         * or automatically via the auto-dismiss timer.
         *
         * Example:
         * ```kotlin
         * mapView.setOnInfoWindowCloseListener { marker ->
         *     Log.d("Map", "Info window closed: ${marker.title}")
         * }
         * ```
         *
         * @param listener The listener to receive info window close events, or null to clear the listener
         * @see hideInfoWindow
         * @see UiSettings.infoWindowAutoDismiss
         */
        fun setOnInfoWindowCloseListener(listener: OnInfoWindowCloseListener?) {
            controller.onInfoWindowCloseListener = listener
        }

        /**
         * Sets a listener for info window long clicks.
         *
         * Called when the user presses and holds the info window of a marker. The long press
         * is then not reported to the [OnMapLongClickListener].
         *
         * Example:
         * ```kotlin
         * mapView.setOnInfoWindowLongClickListener { marker ->
         *     Log.d("Map", "Info window of ${marker.title} long clicked")
         * }
         * ```
         *
         * @param listener The listener, or null to remove it
         */
        fun setOnInfoWindowLongClickListener(listener: OnInfoWindowLongClickListener?) {
            controller.onInfoWindowLongClickListener = listener
        }

        /**
         * Sets a listener to handle marker drag events.
         *
         * Called when a draggable marker is dragged by the user. The marker must have
         * its draggable property set to true to receive drag events.
         *
         * Example:
         * ```kotlin
         * mapView.setOnMarkerDragListener(object : OnMarkerDragListener {
         *     override fun onMarkerDragStart(marker: Marker) {
         *         Log.d("Map", "Drag started: ${marker.title}")
         *     }
         *
         *     override fun onMarkerDrag(marker: Marker) {
         *         Log.d("Map", "Dragging: ${marker.position}")
         *     }
         *
         *     override fun onMarkerDragEnd(marker: Marker) {
         *         Log.d("Map", "Drag ended: ${marker.position}")
         *     }
         * })
         * ```
         *
         * @param listener The listener to receive drag events, or null to clear the listener
         */
        fun setOnMarkerDragListener(listener: OnMarkerDragListener?) {
            onMarkerDragListener = listener
        }

        /**
         * Sets a listener to handle polyline click events.
         *
         * Called when a clickable polyline is clicked. The polyline must have
         * its clickable property set to true to receive click events.
         *
         * Example:
         * ```kotlin
         * mapView.setOnPolylineClickListener { polyline ->
         *     Toast.makeText(context, "Polyline clicked: ${polyline.tag}", Toast.LENGTH_SHORT).show()
         * }
         * ```
         *
         * @param listener The listener to receive polyline click events, or null to clear the listener
         */
        fun setOnPolylineClickListener(listener: OnPolylineClickListener?) {
            controller.onPolylineClickListener = listener
        }

        /**
         * Sets a listener to handle polygon click events.
         *
         * Called when a clickable polygon is clicked. The polygon must have
         * its clickable property set to true to receive click events.
         *
         * Example:
         * ```kotlin
         * mapView.setOnPolygonClickListener { polygon ->
         *     Toast.makeText(context, "Polygon clicked: ${polygon.tag}", Toast.LENGTH_SHORT).show()
         * }
         * ```
         *
         * @param listener The listener to receive polygon click events, or null to clear the listener
         */
        fun setOnPolygonClickListener(listener: OnPolygonClickListener?) {
            controller.onPolygonClickListener = listener
        }

        /**
         * Sets a listener to handle circle click events.
         *
         * Called when the user taps on a clickable circle on the map.
         *
         * @param listener Callback invoked when a circle is clicked, or null to remove
         */
        fun setOnCircleClickListener(listener: OnCircleClickListener?) {
            controller.onCircleClickListener = listener
        }

        /**
         * Sets a listener to handle ground overlay click events.
         *
         * Called when the user taps on a clickable ground overlay on the map.
         *
         * @param listener Callback invoked when a ground overlay is clicked, or null to remove
         */
        fun setOnGroundOverlayClickListener(listener: OnGroundOverlayClickListener?) {
            controller.onGroundOverlayClickListener = listener
        }

        /**
         * Sets a listener to handle map click events.
         *
         * Called when the user taps on the map (not on a marker or other overlay).
         *
         * Example:
         * ```kotlin
         * mapView.setOnMapClickListener { latLng ->
         *     Toast.makeText(context, "Clicked: ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()
         * }
         * ```
         *
         * @param listener Callback invoked when the map is clicked
         */
        fun setOnMapClickListener(listener: OnMapClickListener?) {
            onMapClickListener = listener
        }

        /**
         * Sets a listener to handle map long-click events.
         *
         * Called when the user long-presses on the map (not on a marker or other overlay).
         *
         * Example:
         * ```kotlin
         * mapView.setOnMapLongClickListener { latLng ->
         *     Toast.makeText(context, "Long-clicked: ${latLng.latitude}, ${latLng.longitude}", Toast.LENGTH_SHORT).show()
         * }
         * ```
         *
         * @param listener Callback invoked when the map is long-clicked
         */
        fun setOnMapLongClickListener(listener: OnMapLongClickListener?) {
            onMapLongClickListener = listener
        }

        /**
         * Sets a listener to handle camera movement start events.
         *
         * Called when the camera starts moving, providing the reason for the movement.
         *
         * @param listener The listener to receive camera move started events, or null to clear the listener
         */
        fun setOnCameraMoveStartedListener(listener: OnCameraMoveStartedListener?) {
            controller.onCameraMoveStartedListener = listener
        }

        /**
         * Sets a listener to handle camera movement events.
         *
         * Called repeatedly while the camera is moving.
         *
         * @param listener The listener to receive camera move events, or null to clear the listener
         */
        fun setOnCameraMoveListener(listener: OnCameraMoveListener?) {
            controller.onCameraMoveListener = listener
        }

        /**
         * Sets a listener to handle camera idle events.
         *
         * Called when the camera stops moving after gestures or animations complete.
         *
         * @param listener The listener to receive camera idle events, or null to clear the listener
         */
        fun setOnCameraIdleListener(listener: OnCameraIdleListener?) {
            controller.onCameraIdleListener = listener
        }

        /**
         * Sets a callback that is invoked once when the map has finished rendering, i.e.
         * all visible base map tiles are drawn and the camera is not moving.
         *
         * If the map is already fully drawn, the callback is invoked after the next frame.
         * It is not invoked while tiles can't be loaded (e.g. offline) or while the camera
         * keeps moving. Tile overlays are not waited for.
         *
         * Example usage:
         * ```kotlin
         * mapView.setOnMapLoadedCallback {
         *     mapView.snapshot { bitmap -> bitmap?.let { saveToFile(it) } }
         * }
         * ```
         *
         * @param callback The callback to invoke, or null to remove a pending one
         */
        fun setOnMapLoadedCallback(callback: OnMapLoadedCallback?) {
            controller.setOnMapLoadedCallback(callback)
        }

        /**
         * Sets a listener to handle camera movement cancellation events.
         *
         * Called when an ongoing animation is interrupted before completion.
         *
         * @param listener The listener to receive camera move canceled events, or null to clear the listener
         */
        fun setOnCameraMoveCanceledListener(listener: OnCameraMoveCanceledListener?) {
            controller.onCameraMoveCanceledListener = listener
        }

        /**
         * Adds a polyline to the map.
         *
         * @param polyline The polyline to add
         * @return The added polyline instance
         */
        fun addPolyline(polyline: Polyline): Polyline {
            val result = controller.addPolyline(polyline)
            // A live handle: its setters redraw this map
            result.mapView = this
            invalidate()
            return result
        }

        /**
         * Adds a polyline to the map using PolylineOptions builder.
         *
         * Example:
         * ```kotlin
         * mapView.addPolyline(
         *     PolylineOptions()
         *         .add(LatLng(51.5, 0.0))
         *         .add(LatLng(51.6, 0.1))
         *         .color(Color.BLUE)
         *         .width(5f)
         * )
         * ```
         *
         * @param polylineOptions Configuration for the polyline
         * @return The created Polyline instance
         */
        fun addPolyline(polylineOptions: PolylineOptions): Polyline {
            val polyline = polylineOptions.build()
            return addPolyline(polyline)
        }

        /**
         * Removes a polyline from the map.
         *
         * @param polyline The polyline to remove
         * @return true if the polyline was found and removed, false otherwise
         */
        fun removePolyline(polyline: Polyline): Boolean {
            val result = controller.removePolyline(polyline)
            if (result) {
                polyline.mapView = null
                invalidate()
            }
            return result
        }

        /**
         * Removes all polylines from the map.
         */
        fun clearPolylines() {
            controller.getPolylines().forEach { it.mapView = null }
            controller.clearPolylines()
            invalidate()
        }

        /**
         * Returns a list of all polylines currently on the map.
         *
         * @return A list copy of all polylines
         */
        fun getPolylines(): List<Polyline> = controller.getPolylines()

        /**
         * Adds a polygon to the map.
         *
         * @param polygon The polygon to add
         * @return The added polygon instance
         */
        fun addPolygon(polygon: Polygon): Polygon {
            val result = controller.addPolygon(polygon)
            // A live handle: its setters redraw this map
            result.mapView = this
            invalidate()
            return result
        }

        /**
         * Adds a polygon to the map using PolygonOptions builder.
         *
         * Example:
         * ```kotlin
         * mapView.addPolygon(
         *     PolygonOptions()
         *         .add(LatLng(51.5, 0.0))
         *         .add(LatLng(51.6, 0.0))
         *         .add(LatLng(51.6, 0.1))
         *         .fillColor(Color.argb(128, 255, 0, 0))
         * )
         * ```
         *
         * @param polygonOptions Configuration for the polygon
         * @return The created Polygon instance
         */
        fun addPolygon(polygonOptions: PolygonOptions): Polygon {
            val polygon = polygonOptions.build()
            return addPolygon(polygon)
        }

        /**
         * Removes a polygon from the map.
         *
         * @param polygon The polygon to remove
         * @return true if the polygon was found and removed, false otherwise
         */
        fun removePolygon(polygon: Polygon): Boolean {
            val result = controller.removePolygon(polygon)
            if (result) {
                polygon.mapView = null
                invalidate()
            }
            return result
        }

        /**
         * Removes all polygons from the map.
         */
        fun clearPolygons() {
            controller.getPolygons().forEach { it.mapView = null }
            controller.clearPolygons()
            invalidate()
        }

        /**
         * Returns a list of all polygons currently on the map.
         *
         * @return A list copy of all polygons
         */
        fun getPolygons(): List<Polygon> = controller.getPolygons()

        /**
         * Adds a circle to the map using CircleOptions builder.
         *
         * Example:
         * ```kotlin
         * mapView.addCircle(
         *     CircleOptions()
         *         .center(LatLng(51.5, 0.0))
         *         .radius(1000f)
         *         .fillColor(Color.argb(128, 0, 0, 255))
         * )
         * ```
         *
         * @param circleOptions Configuration for the circle
         * @return The created Circle instance
         */
        fun addCircle(circleOptions: CircleOptions): Circle {
            val circle = circleOptions.build()
            return addCircle(circle)
        }

        /**
         * Adds a circle to the map (Kotlin-style direct instantiation).
         *
         * Example:
         * ```kotlin
         * mapView.addCircle(
         *     Circle(
         *         center = LatLng(51.5, 0.0),
         *         radius = 1000f,
         *         fillColor = Color.argb(128, 0, 0, 255)
         *     )
         * )
         * ```
         *
         * @param circle The circle to add
         * @return The added circle instance
         */
        fun addCircle(circle: Circle): Circle {
            val result = controller.addCircle(circle)
            // A live handle: its setters redraw this map
            result.mapView = this
            invalidate()
            return result
        }

        /**
         * Removes a circle from the map.
         *
         * @param circle The circle to remove
         * @return true if the circle was found and removed, false otherwise
         */
        fun removeCircle(circle: Circle): Boolean {
            val result = controller.removeCircle(circle)
            if (result) {
                circle.mapView = null
                invalidate()
            }
            return result
        }

        /**
         * Removes all circles from the map.
         */
        fun clearCircles() {
            controller.getCircles().forEach { it.mapView = null }
            controller.clearCircles()
            invalidate()
        }

        /**
         * Returns a list of all circles currently on the map.
         *
         * @return A list copy of all circles
         */
        fun getCircles(): List<Circle> = controller.getCircles()

        /**
         * Adds a ground overlay to the map using GroundOverlayOptions builder pattern.
         *
         * @param groundOverlayOptions Builder containing overlay configuration
         * @return The created GroundOverlay instance
         * @throws IllegalArgumentException if image or position/bounds is not set
         */
        fun addGroundOverlay(groundOverlayOptions: GroundOverlayOptions): GroundOverlay {
            val overlay = groundOverlayOptions.build()
            return addGroundOverlay(overlay)
        }

        /**
         * Adds a ground overlay to the map directly (Kotlin style).
         *
         * @param groundOverlay The ground overlay to add
         * @return The added GroundOverlay instance
         */
        fun addGroundOverlay(groundOverlay: GroundOverlay): GroundOverlay {
            val result = controller.addGroundOverlay(groundOverlay)
            // A live handle: its setters redraw this map
            result.mapView = this
            invalidate()
            return result
        }

        /**
         * Removes a ground overlay from the map.
         *
         * @param groundOverlay The ground overlay to remove
         * @return true if the overlay was found and removed, false otherwise
         */
        fun removeGroundOverlay(groundOverlay: GroundOverlay): Boolean {
            val result = controller.removeGroundOverlay(groundOverlay)
            if (result) {
                groundOverlay.mapView = null
                invalidate()
            }
            return result
        }

        /**
         * Removes all ground overlays from the map.
         */
        fun clearGroundOverlays() {
            controller.getGroundOverlays().forEach { it.mapView = null }
            controller.clearGroundOverlays()
            invalidate()
        }

        /**
         * Returns a list of all ground overlays currently on the map.
         *
         * @return A list copy of all ground overlays
         */
        fun getGroundOverlays(): List<GroundOverlay> = controller.getGroundOverlays()

        /**
         * Adds a tile overlay to the map using TileOverlayOptions builder pattern.
         *
         * @param tileOverlayOptions Builder containing overlay configuration
         * @return The created TileOverlay instance
         * @throws IllegalArgumentException if tileProvider is not set
         */
        fun addTileOverlay(tileOverlayOptions: TileOverlayOptions): TileOverlay {
            val overlay = tileOverlayOptions.build()
            return addTileOverlay(overlay)
        }

        /**
         * Adds a tile overlay to the map directly (Kotlin style).
         *
         * @param tileOverlay The tile overlay to add
         * @return The added TileOverlay instance
         */
        fun addTileOverlay(tileOverlay: TileOverlay): TileOverlay {
            val result = controller.addTileOverlay(tileOverlay)
            // A live handle: its setters redraw this map
            result.mapView = this
            invalidate()
            return result
        }

        /**
         * Removes a tile overlay from the map.
         *
         * @param tileOverlay The tile overlay to remove
         * @return true if the overlay was found and removed, false otherwise
         */
        fun removeTileOverlay(tileOverlay: TileOverlay): Boolean {
            val result = controller.removeTileOverlay(tileOverlay)
            if (result) {
                tileOverlay.mapView = null
                invalidate()
            }
            return result
        }

        /**
         * Drops the downloaded tiles of a tile overlay, so they are requested again, e.g.
         * after the data behind its tile provider changed.
         *
         * @param tileOverlay The tile overlay whose tiles to drop
         */
        fun clearTileOverlayCache(tileOverlay: TileOverlay) {
            controller.clearTileOverlayCache(tileOverlay)
            invalidate()
        }

        /**
         * Removes all tile overlays from the map.
         */
        fun clearTileOverlays() {
            controller.getTileOverlays().forEach { it.mapView = null }
            controller.clearTileOverlays()
            invalidate()
        }

        /**
         * Returns a list of all tile overlays currently on the map.
         *
         * @return A list copy of all tile overlays
         */
        fun getTileOverlays(): List<TileOverlay> = controller.getTileOverlays()

        /**
         * Removes all markers, polylines, polygons, circles, ground overlays and tile overlays
         * from the map.
         *
         * This is equivalent to calling clearMarkers(), clearPolylines(), clearPolygons(),
         * clearCircles(), clearGroundOverlays() and clearTileOverlays().
         */
        fun clear() {
            clearMarkers()
            clearPolylines()
            clearPolygons()
            clearCircles()
            clearGroundOverlays()
            clearTileOverlays()
        }

        /**
         * Imports GeoJSON data and adds all contained features to the map.
         *
         * Supports Point, LineString, Polygon, and their Multi- variants,
         * as well as Feature and FeatureCollection.
         *
         * @param geoJsonString The GeoJSON string to parse and add
         * @return A GeoJsonResult containing all added markers, polylines, and polygons
         */
        fun addGeoJson(geoJsonString: String): GeoJsonResult {
            val result = controller.addGeoJson(geoJsonString)
            result.polylines.forEach { it.mapView = this }
            result.polygons.forEach { it.mapView = this }
            invalidate()
            return result
        }

        /**
         * Sets a listener to handle clicks on the OSM attribution overlay.
         *
         * @param listener Callback invoked when the attribution text is clicked
         */
        fun setOnAttributionClickListener(listener: () -> Unit) {
            attributionOverlay.onAttributionClickListener = listener
        }

        /**
         * Gets the current attribution URL for the active tile source.
         *
         * @return The URL to the attribution/copyright page
         */
        fun getAttributionUrl(): String = attributionOverlay.getAttributionUrl()

        /**
         * Sets custom attribution text to be displayed.
         *
         * @param text The attribution text to display
         */
        fun setAttributionText(text: String) {
            attributionOverlay.setAttributionText(text)
            invalidate()
        }

        /**
         * Schedules auto-dismiss of info windows if enabled in UiSettings.
         *
         * Called when an info window is shown. Only starts the timer if
         * infoWindowAutoDismiss is positive; Duration.ZERO means disabled.
         */
        private fun scheduleInfoWindowAutoDismiss() {
            infoWindowDismissRunnable?.let { infoWindowHandler.removeCallbacks(it) }
            val duration = uiSettings.infoWindowAutoDismiss
            if (duration == Duration.ZERO) return
            val runnable =
                Runnable {
                    controller.getMarkers().forEach { marker ->
                        if (marker.isInfoWindowShown) {
                            marker.setInfoWindowShownInternal(false)
                            controller.onInfoWindowCloseListener?.onInfoWindowClose(marker)
                        }
                    }
                    // OpenMapView is a traditional Android View (not Compose), so we must
                    // call invalidate() to trigger a redraw after hiding info windows
                    invalidate()
                }
            infoWindowDismissRunnable = runnable
            infoWindowHandler.postDelayed(runnable, duration.inWholeMilliseconds)
        }

        /**
         * Cancels any pending auto-dismiss of info windows.
         *
         * Called when an info window is manually hidden by the user.
         */
        private fun cancelInfoWindowAutoDismiss() {
            infoWindowDismissRunnable?.let { infoWindowHandler.removeCallbacks(it) }
            infoWindowDismissRunnable = null
        }

        override fun onAttachedToWindow() {
            super.onAttachedToWindow()
            observedLifecycle =
                findViewTreeLifecycleOwner()?.lifecycle?.also {
                    // Does nothing if the app has already added this view as an observer
                    it.addObserver(this)
                }
            updateLocationUpdates()
        }

        override fun onDetachedFromWindow() {
            // Also stops a lifecycle that outlives the view (e.g. the Activity behind a
            // Compose screen) from keeping this view in memory
            observedLifecycle?.removeObserver(this)
            observedLifecycle = null
            onDestroy()
            super.onDetachedFromWindow()
        }

        override fun onResume(owner: LifecycleOwner) {
            controller.onResume()
            isPaused = false
            updateLocationUpdates()
        }

        override fun onPause(owner: LifecycleOwner) {
            controller.onPause()
            isPaused = true
            stopLocationUpdates()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            onDestroy()
        }

        /**
         * Stops tile downloads and camera animations and closes the HTTP client.
         *
         * Called automatically when the view is detached from its window or its lifecycle
         * owner is destroyed, so apps usually don't need to call it. Calling it more than
         * once is safe, and the map keeps working if it is drawn again.
         */
        fun onDestroy() {
            cancelInfoWindowAutoDismiss()
            stopLocationUpdates()
            controller.onDestroy()
        }
    }
