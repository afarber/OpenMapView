/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MapLoadedCallbackTest {
    private lateinit var controller: MapController
    private var loadedCount = 0
    private val callback = OnMapLoadedCallback { loadedCount++ }

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(SIZE, SIZE)
        // Memory-only cache, so no tile comes from disk and no download can finish
        setField("tileCache", TileCache())
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    @Test
    fun testFiresOnceWhenAllTilesAreDrawn() {
        controller.setOnMapLoadedCallback(callback)
        putVisibleTiles()

        draw()
        draw()

        assertEquals(1, loadedCount)
    }

    @Test
    fun testWaitsForMissingTile() {
        controller.setOnMapLoadedCallback(callback)
        val tiles = visibleTiles()
        tiles.drop(1).forEach { tileCache().put(it, tileBitmap()) }

        draw()
        assertEquals(0, loadedCount)

        tileCache().put(tiles.first(), tileBitmap())
        draw()
        assertEquals(1, loadedCount)
    }

    @Test
    fun testFiresForAlreadyLoadedMap() {
        putVisibleTiles()
        draw()
        var redrawRequested = false
        controller.setOnTileLoadedCallback { redrawRequested = true }

        controller.setOnMapLoadedCallback(callback)

        // OpenMapView redraws on this request, the draw then reports the loaded map
        assertEquals(true, redrawRequested)
        draw()
        assertEquals(1, loadedCount)
    }

    @Test
    fun testFiresWithoutBaseMap() {
        controller.setMapType(MapType.NONE)
        controller.setOnMapLoadedCallback(callback)

        draw()

        assertEquals(1, loadedCount)
    }

    @Test
    fun testWaitsWhileCameraIsMoving() {
        controller.setOnMapLoadedCallback(callback)
        putVisibleTiles()
        controller.isCameraMoving = true

        draw()
        assertEquals(0, loadedCount)

        controller.isCameraMoving = false
        draw()
        assertEquals(1, loadedCount)
    }

    @Test
    fun testNullRemovesPendingCallback() {
        controller.setOnMapLoadedCallback(callback)
        controller.setOnMapLoadedCallback(null)
        putVisibleTiles()

        draw()

        assertEquals(0, loadedCount)
    }

    @Test
    fun testOpenMapViewForwardsCallback() {
        val mapView = OpenMapView(RuntimeEnvironment.getApplication())
        mapView.layout(0, 0, SIZE, SIZE)
        mapView.setMapType(MapType.NONE)
        mapView.setOnMapLoadedCallback(callback)

        mapView.draw(Canvas(Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)))
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, loadedCount)
    }

    // Draws once and runs the callbacks posted to the main thread. A mocked canvas, because
    // Robolectric's legacy graphics can't draw bitmaps that are partly outside the canvas.
    private fun draw() {
        controller.draw(mockk<Canvas>(relaxed = true))
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun visibleTiles(): List<TileCoordinate> =
        ViewportCalculator.getVisibleTiles(controller.getCenter(), controller.getZoom().toInt(), SIZE, SIZE)

    private fun putVisibleTiles() = visibleTiles().forEach { tileCache().put(it, tileBitmap()) }

    private fun tileBitmap(): Bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.RGB_565)

    private fun tileCache(): TileCache {
        val field = MapController::class.java.getDeclaredField("tileCache")
        field.isAccessible = true
        return field.get(controller) as TileCache
    }

    private fun setField(
        name: String,
        value: Any,
    ) {
        val field = MapController::class.java.getDeclaredField(name)
        field.isAccessible = true
        field.set(controller, value)
    }

    private companion object {
        const val SIZE = 256
    }
}
