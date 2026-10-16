/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.clustering

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

/**
 * Groups items with a non-hierarchical distance based algorithm: each item not yet visited starts a cluster that
 * takes all items within a square of [maxDistance] pixels around it at the zoom level.
 * An item in reach of several clusters joins the closest one.
 */
internal object DistanceBasedAlgorithm {
    fun <T : ClusterItem> cluster(
        items: Collection<T>,
        zoom: Int,
        maxDistance: Double,
    ): List<Cluster<T>> {
        val worldSize = 256.0 * 2.0.pow(zoom)
        val points = items.associateWith { worldPoint(it, worldSize) }

        // Items in grid cells as large as the search square, so a search looks at 9 cells
        val grid = HashMap<Pair<Long, Long>, MutableList<T>>()

        fun cell(point: Pair<Double, Double>) = Pair(floor(point.first / maxDistance).toLong(), floor(point.second / maxDistance).toLong())
        for ((item, point) in points) grid.getOrPut(cell(point)) { ArrayList() }.add(item)

        val visited = HashSet<T>()
        val clusterOf = HashMap<T, StaticCluster<T>>()
        val distanceOf = HashMap<T, Double>()
        val clusters = ArrayList<StaticCluster<T>>()
        val half = maxDistance / 2

        for (candidate in items) {
            if (candidate in visited) continue
            val center = points.getValue(candidate)
            val cluster = StaticCluster<T>(candidate.getPosition())
            clusters.add(cluster)

            val (cellX, cellY) = cell(center)
            for (dx in -1L..1L) {
                for (dy in -1L..1L) {
                    for (item in grid[Pair(cellX + dx, cellY + dy)].orEmpty()) {
                        val point = points.getValue(item)
                        val offsetX = point.first - center.first
                        val offsetY = point.second - center.second
                        if (offsetX < -half || offsetX > half || offsetY < -half || offsetY > half) continue

                        val distance = offsetX * offsetX + offsetY * offsetY
                        val previous = distanceOf[item]
                        // Already in a closer cluster
                        if (previous != null && previous <= distance) continue

                        clusterOf[item]?.items?.remove(item)
                        cluster.items.add(item)
                        clusterOf[item] = cluster
                        distanceOf[item] = distance
                        visited.add(item)
                    }
                }
            }
        }
        // Clusters whose items all moved to closer ones
        return clusters.filter { it.size > 0 }
    }

    // Web Mercator world pixel coordinates, with doubles for precision at high zoom
    private fun worldPoint(
        item: ClusterItem,
        worldSize: Double,
    ): Pair<Double, Double> {
        val position = item.getPosition()
        val x = (position.longitude + 180) / 360 * worldSize
        val latRad = position.latitude.coerceIn(-85.05112878, 85.05112878) * PI / 180
        val y = (1 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2 * worldSize
        return Pair(x, y)
    }
}
