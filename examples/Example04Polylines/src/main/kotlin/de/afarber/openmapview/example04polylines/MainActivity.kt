/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example04polylines

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OnCameraMoveStartedListener
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.Polygon
import de.afarber.openmapview.Polyline

/**
 * Main activity demonstrating OpenMapView polyline and polygon navigation.
 *
 * This example showcases:
 * - Displaying polylines and polygons at real Bochum locations
 * - Navigating between overlays with prev/next buttons
 * - Highlighting selected overlay with thicker stroke via FAB
 * - Camera animation when centering on overlays
 * - Real-time selection index and highlight state tracking
 * - Camera state monitoring
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MapViewScreen()
                }
            }
        }
    }
}

/**
 * Main composable screen containing the map and overlay navigation controls.
 *
 * Displays an OpenMapView with polylines and polygons at notable Bochum locations,
 * a status toolbar showing selection state, and an overlay toolbar for navigation.
 */
@Composable
fun MapViewScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current

    // Calculate initial location from all overlay points
    val allPoints = poiOverlays.flatMap { it.points }
    val initialLocation = LatLng(
        allPoints.map { it.latitude }.average(),
        allPoints.map { it.longitude }.average(),
    )
    val initialZoom = 13.0f

    // State variables
    var mapView: OpenMapView? by remember { mutableStateOf(null) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var cameraState by remember { mutableStateOf("Idle") }
    var isHighlighted by remember { mutableStateOf(false) }

    // Track created overlays for reference
    var createdPolylines by remember { mutableStateOf<List<Polyline>>(emptyList()) }
    var createdPolygons by remember { mutableStateOf<List<Polygon>>(emptyList()) }

    // Derived state - current overlay data
    val selectedOverlay: OverlayData? = poiOverlays.getOrNull(selectedIndex)

    /**
     * Creates overlays on the map from the predefined data.
     */
    fun createOverlays(map: OpenMapView, highlighted: Boolean = false, highlightIndex: Int = -1) {
        val polylines = mutableListOf<Polyline>()
        val polygons = mutableListOf<Polygon>()

        poiOverlays.forEachIndexed { index, data ->
            val isThisHighlighted = highlighted && index == highlightIndex
            val strokeMultiplier = if (isThisHighlighted) 2.0f else 1.0f

            when (data) {
                is PolylineData -> {
                    val polyline = Polyline(
                        points = data.points,
                        strokeColor = data.color,
                        strokeWidth = data.width * strokeMultiplier,
                        clickable = true,
                        tag = data.title,
                    )
                    map.addPolyline(polyline)
                    polylines.add(polyline)
                }
                is PolygonData -> {
                    val polygon = Polygon(
                        points = data.points,
                        holes = data.holes,
                        strokeColor = data.strokeColor,
                        strokeWidth = 4f * strokeMultiplier,
                        fillColor = data.fillColor,
                        clickable = true,
                        tag = data.title,
                    )
                    map.addPolygon(polygon)
                    polygons.add(polygon)
                }
            }
        }

        createdPolylines = polylines
        createdPolygons = polygons
    }

    /**
     * Recreates all overlays with updated highlight state.
     */
    fun updateHighlight(map: OpenMapView, highlighted: Boolean, highlightIndex: Int) {
        // Remove all existing overlays
        createdPolylines.forEach { map.removePolyline(it) }
        createdPolygons.forEach { map.removePolygon(it) }

        // Recreate with new highlight state
        createOverlays(map, highlighted, highlightIndex)
    }

    /**
     * Calculates the center point of an overlay.
     */
    fun getOverlayCenter(overlay: OverlayData): LatLng = LatLng(
        overlay.points.map { it.latitude }.average(),
        overlay.points.map { it.longitude }.average(),
    )

    /**
     * Gets the overlay type as a string.
     */
    fun getOverlayType(overlay: OverlayData): String = when (overlay) {
        is PolylineData -> "Polyline"
        else -> "Polygon"
    }

    /**
     * Finds the index of an overlay by its title.
     */
    fun findOverlayIndexByTitle(title: String): Int = poiOverlays.indexOfFirst { it.title == title }.coerceAtLeast(0)

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(initialLocation)
                    setZoom(initialZoom)

                    createOverlays(this)

                    setOnCameraMoveStartedListener { reason ->
                        cameraState = when (reason) {
                            OnCameraMoveStartedListener.REASON_GESTURE -> "Moving (gesture)"
                            OnCameraMoveStartedListener.REASON_API_ANIMATION -> "Moving (animation)"
                            OnCameraMoveStartedListener.REASON_DEVELOPER_ANIMATION -> "Moving (programmatic)"
                            else -> "Moving"
                        }
                    }

                    setOnCameraIdleListener {
                        cameraState = "Idle"
                    }

                    setOnPolylineClickListener { polyline ->
                        val tag = polyline.tag as? String ?: return@setOnPolylineClickListener
                        val newIndex = findOverlayIndexByTitle(tag)
                        if (newIndex != selectedIndex) {
                            // Turn off highlight when selecting different overlay
                            if (isHighlighted) {
                                isHighlighted = false
                                mapView?.let { updateHighlight(it, false, -1) }
                            }
                        }
                        selectedIndex = newIndex
                        animateCamera(CameraUpdateFactory.newLatLng(getOverlayCenter(poiOverlays[newIndex])), 500)
                    }

                    setOnPolygonClickListener { polygon ->
                        val tag = polygon.tag as? String ?: return@setOnPolygonClickListener
                        val newIndex = findOverlayIndexByTitle(tag)
                        if (newIndex != selectedIndex) {
                            // Turn off highlight when selecting different overlay
                            if (isHighlighted) {
                                isHighlighted = false
                                mapView?.let { updateHighlight(it, false, -1) }
                            }
                        }
                        selectedIndex = newIndex
                        animateCamera(CameraUpdateFactory.newLatLng(getOverlayCenter(poiOverlays[newIndex])), 500)
                    }

                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Status overlay at left
        StatusToolbar(
            totalCount = poiOverlays.size,
            selectedIndex = selectedIndex,
            overlayTitle = selectedOverlay?.title ?: "None",
            overlayType = selectedOverlay?.let { getOverlayType(it) } ?: "",
            cameraState = cameraState,
            isHighlighted = isHighlighted,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(16.dp),
        )

        // Overlay toolbar at bottom
        OverlayToolbar(
            onPrevClick = {
                val newIndex = (selectedIndex - 1 + poiOverlays.size) % poiOverlays.size
                if (isHighlighted && newIndex != selectedIndex) {
                    isHighlighted = false
                    mapView?.let { updateHighlight(it, false, -1) }
                }
                selectedIndex = newIndex
                mapView?.animateCamera(
                    CameraUpdateFactory.newLatLng(getOverlayCenter(poiOverlays[newIndex])),
                    500,
                )
            },
            onNextClick = {
                val newIndex = (selectedIndex + 1) % poiOverlays.size
                if (isHighlighted && newIndex != selectedIndex) {
                    isHighlighted = false
                    mapView?.let { updateHighlight(it, false, -1) }
                }
                selectedIndex = newIndex
                mapView?.animateCamera(
                    CameraUpdateFactory.newLatLng(getOverlayCenter(poiOverlays[newIndex])),
                    500,
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
        )

        // FAB for highlight toggle
        FloatingActionButton(
            onClick = {
                isHighlighted = !isHighlighted
                mapView?.let { updateHighlight(it, isHighlighted, selectedIndex) }
            },
            containerColor = if (isHighlighted) OsmHighwayPink else OsmWaterBlue,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Highlight,
                contentDescription = "Toggle Highlight",
            )
        }
    }
}
