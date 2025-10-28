/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Represents a single map tile containing image data.
 *
 * Tiles are typically 256×256 pixel PNG images, but can be any size or format
 * supported by Android's BitmapFactory (PNG, JPEG, WebP, etc.).
 *
 * The [data] field contains the raw image bytes (not decoded bitmap). The image
 * will be decoded on demand when the tile is rendered.
 *
 * Example usage:
 * ```kotlin
 * // Create a tile from PNG bytes
 * val tile = Tile(256, 256, pngByteArray)
 *
 * // Indicate no tile is available
 * val noTile: Tile? = null
 * ```
 *
 * @property width Tile width in pixels (typically 256)
 * @property height Tile height in pixels (typically 256)
 * @property data Raw image data (PNG, JPEG, WebP, etc.)
 *
 * @see TileProvider
 * @see TileOverlay
 */
data class Tile(
    val width: Int,
    val height: Int,
    val data: ByteArray,
) {
    init {
        require(width > 0) { "Tile width must be positive: $width" }
        require(height > 0) { "Tile height must be positive: $height" }
        require(data.isNotEmpty()) { "Tile data must not be empty" }
    }

    /**
     * Two tiles are equal if they have the same dimensions and data.
     * Uses contentEquals for byte array comparison.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Tile

        if (width != other.width) return false
        if (height != other.height) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    /**
     * Hash code based on width, height, and data contents.
     */
    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + data.contentHashCode()
        return result
    }

    override fun toString(): String = "Tile(width=$width, height=$height, dataSize=${data.size} bytes)"
}
