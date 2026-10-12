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
        assertEquals("© OpenStreetMap contributors", TileSource.STANDARD.attributionText)
    }

    @Test
    fun testTileSource_Humanitarian() {
        val tile = TileCoordinate(x = 50, y = 75, zoom = 8)
        val url = TileSource.HUMANITARIAN.getTileUrl(tile)

        // Subdomain is calculated as (x + y) % 3 = (50 + 75) % 3 = 2 -> 'c'
        assertEquals("https://tile-c.openstreetmap.fr/hot/8/50/75.png", url)
        assertTrue(TileSource.HUMANITARIAN.attributionText.contains("OpenStreetMap"))
    }

    @Test
    fun testTileSource_TracesTrackTopo() {
        val tile = TileCoordinate(x = 25, y = 30, zoom = 6)
        val url = TileSource.TRACESTRACK_TOPO.getTileUrl(tile)

        // Note: This test will fail without API key configured
        assertTrue(url.contains("tile.tracestrack.com") || url.isEmpty())
        assertTrue(TileSource.TRACESTRACK_TOPO.attributionText.contains("OpenStreetMap"))
        assertTrue(TileSource.TRACESTRACK_TOPO.attributionText.contains("Tracestrack"))
    }

    @Test
    fun testTileSource_CyclOSM() {
        val tile = TileCoordinate(x = 10, y = 20, zoom = 5)
        val url = TileSource.CYCLOSM.getTileUrl(tile)

        assertEquals("https://a.tile-cyclosm.openstreetmap.fr/cyclosm/5/10/20.png", url)
        assertTrue(TileSource.CYCLOSM.attributionText.contains("OpenStreetMap"))
        assertTrue(TileSource.CYCLOSM.attributionText.contains("France"))
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
                source.attributionText.isNotEmpty(),
            )
        }
    }
}
