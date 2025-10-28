/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

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
 * The view must be registered with a lifecycle owner to ensure proper resource cleanup:
 * ```kotlin
 * lifecycle.addObserver(mapView)
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
        private val uiSettings = UiSettings()
        private var lastTouchX = 0f
        private var lastTouchY = 0f
        private var onMapClickListener: OnMapClickListener? = null
        private var onMapLongClickListener: OnMapLongClickListener? = null
        private var onMarkerDragListener: OnMarkerDragListener? = null
        private var draggedMarker: Marker? = null
        private var isDragging = false

        private val gestureDetector =
            GestureDetector(
                context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onLongPress(e: MotionEvent) {
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
                        val focusX = detector.focusX
                        val focusY = detector.focusY
                        controller.zoom(scaleFactor, focusX, focusY)
                        invalidate()
                        return true
                    }
                },
            )

        init {
            setWillNotDraw(false)
            controller.setOnTileLoadedCallback {
                invalidate()
            }
        }

        override fun dispatchDraw(canvas: Canvas) {
            super.dispatchDraw(canvas)
            controller.draw(canvas)
            attributionOverlay.draw(canvas, width, height)
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

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Let gesture detector handle long press (but not during drag)
            if (!isDragging) {
                gestureDetector.onTouchEvent(event)
            }

            // Let scale detector handle pinch gestures only if zoom gestures are enabled
            if (uiSettings.isZoomGesturesEnabled) {
                scaleGestureDetector.onTouchEvent(event)
            }

            // Handle dragging and panning only if not scaling
            if (!scaleGestureDetector.isInProgress) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x
                        lastTouchY = event.y
                        // Check if touch is on a draggable marker
                        val touchedMarker = controller.handleMarkerTouch(event.x, event.y)
                        if (touchedMarker != null && touchedMarker.draggable) {
                            draggedMarker = touchedMarker
                            isDragging = false
                        }
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        val movementDistance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

                        if (draggedMarker != null) {
                            // Start dragging if moved more than threshold
                            if (!isDragging && movementDistance > 10) {
                                isDragging = true
                                controller.commitPan()
                                onMarkerDragListener?.onMarkerDragStart(draggedMarker!!)
                            }

                            // Continue dragging
                            if (isDragging) {
                                val latLng = controller.screenToLatLng(event.x, event.y)
                                draggedMarker!!.position = latLng
                                onMarkerDragListener?.onMarkerDrag(draggedMarker!!)
                                invalidate()
                                lastTouchX = event.x
                                lastTouchY = event.y
                                return true
                            }
                        }

                        // Pan the map if not dragging a marker and scroll gestures are enabled
                        if (!isDragging && uiSettings.isScrollGesturesEnabled) {
                            // Fire camera move started event on first pan movement
                            if (movementDistance > 0 && !controller.isCameraMoving) {
                                controller.isCameraMoving = true
                                controller.currentMoveReason = de.afarber.openmapview.OnCameraMoveStartedListener.REASON_GESTURE
                                controller.onCameraMoveStartedListener?.onCameraMoveStarted(
                                    de.afarber.openmapview.OnCameraMoveStartedListener.REASON_GESTURE,
                                )
                            }

                            controller.updatePanOffset(dx, dy)

                            // Fire camera move event during pan
                            if (controller.isCameraMoving) {
                                controller.onCameraMoveListener?.onCameraMove()
                            }

                            lastTouchX = event.x
                            lastTouchY = event.y
                            invalidate()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
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
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        val movementDistance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble())

                        if (movementDistance < 10) {
                            // Check attribution overlay first
                            if (attributionOverlay.handleTouch(event.x, event.y, width, height)) {
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
                                // Hide all other info windows (only one can be shown at a time)
                                controller.getMarkers().forEach { it.hideInfoWindow() }
                                // Show info window for clicked marker if it has title or snippet
                                if (touchedMarker.title != null || touchedMarker.snippet != null) {
                                    touchedMarker.showInfoWindow()
                                }

                                val consumed = controller.onMarkerClickListener?.invoke(touchedMarker) ?: false
                                controller.commitPan()
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
        fun setZoom(zoom: Double) {
            controller.setZoom(zoom)
            invalidate()
        }

        /**
         * Returns the current zoom level of the map.
         *
         * @return The current zoom level (2.0 to 19.0)
         */
        fun getZoom(): Double = controller.getZoom()

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
         * Moves the camera to a new position instantly, without animation.
         *
         * Use [CameraUpdateFactory] to create camera updates:
         * ```kotlin
         * moveCamera(CameraUpdateFactory.newLatLng(LatLng(51.5074, -0.1278)))
         * moveCamera(CameraUpdateFactory.zoomIn())
         * ```
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
         * An animation can be cancelled by calling [stopAnimation] or by starting another
         * camera move or animation.
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
         * An animation can be cancelled by calling [stopAnimation] or by starting another
         * camera move or animation.
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
         * Stops the current camera animation, if any.
         *
         * The camera will remain at its current position when the animation is stopped.
         * If a listener was provided, its [OnCameraAnimationListener.onCancel] callback will be invoked.
         */
        fun stopAnimation() {
            controller.stopAnimation()
            invalidate()
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
            val result = controller.addMarker(marker)
            invalidate()
            return result
        }

        /**
         * Adds a marker to the map using MarkerOptions builder (Google Maps API style).
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
         * Removes a marker from the map.
         *
         * @param marker The marker to remove
         * @return true if the marker was found and removed, false otherwise
         */
        fun removeMarker(marker: Marker): Boolean {
            val result = controller.removeMarker(marker)
            if (result) invalidate()
            return result
        }

        /**
         * Removes all markers from the map.
         */
        fun clearMarkers() {
            controller.clearMarkers()
            invalidate()
        }

        /**
         * Returns a list of all markers currently on the map.
         *
         * @return A list copy of all markers
         */
        fun getMarkers(): List<Marker> = controller.getMarkers()

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
         * @param listener Callback invoked when a marker is clicked. Return true to consume the event.
         */
        fun setOnMarkerClickListener(listener: (Marker) -> Boolean) {
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
            invalidate()
            return result
        }

        /**
         * Adds a polyline to the map using PolylineOptions builder (Google Maps API style).
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
            if (result) invalidate()
            return result
        }

        /**
         * Removes all polylines from the map.
         */
        fun clearPolylines() {
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
            invalidate()
            return result
        }

        /**
         * Adds a polygon to the map using PolygonOptions builder (Google Maps API style).
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
            if (result) invalidate()
            return result
        }

        /**
         * Removes all polygons from the map.
         */
        fun clearPolygons() {
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
         * Adds a circle to the map using CircleOptions builder (Google Maps API style).
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
            if (result) invalidate()
            return result
        }

        /**
         * Removes all circles from the map.
         */
        fun clearCircles() {
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
            if (result) invalidate()
            return result
        }

        /**
         * Removes all ground overlays from the map.
         */
        fun clearGroundOverlays() {
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
            if (result) invalidate()
            return result
        }

        /**
         * Removes all tile overlays from the map.
         */
        fun clearTileOverlays() {
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
         * Removes all markers, polylines, polygons, circles, and tile overlays from the map.
         *
         * This is equivalent to calling clearMarkers(), clearPolylines(), clearPolygons(),
         * clearCircles(), and clearTileOverlays().
         */
        fun clear() {
            clearMarkers()
            clearPolylines()
            clearPolygons()
            clearCircles()
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

        override fun onResume(owner: LifecycleOwner) {
            controller.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
            controller.onPause()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            controller.onDestroy()
        }
    }
