/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.core.graphics.createBitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Checks the start and end caps of polylines in real pixels, so it runs with native graphics
 * (available from SDK 26). The line is 20 pixels wide and goes from (50, 50) to (150, 50).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class PolylineCapsTest {
    private lateinit var mapView: OpenMapView
    private lateinit var start: LatLng
    private lateinit var end: LatLng

    // Translucent, so a pixel covered twice would be darker
    private val blue = Color(0x800000FF)

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
        mapView.setMapType(MapType.NONE)
        mapView.layout(0, 0, 200, 100)
        mapView.setCenter(LatLng(51.5, 0.0))
        mapView.setZoom(12f)
        start = mapView.getProjection().fromScreenLocation(Point(50, 50))
        end = mapView.getProjection().fromScreenLocation(Point(150, 50))
    }

    private fun draw(polyline: Polyline): Bitmap {
        mapView.addPolyline(polyline)
        val bitmap = createBitmap(200, 100)
        mapView.draw(Canvas(bitmap))
        return bitmap
    }

    // The map paints a background, so a pixel is compared with the background in a corner
    // and with the line at (80, 50), off the tile edge at x 100, where it is covered exactly once
    private fun Bitmap.near(
        x: Int,
        y: Int,
        refX: Int,
        refY: Int,
    ): Boolean {
        val a = getPixel(x, y)
        val b = getPixel(refX, refY)
        return (0..2).all { abs((a shr (it * 8) and 0xFF) - (b shr (it * 8) and 0xFF)) <= 3 }
    }

    private fun assertLine(
        bitmap: Bitmap,
        x: Int,
        y: Int,
    ) {
        assertFalse(bitmap.near(80, 50, 5, 5), "the line differs from the background")
        assertTrue(bitmap.near(x, y, 80, 50), "line at ($x, $y)")
    }

    private fun assertEmpty(
        bitmap: Bitmap,
        x: Int,
        y: Int,
    ) = assertTrue(bitmap.near(x, y, 5, 5), "nothing at ($x, $y)")

    @Test
    fun `different caps are drawn at their own ends`() {
        val bitmap =
            draw(
                Polyline(
                    points = listOf(start, end),
                    strokeColor = blue,
                    strokeWidth = 20f,
                    startCap = StrokeCap.Round,
                    endCap = StrokeCap.Butt,
                ),
            )

        // A half disc of radius 10 before the start, nothing in its corner
        assertLine(bitmap, 43, 50)
        assertEmpty(bitmap, 41, 41)
        // A butt end: nothing after the end
        assertLine(bitmap, 148, 50)
        assertEmpty(bitmap, 153, 50)
    }

    @Test
    fun `a translucent line with different caps is covered once`() {
        val bitmap =
            draw(
                Polyline(
                    points = listOf(start, end),
                    strokeColor = blue,
                    strokeWidth = 20f,
                    startCap = StrokeCap.Square,
                    endCap = StrokeCap.Round,
                ),
            )

        // No darker overlap and no lighter seam where the caps meet the line
        for (x in listOf(42, 48, 49, 50, 51, 55, 120, 145, 149, 150, 151, 155)) assertLine(bitmap, x, 50)
        // The square start fills its corners, the round end doesn't
        assertLine(bitmap, 42, 42)
        assertEmpty(bitmap, 158, 42)
    }

    @Test
    fun `equal caps are drawn at both ends`() {
        val bitmap =
            draw(
                Polyline(
                    points = listOf(start, end),
                    strokeColor = blue,
                    strokeWidth = 20f,
                    startCap = StrokeCap.Square,
                    endCap = StrokeCap.Square,
                ),
            )

        assertLine(bitmap, 42, 42)
        assertLine(bitmap, 158, 58)
    }

    @Test
    fun `a single segment with spans gets both caps`() {
        val bitmap =
            draw(
                Polyline(
                    points = listOf(start, end),
                    strokeWidth = 20f,
                    startCap = StrokeCap.Butt,
                    endCap = StrokeCap.Square,
                    spans = listOf(StyleSpan(blue)),
                ),
            )

        assertEmpty(bitmap, 47, 50)
        assertLine(bitmap, 158, 58)
    }

    @Test
    fun `a repeated last point keeps the direction of the end`() {
        val bitmap = draw(Polyline(points = listOf(start, end, end), strokeColor = blue, strokeWidth = 20f, endCap = StrokeCap.Square))

        assertLine(bitmap, 158, 58)
        assertEmpty(bitmap, 150, 65)
    }
}
