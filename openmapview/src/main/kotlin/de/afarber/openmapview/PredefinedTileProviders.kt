/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Factory object providing predefined [TileProvider] implementations for popular
 * public tile overlay services.
 *
 * These providers offer various overlay types including weather, transportation,
 * outdoor activities and maritime data.
 *
 * Example usage:
 * ```kotlin
 * // Add OpenSeaMap nautical overlay
 * val seaMapOverlay = TileOverlayOptions()
 *     .tileProvider(PredefinedTileProviders.openSeaMap())
 *     .zIndex(1.0f)
 * mapView.addTileOverlay(seaMapOverlay)
 *
 * // Add railway overlay
 * val railwayOverlay = TileOverlayOptions()
 *     .tileProvider(PredefinedTileProviders.openRailwayMap())
 *     .zIndex(1.0f)
 * mapView.addTileOverlay(railwayOverlay)
 * ```
 *
 * All providers use 256×256 pixel tiles with Web Mercator projection (EPSG:3857).
 * See the "Tile Overlays" section of docs/MAP_TYPES.md for the available services.
 */
object PredefinedTileProviders {
    /**
     * Creates a provider for OpenSeaMap nautical charts overlay.
     *
     * Content: Navigation buoys, lighthouses, harbors, depth contours, marine hazards
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Nautical data © OpenSeaMap"
     * License: CC-BY-SA
     *
     * @return TileProvider for OpenSeaMap
     */
    @JvmStatic
    fun openSeaMap(): TileProvider = object : UrlTileProvider("https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for OpenRailwayMap overlay.
     *
     * Content: Railway lines, stations, signals, electrification, speed limits
     * Zoom levels: 2-19
     * Attribution: "Map data © OpenStreetMap contributors, Railway data © OpenRailwayMap"
     * License: CC-BY-SA
     *
     * @return TileProvider for OpenRailwayMap
     */
    @JvmStatic
    fun openRailwayMap(): TileProvider = object : UrlTileProvider("https://a.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for OpenSnowMap winter sports overlay.
     *
     * Content: Ski pistes, ski lifts, cable cars, winter sports facilities with difficulty ratings
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Piste data © OpenSnowMap"
     *
     * @return TileProvider for OpenSnowMap
     */
    @JvmStatic
    fun openSnowMap(): TileProvider = object : UrlTileProvider("https://tiles.opensnowmap.org/pistes/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for Waymarked Trails hiking overlay.
     *
     * Content: Marked hiking trails with difficulty ratings and route information
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Trail data © Waymarked Trails"
     * License: CC-BY-SA
     *
     * @return TileProvider for hiking trails
     */
    @JvmStatic
    fun waymarkedTrailsHiking(): TileProvider = object : UrlTileProvider("https://tile.waymarkedtrails.org/hiking/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for Waymarked Trails cycling overlay.
     *
     * Content: Marked cycling routes with difficulty ratings and route information
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Trail data © Waymarked Trails"
     * License: CC-BY-SA
     *
     * @return TileProvider for cycling trails
     */
    @JvmStatic
    fun waymarkedTrailsCycling(): TileProvider = object : UrlTileProvider("https://tile.waymarkedtrails.org/cycling/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for Waymarked Trails mountain biking overlay.
     *
     * Content: Mountain biking trails with difficulty ratings and route information
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Trail data © Waymarked Trails"
     * License: CC-BY-SA
     *
     * @return TileProvider for MTB trails
     */
    @JvmStatic
    fun waymarkedTrailsMtb(): TileProvider = object : UrlTileProvider("https://tile.waymarkedtrails.org/mtb/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for Waymarked Trails skating overlay.
     *
     * Content: Skating routes and paths
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Trail data © Waymarked Trails"
     * License: CC-BY-SA
     *
     * @return TileProvider for skating trails
     */
    @JvmStatic
    fun waymarkedTrailsSkating(): TileProvider = object : UrlTileProvider("https://tile.waymarkedtrails.org/skating/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for the Stamen Toner lines overlay.
     *
     * Content: Roads, railways and borders on transparent background, without labels.
     * Stadia Maps does not serve the old combined hybrid style, so this is the lines layer.
     * Zoom levels: 0-20
     * Attribution: "© OpenStreetMap contributors. Tiles © Stamen Design, © Stadia Maps"
     * Requires a Stadia Maps API key, see [ApiKeyManager].
     *
     * @return TileProvider for Stamen Toner lines
     */
    @JvmStatic
    fun stamenTonerHybrid(): TileProvider = stadia("stamen_toner_lines")

    /**
     * Creates a provider for the Stamen Terrain lines overlay.
     *
     * Content: Roads, railways and borders in the Stamen Terrain style on transparent background
     * Zoom levels: 0-18
     * Attribution: "© OpenStreetMap contributors. Tiles © Stamen Design, © Stadia Maps"
     * Requires a Stadia Maps API key, see [ApiKeyManager].
     *
     * @return TileProvider for Stamen Terrain lines
     */
    @JvmStatic
    fun stamenTerrainLines(): TileProvider = stadia("stamen_terrain_lines")

    /**
     * Creates a custom URL-based tile provider with the specified URL template.
     *
     * URL template should contain placeholders {z}, {x}, {y} for zoom, column, and row.
     *
     * Example:
     * ```kotlin
     * val customProvider = PredefinedTileProviders.custom(
     *     "https://example.com/tiles/{z}/{x}/{y}.png"
     * )
     * ```
     *
     * @param urlTemplate URL pattern with {z}, {x}, {y} placeholders
     * @return TileProvider for the custom URL
     */
    @JvmStatic
    fun custom(urlTemplate: String): TileProvider = object : UrlTileProvider(urlTemplate) {}

    // Stadia Maps tiles with the "stadia" key from ApiKeyManager, looked up per tile
    // so that a key set after creating the provider is still used
    private fun stadia(style: String): TileProvider =
        object : UrlTileProvider("https://tiles.stadiamaps.com/tiles/$style/{z}/{x}/{y}.png?api_key={apikey}") {
            override fun getTileUrl(
                x: Int,
                y: Int,
                zoom: Int,
            ): String = super.getTileUrl(x, y, zoom).replace("{apikey}", ApiKeyManager.getApiKey("stadia").orEmpty())
        }
}
