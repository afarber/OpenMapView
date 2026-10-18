/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Represents the coordinates of a map tile in the tile grid.
 *
 * Uses the standard XYZ tile addressing scheme where tiles are identified by
 * their column (x), row (y), and zoom level.
 *
 * @property x The tile column (increases eastward); outside 0 until 2^zoom for a tile of a
 *   repeated copy of the world, see [wrapped]
 * @property y The tile row (increases southward)
 * @property zoom The zoom level (2-19)
 */
data class TileCoordinate(
    val x: Int,
    val y: Int,
    val zoom: Int,
) {
    /**
     * The tile of the main world that a tile of a repeated copy shows, the one to load and
     * cache. A tile of the main world returns itself.
     */
    internal fun wrapped(): TileCoordinate {
        val x = Math.floorMod(x, 1 shl zoom)
        return if (x == this.x) this else TileCoordinate(x, y, zoom)
    }
}
