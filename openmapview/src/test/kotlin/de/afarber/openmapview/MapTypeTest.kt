/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapTypeTest {
    private lateinit var context: Context
    private lateinit var mapView: OpenMapView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
    }

    @Test
    fun testMapTypeConstants() {
        assertEquals(0, MapType.NONE)
        assertEquals(1, MapType.NORMAL)
        assertEquals(2, MapType.SATELLITE)
        assertEquals(3, MapType.TERRAIN)
        assertEquals(4, MapType.HYBRID)
        assertEquals(5, MapType.HUMANITARIAN)
        assertEquals(6, MapType.TOPO)
        assertEquals(7, MapType.CYCLE)
    }

    @Test
    fun testDefaultMapType() {
        assertEquals(MapType.NORMAL, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Normal() {
        mapView.setMapType(MapType.NORMAL)
        assertEquals(MapType.NORMAL, mapView.getMapType())
    }

    @Test
    fun testSetMapType_None() {
        mapView.setMapType(MapType.NONE)
        assertEquals(MapType.NONE, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Terrain() {
        mapView.setMapType(MapType.TERRAIN)
        assertEquals(MapType.TERRAIN, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Topo() {
        mapView.setMapType(MapType.TOPO)
        assertEquals(MapType.TOPO, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Humanitarian() {
        mapView.setMapType(MapType.HUMANITARIAN)
        assertEquals(MapType.HUMANITARIAN, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Cycle() {
        mapView.setMapType(MapType.CYCLE)
        assertEquals(MapType.CYCLE, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Satellite_ThrowsException() {
        val exception =
            assertThrows(UnsupportedOperationException::class.java) {
                mapView.setMapType(MapType.SATELLITE)
            }
        assertNotNull(exception.message)
        assert(exception.message!!.contains("SATELLITE"))
        assert(exception.message!!.contains("not supported"))
    }

    @Test
    fun testSetMapType_Hybrid_ThrowsException() {
        val exception =
            assertThrows(UnsupportedOperationException::class.java) {
                mapView.setMapType(MapType.HYBRID)
            }
        assertNotNull(exception.message)
        assert(exception.message!!.contains("HYBRID"))
        assert(exception.message!!.contains("not supported"))
    }

    @Test
    fun testSetMapType_InvalidType_ThrowsException() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                mapView.setMapType(99)
            }
        assertNotNull(exception.message)
        assert(exception.message!!.contains("Unknown map type"))
    }

    @Test
    fun testSetMapType_SwitchingTypes() {
        mapView.setMapType(MapType.TERRAIN)
        assertEquals(MapType.TERRAIN, mapView.getMapType())

        mapView.setMapType(MapType.HUMANITARIAN)
        assertEquals(MapType.HUMANITARIAN, mapView.getMapType())

        mapView.setMapType(MapType.NORMAL)
        assertEquals(MapType.NORMAL, mapView.getMapType())
    }
}
