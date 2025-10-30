/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Map type constants for OpenMapView.
 *
 * Defines 9 map tile styles matching the openstreetmap.org layer switcher.
 * Six map types work without API keys, three require API keys from third-party providers.
 *
 * **Free Map Types (No API Key Required):**
 * - [NONE]: No base map tiles
 * - [STANDARD]: Default OpenStreetMap Mapnik rendering
 * - [CYCLOSM]: Cycling-focused map from OpenStreetMap France
 * - [HUMANITARIAN]: Humanitarian OSM style for emergency response
 * - [OPNVKARTE]: German public transport map
 *
 * **Premium Map Types (API Key Required):**
 * - [CYCLEMAP]: Thunderforest cycling map (requires Thunderforest API key)
 * - [TRANSPORT]: Public transport focused map (requires Thunderforest API key)
 * - [TRANSPORT_DARK]: Dark mode public transport map (requires Thunderforest API key)
 * - [TRACESTRACK_TOPO]: Topographic with contours (requires Tracestrack API key)
 *
 * ## API Key Configuration
 *
 * Configure API keys in AndroidManifest.xml:
 * ```xml
 * <meta-data
 *     android:name="de.afarber.openmapview.THUNDERFOREST_API_KEY"
 *     android:value="your_key_here"/>
 * <meta-data
 *     android:name="de.afarber.openmapview.TRACESTRACK_API_KEY"
 *     android:value="your_key_here"/>
 * ```
 *
 * Or programmatically:
 * ```kotlin
 * ApiKeyManager.setApiKey("thunderforest", "your_key_here")
 * ```
 *
 * When an API key is required but not configured, OpenMapView displays the STANDARD map
 * with an overlay indicating the missing key. The map remains interactive.
 *
 * ## Obtaining API Keys
 * - **Thunderforest** (CYCLEMAP, TRANSPORT, TRANSPORT_DARK): https://www.thunderforest.com/pricing/
 *   - Free tier: 150,000 tiles/month
 * - **Tracestrack** (TRACESTRACK_TOPO): https://www.tracestrack.com/en/signup
 *   - Free tier: 100,000 tiles/month
 *
 * @see OpenMapView.setMapType
 * @see OpenMapView.getMapType
 * @see ApiKeyManager
 * @see TileSource
 */
object MapType {
    /**
     * No base map tiles displayed.
     *
     * The map view shows no background tiles. Overlays (markers, polylines, etc.) are still visible.
     * Useful for custom tile sources or offline-only applications.
     */
    const val NONE: Int = 0

    /**
     * Standard OpenStreetMap Mapnik rendering (default).
     *
     * The classic OpenStreetMap style showing roads, buildings, land use, water features,
     * and comprehensive labels. This is the default map type.
     *
     * - **Tile Server**: tile.openstreetmap.org
     * - **Max Zoom**: 19
     * - **API Key**: Not required
     * - **Use Cases**: General purpose mapping, navigation, POI display
     */
    const val STANDARD: Int = 1

    /**
     * CyclOSM - Cycling-focused map from OpenStreetMap France.
     *
     * Emphasizes cycling infrastructure including bike lanes, paths, parking, shops,
     * and difficulty ratings. Uses clear colors to distinguish cycleway types.
     *
     * - **Tile Server**: tile-cyclosm.openstreetmap.fr
     * - **Max Zoom**: 20
     * - **API Key**: Not required
     * - **Use Cases**: Cycling apps, route planning, bikeshare applications
     */
    const val CYCLOSM: Int = 2

    /**
     * Cycle Map - Thunderforest cycling map.
     *
     * Professional cycling map with elevation contours, cycle routes, and comprehensive
     * cycling infrastructure. Higher quality than CyclOSM but requires API key.
     *
     * - **Tile Server**: tile.thunderforest.com
     * - **Max Zoom**: 21
     * - **API Key**: Required (Thunderforest)
     * - **Use Cases**: Premium cycling apps, detailed route planning
     *
     * @see ApiKeyManager
     */
    const val CYCLEMAP: Int = 3

    /**
     * Transport Map - Public transport focused map.
     *
     * Emphasizes public transportation including bus routes, train lines, tram tracks,
     * and transit stations. Ideal for transit planning applications.
     *
     * - **Tile Server**: tile.thunderforest.com
     * - **Max Zoom**: 21
     * - **API Key**: Required (Thunderforest)
     * - **Use Cases**: Public transit apps, journey planning, transportation analysis
     *
     * @see ApiKeyManager
     */
    const val TRANSPORT: Int = 4

    /**
     * Transport Dark Map - Dark mode public transport map.
     *
     * Dark variant of the public transport map with bus routes, train lines, tram tracks,
     * and transit stations rendered in a dark color scheme. Suitable for night mode or
     * reducing eye strain in low-light conditions.
     *
     * - **Tile Server**: tile.thunderforest.com
     * - **Max Zoom**: 21
     * - **API Key**: Required (Thunderforest)
     * - **Use Cases**: Night mode transit apps, low-light journey planning
     *
     * @see ApiKeyManager
     */
    const val TRANSPORT_DARK: Int = 5

    /**
     * Tracestrack Topo - Topographic map with elevation contours.
     *
     * Detailed topographic map showing elevation contours, hillshading, and terrain features.
     * Excellent for hiking, outdoor activities, and terrain analysis.
     *
     * - **Tile Server**: tile.tracestrack.com
     * - **Max Zoom**: 19
     * - **API Key**: Required (Tracestrack)
     * - **Use Cases**: Hiking apps, outdoor navigation, elevation analysis
     *
     * @see ApiKeyManager
     */
    const val TRACESTRACK_TOPO: Int = 6

    /**
     * Humanitarian - HOT style for emergency and disaster response.
     *
     * Humanitarian OpenStreetMap Team style with red/orange color scheme.
     * Emphasizes hospitals, schools, water sources, emergency services, and
     * infrastructure critical for disaster response.
     *
     * - **Tile Server**: tile.openstreetmap.fr/hot
     * - **Max Zoom**: 20
     * - **API Key**: Not required
     * - **Use Cases**: Emergency response, disaster management, humanitarian aid
     */
    const val HUMANITARIAN: Int = 7

    /**
     * OPNVKarte - German public transport map.
     *
     * Detailed public transport map focusing on German transit networks.
     * Shows bus, tram, subway, and train routes with high detail.
     *
     * - **Tile Server**: tileserver.memomaps.de (MeMoMaps)
     * - **Max Zoom**: 18
     * - **API Key**: Not required
     * - **Use Cases**: German transit apps, ÖPNV journey planning
     */
    const val OPNVKARTE: Int = 8
}
