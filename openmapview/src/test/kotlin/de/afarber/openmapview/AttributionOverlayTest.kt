/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AttributionOverlayTest {
    private lateinit var context: Context
    private lateinit var overlay: AttributionOverlay

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        overlay = AttributionOverlay(context)
    }

    @Test
    fun `draw renders without crashing`() {
        val canvas = Canvas()
        overlay.draw(canvas, 800, 600)
    }

    @Test
    fun `handleTouch detects click in bottom right corner`() {
        val viewWidth = 800
        val viewHeight = 600

        var clicked = false
        overlay.onAttributionClickListener = {
            clicked = true
        }

        val result = overlay.handleTouch(750f, 580f, viewWidth, viewHeight)

        assertTrue(result, "Touch in attribution area should return true")
        assertTrue(clicked, "Attribution click listener should be invoked")
    }

    @Test
    fun `handleTouch ignores click outside attribution area`() {
        val viewWidth = 800
        val viewHeight = 600

        var clicked = false
        overlay.onAttributionClickListener = {
            clicked = true
        }

        val result = overlay.handleTouch(100f, 100f, viewWidth, viewHeight)

        assertFalse(result, "Touch outside attribution area should return false")
        assertFalse(clicked, "Attribution click listener should not be invoked")
    }

    @Test
    fun `handleTouch works without listener set`() {
        val viewWidth = 800
        val viewHeight = 600

        val result = overlay.handleTouch(750f, 580f, viewWidth, viewHeight)

        assertTrue(result, "Touch in attribution area should return true even without listener")
    }
}
