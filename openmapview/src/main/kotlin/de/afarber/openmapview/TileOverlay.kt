/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Represents a tile overlay layer on the map.
 *
 * Tile overlays display additional tile-based content on top of or below the base map tiles.
 * Common use cases include:
 * - Weather overlays (temperature, precipitation, clouds)
 * - Transportation overlays (railways, public transit)
 * - Outdoor activity overlays (hiking trails, ski pistes)
 * - Maritime overlays (nautical charts, depth contours)
 * - Custom map styles or data visualizations
 *
 * Tile overlays use the same 256×256 pixel tiling system as the base map, with tiles
 * loaded dynamically based on the viewport and zoom level.
 *
 * Example usage:
 * ```kotlin
 * // Direct instantiation (Kotlin style)
 * val overlay = TileOverlay(
 *     tileProvider = OpenSeaMapProvider(),
 *     transparency = 0.0f,
 *     zIndex = 1.0f,
 *     visible = true
 * )
 * mapView.addTileOverlay(overlay)
 *
 * // Builder pattern (Google Maps style)
 * val overlay = TileOverlayOptions()
 *     .tileProvider(OpenSeaMapProvider())
 *     .transparency(0.0f)
 *     .zIndex(1.0f)
 *     .visible(true)
 * mapView.addTileOverlay(overlay)
 * ```
 *
 * @property tileProvider Provider for tile data
 * @property transparency Overlay transparency (0.0 = opaque, 1.0 = fully transparent)
 * @property zIndex Draw order (higher values drawn on top, base tiles are at 0.0)
 * @property visible Whether the overlay is visible
 * @property fadeIn Whether tiles fade in when loaded (currently not implemented)
 * @property tag User data associated with this overlay
 *
 * @see TileOverlayOptions
 * @see TileProvider
 * @see UrlTileProvider
 */
data class TileOverlay(
    val tileProvider: TileProvider,
    val transparency: Float = 0f,
    val zIndex: Float = 0f,
    val visible: Boolean = true,
    val fadeIn: Boolean = false,
    val tag: Any? = null,
) {
    internal val id: String = "tileOverlay_${System.nanoTime()}_${System.identityHashCode(this)}"

    init {
        require(transparency in 0f..1f) {
            "Transparency must be between 0.0 and 1.0: $transparency"
        }
    }

    /**
     * Returns a copy of this overlay with modified properties.
     *
     * @param transparency New transparency value (0.0 = opaque, 1.0 = fully transparent)
     * @return New TileOverlay instance with updated transparency
     */
    fun withTransparency(transparency: Float): TileOverlay = copy(transparency = transparency)

    /**
     * Returns a copy of this overlay with modified visibility.
     *
     * @param visible New visibility state
     * @return New TileOverlay instance with updated visibility
     */
    fun withVisibility(visible: Boolean): TileOverlay = copy(visible = visible)

    /**
     * Returns a copy of this overlay with modified z-index.
     *
     * @param zIndex New z-index value (higher values drawn on top)
     * @return New TileOverlay instance with updated z-index
     */
    fun withZIndex(zIndex: Float): TileOverlay = copy(zIndex = zIndex)

    override fun toString(): String = "TileOverlay(id=$id, transparency=$transparency, zIndex=$zIndex, visible=$visible)"
}
