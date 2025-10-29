/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapFeaturesTest {
    private lateinit var context: Context
    private lateinit var mapView: OpenMapView

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
    }

    @Test
    fun testIsIndoorEnabled_ReturnsFalse() {
        assertFalse(mapView.isIndoorEnabled())
    }

    @Test
    fun testIsTrafficEnabled_ReturnsFalse() {
        assertFalse(mapView.isTrafficEnabled())
    }

    @Test
    fun testIsBuildingsEnabled_ReturnsTrue() {
        assertTrue(mapView.isBuildingsEnabled())
    }

    @Test
    fun testIsMyLocationEnabled_ReturnsFalse() {
        assertFalse(mapView.isMyLocationEnabled())
    }
}
