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
     * OpenTopoMap - Free topographic map.
     *
     * Worldwide topographic map with elevation contours and hillshading.
     * Free alternative to TRACESTRACK_TOPO without API key requirement.
     * Uses subdomain rotation (a, b, c) for load balancing.
     */
    OPENTOPOMAP(
        urlTemplate = "https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles courtesy of OpenTopoMap",
        attributionUrl = "https://opentopomap.org/",
        maxZoom = 17,
    ),

    /**
     * Carto Light - Minimalist light basemap.
     *
     * Clean light theme designed as neutral background for data visualization.
     * Free for non-commercial use. Uses subdomain rotation (a, b, c, d).
     */
    CARTO_LIGHT(
        urlTemplate = "https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles © CARTO",
        attributionUrl = "https://carto.com/basemaps/",
        maxZoom = 20,
    ),

    /**
     * Carto Dark - Dark theme basemap.
     *
     * Dark theme suitable for night mode and low-light conditions.
     * Free alternative to TRANSPORT_DARK for general-purpose use.
     * Uses subdomain rotation (a, b, c, d).
     */
    CARTO_DARK(
        urlTemplate = "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles © CARTO",
        attributionUrl = "https://carto.com/basemaps/",
        maxZoom = 20,
    ),

    /**
     * Carto Voyager - Modern colorful basemap.
     *
     * Contemporary design with vibrant colors and clear labels.
     * Modern alternative to STANDARD OSM rendering.
     */
    CARTO_VOYAGER(
        urlTemplate = "https://basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles © CARTO",
        attributionUrl = "https://carto.com/basemaps/",
        maxZoom = 18,
    ),

    /**
     * Stamen Toner - High-contrast black and white.
     *
     * Stark B&W design with maximum contrast for data visualization.
     * Most popular Stamen style. Hosted by Stadia Maps.
     */
    STAMEN_TONER(
        urlTemplate = "https://tiles.stadiamaps.com/tiles/stamen_toner/{z}/{x}/{y}.png",
        attributionText = "© OpenStreetMap contributors. Tiles © Stamen Design, © Stadia Maps",
        attributionUrl = "https://stadiamaps.com/stamen/",
        maxZoom = 20,
    ),

    /**
     * Stamen Watercolor - Artistic watercolor rendering.
     *
     * Unique hand-painted watercolor aesthetic. Hosted by Stadia Maps.
     */
    STAMEN_WATERCOLOR(
        urlTemplate = "https://tiles.stadiamaps.com/tiles/stamen_watercolor/{z}/{x}/{y}.jpg",
        attributionText = "© OpenStreetMap contributors. Tiles © Stamen Design, © Stadia Maps",
        attributionUrl = "https://stadiamaps.com/stamen/",
        maxZoom = 16,
    ),
    ;

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
     * @return The complete URL for downloading the tile
     */
    fun getTileUrl(tile: TileCoordinate): String {
        var url = urlTemplate

        // Replace tile coordinates
        url = url.replace("{z}", tile.zoom.toString())
        url = url.replace("{x}", tile.x.toString())
        url = url.replace("{y}", tile.y.toString())

        // Replace subdomain (round-robin: a, b, c, d for CARTO; a, b, c for others)
        if (url.contains("{s}")) {
            val subdomains =
                when (this) {
                    CARTO_LIGHT, CARTO_DARK -> arrayOf("a", "b", "c", "d")
                    else -> arrayOf("a", "b", "c")
                }
            val subdomain = subdomains[(tile.x + tile.y) % subdomains.size]
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
            CARTO_LIGHT, CARTO_DARK, CARTO_VOYAGER -> "CARTO"
            STAMEN_TONER, STAMEN_WATERCOLOR -> "Stadia Maps"
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
                MapType.OPENTOPOMAP -> OPENTOPOMAP
                MapType.CARTO_LIGHT -> CARTO_LIGHT
                MapType.CARTO_DARK -> CARTO_DARK
                MapType.CARTO_VOYAGER -> CARTO_VOYAGER
                MapType.STAMEN_TONER -> STAMEN_TONER
                MapType.STAMEN_WATERCOLOR -> STAMEN_WATERCOLOR
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
                MapType.OPENTOPOMAP -> "OpenTopoMap"
                MapType.CARTO_LIGHT -> "Carto Light"
                MapType.CARTO_DARK -> "Carto Dark"
                MapType.CARTO_VOYAGER -> "Carto Voyager"
                MapType.STAMEN_TONER -> "Stamen Toner"
                MapType.STAMEN_WATERCOLOR -> "Stamen Watercolor"
                else -> "Unknown"
            }
    }
}
