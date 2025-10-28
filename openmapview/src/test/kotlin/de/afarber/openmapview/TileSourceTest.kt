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

class TileSourceTest {
    @Test
    fun testTileSource_Standard() {
        val tile = TileCoordinate(x = 100, y = 200, zoom = 10)
        val url = TileSource.STANDARD.getTileUrl(tile)

        assertEquals("https://tile.openstreetmap.org/10/100/200.png", url)
        assertEquals("© OpenStreetMap contributors", TileSource.STANDARD.attribution)
    }

    @Test
    fun testTileSource_Humanitarian() {
        val tile = TileCoordinate(x = 50, y = 75, zoom = 8)
        val url = TileSource.HUMANITARIAN.getTileUrl(tile)

        assertEquals("https://tile-a.openstreetmap.fr/hot/8/50/75.png", url)
        assertEquals("© OpenStreetMap contributors", TileSource.HUMANITARIAN.attribution)
    }

    @Test
    fun testTileSource_Topo() {
        val tile = TileCoordinate(x = 25, y = 30, zoom = 6)
        val url = TileSource.TOPO.getTileUrl(tile)

        assertEquals("https://a.tile.opentopomap.org/6/25/30.png", url)
        assertTrue(TileSource.TOPO.attribution.contains("OpenStreetMap"))
        assertTrue(TileSource.TOPO.attribution.contains("OpenTopoMap"))
    }

    @Test
    fun testTileSource_Cycle() {
        val tile = TileCoordinate(x = 10, y = 20, zoom = 5)
        val url = TileSource.CYCLE.getTileUrl(tile)

        assertEquals("https://a.tile-cyclosm.openstreetmap.fr/cyclosm/5/10/20.png", url)
        assertTrue(TileSource.CYCLE.attribution.contains("OpenStreetMap"))
        assertTrue(TileSource.CYCLE.attribution.contains("CyclOSM"))
    }

    @Test
    fun testTileSource_ZeroCoordinates() {
        val tile = TileCoordinate(x = 0, y = 0, zoom = 0)
        val url = TileSource.STANDARD.getTileUrl(tile)

        assertEquals("https://tile.openstreetmap.org/0/0/0.png", url)
    }

    @Test
    fun testTileSource_MaxZoom() {
        val tile = TileCoordinate(x = 1000000, y = 2000000, zoom = 19)
        val url = TileSource.STANDARD.getTileUrl(tile)

        assertEquals("https://tile.openstreetmap.org/19/1000000/2000000.png", url)
    }

    @Test
    fun testAllTileSources_HaveAttribution() {
        for (source in TileSource.values()) {
            assertTrue(
                "TileSource ${source.name} should have non-empty attribution",
                source.attribution.isNotEmpty(),
            )
        }
    }
}
