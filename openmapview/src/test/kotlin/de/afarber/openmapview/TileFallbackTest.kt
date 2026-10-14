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
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals

/**
 * Checks that cached tiles of other zoom levels are drawn while a tile is missing.
 * Needs real pixels, so it runs with native graphics (available from SDK 26).
 *
 * The 256x256 view is centered on LatLng(0, 0) at zoom 10, the corner of four tiles. The
 * pixel at (192, 192) lies in the top-left quarter of tile (512, 512), which is the
 * top-left quarter of its parent (256, 256) at zoom 9 and its child (1024, 1024) at zoom 11.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class TileFallbackTest {
    private lateinit var controller: MapController
    private lateinit var cache: TileCache

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

    private fun drawPixel(): Int {
        val bitmap = createBitmap(SIZE, SIZE)
        controller.draw(Canvas(bitmap))
        return bitmap.getPixel(192, 192)
    }

    // A tile whose top-left quarter has the given color and the rest is blue
    private fun tile(topLeftColor: Int): Bitmap =
        createBitmap(256, 256).applyCanvas {
            drawColor(Color.BLUE)
            clipRect(0, 0, 128, 128)
            drawColor(topLeftColor)
        }

    @Test
    fun testMissingTile_WithoutFallback_ShowsPlaceholder() {
        assertEquals(Color.LTGRAY, drawPixel())
    }

    @Test
    fun testMissingTile_ShowsMatchingPartOfCachedParent() {
        cache.put(TileCoordinate(256, 256, 9), tile(Color.RED))

        assertEquals(Color.RED, drawPixel())
    }

    @Test
    fun testMissingTile_ShowsCachedAncestorSeveralLevelsUp() {
        // Tile (512, 512) at zoom 10 is in the top-left 1/16 of (32, 32) at zoom 6
        cache.put(TileCoordinate(32, 32, 6), tile(Color.RED))

        assertEquals(Color.RED, drawPixel())
    }

    @Test
    fun testMissingTile_ShowsCachedChild() {
        cache.put(TileCoordinate(1024, 1024, 11), createBitmap(256, 256).applyCanvas { drawColor(Color.GREEN) })

        assertEquals(Color.GREEN, drawPixel())
    }

    @Test
    fun testCachedTile_IsDrawnInsteadOfFallback() {
        cache.put(TileCoordinate(256, 256, 9), tile(Color.RED))
        cache.put(TileCoordinate(512, 512, 10), createBitmap(256, 256).applyCanvas { drawColor(Color.YELLOW) })

        assertEquals(Color.YELLOW, drawPixel())
    }

    private companion object {
        const val SIZE = 256
    }
}
