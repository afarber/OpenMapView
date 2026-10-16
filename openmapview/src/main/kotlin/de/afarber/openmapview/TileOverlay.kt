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
 * // Builder pattern
 * val overlay = TileOverlayOptions()
 *     .tileProvider(OpenSeaMapProvider())
 *     .transparency(0.0f)
 *     .zIndex(1.0f)
 *     .visible(true)
 * mapView.addTileOverlay(overlay)
 * ```
 *
 * The overlay is a live handle, as returned by [OpenMapView.addTileOverlay]: setting a
 * property, e.g. `overlay.transparency = 0.5f`, redraws the map right away and keeps the
 * overlay's downloaded tiles, and [remove] takes it off the map. Java code uses the getters
 * and setters, e.g. `overlay.setTransparency(0.5f)`.
 *
 * @param tileProvider Provider for tile data
 * @param transparency Overlay transparency (0.0 = opaque, 1.0 = fully transparent)
 * @param zIndex Draw order (higher values drawn on top, base tiles are at 0.0)
 * @param visible Whether the overlay is visible
 * @param fadeIn Whether tiles fade in when loaded (currently not implemented)
 * @param tag User data associated with this overlay
 *
 * @see TileOverlayOptions
 * @see TileProvider
 * @see UrlTileProvider
 */
class TileOverlay(
    /** Provider for tile data */
    val tileProvider: TileProvider,
    transparency: Float = 0f,
    zIndex: Float = 0f,
    visible: Boolean = true,
    fadeIn: Boolean = false,
    tag: Any? = null,
) {
    // The map that shows the overlay, redrawn when a property changes
    internal var mapView: OpenMapView? = null

    private fun <T> property(
        initial: T,
        check: (T) -> Unit = {},
    ) = ShapeProperty(initial, check) { mapView?.invalidate() }

    /** Overlay transparency (0.0 = opaque, 1.0 = fully transparent) */
    var transparency: Float by property(transparency) {
        require(it in 0f..1f) { "Transparency must be between 0.0 and 1.0: $it" }
    }

    /** Draw order (higher values drawn on top, base tiles are at 0.0) */
    var zIndex: Float by property(zIndex)

    /** Whether the overlay is visible */
    var visible: Boolean by property(visible)

    /** Whether tiles fade in when loaded (currently not implemented) */
    var fadeIn: Boolean by property(fadeIn)

    /** User data associated with this overlay */
    var tag: Any? by property(tag)

    internal val id: String = "tileOverlay_${System.nanoTime()}_${System.identityHashCode(this)}"

    /** Removes the overlay from the map that shows it. */
    fun remove() {
        mapView?.removeTileOverlay(this)
    }

    /**
     * Drops the downloaded tiles of this overlay, so they are requested again, e.g. after
     * the data behind the [tileProvider] changed.
     */
    fun clearTileCache() {
        mapView?.clearTileOverlayCache(this)
    }

    /** Returns a new overlay with the given properties changed, not added to any map. */
    fun copy(
        tileProvider: TileProvider = this.tileProvider,
        transparency: Float = this.transparency,
        zIndex: Float = this.zIndex,
        visible: Boolean = this.visible,
        fadeIn: Boolean = this.fadeIn,
        tag: Any? = this.tag,
    ): TileOverlay = TileOverlay(tileProvider, transparency, zIndex, visible, fadeIn, tag)

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TileOverlay) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    override fun toString(): String = "TileOverlay(id=$id, transparency=$transparency, zIndex=$zIndex, visible=$visible)"
}
