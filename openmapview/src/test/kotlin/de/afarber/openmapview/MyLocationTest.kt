/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.Manifest
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class MyLocationTest {
    private val center = LatLng(51.4661, 7.2491)
    private val nearby = LatLng(51.4700, 7.2550)
    private val dotBlue = Color.rgb(66, 133, 244)

    private lateinit var lifecycle: LifecycleRegistry
    private lateinit var activity: Activity
    private lateinit var container: FrameLayout
    private lateinit var mapView: OpenMapView

    private class FakeLocationSource : LocationSource {
        var listener: LocationSource.OnLocationChangedListener? = null
        var activations = 0
        var deactivations = 0

        override fun activate(listener: LocationSource.OnLocationChangedListener) {
            this.listener = listener
            activations++
        }

        override fun deactivate() {
            listener = null
            deactivations++
        }

        fun send(
            position: LatLng,
            accuracy: Float = 0f,
        ) {
            listener?.onLocationChanged(location(position, accuracy))
        }
    }

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val owner =
            object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry(this)
            }
        lifecycle = owner.lifecycle
        lifecycle.currentState = Lifecycle.State.RESUMED
        container = FrameLayout(activity)
        container.setViewTreeLifecycleOwner(owner)
        activity.setContentView(container)
        mapView = OpenMapView(activity)
        mapView.setMapType(MapType.NONE)
        container.addView(mapView)
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        mapView.layout(0, 0, 400, 400)
        mapView.setCenter(center)
        mapView.setZoom(14f)
    }

    private fun drawMap() = createBitmap(400, 400).also { mapView.draw(Canvas(it)) }

    // The map without tiles (MapType.NONE) is light gray
    private val background = Color.rgb(204, 204, 204)

    private fun tap(
        x: Float,
        y: Float,
    ) {
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
            val event = MotionEvent.obtain(0, time, action, x, y, 0)
            mapView.onTouchEvent(event)
            event.recycle()
        }
    }

    @Test(expected = SecurityException::class)
    fun testDefaultSourceWithoutPermission_Throws() {
        mapView.setMyLocationEnabled(true)
    }

    @Test
    fun testCustomSource_NeedsNoPermissionAndIsActivatedWhileShown() {
        val source = FakeLocationSource()
        mapView.setLocationSource(source)
        assertFalse(mapView.isMyLocationEnabled())

        mapView.setMyLocationEnabled(true)
        assertTrue(mapView.isMyLocationEnabled())
        assertEquals(1, source.activations)

        // Paused, resumed, detached
        lifecycle.currentState = Lifecycle.State.STARTED
        assertEquals(1, source.deactivations)
        lifecycle.currentState = Lifecycle.State.RESUMED
        assertEquals(2, source.activations)
        container.removeView(mapView)
        assertEquals(2, source.deactivations)

        container.addView(mapView)
        assertEquals(3, source.activations)
        mapView.setMyLocationEnabled(false)
        assertEquals(3, source.deactivations)
    }

    @Test
    fun testLocation_IsDrawnAsBlueDotWithAccuracyCircle() {
        val source = FakeLocationSource()
        mapView.setLocationSource(source)
        mapView.setMyLocationEnabled(true)
        assertEquals(background, drawMap().getPixel(200, 200))

        source.send(center, accuracy = 300f)
        val bitmap = drawMap()

        assertEquals(dotBlue, bitmap.getPixel(200, 200))
        // At zoom 14 in Bochum 300 m are about 50 px: inside the circle, but not on the dot
        assertNotEquals(background, bitmap.getPixel(200, 170))
        assertNotEquals(dotBlue, bitmap.getPixel(200, 170))
        assertEquals(background, bitmap.getPixel(200, 120))
    }

    @Test
    fun testPlatformSource_UsesLocationManager() {
        shadowOf(activity.application).grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowOf(locationManager).setLocationEnabled(true)
        shadowOf(locationManager).setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        // Sets the last known location, which is shown right away
        shadowOf(locationManager).simulateLocation(location(nearby, 0f))

        mapView.setMyLocationEnabled(true)
        val nearbyPoint = mapView.getProjection().toScreenLocation(nearby)
        assertEquals(dotBlue, drawMap().getPixel(nearbyPoint.x, nearbyPoint.y))

        // Then the updates, at most one per second
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2))
        shadowOf(locationManager).simulateLocation(location(center, 0f).apply { time += 2000 })
        shadowOf(Looper.getMainLooper()).idle()

        // Idling the looper also lays the map out again, at the size of the test window
        val centerPoint = mapView.getProjection().toScreenLocation(center)
        assertEquals(dotBlue, drawMap().getPixel(centerPoint.x, centerPoint.y))
    }

    @Test
    fun testButton_AnimatesCameraToLocation() {
        val source = FakeLocationSource()
        mapView.setLocationSource(source)
        mapView.setMyLocationEnabled(true)
        val reasons = mutableListOf<Int>()
        mapView.setOnCameraMoveStartedListener { reasons.add(it) }
        source.send(nearby)
        drawMap()

        // The button is in the top right corner: 16 dp margin, 20 dp radius
        tap(364f, 36f)
        shadowOf(Looper.getMainLooper()).idle()

        // The animation runs on wall clock time, so only its start is checked. Like the
        // zoom buttons, the map's own button reports an API animation.
        assertEquals(listOf(OnCameraMoveStartedListener.REASON_API_ANIMATION), reasons)
    }

    @Test
    fun testButton_StaysInsideMapPadding() {
        val source = FakeLocationSource()
        mapView.setLocationSource(source)
        mapView.setMyLocationEnabled(true)
        var clicks = 0
        mapView.setOnMyLocationButtonClickListener {
            clicks++
            true
        }
        // E.g. a toolbar at the top and a side panel on the right
        mapView.setMapPadding(0, 100, 50, 0)
        source.send(nearby)
        drawMap()

        tap(364f, 36f)
        assertEquals(0, clicks)

        // The button moved down and left by the padding
        tap(314f, 136f)
        assertEquals(1, clicks)
    }

    @Test
    fun testButtonListenerReturningTrue_KeepsCamera() {
        val source = FakeLocationSource()
        mapView.setLocationSource(source)
        mapView.setMyLocationEnabled(true)
        var clicks = 0
        mapView.setOnMyLocationButtonClickListener {
            clicks++
            true
        }
        source.send(nearby)
        drawMap()

        tap(364f, 36f)
        shadowOf(Looper.getMainLooper()).idle()

        assertEquals(1, clicks)
        assertEquals(center.latitude, mapView.getCameraPosition().target.latitude, 1e-4)
    }

    @Test
    fun testButtonDisabled_IsNotDrawnOrTapped() {
        val source = FakeLocationSource()
        mapView.setLocationSource(source)
        mapView.setMyLocationEnabled(true)
        mapView.getUiSettings().isMyLocationButtonEnabled = false
        var mapClicks = 0
        mapView.setOnMapClickListener { mapClicks++ }

        assertEquals(background, drawMap().getPixel(364, 36))
        tap(364f, 36f)

        assertEquals(1, mapClicks)
    }

    @Test
    fun testTapOnDot_CallsMyLocationClickListener() {
        val source = FakeLocationSource()
        mapView.setLocationSource(source)
        mapView.setMyLocationEnabled(true)
        var clicked: Location? = null
        mapView.setOnMyLocationClickListener { clicked = it }
        source.send(center)
        drawMap()

        tap(203f, 198f)

        assertEquals(center.latitude, clicked!!.latitude, 1e-9)
    }

    @Test
    fun testReplacedSource_IgnoresLateLocations() {
        val old = FakeLocationSource()
        mapView.setLocationSource(old)
        mapView.setMyLocationEnabled(true)
        val oldListener = old.listener!!

        val new = FakeLocationSource()
        mapView.setLocationSource(new)
        oldListener.onLocationChanged(location(center, 0f))

        assertEquals(1, old.deactivations)
        assertSame(null, old.listener)
        assertEquals(background, drawMap().getPixel(200, 200))
    }

    companion object {
        fun location(
            position: LatLng,
            accuracy: Float,
        ) = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = position.latitude
            longitude = position.longitude
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
            if (accuracy > 0) this.accuracy = accuracy
        }
    }
}
