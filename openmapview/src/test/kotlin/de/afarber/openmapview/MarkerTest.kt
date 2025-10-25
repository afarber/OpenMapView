/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class MarkerTest {
    @Test
    fun testMarkerCreation_MinimalParameters() {
        val position = LatLng(51.4661, 7.2491)
        val marker = Marker(position = position)

        assertEquals(position, marker.position)
        assertNull(marker.title)
        assertNull(marker.snippet)
        assertNull(marker.icon)
        assertNull(marker.tag)
        assertEquals(Pair(0.5f, 1.0f), marker.anchor)
    }

    @Test
    fun testMarkerCreation_AllParameters() {
        val position = LatLng(51.4661, 7.2491)
        val marker =
            Marker(
                position = position,
                title = "Test Title",
                snippet = "Test Snippet",
                anchor = Pair(0.0f, 0.0f),
                tag = "CustomTag",
            )

        assertEquals(position, marker.position)
        assertEquals("Test Title", marker.title)
        assertEquals("Test Snippet", marker.snippet)
        assertEquals(Pair(0.0f, 0.0f), marker.anchor)
        assertEquals("CustomTag", marker.tag)
    }

    @Test
    fun testMarkerEquality_SameData() {
        val position = LatLng(51.4661, 7.2491)
        val marker1 =
            Marker(
                position = position,
                title = "Title",
            )
        val marker2 =
            Marker(
                position = position,
                title = "Title",
            )

        // Data classes with same data should NOT be equal because of unique ID
        // The ID is generated per instance
        assertNotEquals(marker1, marker2)
        assertNotEquals(marker1.id, marker2.id)
    }

    @Test
    fun testMarkerHasUniqueId() {
        val position = LatLng(51.4661, 7.2491)
        val marker1 = Marker(position = position)
        val marker2 = Marker(position = position)

        assertNotNull(marker1.id)
        assertNotNull(marker2.id)
        assertNotEquals(marker1.id, marker2.id)
    }

    @Test
    fun testAnchorPoint_Default() {
        val marker = Marker(position = LatLng(0.0, 0.0))
        assertEquals(0.5f, marker.anchor.first, 0.001f)
        assertEquals(1.0f, marker.anchor.second, 0.001f)
    }

    @Test
    fun testAnchorPoint_Custom() {
        val marker =
            Marker(
                position = LatLng(0.0, 0.0),
                anchor = Pair(0.25f, 0.75f),
            )
        assertEquals(0.25f, marker.anchor.first, 0.001f)
        assertEquals(0.75f, marker.anchor.second, 0.001f)
    }

    @Test
    fun testMarkerWithTag() {
        data class CustomData(
            val id: Int,
            val name: String,
        )

        val customData = CustomData(42, "Answer")
        val marker =
            Marker(
                position = LatLng(0.0, 0.0),
                tag = customData,
            )

        assertNotNull(marker.tag)
        assertEquals(customData, marker.tag)
    }

    @Test
    fun testLatLngInMarker() {
        val latitude = 52.52
        val longitude = 13.405
        val marker = Marker(position = LatLng(latitude, longitude))

        assertEquals(latitude, marker.position.latitude, 0.0001)
        assertEquals(longitude, marker.position.longitude, 0.0001)
    }
}
