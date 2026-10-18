/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.graphics.Point
import android.view.View
import de.afarber.openmapview.clustering.ClusterItem
import de.afarber.openmapview.clustering.ClusterManager
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.log2

/**
 * Tests that the map is measured in dp, as with the Maps SDK: at density 3 a tile is 768 px.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xxhdpi")
class ScreenDensityTest {
    private class Shop(
        private val position: LatLng,
    ) : ClusterItem {
        override fun getPosition() = position

        override fun getTitle(): String? = null

        override fun getSnippet(): String? = null
    }

    private val center = LatLng(0.0, 0.0)

    // Degrees of longitude per dp at zoom 10
    private val dpToLongitude = 360.0 / (256.0 * 1024)

    private lateinit var activity: Activity
    private lateinit var mapView: OpenMapView

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.setMapType(MapType.NONE)
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1200, View.MeasureSpec.EXACTLY),
        )
        mapView.layout(0, 0, 1200, 1200)
        mapView.setCenter(center)
        mapView.setZoom(10f)
    }

    @Test
    fun testDefaultTileScale_IsTheScreenDensity() {
        assertEquals(3f, mapView.getTileScale(), 0f)
    }

    @Test
    fun testProjection_MeasuresInDp() {
        // 100 dp east of the center is 300 px to the right
        val east = LatLng(0.0, 100 * dpToLongitude)
        assertEquals(Point(900, 600), mapView.getProjection().toScreenLocation(east))

        val latLng = mapView.getProjection().fromScreenLocation(Point(900, 600))
        assertEquals(east.longitude, latLng.longitude, 1e-6)
    }

    @Test
    fun testTileScaleOne_DrawsOneTilePixelPerScreenPixel() {
        mapView.setTileScale(1f)

        val east = LatLng(0.0, 100 * dpToLongitude)
        assertEquals(Point(700, 600), mapView.getProjection().toScreenLocation(east))
        assertEquals(center, mapView.getCameraPosition().target)
        assertEquals(10f, mapView.getCameraPosition().zoom, 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTileScale_MustBePositive() {
        mapView.setTileScale(0f)
    }

    @Test
    fun testNewLatLngBounds_FitsTheBoundsInScreenPixels() {
        val bounds = LatLngBounds(LatLng(-10.0, -10.0), LatLng(10.0, 10.0))

        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
        val zoomAtDensity3 = mapView.getCameraPosition().zoom
        mapView.setTileScale(1f)
        mapView.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
        val zoomAtScale1 = mapView.getCameraPosition().zoom

        // Tiles 3 times as large need log2(3) zoom levels less to fit the same view
        assertEquals(log2(3f), zoomAtScale1 - zoomAtDensity3, 0.001f)
    }

    @Test
    fun testScrollBy_MovesByScreenPixels() {
        mapView.moveCamera(CameraUpdateFactory.scrollBy(300f, 0f))

        // 300 px are 100 dp
        assertEquals(100 * dpToLongitude, mapView.getCameraPosition().target.longitude, 1e-6)
    }

    @Test
    fun testClustering_MeasuresInDp() {
        // The search square reaches half the maximum distance, so two shops 150 dp apart
        // are clustered with 400 dp, but not with 100 dp
        val manager = ClusterManager<Shop>(activity, mapView)
        manager.minClusterSize = 2
        manager.addItems(listOf(Shop(center), Shop(LatLng(0.0, 150 * dpToLongitude))))
        manager.cluster()
        assertEquals(2, mapView.getMarkers().size)

        manager.maxDistanceDp = 400
        manager.cluster()
        assertEquals(1, mapView.getMarkers().size)
    }
}
