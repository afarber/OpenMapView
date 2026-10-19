/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.clustering.Cluster
import de.afarber.openmapview.clustering.ClusterItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ClusteringComposeTest {
    @get:Rule
    val composeRule = createComposeRule()

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

    private lateinit var map: OpenMapView
    private val cameraPositionState = CameraPositionState(CameraPosition(bochum, 10f))

    // Shops about 100 m apart: one cluster at zoom 10, single markers at zoom 18
    private fun shopsNear(
        position: LatLng,
        count: Int,
    ) = (0 until count).map { Shop(LatLng(position.latitude, position.longitude + it * 0.0015), "Shop $it") }

    private fun showMap(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenMap(
                modifier = FillTestWindow,
                cameraPositionState = cameraPositionState,
                onMapCreated = { map = it },
                content = content,
            )
        }
        composeRule.waitForIdle()
    }

    // Taps the center of a cluster marker, which is anchored at its center
    private fun tapMarkerAt(position: LatLng) {
        val point = map.getProjection().toScreenLocation(position)
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
            val event = MotionEvent.obtain(0, time, action, point.x.toFloat(), point.y.toFloat() - 10f, 0)
            map.onTouchEvent(event)
            event.recycle()
        }
        confirmTap()
    }

    @Test
    fun testItems_AreClusteredAndRemovedWithComposition() {
        var show by mutableStateOf(true)
        showMap { if (show) Clustering(items = shopsNear(bochum, 5) + shopsNear(dortmund, 1)) }

        val markers = map.getMarkers()
        assertEquals(2, markers.size)
        assertTrue(markers.single { it.title == null }.icon is BitmapDescriptor.BitmapMarker)

        show = false
        composeRule.waitForIdle()

        assertEquals(0, map.getMarkers().size)
    }

    @Test
    fun testChangedItems_UpdateClusters() {
        var items by mutableStateOf(shopsNear(bochum, 5))
        showMap { Clustering(items = items) }
        assertEquals(1, map.getMarkers().size)

        items = shopsNear(bochum, 2)
        composeRule.waitForIdle()

        assertEquals(2, map.getMarkers().size)
    }

    @Test
    fun testZoomingIn_SplitsClusters() {
        showMap { Clustering(items = shopsNear(bochum, 5)) }
        assertEquals(1, map.getMarkers().size)

        composeRule.runOnIdle { cameraPositionState.position = CameraPosition(bochum, 18f) }
        composeRule.waitForIdle()

        assertEquals(5, map.getMarkers().size)
    }

    @Test
    fun testTaps_CallClusterAndItemCallbacks() {
        val lonely = Shop(dortmund, "Lonely")
        var clickedCluster: Cluster<Shop>? = null
        var clickedItem: Shop? = null
        showMap {
            Clustering(
                items = shopsNear(bochum, 5) + lonely,
                onClusterClick = {
                    clickedCluster = it
                    true
                },
                onClusterItemClick = {
                    clickedItem = it
                    true
                },
            )
        }

        composeRule.runOnIdle {
            tapMarkerAt(bochum)
            // The item marker is anchored at its bottom
            tapMarkerAt(dortmund)
        }

        assertEquals(5, clickedCluster!!.size)
        assertSame(lonely, clickedItem)
    }

    @Test
    fun testMarkerComposables_StillGetTheirClicks() {
        var markerClicks = 0
        var clusterClicks = 0
        showMap {
            Clustering(items = shopsNear(dortmund, 5), onClusterClick = {
                clusterClicks++
                true
            })
            Marker(state = MarkerState(bochum), onClick = {
                markerClicks++
                false
            })
        }

        composeRule.runOnIdle { tapMarkerAt(bochum) }

        assertEquals(1, markerClicks)
        assertEquals(0, clusterClicks)
    }

    @Test
    fun testCustomContent_DrawsMarkerIcons() {
        showMap {
            Clustering(
                items = shopsNear(bochum, 5) + Shop(dortmund, "Lonely"),
                clusterContent = { Box(Modifier.size(30.dp).background(Color.Red)) },
                clusterItemContent = { Box(Modifier.size(20.dp).background(Color.Blue)) },
            )
        }

        val markers = map.getMarkers()
        val clusterIcon = markers.single { it.title == null }.icon as BitmapDescriptor.BitmapMarker
        val itemIcon = markers.single { it.title != null }.icon as BitmapDescriptor.BitmapMarker
        assertEquals(30, clusterIcon.bitmap.width)
        assertEquals(20, itemIcon.bitmap.width)
        assertNull(markers.firstOrNull { it.icon == null })
        assertEquals(0, map.childCount)
    }
}
