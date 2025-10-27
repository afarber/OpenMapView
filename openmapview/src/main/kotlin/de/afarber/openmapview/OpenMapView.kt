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
 * This view provides a drop-in replacement for Google MapView, supporting pan and zoom gestures,
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
        private var lastTouchX = 0f
        private var lastTouchY = 0f
        private var onMapClickListener: OnMapClickListener? = null
        private var onMapLongClickListener: OnMapLongClickListener? = null

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
            // Let gesture detector handle long press
            gestureDetector.onTouchEvent(event)

            // Let scale detector handle pinch gestures
            scaleGestureDetector.onTouchEvent(event)

            // Handle panning only if not scaling
            if (!scaleGestureDetector.isInProgress) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.x
                        lastTouchY = event.y
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - lastTouchX
                        val dy = event.y - lastTouchY
                        controller.updatePanOffset(dx, dy)
                        lastTouchX = event.x
                        lastTouchY = event.y
                        invalidate()
                        return true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
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

                            // Check for marker touch
                            val touchedMarker = controller.handleMarkerTouch(event.x, event.y)
                            if (touchedMarker != null) {
                                val consumed = controller.onMarkerClickListener?.invoke(touchedMarker) ?: false
                                if (consumed) {
                                    controller.commitPan()
                                    invalidate()
                                    return true
                                }
                            }

                            // Fire map click event if nothing else was clicked
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
         * Animates the camera to a new position with custom duration and callbacks.
         *
         * The listener will receive callbacks when the animation completes or is cancelled.
         * An animation can be cancelled by calling [stopAnimation] or by starting another
         * camera move or animation.
         *
         * @param cameraUpdate The camera update to animate to
         * @param durationMs Duration of the animation in milliseconds
         * @param listener Optional listener for animation completion events
         */
        fun animateCamera(
            cameraUpdate: CameraUpdate,
            durationMs: Int,
            listener: OnCameraAnimationListener?,
        ) {
            controller.animateCamera(cameraUpdate, durationMs, listener)
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
         * Removes all markers, polylines, and polygons from the map.
         *
         * This is equivalent to calling clearMarkers(), clearPolylines(), and clearPolygons().
         */
        fun clear() {
            clearMarkers()
            clearPolylines()
            clearPolygons()
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
