/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Checks that the map never draws outside its own bounds, even when the parent does not
 * clip it. Needs real pixels, so it runs with native graphics (available from SDK 26).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class MapClippingTest {
    @Test
    fun `shapes larger than the view are clipped to its bounds`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val center = LatLng(51.5074, -0.1278)
        val mapView = OpenMapView(context)
        mapView.setMapType(MapType.NONE)
        mapView.layout(0, 0, 100, 100)
        mapView.setCenter(center)
        mapView.setZoom(12f)
        mapView.addPolygon(
            Polygon(
                points =
                    listOf(
                        LatLng(center.latitude - 1, center.longitude - 1),
                        LatLng(center.latitude - 1, center.longitude + 1),
                        LatLng(center.latitude + 1, center.longitude + 1),
                        LatLng(center.latitude + 1, center.longitude - 1),
                    ),
                fillColor = Color.Red,
                strokeColor = Color.Red,
            ),
        )

        // A canvas larger than the view, like a parent that does not clip its children
        val bitmap = createBitmap(200, 200)
        mapView.draw(Canvas(bitmap))

        assertNotEquals(0, bitmap.getPixel(50, 50), "polygon inside the view")
        assertEquals(0, bitmap.getPixel(150, 150), "pixel outside the view")
    }
}
