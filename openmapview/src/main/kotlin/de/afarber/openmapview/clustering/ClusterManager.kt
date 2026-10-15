/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.clustering

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.graphics.createBitmap
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.CameraUpdateFactory
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.Marker
import de.afarber.openmapview.OnCameraIdleListener
import de.afarber.openmapview.OnMarkerClickListener
import de.afarber.openmapview.OpenMapView
import kotlin.math.floor

/**
 * Groups nearby markers into clusters that show their count, as in the Google Maps Utility
 * Library. Clusters are computed again when the map stops at a new integer zoom level.
 *
 * Like there, the manager is the map's camera idle and marker click listener:
 * ```kotlin
 * val clusterManager = ClusterManager<Shop>(context, mapView)
 * mapView.setOnCameraIdleListener(clusterManager)
 * mapView.setOnMarkerClickListener(clusterManager)
 * clusterManager.addItems(shops)
 * clusterManager.cluster()
 * ```
 *
 * Tapping a cluster zooms in to show its items, unless an [OnClusterClickListener] returns true.
 *
 * @param T The type of the items
 * @param context Context for the screen density
 * @param map The map to draw the markers on
 */
class ClusterManager<T : ClusterItem>(
    context: Context,
    private val map: OpenMapView,
) : OnCameraIdleListener,
    OnMarkerClickListener {
    /** Called when a cluster marker is tapped. */
    fun interface OnClusterClickListener<T : ClusterItem> {
        /**
         * @param cluster The tapped cluster
         * @return true to consume the tap, false to also zoom in to the cluster's items
         */
        fun onClusterClick(cluster: Cluster<T>): Boolean
    }

    /** Called when the marker of a single item is tapped. */
    fun interface OnClusterItemClickListener<T : ClusterItem> {
        /**
         * @param item The tapped item
         * @return true if the tap was handled
         */
        fun onClusterItemClick(item: T): Boolean
    }

    /** Provides the icon of a cluster marker, e.g. to draw clusters in your own style. */
    fun interface ClusterIconProvider<T : ClusterItem> {
        /** @param cluster The cluster to draw */
        fun getIcon(cluster: Cluster<T>): BitmapDescriptor
    }

    /** Provides the icon of an item's marker, or null for the default marker. */
    fun interface ClusterItemIconProvider<T : ClusterItem> {
        /** @param item The item to draw */
        fun getIcon(item: T): BitmapDescriptor?
    }

    private val density = context.resources.displayMetrics.density
    private val items = LinkedHashSet<T>()

    // The markers on the map and what they show: a Cluster, or an item of type T
    private val markers = HashMap<Marker, Any>()
    private val clusterIconCache = HashMap<String, BitmapDescriptor>()
    private var clusteredZoom: Int? = null
    private var onClusterClickListener: OnClusterClickListener<T>? = null
    private var onClusterItemClickListener: OnClusterItemClickListener<T>? = null

    /**
     * The smallest group drawn as a cluster; smaller groups show their items' markers.
     * Default is 4, as in the Google Maps Utility Library.
     */
    var minClusterSize: Int = 4

    /** How close items must be to share a cluster, in dp on the screen. Default is 100. */
    var maxDistanceDp: Int = 100

    /** Draws the cluster markers, or null for circles with the count. */
    var clusterIconProvider: ClusterIconProvider<T>? = null

    /** Draws the item markers, or null for the default marker. */
    var clusterItemIconProvider: ClusterItemIconProvider<T>? = null

    /** Adds an item. Call [cluster] to show it. */
    fun addItem(item: T): Boolean = items.add(item).also { if (it) clusteredZoom = null }

    /** Adds items. Call [cluster] to show them. */
    fun addItems(items: Collection<T>): Boolean = this.items.addAll(items).also { if (it) clusteredZoom = null }

    /** Removes an item. Call [cluster] to update the map. */
    fun removeItem(item: T): Boolean = items.remove(item).also { if (it) clusteredZoom = null }

    /** Removes items. Call [cluster] to update the map. */
    fun removeItems(items: Collection<T>): Boolean = this.items.removeAll(items.toSet()).also { if (it) clusteredZoom = null }

    /** Removes all items. Call [cluster] to update the map. */
    fun clearItems() {
        items.clear()
        clusteredZoom = null
    }

    /** Returns the items of the manager. */
    fun getItems(): Collection<T> = items.toList()

    /** Sets a listener called when a cluster is tapped. */
    fun setOnClusterClickListener(listener: OnClusterClickListener<T>?) {
        onClusterClickListener = listener
    }

    /** Sets a listener called when the marker of a single item is tapped. */
    fun setOnClusterItemClickListener(listener: OnClusterItemClickListener<T>?) {
        onClusterItemClickListener = listener
    }

    /** Computes the clusters for the current zoom level and draws their markers. */
    fun cluster() {
        val zoom = currentZoom()
        val clusters = DistanceBasedAlgorithm.cluster(items, zoom, maxDistanceDp * density.toDouble())

        markers.keys.forEach { map.removeMarker(it) }
        markers.clear()
        for (cluster in clusters) {
            if (cluster.size >= minClusterSize) {
                val marker =
                    Marker(
                        position = cluster.position,
                        icon = clusterIconProvider?.getIcon(cluster) ?: defaultClusterIcon(cluster.size),
                        anchor = Pair(0.5f, 0.5f),
                    )
                markers[map.addMarker(marker)] = cluster
            } else {
                for (item in cluster.items) {
                    val marker =
                        Marker(
                            position = item.getPosition(),
                            title = item.getTitle(),
                            snippet = item.getSnippet(),
                            icon = clusterItemIconProvider?.getIcon(item),
                            zIndex = item.getZIndex() ?: 0f,
                        )
                    markers[map.addMarker(marker)] = item
                }
            }
        }
        clusteredZoom = zoom
        map.invalidate()
    }

    /** Clusters again when the camera stops at another integer zoom level. */
    override fun onCameraIdle() {
        if (currentZoom() != clusteredZoom) cluster()
    }

    /** Handles taps on the manager's markers; returns false for other markers. */
    override fun onMarkerClick(marker: Marker): Boolean {
        val target = markers[marker] ?: return false
        if (target is Cluster<*>) {
            @Suppress("UNCHECKED_CAST")
            val cluster = target as Cluster<T>
            if (onClusterClickListener?.onClusterClick(cluster) != true) zoomTo(cluster)
            return true
        }
        @Suppress("UNCHECKED_CAST")
        return onClusterItemClickListener?.onClusterItemClick(target as T) ?: false
    }

    private fun currentZoom(): Int = floor(map.getCameraPosition().zoom).toInt()

    // Shows all items of the cluster, which splits it at the higher zoom level
    private fun zoomTo(cluster: Cluster<T>) {
        val bounds =
            cluster.items.fold(null as LatLngBounds?) { bounds, item ->
                bounds?.including(item.getPosition()) ?: LatLngBounds(item.getPosition(), item.getPosition())
            } ?: return
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, (CLUSTER_ZOOM_PADDING_DP * density).toInt()))
    }

    // A colored circle with the count, rounded down to a bucket above 10 (e.g. "50+")
    private fun defaultClusterIcon(size: Int): BitmapDescriptor {
        val bucket = BUCKETS.lastOrNull { size >= it }
        val label = if (bucket == null) size.toString() else "$bucket+"
        return clusterIconCache.getOrPut(label) {
            val radius = (CLUSTER_RADIUS_DP + 2 * BUCKETS.indexOf(bucket).coerceAtLeast(0)) * density
            val diameter = (radius * 2).toInt()
            val bitmap = createBitmap(diameter, diameter)
            val canvas = Canvas(bitmap)
            val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bucketColor(bucket) }
            val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(160, 255, 255, 255) }
            val text =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textAlign = Paint.Align.CENTER
                    textSize = 14 * density
                    typeface = Typeface.DEFAULT_BOLD
                }
            canvas.drawCircle(radius, radius, radius, ring)
            canvas.drawCircle(radius, radius, radius - 3 * density, fill)
            canvas.drawText(label, radius, radius - (text.ascent() + text.descent()) / 2, text)
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    // From blue for small clusters to red for large ones
    private fun bucketColor(bucket: Int?): Int {
        val hues = floatArrayOf(220f, 200f, 170f, 120f, 60f, 35f, 15f, 0f)
        val hue = hues[(BUCKETS.indexOf(bucket) + 1).coerceIn(0, hues.lastIndex)]
        return Color.HSVToColor(floatArrayOf(hue, 0.75f, 0.85f))
    }

    private companion object {
        val BUCKETS = listOf(10, 20, 50, 100, 200, 500, 1000)
        const val CLUSTER_RADIUS_DP = 18
        const val CLUSTER_ZOOM_PADDING_DP = 48
    }
}
