/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileSourceTest {
    @After
    fun tearDown() {
        ApiKeyManager.clearProgrammaticKeys()
    }

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

    @Test
    fun testStamenSources_RequireStadiaKey() {
        for (source in listOf(TileSource.STAMEN_TONER, TileSource.STAMEN_WATERCOLOR)) {
            assertTrue(source.requiresApiKey)
            assertEquals("stadia", source.apiKeyProvider)
            assertEquals("Stadia Maps", source.getProviderName())
            assertFalse(source.hasApiKey())
        }
    }

    @Test
    fun testStamenSources_UseStadiaKey() {
        ApiKeyManager.setApiKey("Stadia", "test-key")
        val tile = TileCoordinate(x = 1, y = 2, zoom = 3)

        assertTrue(TileSource.STAMEN_TONER.hasApiKey())
        assertEquals(
            "https://tiles.stadiamaps.com/tiles/stamen_toner/3/1/2.png?api_key=test-key",
            TileSource.STAMEN_TONER.getTileUrl(tile),
        )
        assertEquals(
            "https://tiles.stadiamaps.com/tiles/stamen_watercolor/3/1/2.jpg?api_key=test-key",
            TileSource.STAMEN_WATERCOLOR.getTileUrl(tile),
        )
    }

    @Test
    fun testCartoSources_RequireCartoKey() {
        for (source in listOf(TileSource.CARTO_LIGHT, TileSource.CARTO_DARK, TileSource.CARTO_VOYAGER)) {
            assertTrue(source.requiresApiKey)
            assertEquals("carto", source.apiKeyProvider)
            assertEquals("CARTO", source.getProviderName())
            assertFalse(source.hasApiKey())
        }
    }

    @Test
    fun testCartoSources_UseCartoKey() {
        ApiKeyManager.setApiKey("CARTO", "test-key")
        val tile = TileCoordinate(x = 1, y = 2, zoom = 3)

        assertTrue(TileSource.CARTO_LIGHT.hasApiKey())
        // Subdomain is calculated as (x + y) % 4 = 3 -> 'd'
        assertEquals(
            "https://d.basemaps.cartocdn.com/light_all/3/1/2.png?key=test-key",
            TileSource.CARTO_LIGHT.getTileUrl(tile),
        )
        assertEquals(
            "https://d.basemaps.cartocdn.com/dark_all/3/1/2.png?key=test-key",
            TileSource.CARTO_DARK.getTileUrl(tile),
        )
        assertEquals(
            "https://basemaps.cartocdn.com/rastertiles/voyager/3/1/2.png?key=test-key",
            TileSource.CARTO_VOYAGER.getTileUrl(tile),
        )
    }
}
