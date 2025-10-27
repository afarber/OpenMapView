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
 * @property x The tile column (increases eastward)
 * @property y The tile row (increases southward)
 * @property zoom The zoom level (2-19)
 */
data class TileCoordinate(
    val x: Int,
    val y: Int,
    val zoom: Int,
)
