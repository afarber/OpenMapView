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
    fun testMapTypeConstantsAreUnique() {
        val values =
            setOf(
                MapType.NONE,
                MapType.STANDARD,
                MapType.CYCLOSM,
                MapType.CYCLEMAP,
                MapType.TRANSPORT,
                MapType.TRANSPORT_DARK,
                MapType.TRACESTRACK_TOPO,
                MapType.HUMANITARIAN,
                MapType.OPNVKARTE,
                MapType.OPENTOPOMAP,
                MapType.CARTO_LIGHT,
                MapType.CARTO_DARK,
                MapType.CARTO_VOYAGER,
                MapType.STAMEN_TONER,
                MapType.STAMEN_WATERCOLOR,
            )
        assertEquals(15, values.size)
    }


    @Test
    fun testAllMapTypesHaveTileSource() {
        for (type in 0..14) {
            val source = TileSource.fromMapType(type)
            assertNotNull(source)
        }
    }

    @Test
    fun testAllMapTypesHaveDisplayName() {
        for (type in 0..14) {
            val name = TileSource.getMapTypeName(type)
            assertNotNull(name)
            assert(name != "Unknown")
        }
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

    @Test
    fun testSetMapType_OpenTopoMap() {
        mapView.setMapType(MapType.OPENTOPOMAP)
        assertEquals(MapType.OPENTOPOMAP, mapView.getMapType())
    }

    @Test
    fun testSetMapType_CartoLight() {
        mapView.setMapType(MapType.CARTO_LIGHT)
        assertEquals(MapType.CARTO_LIGHT, mapView.getMapType())
    }

    @Test
    fun testSetMapType_CartoDark() {
        mapView.setMapType(MapType.CARTO_DARK)
        assertEquals(MapType.CARTO_DARK, mapView.getMapType())
    }

    @Test
    fun testSetMapType_CartoVoyager() {
        mapView.setMapType(MapType.CARTO_VOYAGER)
        assertEquals(MapType.CARTO_VOYAGER, mapView.getMapType())
    }

    @Test
    fun testSetMapType_StamenToner() {
        mapView.setMapType(MapType.STAMEN_TONER)
        assertEquals(MapType.STAMEN_TONER, mapView.getMapType())
    }

    @Test
    fun testSetMapType_StamenWatercolor() {
        mapView.setMapType(MapType.STAMEN_WATERCOLOR)
        assertEquals(MapType.STAMEN_WATERCOLOR, mapView.getMapType())
    }
}
