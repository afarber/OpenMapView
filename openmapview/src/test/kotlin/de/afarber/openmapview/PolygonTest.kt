/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PolygonTest {
    @Test
    fun testPolygonCreation_MinimalParameters() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points)

        assertEquals(points, polygon.points)
        assertEquals(Color.Black, polygon.strokeColor)
        assertEquals(10f, polygon.strokeWidth, 0.001f)
        assertEquals(Color(red = 128, green = 128, blue = 128, alpha = 128), polygon.fillColor)
        assertTrue(polygon.holes.isEmpty())
        assertNull(polygon.tag)
    }

    @Test
    fun testPolygonCreation_AllParameters() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
                LatLng(51.4640, 7.2420),
            )
        val hole =
            listOf(
                LatLng(51.4665, 7.2495),
                LatLng(51.4670, 7.2500),
                LatLng(51.4665, 7.2505),
            )
        val polygon =
            Polygon(
                points = points,
                strokeColor = Color.Red,
                strokeWidth = 5f,
                fillColor = Color(red = 0, green = 255, blue = 0, alpha = 100),
                holes = listOf(hole),
                tag = "Area1",
            )

        assertEquals(points, polygon.points)
        assertEquals(Color.Red, polygon.strokeColor)
        assertEquals(5f, polygon.strokeWidth, 0.001f)
        assertEquals(Color(red = 0, green = 255, blue = 0, alpha = 100), polygon.fillColor)
        assertEquals(1, polygon.holes.size)
        assertEquals(hole, polygon.holes[0])
        assertEquals("Area1", polygon.tag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPolygonCreation_LessThanThreePoints() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
            )
        Polygon(points = points)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPolygonCreation_EmptyPoints() {
        val points = emptyList<LatLng>()
        Polygon(points = points)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testPolygonCreation_HoleWithLessThanThreePoints() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val invalidHole =
            listOf(
                LatLng(51.4665, 7.2495),
                LatLng(51.4670, 7.2500),
            )
        Polygon(points = points, holes = listOf(invalidHole))
    }

    @Test
    fun testPolygonHasUniqueId() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon1 = Polygon(points = points)
        val polygon2 = Polygon(points = points)

        assertNotNull(polygon1.id)
        assertNotNull(polygon2.id)
        assertNotEquals(polygon1.id, polygon2.id)
    }

    @Test
    fun testPolygonEquality_SameData() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon1 =
            Polygon(
                points = points,
                strokeColor = Color.Blue,
            )
        val polygon2 =
            Polygon(
                points = points,
                strokeColor = Color.Blue,
            )

        // Polygons with same data should NOT be equal because of unique ID
        assertNotEquals(polygon1, polygon2)
        assertNotEquals(polygon1.id, polygon2.id)
    }

    @Test
    fun testPolygonWithMultipleHoles() {
        val points =
            listOf(
                LatLng(51.4600, 7.2400),
                LatLng(51.4750, 7.2400),
                LatLng(51.4750, 7.2600),
                LatLng(51.4600, 7.2600),
            )
        val hole1 =
            listOf(
                LatLng(51.4620, 7.2420),
                LatLng(51.4640, 7.2420),
                LatLng(51.4640, 7.2440),
                LatLng(51.4620, 7.2440),
            )
        val hole2 =
            listOf(
                LatLng(51.4700, 7.2500),
                LatLng(51.4720, 7.2500),
                LatLng(51.4720, 7.2520),
                LatLng(51.4700, 7.2520),
            )
        val polygon =
            Polygon(
                points = points,
                holes = listOf(hole1, hole2),
            )

        assertEquals(2, polygon.holes.size)
        assertEquals(hole1, polygon.holes[0])
        assertEquals(hole2, polygon.holes[1])
    }

    @Test
    fun testPolygonWithTag() {
        data class AreaData(
            val id: Int,
            val name: String,
            val type: String,
        )

        val areaData = AreaData(42, "Park", "Recreation")
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon =
            Polygon(
                points = points,
                tag = areaData,
            )

        assertNotNull(polygon.tag)
        assertEquals(areaData, polygon.tag)
    }

    @Test
    fun testPolygonColors() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )

        val redPolygon =
            Polygon(
                points = points,
                strokeColor = Color.Red,
                fillColor = Color(red = 255, green = 0, blue = 0, alpha = 80),
            )
        val bluePolygon =
            Polygon(
                points = points,
                strokeColor = Color.Blue,
                fillColor = Color(red = 0, green = 0, blue = 255, alpha = 80),
            )

        assertEquals(Color.Red, redPolygon.strokeColor)
        assertEquals(Color(red = 255, green = 0, blue = 0, alpha = 80), redPolygon.fillColor)
        assertEquals(Color.Blue, bluePolygon.strokeColor)
        assertEquals(Color(red = 0, green = 0, blue = 255, alpha = 80), bluePolygon.fillColor)
    }

    @Test
    fun testPolygonWithComplexShape() {
        // Pentagon shape
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4680, 7.2510),
                LatLng(51.4670, 7.2540),
                LatLng(51.4640, 7.2540),
                LatLng(51.4630, 7.2510),
            )
        val polygon = Polygon(points = points)

        assertEquals(5, polygon.points.size)
        assertEquals(points, polygon.points)
    }

    @Test
    fun testPolygonStrokeAndFillWidths() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )

        val thinBorder = Polygon(points = points, strokeWidth = 1f)
        val thickBorder = Polygon(points = points, strokeWidth = 15f)

        assertEquals(1f, thinBorder.strokeWidth, 0.001f)
        assertEquals(15f, thickBorder.strokeWidth, 0.001f)
    }

    @Test
    fun testPolygonVisibility_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points)
        assertEquals(true, polygon.visible)
    }

    @Test
    fun testPolygonVisibility_SetToFalse() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points, visible = false)
        assertEquals(false, polygon.visible)
    }

    @Test
    fun testPolygonClickable_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points)
        assertEquals(false, polygon.clickable)
    }

    @Test
    fun testPolygonClickable_SetToTrue() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points, clickable = true)
        assertEquals(true, polygon.clickable)
    }

    @Test
    fun testPolygonZIndex_Default() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points)
        assertEquals(0f, polygon.zIndex, 0.001f)
    }

    @Test
    fun testPolygonZIndex_Custom() {
        val points =
            listOf(
                LatLng(51.4661, 7.2491),
                LatLng(51.4700, 7.2550),
                LatLng(51.4620, 7.2430),
            )
        val polygon = Polygon(points = points, zIndex = 2.5f)
        assertEquals(2.5f, polygon.zIndex, 0.001f)
    }
}
