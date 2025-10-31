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
        assertEquals(Color.Black, polyline.strokeColor)
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
                strokeColor = Color.Blue,
                strokeWidth = 8f,
                tag = "Route1",
            )

        assertEquals(points, polyline.points)
        assertEquals(Color.Blue, polyline.strokeColor)
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
                strokeColor = Color.Red,
            )
        val polyline2 =
            Polyline(
                points = points,
                strokeColor = Color.Red,
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

        val redLine = Polyline(points = points, strokeColor = Color.Red)
        val blueLine = Polyline(points = points, strokeColor = Color.Blue)
        val customLine = Polyline(points = points, strokeColor = Color(red = 255, green = 0, blue = 0, alpha = 128))

        assertEquals(Color.Red, redLine.strokeColor)
        assertEquals(Color.Blue, blueLine.strokeColor)
        assertEquals(Color(red = 255, green = 0, blue = 0, alpha = 128), customLine.strokeColor)
    }

    @Test
    fun testPolylineVisibility_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)
        assertEquals(true, polyline.visible)
    }

    @Test
    fun testPolylineVisibility_SetToFalse() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points, visible = false)
        assertEquals(false, polyline.visible)
    }

    @Test
    fun testPolylineClickable_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)
        assertEquals(false, polyline.clickable)
    }

    @Test
    fun testPolylineClickable_SetToTrue() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points, clickable = true)
        assertEquals(true, polyline.clickable)
    }

    @Test
    fun testPolylineZIndex_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)
        assertEquals(0f, polyline.zIndex, 0.001f)
    }

    @Test
    fun testPolylineZIndex_Custom() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points, zIndex = 3.5f)
        assertEquals(3.5f, polyline.zIndex, 0.001f)
    }

    @Test
    fun testPolylineStrokePattern_Dashed() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val pattern = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
        val polyline = Polyline(points = points, strokePattern = pattern)

        assertEquals(pattern, polyline.strokePattern)
    }

    @Test
    fun testPolylineStrokePattern_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)

        assertEquals(null, polyline.strokePattern)
    }

    @Test
    fun testPolylineStrokeCap_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)

        assertEquals(StrokeCap.Round, polyline.strokeCap)
    }

    @Test
    fun testPolylineStrokeCap_Custom() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points, strokeCap = StrokeCap.Square)

        assertEquals(StrokeCap.Square, polyline.strokeCap)
    }

    @Test
    fun testPolylineStrokeJoin_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points)

        assertEquals(StrokeJoin.Round, polyline.strokeJoin)
    }

    @Test
    fun testPolylineStrokeJoin_Custom() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val polyline = Polyline(points = points, strokeJoin = StrokeJoin.Miter)

        assertEquals(StrokeJoin.Miter, polyline.strokeJoin)
    }

    @Test
    fun testPolylineWithAllStrokeProperties() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        val pattern = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
        val polyline =
            Polyline(
                points = points,
                strokeColor = Color.Blue,
                strokeWidth = 8f,
                strokePattern = pattern,
                strokeCap = StrokeCap.Butt,
                strokeJoin = StrokeJoin.Bevel,
            )

        assertEquals(Color.Blue, polyline.strokeColor)
        assertEquals(8f, polyline.strokeWidth, 0.001f)
        assertEquals(pattern, polyline.strokePattern)
        assertEquals(StrokeCap.Butt, polyline.strokeCap)
        assertEquals(StrokeJoin.Bevel, polyline.strokeJoin)
    }
}
