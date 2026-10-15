/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.clustering

import de.afarber.openmapview.LatLng

/**
 * A group of nearby [ClusterItem]s, drawn as one marker with their count.
 *
 * @param T The type of the items
 */
interface Cluster<T : ClusterItem> {
    /** Where the cluster marker is drawn: the position of its first item. */
    val position: LatLng

    /** The items in the cluster. */
    val items: Collection<T>

    /** The number of items in the cluster. */
    val size: Int
}

internal class StaticCluster<T : ClusterItem>(
    override val position: LatLng,
) : Cluster<T> {
    override val items: MutableCollection<T> = LinkedHashSet()

    override val size: Int
        get() = items.size
}
