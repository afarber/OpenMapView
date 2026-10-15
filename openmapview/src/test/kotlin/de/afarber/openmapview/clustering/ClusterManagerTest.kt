/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.clustering

import android.app.Activity
import android.graphics.Bitmap
import android.os.Looper
import android.view.View
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.BitmapDescriptorFactory
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.MapType
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ClusterManagerTest {
    private class Shop(
        private val position: LatLng,
        private val name: String,
    ) : ClusterItem {
        override fun getPosition() = position

        override fun getTitle() = name

        override fun getSnippet(): String? = null
    }

    private val bochum = LatLng(51.4661, 7.2491)
    private val dortmund = LatLng(51.5136, 7.4653)

    private lateinit var mapView: OpenMapView
    private lateinit var manager: ClusterManager<Shop>

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.setMapType(MapType.NONE)
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        mapView.layout(0, 0, 400, 400)
        mapView.setCenter(bochum)
        mapView.setZoom(10f)
        manager = ClusterManager(activity, mapView)
        mapView.setOnCameraIdleListener(manager)
        mapView.setOnMarkerClickListener(manager)
    }

    // Shops in a row about 100 m apart: a few pixels at zoom 10, far apart at zoom 18
    private fun shopsNear(
        position: LatLng,
        count: Int,
    ) = (0 until count).map { Shop(LatLng(position.latitude, position.longitude + it * 0.0015), "Shop $it") }

    @Test
    fun testAlgorithm_GroupsNearbyItemsAndSplitsThemWhenZoomedIn() {
        val shops = shopsNear(bochum, 5) + shopsNear(dortmund, 3)

        val atZoom10 = DistanceBasedAlgorithm.cluster(shops, 10, 100.0)
        assertEquals(listOf(5, 3), atZoom10.map { it.size })
        assertEquals(bochum, atZoom10[0].position)

        val atZoom18 = DistanceBasedAlgorithm.cluster(shops, 18, 100.0)
        assertEquals(8, atZoom18.size)
    }

    @Test
    fun testAlgorithm_ItemMovesToCloserCluster() {
        // At zoom 10 the search square reaches 50 px from a cluster's first item.
        // "between" is 45 px right of "first" and joins its cluster; "second" is 60 px right
        // of "first", out of its reach, and starts a cluster only 15 px from "between".
        val pxToLongitude = 360.0 / (256.0 * 1024)
        val first = Shop(LatLng(0.0, 0.0), "first")
        val second = Shop(LatLng(0.0, 60 * pxToLongitude), "second")
        val between = Shop(LatLng(0.0, 45 * pxToLongitude), "between")

        val clusters = DistanceBasedAlgorithm.cluster(listOf(first, between, second), 10, 100.0)

        assertEquals(listOf(setOf(first), setOf(second, between)), clusters.map { it.items.toSet() })
    }

    @Test
    fun testCluster_DrawsClusterMarkerAndSingleItems() {
        manager.addItems(shopsNear(bochum, 12) + shopsNear(dortmund, 2))
        manager.cluster()

        val markers = mapView.getMarkers()
        assertEquals(3, markers.size)
        val clusterMarker = markers.single { it.title == null }
        assertTrue(clusterMarker.icon is BitmapDescriptor.BitmapMarker)
        assertEquals(Pair(0.5f, 0.5f), clusterMarker.anchor)
        assertEquals(setOf("Shop 0", "Shop 1"), markers.mapNotNull { it.title }.toSet())
    }

    @Test
    fun testGroupsBelowMinClusterSize_ShowItems() {
        manager.addItems(shopsNear(bochum, 3))
        manager.cluster()
        assertEquals(3, mapView.getMarkers().size)

        manager.minClusterSize = 2
        manager.cluster()
        assertEquals(1, mapView.getMarkers().size)
    }

    @Test
    fun testCameraIdleAtNewZoom_ClustersAgain() {
        manager.addItems(shopsNear(bochum, 5))
        manager.cluster()
        assertEquals(1, mapView.getMarkers().size)

        mapView.setZoom(18f)
        manager.onCameraIdle()

        assertEquals(5, mapView.getMarkers().size)
    }

    @Test
    fun testClusterClick_ZoomsInUnlessListenerConsumesIt() {
        manager.addItems(shopsNear(bochum, 5))
        manager.cluster()
        val clusterMarker = mapView.getMarkers().single()
        var moves = 0
        mapView.setOnCameraMoveStartedListener { moves++ }

        var clicked: Cluster<Shop>? = null
        manager.setOnClusterClickListener {
            clicked = it
            true
        }
        assertTrue(manager.onMarkerClick(clusterMarker))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(5, clicked!!.size)
        assertEquals(0, moves)

        manager.setOnClusterClickListener(null)
        manager.onMarkerClick(clusterMarker)
        shadowOf(Looper.getMainLooper()).idle()
        // The animation runs on wall clock time, so only its start is checked
        assertEquals(1, moves)
    }

    @Test
    fun testItemClick_CallsItemListener() {
        val shop = shopsNear(dortmund, 1).single()
        manager.addItem(shop)
        manager.cluster()
        var clicked: Shop? = null
        manager.setOnClusterItemClickListener {
            clicked = it
            true
        }

        assertTrue(manager.onMarkerClick(mapView.getMarkers().single()))

        assertSame(shop, clicked)
    }

    @Test
    fun testOtherMarkers_AreNotHandled() {
        val other = mapView.addMarker(de.afarber.openmapview.Marker(position = bochum))

        assertFalse(manager.onMarkerClick(other))
    }

    @Test
    fun testRemovingAndClearingItems_UpdatesMarkers() {
        val shops = shopsNear(bochum, 5)
        manager.addItems(shops)
        manager.cluster()

        manager.removeItems(shops.take(2))
        manager.cluster()
        assertEquals(3, mapView.getMarkers().size)

        manager.clearItems()
        manager.cluster()
        assertEquals(0, mapView.getMarkers().size)
        assertTrue(manager.getItems().isEmpty())
    }

    @Test
    fun testIconProviders_DrawMarkers() {
        val clusterIcon = BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888))
        val itemIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        manager.clusterIconProvider = ClusterManager.ClusterIconProvider { clusterIcon }
        manager.clusterItemIconProvider = ClusterManager.ClusterItemIconProvider { itemIcon }
        manager.addItems(shopsNear(bochum, 5) + shopsNear(dortmund, 1))
        manager.cluster()

        val markers = mapView.getMarkers()
        assertSame(clusterIcon, markers.single { it.title == null }.icon)
        assertSame(itemIcon, markers.single { it.title != null }.icon)
        assertNull(markers.firstOrNull { it.icon == null })
    }
}
