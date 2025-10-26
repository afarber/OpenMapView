/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PolylineTest {
    @Test
    fun testPolylineCreation_MinimalParameters() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)

        assertEquals(points, polyline.points)
        assertEquals(Color.BLACK, polyline.strokeColor)
        assertEquals(10f, polyline.strokeWidth, 0.001f)
        assertNull(polyline.tag)
    }

    @Test
    fun testPolylineCreation_AllParameters() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polyline =
            Polyline(
                points = points,
                strokeColor = Color.BLUE,
                strokeWidth = 8f,
                tag = "Route1",
            )

        assertEquals(points, polyline.points)
        assertEquals(Color.BLUE, polyline.strokeColor)
        assertEquals(8f, polyline.strokeWidth, 0.001f)
        assertEquals("Route1", polyline.tag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPolylineCreation_LessThanTwoPoints() {
        val points = listOf(LatLng(51.4661, 7.2491))
        Polyline(points = points)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPolylineCreation_EmptyPoints() {
        val points = emptyList<LatLng>()
        Polyline(points = points)
    }

    @Test
    fun testPolylineHasUniqueId() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline1 = Polyline(points = points)
        val polyline2 = Polyline(points = points)

        assertNotNull(polyline1.id)
        assertNotNull(polyline2.id)
        assertNotEquals(polyline1.id, polyline2.id)
    }

    @Test
    fun testPolylineEquality_SameData() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline1 =
            Polyline(
                points = points,
                strokeColor = Color.RED,
            )
        val polyline2 =
            Polyline(
                points = points,
                strokeColor = Color.RED,
            )

        // Polylines with same data should NOT be equal because of unique ID
        assertNotEquals(polyline1, polyline2)
        assertNotEquals(polyline1.id, polyline2.id)
    }

    @Test
    fun testPolylineWithManyPoints() {
        val points =
            List(100) { index ->
                LatLng(51.4661 + index * 0.001, 7.2491 + index * 0.001)
            }
        val polyline = Polyline(points = points)

        assertEquals(100, polyline.points.size)
        assertEquals(points, polyline.points)
    }

    @Test
    fun testPolylineWithTag() {
        data class RouteData(
            val id: Int,
            val name: String,
        )

        val routeData = RouteData(1, "Main Route")
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline =
            Polyline(
                points = points,
                tag = routeData,
            )

        assertNotNull(polyline.tag)
        assertEquals(routeData, polyline.tag)
    }

    @Test
    fun testPolylineStrokeWidth() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )

        val thinLine = Polyline(points = points, strokeWidth = 2f)
        val thickLine = Polyline(points = points, strokeWidth = 20f)

        assertEquals(2f, thinLine.strokeWidth, 0.001f)
        assertEquals(20f, thickLine.strokeWidth, 0.001f)
    }

    @Test
    fun testPolylineColors() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )

        val redLine = Polyline(points = points, strokeColor = Color.RED)
        val blueLine = Polyline(points = points, strokeColor = Color.BLUE)
        val customLine = Polyline(points = points, strokeColor = Color.argb(128, 255, 0, 0))

        assertEquals(Color.RED, redLine.strokeColor)
        assertEquals(Color.BLUE, blueLine.strokeColor)
        assertEquals(Color.argb(128, 255, 0, 0), customLine.strokeColor)
    }
}
