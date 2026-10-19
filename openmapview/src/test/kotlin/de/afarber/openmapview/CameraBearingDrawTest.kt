/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertNotEquals

/**
 * Checks in real pixels that a turned map still covers the whole view, so it runs with
 * native graphics (available from SDK 26). Without a tile source every visible tile is
 * drawn as a gray placeholder.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class CameraBearingDrawTest {
    @Test
    fun `a turned map covers the corners of the view`() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val mapView = OpenMapView(context)
        mapView.setMapType(MapType.NONE)
        // Wide and flat, so the corners of the turned view reach far beyond the tiles of the
        // unturned view, including their one-tile buffer
        mapView.layout(0, 0, 2000, 200)
        mapView.moveCamera(CameraUpdateFactory.newCameraPosition(CameraPosition(LatLng(51.4661, 7.2491), 12f, 45f)))

        val bitmap = createBitmap(2000, 200)
        mapView.draw(Canvas(bitmap))

        for ((x, y) in listOf(1 to 1, 1998 to 1, 1 to 198, 1998 to 198, 1000 to 100)) {
            assertNotEquals(0, bitmap.getPixel(x, y) ushr 24, "map at ($x, $y)")
        }
    }
}
