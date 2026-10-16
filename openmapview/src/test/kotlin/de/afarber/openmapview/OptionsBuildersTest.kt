/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests for the *Options builders: every setter must reach the built object.
 */
@RunWith(RobolectricTestRunner::class)
class OptionsBuildersTest {
    private val point1 = LatLng(51.0, 7.0)
    private val point2 = LatLng(52.0, 8.0)
    private val point3 = LatLng(51.5, 8.5)
    private val pattern = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))

    @Test
    fun testMarkerOptions() {
        val icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
        val marker =
            MarkerOptions()
                .position(point1)
                .title("Title")
                .snippet("Snippet")
                .icon(icon)
                .anchor(0.25f, 0.75f)
                .visible(false)
                .alpha(0.5f)
                .draggable(true)
                .zIndex(2f)
                .tag("tag")
                .build()

        assertEquals(point1, marker.position)
        assertEquals("Title", marker.title)
        assertEquals("Snippet", marker.snippet)
        assertEquals(icon, marker.icon)
        assertEquals(Pair(0.25f, 0.75f), marker.anchor)
        assertFalse(marker.visible)
        assertEquals(0.5f, marker.alpha)
        assertTrue(marker.draggable)
        assertEquals(2f, marker.zIndex)
        assertEquals("tag", marker.tag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testMarkerOptionsWithoutPositionThrows() {
        MarkerOptions().title("No position").build()
    }

    @Test
    fun testPolylineOptions() {
        val spans = listOf(StyleSpan(Color.Red, segments = 2))
        val polyline =
            PolylineOptions()
                .add(point1)
                .addAll(listOf(point2, point3))
                .color(Color.Blue)
                .width(4f)
                .strokePattern(pattern)
                .startCap(StrokeCap.Round)
                .endCap(StrokeCap.Square)
                .strokeJoin(StrokeJoin.Bevel)
                .geodesic(true)
                .spans(spans)
                .visible(false)
                .clickable(true)
                .zIndex(3f)
                .tag(42)
                .build()

        assertEquals(listOf(point1, point2, point3), polyline.points)
        assertEquals(Color.Blue, polyline.strokeColor)
        assertEquals(4f, polyline.strokeWidth)
        assertSame(pattern, polyline.strokePattern)
        assertEquals(StrokeCap.Round, polyline.startCap)
        assertEquals(StrokeCap.Square, polyline.endCap)
        assertEquals(StrokeJoin.Bevel, polyline.strokeJoin)
        assertTrue(polyline.geodesic)
        assertEquals(spans, polyline.spans)
        assertFalse(polyline.visible)
        assertTrue(polyline.clickable)
        assertEquals(3f, polyline.zIndex)
        assertEquals(42, polyline.tag)
    }

    @Test
    fun testPolygonOptions() {
        val hole = listOf(LatLng(51.4, 7.4), LatLng(51.5, 7.5), LatLng(51.4, 7.6))
        val polygon =
            PolygonOptions()
                .add(point1)
                .addAll(listOf(point2, point3))
                .addHole(hole)
                .strokeColor(Color.Red)
                .strokeWidth(2f)
                .strokePattern(pattern)
                .strokeCap(StrokeCap.Square)
                .strokeJoin(StrokeJoin.Miter)
                .fillColor(Color.Green)
                .geodesic(true)
                .visible(false)
                .clickable(true)
                .zIndex(1f)
                .tag("polygon")
                .build()

        assertEquals(listOf(point1, point2, point3), polygon.points)
        assertEquals(listOf(hole), polygon.holes)
        assertEquals(Color.Red, polygon.strokeColor)
        assertEquals(2f, polygon.strokeWidth)
        assertSame(pattern, polygon.strokePattern)
        assertEquals(StrokeCap.Square, polygon.strokeCap)
        assertEquals(StrokeJoin.Miter, polygon.strokeJoin)
        assertEquals(Color.Green, polygon.fillColor)
        assertTrue(polygon.geodesic)
        assertFalse(polygon.visible)
        assertTrue(polygon.clickable)
        assertEquals(1f, polygon.zIndex)
        assertEquals("polygon", polygon.tag)
    }

    @Test
    fun testCircleOptions() {
        val circle =
            CircleOptions()
                .center(point1)
                .radius(250f)
                .strokeColor(Color.Red)
                .strokeWidth(3f)
                .strokePattern(pattern)
                .strokeCap(StrokeCap.Butt)
                .strokeJoin(StrokeJoin.Bevel)
                .fillColor(Color.Yellow)
                .visible(false)
                .clickable(true)
                .zIndex(5f)
                .tag("circle")
                .build()

        assertEquals(point1, circle.center)
        assertEquals(250f, circle.radius)
        assertEquals(Color.Red, circle.strokeColor)
        assertEquals(3f, circle.strokeWidth)
        assertSame(pattern, circle.strokePattern)
        assertEquals(StrokeCap.Butt, circle.strokeCap)
        assertEquals(StrokeJoin.Bevel, circle.strokeJoin)
        assertEquals(Color.Yellow, circle.fillColor)
        assertFalse(circle.visible)
        assertTrue(circle.clickable)
        assertEquals(5f, circle.zIndex)
        assertEquals("circle", circle.tag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCircleOptionsWithoutCenterThrows() {
        CircleOptions().radius(100f).build()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testCircleOptionsWithoutRadiusThrows() {
        CircleOptions().center(point1).build()
    }

    @Test
    fun testGroundOverlayOptionsWithPosition() {
        val image = BitmapDescriptorFactory.defaultMarker()
        val overlay =
            GroundOverlayOptions()
                .image(image)
                .position(point1, 1000f, 500f)
                .anchor(0f, 1f)
                .bearing(45f)
                .transparency(0.3f)
                .visible(false)
                .zIndex(2f)
                .clickable(true)
                .tag("overlay")
                .build()

        assertEquals(image, overlay.image)
        assertEquals(point1, overlay.position)
        assertEquals(1000f, overlay.width)
        assertEquals(500f, overlay.height)
        assertNull(overlay.bounds)
        assertEquals(Pair(0f, 1f), overlay.anchor)
        assertEquals(45f, overlay.bearing)
        assertEquals(0.3f, overlay.transparency)
        assertFalse(overlay.visible)
        assertEquals(2f, overlay.zIndex)
        assertTrue(overlay.clickable)
        assertEquals("overlay", overlay.tag)
    }

    @Test
    fun testGroundOverlayOptionsBoundsReplacePosition() {
        val bounds = LatLngBounds(point1, point2)
        val overlay =
            GroundOverlayOptions()
                .image(BitmapDescriptorFactory.defaultMarker())
                .position(point1, 1000f)
                .positionFromBounds(bounds)
                .build()

        assertEquals(bounds, overlay.bounds)
        assertNull(overlay.position)
        assertNull(overlay.width)
        assertNull(overlay.height)
    }

    @Test
    fun testGroundOverlayOptionsPositionReplacesBounds() {
        val overlay =
            GroundOverlayOptions()
                .image(BitmapDescriptorFactory.defaultMarker())
                .positionFromBounds(LatLngBounds(point1, point2))
                .position(point3, 800f)
                .build()

        assertEquals(point3, overlay.position)
        assertEquals(800f, overlay.width)
        assertNull(overlay.bounds)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGroundOverlayOptionsWithoutImageThrows() {
        GroundOverlayOptions().position(point1, 1000f).build()
    }

    @Test
    fun testTileOverlayOptions() {
        val provider = TileProvider { _, _, _ -> null }
        val overlay =
            TileOverlayOptions()
                .tileProvider(provider)
                .transparency(0.4f)
                .zIndex(-1f)
                .visible(false)
                .fadeIn(true)
                .tag("tiles")
                .build()

        assertSame(provider, overlay.tileProvider)
        assertEquals(0.4f, overlay.transparency)
        assertEquals(-1f, overlay.zIndex)
        assertFalse(overlay.visible)
        assertTrue(overlay.fadeIn)
        assertEquals("tiles", overlay.tag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTileOverlayOptionsWithoutProviderThrows() {
        TileOverlayOptions().zIndex(1f).build()
    }
}
