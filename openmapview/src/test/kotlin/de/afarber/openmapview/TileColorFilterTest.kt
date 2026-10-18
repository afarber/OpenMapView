/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.graphics.Color as ComposeColor

/**
 * Tests that the tile color filter changes the base map tiles and nothing else. The map is
 * 256x256 px at zoom 10, centered on tile (512, 512), whose top-left corner is the center.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class TileColorFilterTest {
    private lateinit var controller: MapController
    private lateinit var cache: TileCache

    // Turns every color black and keeps the alpha
    private val blackFilter = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0f, 0f, 0f, 1f) })

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        // No tile source: nothing is downloaded, so only the cached tiles are drawn
        controller.setMapType(MapType.NONE)
        cache = TileCache()
        val field = MapController::class.java.getDeclaredField("tileCache")
        field.isAccessible = true
        field.set(controller, cache)
        controller.setViewSize(SIZE, SIZE)
        controller.setCenter(LatLng(0.0, 0.0))
        controller.setZoom(10f)
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    private fun draw(): Bitmap = createBitmap(SIZE, SIZE).also { controller.draw(Canvas(it)) }

    private fun solid(
        color: Int,
        size: Int = 256,
    ): Bitmap = createBitmap(size, size).applyCanvas { drawColor(color) }

    // Fills the visible tiles of zoom 10 with one color
    private fun cacheTiles(color: Int) {
        for (x in 511..512) {
            for (y in 511..512) cache.put(TileCoordinate(x, y, 10), solid(color))
        }
    }

    @Test
    fun testFilter_ChangesTilesUntilCleared() {
        cacheTiles(Color.YELLOW)

        controller.setTileColorFilter(blackFilter)
        assertSame(blackFilter, controller.getTileColorFilter())
        assertEquals(Color.BLACK, draw().getPixel(64, 64))

        controller.setTileColorFilter(null)
        assertNull(controller.getTileColorFilter())
        assertEquals(Color.YELLOW, draw().getPixel(64, 64))
    }

    @Test
    fun testFilter_ChangesPlaceholderAndFallbackTiles() {
        // Tile (512, 512) is missing, its parent at zoom 9 is cached; the others are placeholders
        cache.put(TileCoordinate(256, 256, 9), solid(Color.RED))
        controller.setTileColorFilter(blackFilter)

        val bitmap = draw()

        assertEquals(Color.BLACK, bitmap.getPixel(192, 192))
        assertEquals(Color.BLACK, bitmap.getPixel(64, 64))
    }

    @Test
    fun testFilter_KeepsMarkerAndShapeColors() {
        cacheTiles(Color.YELLOW)
        controller.addPolygon(
            Polygon(
                points = listOf(LatLng(-0.1, -0.1), LatLng(-0.1, 0.1), LatLng(0.1, 0.1), LatLng(0.1, -0.1)),
                fillColor = ComposeColor.Green,
            ),
        )
        controller.addMarker(
            Marker(
                position = LatLng(0.0, 0.0),
                icon = BitmapDescriptorFactory.fromBitmap(solid(Color.MAGENTA, 10)),
                anchor = Pair(0.5f, 0.5f),
            ),
        )
        controller.setTileColorFilter(blackFilter)

        val bitmap = draw()

        assertEquals(Color.BLACK, bitmap.getPixel(10, 10))
        assertEquals(Color.GREEN, bitmap.getPixel(128 + 50, 128))
        assertEquals(Color.MAGENTA, bitmap.getPixel(128, 128))
    }

    @Test
    fun testInvertedDark_MakesLightTilesDarkAndKeepsWaterBlue() {
        val water = Color.rgb(170, 211, 223)
        cacheTiles(Color.WHITE)
        cache.put(TileCoordinate(512, 512, 10), solid(water))
        controller.setTileColorFilter(TileColorFilters.invertedDark())

        val bitmap = draw()

        // White becomes dark gray
        val background = bitmap.getPixel(64, 64)
        assertTrue(Color.red(background) < 40 && Color.red(background) == Color.blue(background))
        // Light blue becomes dark blue
        val darkWater = bitmap.getPixel(192, 192)
        assertTrue(Color.blue(darkWater) < 128 && Color.blue(darkWater) > Color.red(darkWater))
    }

    @Test
    fun testInvertedDark_ReturnsSameFilter() {
        assertSame(TileColorFilters.invertedDark(), TileColorFilters.invertedDark())
    }

    private companion object {
        const val SIZE = 256
    }
}
