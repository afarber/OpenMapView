/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.Point
import android.os.Parcel
import android.view.MotionEvent
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import kotlin.math.abs

/**
 * Tests for the camera bearing: the camera position, the projection, hit tests and touches
 * on a turned map.
 */
@RunWith(RobolectricTestRunner::class)
class CameraBearingTest {
    private lateinit var controller: MapController
    private val target = LatLng(51.4661, 7.2491)

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(800, 600)
        controller.setCenter(target)
        controller.setZoom(12f)
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    private fun turnTo(bearing: Float) {
        controller.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(target, 12f, bearing)))
    }

    @Test
    fun testCameraPosition_BearingIsNormalized() {
        assertEquals(270f, CameraPosition(target, 12f, -90f).bearing)
        assertEquals(0f, CameraPosition(target, 12f, 720f).bearing)
        assertEquals(CameraPosition(target, 12f, 270f), CameraPosition(target, 12f, -90f))
        assertNotEquals(CameraPosition(target, 12f, 90f), CameraPosition(target, 12f))
        assertEquals(0f, CameraPosition(target, 12f).bearing)
    }

    @Test
    fun testCameraPosition_BuilderAndCopy() {
        val position =
            CameraPosition
                .builder()
                .target(target)
                .zoom(14f)
                .bearing(45f)
                .build()

        assertEquals(CameraPosition(target, 14f, 45f), position)
        assertEquals(position, CameraPosition.builder(position).build())
        assertEquals(CameraPosition(target, 14f, 90f), position.copy(bearing = 90f))
        assertEquals(CameraPosition(target, 10f), CameraPosition.fromLatLngZoom(target, 10f))
        val (t, zoom, bearing) = position
        assertEquals(target, t)
        assertEquals(14f, zoom)
        assertEquals(45f, bearing)
    }

    @Test
    fun testCameraPosition_SurvivesParcel() {
        val position = CameraPosition(target, 13.5f, 217f)
        val parcel = Parcel.obtain()
        parcel.writeParcelable(position, 0)
        parcel.setDataPosition(0)

        @Suppress("DEPRECATION")
        val restored = parcel.readParcelable<CameraPosition>(CameraPosition::class.java.classLoader)
        parcel.recycle()

        assertEquals(position, restored)
    }

    @Test
    fun testCameraUpdates_KeepOrResetTheBearing() {
        turnTo(90f)
        assertEquals(90f, controller.getCameraPosition().bearing)

        controller.moveCamera(CameraUpdateFactory.zoomIn())
        assertEquals(90f, controller.getCameraPosition().bearing)
        controller.moveCamera(CameraUpdateFactory.newLatLng(LatLng(51.5, 7.3)))
        assertEquals(90f, controller.getCameraPosition().bearing)

        // Fitting bounds faces north, as in the Maps SDK
        controller.moveCamera(CameraUpdateFactory.newLatLngBounds(LatLngBounds(LatLng(51.4, 7.1), LatLng(51.5, 7.3)), 0))
        assertEquals(0f, controller.getCameraPosition().bearing)
    }

    @Test
    fun testInterpolation_TurnsTheShorterWay() {
        val start = CameraPosition(target, 12f, 350f)
        val end = CameraPosition(target, 12f, 10f)

        assertEquals(0f, interpolateCamera(start, end, 0.5f).bearing, 0.001f)
        assertEquals(355f, interpolateCamera(start, end, 0.25f).bearing, 0.001f)
        assertEquals(10f, interpolateCamera(start, end, 1f).bearing, 0.001f)
    }

    @Test
    fun testProjection_RoundTripsAtAnyBearing() {
        for (padding in listOf(0, 120)) {
            controller.setMapPadding(padding, 0, 0, padding)
            for (bearing in listOf(0f, 90f, 217f)) {
                turnTo(bearing)
                val projection = controller.createProjection()
                for (point in listOf(Point(10, 20), Point(400, 300), Point(790, 590), Point(123, 456))) {
                    val back = projection.toScreenLocation(projection.fromScreenLocation(point))
                    assertTrue("$point at $bearing and padding $padding: $back", abs(back.x - point.x) <= 1 && abs(back.y - point.y) <= 1)
                }
                // The camera target stays at the center of the padded area
                val center = projection.toScreenLocation(target)
                assertTrue(
                    "target at $bearing: $center",
                    abs(center.x - (400 + padding / 2)) <= 1 && abs(center.y - (300 - padding / 2)) <= 1,
                )
            }
        }
    }

    @Test
    fun testProjection_EastIsUpAtBearing90() {
        turnTo(90f)
        val projection = controller.createProjection()
        val east = projection.toScreenLocation(LatLng(target.latitude, target.longitude + 0.05))
        val north = projection.toScreenLocation(LatLng(target.latitude + 0.05, target.longitude))

        assertTrue("east $east", abs(east.x - 400) <= 1 && east.y < 300 - 100)
        assertTrue("north $north", north.x < 400 - 100 && abs(north.y - 300) <= 1)
        // screenToLatLng agrees with the projection
        val location = controller.screenToLatLng(400f, 100f)
        assertTrue(location.longitude > target.longitude)
        assertEquals(target.latitude, location.latitude, 1e-4)
    }

    @Test
    fun testVisibleRegion_HasTurnedCornersInItsBounds() {
        turnTo(45f)
        val region = controller.createProjection().getVisibleRegion()

        for (corner in listOf(region.nearLeft, region.nearRight, region.farLeft, region.farRight)) {
            assertTrue("$corner in ${region.latLngBounds}", region.latLngBounds.contains(corner))
        }
        // At 45 degrees the top left corner of the screen is the northernmost point
        assertEquals(region.latLngBounds.northeast.latitude, region.farLeft.latitude, 1e-9)
    }

    private fun icon(): BitmapDescriptor = BitmapDescriptorFactory.fromBitmap(Bitmap.createBitmap(20, 40, Bitmap.Config.ARGB_8888))

    @Test
    fun testMarkers_FaceTheScreenUnlessFlat() {
        turnTo(90f)
        // East of the target, so drawn above it at bearing 90
        val position = LatLng(target.latitude, target.longitude + 0.02)
        val upright = controller.addMarker(Marker(position = position, icon = icon()))
        val point = controller.createProjection().toScreenLocation(position)

        // The 20 x 40 icon stands upright above its position
        assertSame(upright, controller.handleMarkerTouch(point.x.toFloat(), point.y - 20f))
        assertNull(controller.handleMarkerTouch(point.x - 20f, point.y.toFloat()))

        // A flat icon lies on the map, whose north is on the left of the screen
        upright.isFlat = true
        assertSame(upright, controller.handleMarkerTouch(point.x - 20f, point.y.toFloat()))
        assertNull(controller.handleMarkerTouch(point.x.toFloat(), point.y - 20f))
    }

    @Test
    fun testInfoWindow_StaysUprightAboveTheIcon() {
        turnTo(217f)
        val position = LatLng(target.latitude + 0.01, target.longitude + 0.01)
        val marker = controller.addMarker(Marker(position = position, title = "Title", snippet = "Snippet", icon = icon()))
        marker.showInfoWindow()
        val point = controller.createProjection().toScreenLocation(position)

        // Above the upright 40 px icon, with the pointer and gap of 12 px
        assertSame(marker, controller.handleInfoWindowTouch(point.x.toFloat(), point.y - 60f))
        assertNull(controller.handleInfoWindowTouch(point.x.toFloat(), point.y + 60f))
    }

    @Test
    fun testZoom_KeepsFocusLocationOnTurnedMap() {
        turnTo(217f)
        val before = controller.screenToLatLng(200f, 150f)

        controller.zoom(2f, 200f, 150f)
        controller.commitPan()

        // Within float rounding, as for the unturned map in MapControllerTest
        val after = controller.screenToLatLng(200f, 150f)
        assertEquals(before.latitude, after.latitude, 1e-5)
        assertEquals(before.longitude, after.longitude, 1e-5)
    }

    @Test
    fun testDrag_MovesTheMapWithTheFingerOnTurnedMap() {
        val mapView = OpenMapView(ApplicationProvider.getApplicationContext())
        mapView.setMapType(MapType.NONE)
        mapView.layout(0, 0, 800, 600)
        mapView.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(target, 12f, 90f)))
        val location = mapView.getProjection().fromScreenLocation(Point(300, 300))

        // Drag 100 px to the right
        for ((action, x) in listOf(
            MotionEvent.ACTION_DOWN to 300f,
            MotionEvent.ACTION_MOVE to 350f,
            MotionEvent.ACTION_MOVE to 400f,
            MotionEvent.ACTION_UP to 400f,
        )) {
            val event = MotionEvent.obtain(0, 0, action, x, 300f, 0)
            mapView.onTouchEvent(event)
            event.recycle()
        }

        // The location under the finger moved with it
        val point = mapView.getProjection().toScreenLocation(location)
        assertTrue("$point", abs(point.x - 400) <= 1 && abs(point.y - 300) <= 1)
        assertEquals(90f, mapView.getCameraPosition().bearing)
    }
}
