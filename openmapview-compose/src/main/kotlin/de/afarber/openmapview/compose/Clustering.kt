/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.runtime.rememberUpdatedState
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.clustering.Cluster
import de.afarber.openmapview.clustering.ClusterItem
import de.afarber.openmapview.clustering.ClusterManager
import de.afarber.openmapview.Marker as MapMarker

/**
 * Groups [items] into clusters that show their count.
 * Declare it in [OpenMap]'s content; the clusters are computed again when the camera stops
 * at another integer zoom level, and when [items] change.
 *
 * Example:
 * ```kotlin
 * OpenMap(cameraPositionState = cameraPositionState) {
 *     Clustering(
 *         items = shops,
 *         onClusterItemClick = { shop -> selected = shop; false },
 *         clusterContent = { cluster ->
 *             Box(Modifier.size(40.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
 *                 Text("${cluster.size}", color = Color.White)
 *             }
 *         },
 *     )
 * }
 * ```
 *
 * @param items The items to show, see [ClusterItem]
 * @param onClusterClick Called when a cluster is tapped; return true to keep the camera,
 *   false to zoom in to the cluster's items
 * @param onClusterItemClick Called when the marker of a single item is tapped
 * @param clusterContent Draws the cluster markers, or null for circles with the count.
 *   Like [MarkerComposable], the content is drawn into an image.
 * @param clusterItemContent Draws the item markers, or null for the default marker
 */
@Composable
fun <T : ClusterItem> Clustering(
    items: Collection<T>,
    onClusterClick: (Cluster<T>) -> Boolean = { false },
    onClusterItemClick: (T) -> Boolean = { false },
    clusterContent: @Composable ((Cluster<T>) -> Unit)? = null,
    clusterItemContent: @Composable ((T) -> Unit)? = null,
) {
    val mapContext = LocalMapContext.current ?: return
    // Lets the custom content use the composition locals of the screen, e.g. MaterialTheme
    val compositionContext = rememberCompositionContext()
    val currentOnClusterClick by rememberUpdatedState(onClusterClick)
    val currentOnClusterItemClick by rememberUpdatedState(onClusterItemClick)
    val manager = remember(mapContext) { ClusterManager<T>(mapContext.map.context, mapContext.map) }

    // Sets icon providers for the current content, before each clustering
    fun setIconProviders() {
        manager.clusterIconProvider =
            clusterContent?.let { content ->
                ClusterManager.ClusterIconProvider { cluster ->
                    BitmapDescriptorFactory.fromBitmap(drawComposable(mapContext.map, compositionContext) { content(cluster) })
                }
            }
        manager.clusterItemIconProvider =
            clusterItemContent?.let { content ->
                ClusterManager.ClusterItemIconProvider { item ->
                    BitmapDescriptorFactory.fromBitmap(drawComposable(mapContext.map, compositionContext) { content(item) })
                }
            }
    }

    // For later clusterings, e.g. when the zoom changes
    SideEffect { setIconProviders() }

    DisposableEffect(manager) {
        manager.setOnClusterClickListener { currentOnClusterClick(it) }
        manager.setOnClusterItemClickListener { currentOnClusterItemClick(it) }
        // OpenMap is the map's camera idle and marker click listener, and forwards to these
        val onCameraIdle: () -> Unit = { manager.onCameraIdle() }
        val onMarkerClick: (MapMarker) -> Boolean = { manager.onMarkerClick(it) }
        mapContext.cameraIdleListeners.add(onCameraIdle)
        mapContext.markerClickHandlers.add(onMarkerClick)
        onDispose {
            mapContext.cameraIdleListeners.remove(onCameraIdle)
            mapContext.markerClickHandlers.remove(onMarkerClick)
            // Removes the markers
            manager.clearItems()
            manager.cluster()
        }
    }

    // Effects like this one run before SideEffect, so the providers are set here too
    DisposableEffect(manager, items) {
        setIconProviders()
        manager.clearItems()
        manager.addItems(items)
        manager.cluster()
        onDispose {}
    }
}
