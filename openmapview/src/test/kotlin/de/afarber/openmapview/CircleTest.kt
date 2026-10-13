/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CircleTest {
    private val center = LatLng(51.4661, 7.2491)

    @Test
    fun testDefaults() {
        val circle = Circle(center = center, radius = 100f)

        assertEquals(center, circle.center)
        assertEquals(100f, circle.radius)
        assertEquals(Color.Black, circle.strokeColor)
        assertEquals(10f, circle.strokeWidth)
        assertNull(circle.strokePattern)
        assertEquals(StrokeCap.Round, circle.strokeCap)
        assertTrue(circle.visible)
        assertFalse(circle.clickable)
        assertEquals(0f, circle.zIndex)
        assertNull(circle.tag)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroRadiusThrows() {
        Circle(center = center, radius = 0f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeRadiusThrows() {
        Circle(center = center, radius = -5f)
    }

    @Test
    fun testEqualityIsByIdentity() {
        val circle1 = Circle(center = center, radius = 100f)
        val circle2 = Circle(center = center, radius = 100f)

        assertEquals(circle1, circle1)
        assertEquals(circle1.hashCode(), circle1.hashCode())
        assertNotEquals(circle1, circle2)
        assertNotEquals(circle1, "not a circle")
    }
}
