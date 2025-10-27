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
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

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
                        }

                        controller.commitPan()
                        invalidate()
                        return true
                    }
                }
            }
            return true
        }

        fun setCenter(latLng: LatLng) {
            controller.setCenter(latLng)
            invalidate()
        }

        fun setZoom(zoom: Double) {
            controller.setZoom(zoom)
            invalidate()
        }

        fun getZoom(): Double = controller.getZoom()

        fun getCameraPosition(): CameraPosition = controller.getCameraPosition()

        fun moveCamera(cameraUpdate: CameraUpdate) {
            controller.moveCamera(cameraUpdate)
            invalidate()
        }

        fun animateCamera(cameraUpdate: CameraUpdate) {
            controller.animateCamera(cameraUpdate)
        }

        fun animateCamera(
            cameraUpdate: CameraUpdate,
            durationMs: Int,
        ) {
            controller.animateCamera(cameraUpdate, durationMs)
        }

        fun animateCamera(
            cameraUpdate: CameraUpdate,
            durationMs: Int,
            listener: OnCameraAnimationListener?,
        ) {
            controller.animateCamera(cameraUpdate, durationMs, listener)
        }

        fun stopAnimation() {
            controller.stopAnimation()
            invalidate()
        }

        fun addMarker(marker: Marker): Marker {
            val result = controller.addMarker(marker)
            invalidate()
            return result
        }

        fun removeMarker(marker: Marker): Boolean {
            val result = controller.removeMarker(marker)
            if (result) invalidate()
            return result
        }

        fun clearMarkers() {
            controller.clearMarkers()
            invalidate()
        }

        fun getMarkers(): List<Marker> = controller.getMarkers()

        fun setOnMarkerClickListener(listener: (Marker) -> Boolean) {
            controller.onMarkerClickListener = listener
        }

        fun addPolyline(polyline: Polyline): Polyline {
            val result = controller.addPolyline(polyline)
            invalidate()
            return result
        }

        fun removePolyline(polyline: Polyline): Boolean {
            val result = controller.removePolyline(polyline)
            if (result) invalidate()
            return result
        }

        fun clearPolylines() {
            controller.clearPolylines()
            invalidate()
        }

        fun getPolylines(): List<Polyline> = controller.getPolylines()

        fun addPolygon(polygon: Polygon): Polygon {
            val result = controller.addPolygon(polygon)
            invalidate()
            return result
        }

        fun removePolygon(polygon: Polygon): Boolean {
            val result = controller.removePolygon(polygon)
            if (result) invalidate()
            return result
        }

        fun clearPolygons() {
            controller.clearPolygons()
            invalidate()
        }

        fun getPolygons(): List<Polygon> = controller.getPolygons()

        fun addGeoJson(geoJsonString: String): GeoJsonResult {
            val result = controller.addGeoJson(geoJsonString)
            invalidate()
            return result
        }

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
