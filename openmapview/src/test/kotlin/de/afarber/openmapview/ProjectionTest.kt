/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectionTest {
    private val epsilonFloat = 0.0001f
    private val epsilonDouble = 0.0001

    @Test
    fun testLatLngToPixel_Equator() {
        // At zoom 0, equator center should be at (128, 128)
        val (x, y) = ProjectionUtils.latLngToPixel(LatLng(0.0, 0.0), 0)
        assertEquals(128.0f, x, epsilonFloat)
        assertEquals(128.0f, y, epsilonFloat)
    }

    @Test
    fun testLatLngToPixel_NullIsland() {
        // Null Island (0,0) at different zooms
        val zoom1 = ProjectionUtils.latLngToPixel(LatLng(0.0, 0.0), 1)
        assertEquals(256.0f, zoom1.first, epsilonFloat)
        assertEquals(256.0f, zoom1.second, epsilonFloat)

        val zoom2 = ProjectionUtils.latLngToPixel(LatLng(0.0, 0.0), 2)
        assertEquals(512.0f, zoom2.first, epsilonFloat)
        assertEquals(512.0f, zoom2.second, epsilonFloat)
    }

    @Test
    fun testPixelToLatLng_Equator() {
        // At zoom 0, pixel (128, 128) should be equator center
        val latLng = ProjectionUtils.pixelToLatLng(128, 128, 0)
        assertEquals(0.0, latLng.latitude, epsilonDouble)
        assertEquals(0.0, latLng.longitude, epsilonDouble)
    }

    @Test
    fun testPixelToLatLng_RoundTrip() {
        // Test round-trip conversion
        val original = LatLng(51.4661, 7.2491) // Bochum
        val zoom = 14

        val (x, y) = ProjectionUtils.latLngToPixel(original, zoom)
        val result = ProjectionUtils.pixelToLatLng(x.toInt(), y.toInt(), zoom)

        assertEquals(original.latitude, result.latitude, epsilonDouble)
        assertEquals(original.longitude, result.longitude, epsilonDouble)
    }

    @Test
    fun testLatLngToTile_Bochum() {
        // Bochum at zoom 14
        val tile = ProjectionUtils.latLngToTile(LatLng(51.4661, 7.2491), 14)
        assertEquals(14, tile.zoom)
        // Bochum is at tile (8521, 5451) at zoom 14
        // Calculated using Web Mercator projection formulas
        assertEquals(8521, tile.x)
        assertEquals(5451, tile.y)
    }

    @Test
    fun testLatLngToTile_Equator() {
        // Equator center at zoom 0
        val tile = ProjectionUtils.latLngToTile(LatLng(0.0, 0.0), 0)
        assertEquals(0, tile.x)
        assertEquals(0, tile.y)
        assertEquals(0, tile.zoom)
    }

    @Test
    fun testTileToPixel() {
        val tile = TileCoordinate(1, 2, 5)
        val (x, y) = ProjectionUtils.tileToPixel(tile)
        assertEquals(256, x) // 1 * 256
        assertEquals(512, y) // 2 * 256
    }

    @Test
    fun testLongitudeWrapping() {
        // Test that longitude wrapping works correctly
        // At zoom 1, the map is 512 pixels wide (2 tiles * 256)
        val positive = ProjectionUtils.latLngToPixel(LatLng(0.0, 180.0), 1)
        val negative = ProjectionUtils.latLngToPixel(LatLng(0.0, -180.0), 1)

        // +180° should be at right edge (512), -180° at left edge (0)
        // They represent the same meridian but wrap around
        assertEquals(512.0f, positive.first, epsilonFloat)
        assertEquals(0.0f, negative.first, epsilonFloat)
    }

    @Test
    fun testNorthernHemisphere() {
        // Berlin
        val berlin = LatLng(52.52, 13.405)
        val (x, y) = ProjectionUtils.latLngToPixel(berlin, 10)

        // Northern hemisphere should have y < center
        val centerY = (256.0 * (1 shl 10)) / 2.0
        assert(y < centerY)
    }

    @Test
    fun testSouthernHemisphere() {
        // Sydney
        val sydney = LatLng(-33.8688, 151.2093)
        val (x, y) = ProjectionUtils.latLngToPixel(sydney, 10)

        // Southern hemisphere should have y > center
        val centerY = (256.0 * (1 shl 10)) / 2.0
        assert(y > centerY)
    }

    @Test
    fun testEasternHemisphere() {
        // Tokyo
        val tokyo = LatLng(35.6762, 139.6503)
        val (x, y) = ProjectionUtils.latLngToPixel(tokyo, 10)

        // Eastern hemisphere should have x > center
        val centerX = (256.0 * (1 shl 10)) / 2.0
        assert(x > centerX)
    }

    @Test
    fun testWesternHemisphere() {
        // New York
        val newYork = LatLng(40.7128, -74.0060)
        val (x, y) = ProjectionUtils.latLngToPixel(newYork, 10)

        // Western hemisphere should have x < center
        val centerX = (256.0 * (1 shl 10)) / 2.0
        assert(x < centerX)
    }

    // Note: Projection class padding support is implemented and tested via MapController integration tests
}
