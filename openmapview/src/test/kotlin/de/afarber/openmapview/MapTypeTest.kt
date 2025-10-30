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
        assertEquals(1, MapType.STANDARD)
        assertEquals(2, MapType.CYCLOSM)
        assertEquals(3, MapType.CYCLEMAP)
        assertEquals(4, MapType.TRANSPORT)
        assertEquals(5, MapType.TRANSPORT_DARK)
        assertEquals(6, MapType.TRACESTRACK_TOPO)
        assertEquals(7, MapType.HUMANITARIAN)
        assertEquals(8, MapType.OPNVKARTE)
    }

    @Test
    fun testDefaultMapType() {
        assertEquals(MapType.STANDARD, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Standard() {
        mapView.setMapType(MapType.STANDARD)
        assertEquals(MapType.STANDARD, mapView.getMapType())
    }

    @Test
    fun testSetMapType_None() {
        mapView.setMapType(MapType.NONE)
        assertEquals(MapType.NONE, mapView.getMapType())
    }

    @Test
    fun testSetMapType_CyclOSM() {
        mapView.setMapType(MapType.CYCLOSM)
        assertEquals(MapType.CYCLOSM, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Humanitarian() {
        mapView.setMapType(MapType.HUMANITARIAN)
        assertEquals(MapType.HUMANITARIAN, mapView.getMapType())
    }

    @Test
    fun testSetMapType_Transport() {
        mapView.setMapType(MapType.TRANSPORT)
        assertEquals(MapType.TRANSPORT, mapView.getMapType())
    }

    @Test
    fun testSetMapType_TransportDark() {
        mapView.setMapType(MapType.TRANSPORT_DARK)
        assertEquals(MapType.TRANSPORT_DARK, mapView.getMapType())
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
        mapView.setMapType(MapType.CYCLOSM)
        assertEquals(MapType.CYCLOSM, mapView.getMapType())

        mapView.setMapType(MapType.HUMANITARIAN)
        assertEquals(MapType.HUMANITARIAN, mapView.getMapType())

        mapView.setMapType(MapType.STANDARD)
        assertEquals(MapType.STANDARD, mapView.getMapType())
    }
}
