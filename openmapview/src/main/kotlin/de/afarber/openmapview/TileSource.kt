/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Defines available tile sources for map rendering.
 *
 * Each source provides a URL template for fetching map tiles, attribution text,
 * maximum zoom level, and API key requirements. Supports subdomain rotation
 * using the {s} placeholder for load balancing across multiple tile servers.
 *
 * ## URL Placeholders
 * - `{z}`: Zoom level
 * - `{x}`: Tile column (west to east)
 * - `{y}`: Tile row (north to south)
 * - `{s}`: Subdomain for load balancing (rotates through a, b, c)
 * - `{apikey}`: API key injected at runtime from ApiKeyManager
 *
 * @property urlTemplate The URL template with placeholders
 * @property attributionText Attribution text required by the tile provider
 * @property attributionUrl URL to the attribution/copyright page
 * @property maxZoom Maximum zoom level supported by this tile source
 * @property requiresApiKey Whether this tile source requires an API key
 * @property apiKeyProvider Provider name for API key lookup (e.g., "thunderforest")
 */
enum class TileSource(
    val urlTemplate: String,
    val attributionText: String,
    val attributionUrl: String,
    val maxZoom: Int,
    val requiresApiKey: Boolean = false,
    val apiKeyProvider: String? = null,
) {
    /**
     * Standard OpenStreetMap Mapnik rendering.
     *
     * The default OSM tile server with classic rendering.
     * Subject to OSM tile usage policy.
     */
    STANDARD(
        urlTemplate = "https://tile.openstreetmap.org/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors",
        attributionUrl = "https://www.openstreetmap.org/copyright",
        maxZoom = 19,
    ),

    /**
     * CyclOSM cycling-focused map from OpenStreetMap France.
     *
     * Emphasizes cycling infrastructure: bike lanes, paths, parking, shops.
     * Uses subdomain rotation (a, b, c) for load balancing.
     */
    CYCLOSM(
        urlTemplate = "https://{s}.tile-cyclosm.openstreetmap.fr/cyclosm/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of OpenStreetMap France",
        attributionUrl = "https://www.cyclosm.org/",
        maxZoom = 20,
    ),

    /**
     * Thunderforest Cycle Map.
     *
     * Professional cycling map with elevation contours and comprehensive infrastructure.
     * Requires Thunderforest API key.
     */
    CYCLEMAP(
        urlTemplate = "https://{s}.tile.thunderforest.com/cycle/{z}/{x}/{y}.png?apikey={apikey}",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of Andy Allan",
        attributionUrl = "https://www.thunderforest.com/",
        maxZoom = 21,
        requiresApiKey = true,
        apiKeyProvider = "thunderforest",
    ),

    /**
     * Thunderforest Transport Map.
     *
     * Public transport focused map with bus routes, train lines, tram tracks.
     * Requires Thunderforest API key.
     */
    TRANSPORT(
        urlTemplate = "https://{s}.tile.thunderforest.com/transport/{z}/{x}/{y}.png?apikey={apikey}",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of Andy Allan",
        attributionUrl = "https://www.thunderforest.com/",
        maxZoom = 21,
        requiresApiKey = true,
        apiKeyProvider = "thunderforest",
    ),

    /**
     * Thunderforest Transport Dark Map.
     *
     * Dark variant of the public transport map with bus routes, train lines, tram tracks.
     * Rendered in a dark color scheme suitable for night mode.
     * Requires Thunderforest API key.
     */
    TRANSPORT_DARK(
        urlTemplate = "https://{s}.tile.thunderforest.com/transport-dark/{z}/{x}/{y}.png?apikey={apikey}",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of Andy Allan",
        attributionUrl = "https://www.thunderforest.com/",
        maxZoom = 21,
        requiresApiKey = true,
        apiKeyProvider = "thunderforest",
    ),

    /**
     * Tracestrack Topographic Map.
     *
     * Detailed topographic map with elevation contours and hillshading.
     * Requires Tracestrack API key.
     */
    TRACESTRACK_TOPO(
        urlTemplate = "https://tile.tracestrack.com/topo__/{z}/{x}/{y}.png?key={apikey}",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of Tracestrack Maps",
        attributionUrl = "https://www.tracestrack.com/",
        maxZoom = 19,
        requiresApiKey = true,
        apiKeyProvider = "tracestrack",
    ),

    /**
     * Humanitarian OpenStreetMap style.
     *
     * Red/orange color scheme emphasizing humanitarian features:
     * hospitals, schools, water sources, disaster response.
     * Uses subdomain rotation (a, b, c).
     */
    HUMANITARIAN(
        urlTemplate = "https://tile-{s}.openstreetmap.fr/hot/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of Humanitarian OpenStreetMap Team",
        attributionUrl = "https://www.openstreetmap.org/copyright",
        maxZoom = 20,
    ),

    /**
     * OPNVKarte - German public transport map.
     *
     * Detailed transit map for German networks (bus, tram, subway, train).
     */
    OPNVKARTE(
        urlTemplate = "https://tileserver.memomaps.de/tilegen/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of MeMoMaps",
        attributionUrl = "https://www.openstreetmap.org/copyright",
        maxZoom = 18,
    ),

    /**
     * Shortbread vector tile style.
     *
     * Modern vector-based map style. Not yet supported (requires MapLibre GL).
     */
    SHORTBREAD(
        urlTemplate = "",
        attributionText = "© OpenStreetMap contributors",
        attributionUrl = "https://www.openstreetmap.org/copyright",
        maxZoom = 23,
    ),

    /**
     * MapTiler OpenMapTiles vector style.
     *
     * High-quality vector tiles. Not yet supported (requires MapLibre GL and API key).
     */
    MAPTILER_OMT(
        urlTemplate = "",
        attributionText = "© OpenStreetMap contributors, © MapTiler, © OpenMapTiles contributors",
        attributionUrl = "https://www.maptiler.com/copyright/",
        maxZoom = 23,
        requiresApiKey = true,
        apiKeyProvider = "maptiler",
    ),
    ;

    /**
     * Returns true if this tile source is supported (has non-empty URL template).
     *
     * Vector tile sources (SHORTBREAD, MAPTILER_OMT) return false as they
     * require MapLibre GL integration which is not yet implemented.
     */
    fun isSupported(): Boolean = urlTemplate.isNotEmpty()

    /**
     * Generates the tile URL for the specified tile coordinate.
     *
     * Replaces placeholders: {z}, {x}, {y}, {s} (subdomain), {apikey}
     *
     * Subdomain rotation uses (x + y) % 3 to distribute load across
     * subdomains a, b, c.
     *
     * API keys are retrieved from ApiKeyManager if required.
     *
     * @param tile The tile coordinate
     * @return The complete URL for downloading the tile, or empty string if unsupported
     */
    fun getTileUrl(tile: TileCoordinate): String {
        if (!isSupported()) return ""

        var url = urlTemplate

        // Replace tile coordinates
        url = url.replace("{z}", tile.zoom.toString())
        url = url.replace("{x}", tile.x.toString())
        url = url.replace("{y}", tile.y.toString())

        // Replace subdomain (round-robin: a, b, c)
        if (url.contains("{s}")) {
            val subdomain = arrayOf("a", "b", "c")[(tile.x + tile.y) % 3]
            url = url.replace("{s}", subdomain)
        }

        // Replace API key
        if (requiresApiKey && url.contains("{apikey}")) {
            val apiKey = apiKeyProvider?.let { ApiKeyManager.getApiKey(it) } ?: ""
            url = url.replace("{apikey}", apiKey)
        }

        return url
    }

    /**
     * Checks if an API key is configured for this tile source.
     *
     * @return true if no API key is required, or if a key is configured
     */
    fun hasApiKey(): Boolean {
        if (!requiresApiKey) return true
        return apiKeyProvider?.let { ApiKeyManager.hasApiKey(it) } ?: false
    }

    /**
     * Returns a human-readable provider name for this tile source.
     *
     * Used for error messages and overlay display.
     */
    fun getProviderName(): String =
        when (this) {
            CYCLEMAP, TRANSPORT, TRANSPORT_DARK -> "Thunderforest"
            TRACESTRACK_TOPO -> "Tracestrack"
            MAPTILER_OMT -> "MapTiler"
            else -> "OpenStreetMap"
        }

    companion object {
        /**
         * Gets the tile source for a given map type.
         *
         * @param mapType The MapType constant
         * @return The corresponding TileSource, or STANDARD for unknown types
         */
        fun fromMapType(mapType: Int): TileSource =
            when (mapType) {
                MapType.NONE -> STANDARD // NONE uses STANDARD but doesn't render
                MapType.STANDARD -> STANDARD
                MapType.CYCLOSM -> CYCLOSM
                MapType.CYCLEMAP -> CYCLEMAP
                MapType.TRANSPORT -> TRANSPORT
                MapType.TRANSPORT_DARK -> TRANSPORT_DARK
                MapType.TRACESTRACK_TOPO -> TRACESTRACK_TOPO
                MapType.HUMANITARIAN -> HUMANITARIAN
                MapType.OPNVKARTE -> OPNVKARTE
                MapType.SHORTBREAD -> SHORTBREAD
                MapType.MAPTILER_OMT -> MAPTILER_OMT
                else -> STANDARD // Fallback for unknown types
            }

        /**
         * Gets a human-readable map type name for error messages.
         *
         * @param mapType The MapType constant
         * @return The display name
         */
        fun getMapTypeName(mapType: Int): String =
            when (mapType) {
                MapType.NONE -> "None"
                MapType.STANDARD -> "Standard"
                MapType.CYCLOSM -> "CyclOSM"
                MapType.CYCLEMAP -> "Cycle Map"
                MapType.TRANSPORT -> "Transport Map"
                MapType.TRANSPORT_DARK -> "Transport Dark"
                MapType.TRACESTRACK_TOPO -> "Tracestrack Topo"
                MapType.HUMANITARIAN -> "Humanitarian"
                MapType.OPNVKARTE -> "OPNVKarte"
                MapType.SHORTBREAD -> "Shortbread"
                MapType.MAPTILER_OMT -> "MapTiler OMT"
                else -> "Unknown"
            }
    }
}
