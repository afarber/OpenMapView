/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Checks in real pixels that markers and shapes are drawn, and can be tapped, on every copy
 * of the world that is visible. At zoom 2 the world is 1024 px wide, narrower than the view.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class WorldCopiesDrawTest {
    private val width = 1408
    private val height = 600
    private val sanFrancisco = LatLng(38.0, -122.4)
    private val beijing = LatLng(38.0, 116.4)
    private val lineColor = Color.Blue
    private val markerColor = Color.Red

    private lateinit var mapView: OpenMapView
    private lateinit var marker: Marker

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
        mapView.setMapType(MapType.NONE)
        mapView.layout(0, 0, width, height)
        mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(sanFrancisco, 2f))

        // A flight along a parallel, so its pixels are easy to find. San Francisco is at the
        // view center (704), Beijing 345 px west of it (359) and on the next copy (1383).
        mapView.addPolyline(Polyline(points = listOf(sanFrancisco, beijing), strokeColor = lineColor, strokeWidth = 10f))
        val icon = createBitmap(20, 20).apply { eraseColor(markerColor.toArgb()) }
        marker =
            mapView.addMarker(
                Marker(position = LatLng(10.0, 116.4), icon = BitmapDescriptorFactory.fromBitmap(icon), anchor = 0.5f to 0.5f),
            )
    }

    private fun drawMap() = createBitmap(width, height).also { mapView.draw(Canvas(it)) }

    private fun screenY(latLng: LatLng) = mapView.getProjection().toScreenLocation(latLng).y

    @Test
    fun `a line is drawn on both visible copies of the world`() {
        val bitmap = drawMap()
        val y = screenY(sanFrancisco)

        assertEquals(lineColor.toArgb(), bitmap.getPixel(500, y), "line west of San Francisco")
        // The next copy: from Beijing at 1383 east toward San Francisco at 1728
        assertEquals(lineColor.toArgb(), bitmap.getPixel(1395, y), "line on the next copy")
    }

    @Test
    fun `a marker is drawn on both visible copies of the world`() {
        val bitmap = drawMap()
        val y = screenY(marker.position)

        assertEquals(markerColor.toArgb(), bitmap.getPixel(359, y), "marker on this copy")
        assertEquals(markerColor.toArgb(), bitmap.getPixel(1383, y), "marker on the next copy")
    }

    @Test
    fun `markers and shapes on the next copy can be tapped`() {
        val controller = MapController(ApplicationProvider.getApplicationContext())
        controller.setViewSize(width, height)
        controller.setZoom(2f)
        controller.setCenter(sanFrancisco)
        val line = controller.addPolyline(Polyline(points = listOf(sanFrancisco, beijing), clickable = true))
        val pin = controller.addMarker(Marker(position = marker.position, icon = marker.icon, anchor = 0.5f to 0.5f))
        val projection = controller.createProjection()

        assertSame(pin, controller.handleMarkerTouch(1383f, projection.toScreenLocation(pin.position).y.toFloat()))
        assertSame(line, controller.handlePolylineTouch(1395f, projection.toScreenLocation(sanFrancisco).y.toFloat()))
        controller.onDestroy()
    }
}
