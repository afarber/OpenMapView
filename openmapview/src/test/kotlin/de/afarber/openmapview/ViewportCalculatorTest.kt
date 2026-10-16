/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ViewportCalculatorTest {
    @Test
    fun testGetVisibleTiles_EmptyViewport() {
        val tiles =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(0.0, 0.0),
                zoom = 10,
                viewWidth = 0,
                viewHeight = 0,
            )
        assertTrue(tiles.isEmpty())
    }

    @Test
    fun testGetVisibleTiles_BasicViewport() {
        // 512x512 viewport at zoom 1 should cover multiple tiles
        val tiles =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(0.0, 0.0),
                zoom = 1,
                viewWidth = 512,
                viewHeight = 512,
            )

        // Should return tiles around center
        assertTrue(tiles.isNotEmpty())
        assertTrue(tiles.all { it.zoom == 1 })
    }

    @Test
    fun testGetVisibleTiles_AllTilesHaveCorrectZoom() {
        val zoom = 5
        val tiles =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(51.4661, 7.2491),
                zoom = zoom,
                viewWidth = 1080,
                viewHeight = 1920,
            )

        // All tiles should have the requested zoom level
        assertTrue(tiles.all { it.zoom == zoom })
    }

    @Test
    fun testGetVisibleTiles_IncludesBufferTiles() {
        // With buffer, we should get more tiles than strictly visible
        val tilesWithoutPan =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(51.4661, 7.2491),
                zoom = 14,
                viewWidth = 256,
                viewHeight = 256,
                panOffsetX = 0f,
                panOffsetY = 0f,
            )

        // ViewportCalculator includes 1-tile buffer
        // For a 256x256 viewport (1 tile), we should get 3x3 = 9 tiles
        assertTrue(tilesWithoutPan.size >= 9)
    }

    @Test
    fun testGetVisibleTiles_WithPanOffset() {
        val tilesNoPan =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(51.4661, 7.2491),
                zoom = 14,
                viewWidth = 512,
                viewHeight = 512,
                panOffsetX = 0f,
                panOffsetY = 0f,
            )

        val tilesWithPan =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(51.4661, 7.2491),
                zoom = 14,
                viewWidth = 512,
                viewHeight = 512,
                panOffsetX = 128f,
                panOffsetY = 128f,
            )

        // Pan offset should affect which tiles are visible
        // Both sets should be non-empty
        assertTrue(tilesNoPan.isNotEmpty())
        assertTrue(tilesWithPan.isNotEmpty())
    }

    @Test
    fun testGetVisibleTiles_DifferentZoomLevels() {
        val center = LatLng(51.4661, 7.2491)
        val viewWidth = 1080
        val viewHeight = 1920

        val tilesZoom5 = ViewportCalculator.getVisibleTiles(center, 5, viewWidth, viewHeight)
        val tilesZoom10 = ViewportCalculator.getVisibleTiles(center, 10, viewWidth, viewHeight)
        val tilesZoom15 = ViewportCalculator.getVisibleTiles(center, 15, viewWidth, viewHeight)

        // All zoom levels should return tiles
        assertTrue(tilesZoom5.isNotEmpty())
        assertTrue(tilesZoom10.isNotEmpty())
        assertTrue(tilesZoom15.isNotEmpty())

        // Each zoom level should have correct zoom property
        assertTrue(tilesZoom5.all { it.zoom == 5 })
        assertTrue(tilesZoom10.all { it.zoom == 10 })
        assertTrue(tilesZoom15.all { it.zoom == 15 })
    }

    @Test
    fun testGetVisibleTiles_LargeViewport() {
        // Very large viewport should return many tiles
        val tiles =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(51.4661, 7.2491),
                zoom = 14,
                viewWidth = 4000,
                viewHeight = 4000,
            )

        // Should have lots of tiles for large viewport
        assertTrue(tiles.size > 100)
    }

    @Test
    fun testGetVisibleTiles_NoDuplicates() {
        val tiles =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(51.4661, 7.2491),
                zoom = 14,
                viewWidth = 1080,
                viewHeight = 1920,
            )

        // Convert to set to check for duplicates
        val uniqueTiles = tiles.toSet()
        assertEquals(tiles.size, uniqueTiles.size)
    }

    @Test
    fun testGetVisibleTiles_EdgeOfWorld() {
        // Test near international date line
        val tiles =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(0.0, 179.9),
                zoom = 10,
                viewWidth = 512,
                viewHeight = 512,
            )

        assertTrue(tiles.isNotEmpty())
        assertTrue(tiles.all { it.zoom == 10 })
    }

    @Test
    fun testGetVisibleTiles_NearPole() {
        // Test near north pole (high latitude)
        val tiles =
            ViewportCalculator.getVisibleTiles(
                center = LatLng(85.0, 0.0),
                zoom = 10,
                viewWidth = 512,
                viewHeight = 512,
            )

        assertTrue(tiles.isNotEmpty())
        assertTrue(tiles.all { it.zoom == 10 })
    }

    @Test
    fun testFractionalZoomUsesNearestTileZoom() {
        val center = LatLng(51.4661, 7.2491)

        assertTrue(ViewportCalculator.getVisibleTiles(center, 12.4f, 1408, 620).all { it.zoom == 12 })
        assertTrue(ViewportCalculator.getVisibleTiles(center, 12.6f, 1408, 620).all { it.zoom == 13 })
    }

    @Test
    fun testFractionalZoomTilesCoverViewport() {
        val center = LatLng(51.4661, 7.2491)
        // Zoom 12.6 draws zoom 13 tiles scaled by 2^-0.4, so more of them are visible
        val zoom = 12.6f
        val scale = 2.0.pow(zoom - 13.0)
        val tiles = ViewportCalculator.getVisibleTiles(center, zoom, 1408, 620)

        val (centerX, centerY) = ProjectionUtils.latLngToPixel(center, 13)
        val left = centerX - 704 / scale
        val right = centerX + 704 / scale
        val top = centerY - 310 / scale
        val bottom = centerY + 310 / scale
        assertTrue(tiles.minOf { it.x } * 256 <= left)
        assertTrue((tiles.maxOf { it.x } + 1) * 256 >= right)
        assertTrue(tiles.minOf { it.y } * 256 <= top)
        assertTrue((tiles.maxOf { it.y } + 1) * 256 >= bottom)
    }
}
