/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.clustering

import de.afarber.openmapview.LatLng

/**
 * An item that a [ClusterManager] groups into clusters.
 *
 * Example:
 * ```kotlin
 * class Shop(private val position: LatLng, private val name: String) : ClusterItem {
 *     override fun getPosition() = position
 *     override fun getTitle() = name
 *     override fun getSnippet(): String? = null
 * }
 * ```
 */
public interface ClusterItem {
    /** The position of the item. */
    public fun getPosition(): LatLng

    /** The title of the item's marker, shown in its info window, or null. */
    public fun getTitle(): String?

    /** The snippet of the item's marker, shown in its info window, or null. */
    public fun getSnippet(): String?

    /** The drawing order of the item's marker, or null for the default. */
    public fun getZIndex(): Float? = null
}
