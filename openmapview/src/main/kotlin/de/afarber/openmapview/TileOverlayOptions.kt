/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Builder class for creating TileOverlay instances with a fluent API.
 * Matches the Google Maps API pattern for tile overlay configuration.
 *
 * Example usage:
 * ```
 * val overlay = TileOverlayOptions()
 *     .tileProvider(OpenSeaMapProvider())
 *     .transparency(0.0f)
 *     .zIndex(1.0f)
 *     .visible(true)
 * ```
 */
class TileOverlayOptions {
    private var tileProvider: TileProvider? = null
    private var transparency: Float = 0f
    private var zIndex: Float = 0f
    private var visible: Boolean = true
    private var fadeIn: Boolean = false
    private var tag: Any? = null

    /**
     * Sets the tile provider for this overlay.
     * @param provider TileProvider implementation that provides tiles
     */
    fun tileProvider(provider: TileProvider): TileOverlayOptions {
        this.tileProvider = provider
        return this
    }

    /**
     * Sets the transparency of the overlay.
     * @param transparency Transparency value (0.0 = opaque, 1.0 = fully transparent)
     */
    fun transparency(transparency: Float): TileOverlayOptions {
        this.transparency = transparency
        return this
    }

    /**
     * Sets the z-index for draw order.
     * Negative values draw below base tiles, positive values draw above.
     * @param zIndex Z-index value (base tiles are at 0.0)
     */
    fun zIndex(zIndex: Float): TileOverlayOptions {
        this.zIndex = zIndex
        return this
    }

    /**
     * Sets whether the overlay is visible.
     * @param visible true to show the overlay, false to hide it
     */
    fun visible(visible: Boolean): TileOverlayOptions {
        this.visible = visible
        return this
    }

    /**
     * Sets whether tiles fade in when loaded.
     * @param fadeIn true to enable fade-in animation (currently not implemented)
     */
    fun fadeIn(fadeIn: Boolean): TileOverlayOptions {
        this.fadeIn = fadeIn
        return this
    }

    /**
     * Sets optional user data associated with the overlay.
     * @param tag Any object to attach to the overlay
     */
    fun tag(tag: Any?): TileOverlayOptions {
        this.tag = tag
        return this
    }

    /**
     * Builds and returns a TileOverlay instance.
     * @throws IllegalArgumentException if tileProvider is not set
     */
    internal fun build(): TileOverlay {
        val providerValue =
            tileProvider
                ?: throw IllegalArgumentException("TileProvider must be set")

        return TileOverlay(
            tileProvider = providerValue,
            transparency = transparency,
            zIndex = zIndex,
            visible = visible,
            fadeIn = fadeIn,
            tag = tag,
        )
    }
}
