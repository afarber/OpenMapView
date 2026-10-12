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
 * outdoor activities, maritime, and emergency services data.
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
 * See docs/TILE_OVERLAYS.md for detailed information about each service including
 * attribution requirements, usage policies, and content descriptions.
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
    fun openRailwayMap(): TileProvider = object : UrlTileProvider("https://a.tiles.openrailwaymap.org/standard/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for OpenFireMap emergency services overlay.
     *
     * Content: Fire stations, fire hydrants, ambulance stations, hospitals
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Emergency data © OpenFireMap"
     *
     * @return TileProvider for OpenFireMap
     */
    fun openFireMap(): TileProvider = object : UrlTileProvider("http://openfiremap.org/hytiles/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for OpenSnowMap winter sports overlay.
     *
     * Content: Ski pistes, ski lifts, cable cars, winter sports facilities with difficulty ratings
     * Zoom levels: 0-18
     * Attribution: "Map data © OpenStreetMap contributors, Piste data © OpenSnowMap"
     *
     * @return TileProvider for OpenSnowMap
     */
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
    fun waymarkedTrailsSkating(): TileProvider = object : UrlTileProvider("https://tile.waymarkedtrails.org/skating/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for Stamen Toner Hybrid overlay (labels only).
     *
     * Content: Map labels and roads on transparent background
     * Zoom levels: 0-20
     * Attribution: "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL"
     *
     * @return TileProvider for Stamen Toner Hybrid
     */
    fun stamenTonerHybrid(): TileProvider =
        object : UrlTileProvider("https://stamen-tiles.a.ssl.fastly.net/toner-hybrid/{z}/{x}/{y}.png") {}

    /**
     * Creates a provider for Stamen Terrain Lines overlay (elevation contours only).
     *
     * Content: Elevation contours on transparent background
     * Zoom levels: 0-18
     * Attribution: "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL"
     *
     * @return TileProvider for Stamen Terrain Lines
     */
    fun stamenTerrainLines(): TileProvider =
        object : UrlTileProvider("https://stamen-tiles.a.ssl.fastly.net/terrain-lines/{z}/{x}/{y}.png") {}

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
    fun custom(urlTemplate: String): TileProvider = object : UrlTileProvider(urlTemplate) {}
}
