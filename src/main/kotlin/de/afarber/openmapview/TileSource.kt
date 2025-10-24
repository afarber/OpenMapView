/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

enum class TileSource(private val urlTemplate: String) {
    STANDARD("https://tile.openstreetmap.org/{z}/{x}/{y}.png");

    fun getTileUrl(tile: TileCoordinate): String {
        return urlTemplate
            .replace("{z}", tile.zoom.toString())
            .replace("{x}", tile.x.toString())
            .replace("{y}", tile.y.toString())
    }
}
