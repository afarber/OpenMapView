/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.createBitmap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class InfoWindowAdapterTest {
    private val center = LatLng(51.4661, 7.2491)
    private lateinit var activity: Activity
    private lateinit var mapView: OpenMapView
    private lateinit var marker: Marker

    // A marker at the center of the 400x400 map: its default icon is 72 px high and
    // anchored at the bottom, so its info window ends 82 px above the center
    private val windowBottom = 200 - 72 - 10

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        mapView = OpenMapView(activity)
        mapView.setMapType(MapType.NONE)
        activity.setContentView(mapView)
        mapView.measure(
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
        )
        mapView.layout(0, 0, 400, 400)
        mapView.setCenter(center)
        mapView.setZoom(14f)
        marker = mapView.addMarker(Marker(position = center, title = "Bochum"))
    }

    private fun greenView(
        width: Int,
        height: Int,
    ) = View(activity).apply {
        minimumWidth = width
        minimumHeight = height
        setBackgroundColor(Color.GREEN)
    }

    private fun adapter(
        window: View? = null,
        contents: View? = null,
    ) = object : InfoWindowAdapter {
        override fun getInfoWindow(marker: Marker) = window

        override fun getInfoContents(marker: Marker) = contents
    }

    private fun drawMap() = createBitmap(400, 400).also { mapView.draw(Canvas(it)) }

    @Test
    fun testGetInfoWindow_ReplacesWholeWindow() {
        val view = greenView(120, 60)
        mapView.setInfoWindowAdapter(adapter(window = view))

        mapView.showInfoWindow(marker)
        val bitmap = drawMap()

        val image = assertNotNullAndGet(marker.infoWindowImage)
        assertEquals(120, image.bitmap.width)
        assertEquals(60, image.bitmap.height)
        assertFalse(image.framed)
        // The window is centered above the icon, with no frame around the view
        assertEquals(Color.GREEN, bitmap.getPixel(200, windowBottom - 30))
        assertEquals(Color.GREEN, bitmap.getPixel(141, windowBottom - 2))
        // The view was only added to the map while being drawn
        assertNull(view.parent)
    }

    @Test
    fun testGetInfoContents_IsDrawnInDefaultFrame() {
        mapView.setInfoWindowAdapter(adapter(contents = greenView(120, 60)))

        mapView.showInfoWindow(marker)
        val bitmap = drawMap()

        assertTrue(assertNotNullAndGet(marker.infoWindowImage).framed)
        // The 100 px high frame: 20 px white padding around the green view
        val frameTop = windowBottom - 100
        assertEquals(Color.GREEN, bitmap.getPixel(200, frameTop + 50))
        assertEquals(Color.WHITE, bitmap.getPixel(200, frameTop + 10))
        assertEquals(Color.WHITE, bitmap.getPixel(135, frameTop + 50))
    }

    @Test
    fun testAdapterReturningNull_ShowsDefaultWindow() {
        mapView.setInfoWindowAdapter(adapter())

        mapView.showInfoWindow(marker)

        assertTrue(marker.isInfoWindowShown)
        assertNull(marker.infoWindowImage)
    }

    @Test
    fun testMarkerWithoutTitle_ShowsAdapterWindow() {
        val untitled = mapView.addMarker(Marker(position = center))
        mapView.showInfoWindow(untitled)
        assertFalse(untitled.isInfoWindowShown)

        mapView.setInfoWindowAdapter(adapter(window = greenView(50, 50)))
        mapView.showInfoWindow(untitled)

        assertTrue(untitled.isInfoWindowShown)
    }

    @Test
    fun testTapOnCustomWindow_CallsInfoWindowClickListener() {
        mapView.setInfoWindowAdapter(adapter(window = greenView(120, 60)))
        var clicked: Marker? = null
        mapView.setOnInfoWindowClickListener { clicked = it }
        mapView.showInfoWindow(marker)

        val y = (windowBottom - 30).toFloat()
        for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
            val event = MotionEvent.obtain(0, time, action, 200f, y, 0)
            mapView.onTouchEvent(event)
            event.recycle()
        }

        assertSame(marker, clicked)
    }

    @Test
    fun testHiding_DropsImageAndShowingAgainAsksAdapterAgain() {
        var calls = 0
        mapView.setInfoWindowAdapter(
            object : InfoWindowAdapter {
                override fun getInfoWindow(marker: Marker): View {
                    calls++
                    return greenView(40, 40)
                }

                override fun getInfoContents(marker: Marker): View? = null
            },
        )

        mapView.showInfoWindow(marker)
        mapView.hideInfoWindow(marker)
        assertNull(marker.infoWindowImage)

        marker.showInfoWindow()
        assertNotNull(marker.infoWindowImage)
        assertEquals(2, calls)
    }

    private fun <T : Any> assertNotNullAndGet(value: T?): T {
        assertNotNull(value)
        return value!!
    }
}
