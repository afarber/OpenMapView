/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Layout tests for [AttributionOverlay]. They need real text measurement and line breaking,
 * which the legacy Robolectric graphics mode does not provide, so they run with native
 * graphics (available from SDK 26).
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class AttributionOverlayLayoutTest {
    private lateinit var overlay: AttributionOverlay

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        overlay = AttributionOverlay(context)
    }

    @Test
    fun `short attribution stays on one line inside the view`() {
        val viewWidth = 2000
        val box = overlay.bounds(viewWidth, 600)

        assertEquals(1, overlay.lineCount(viewWidth))
        // In the bottom-left corner
        assertEquals(0, box.left)
        assertTrue(box.right <= viewWidth)
        assertEquals(600, box.bottom)
    }

    @Test
    fun `long attribution wraps instead of starting off screen`() {
        overlay.setAttributionText(HUMANITARIAN_ATTRIBUTION)
        // Half the width the text needs on a single line
        val singleLineWidth = overlay.bounds(10_000, 600).width()
        val viewWidth = singleLineWidth / 2
        val singleLineHeight = overlay.bounds(10_000, 600).height()

        val box = overlay.bounds(viewWidth, 600)

        assertTrue(overlay.lineCount(viewWidth) >= 2)
        assertEquals(0, box.left)
        assertTrue(box.right <= viewWidth, "Box must not end right of the view, was ${box.right}")
        assertTrue(box.height() > singleLineHeight)
    }

    @Test
    fun `handleTouch detects click on the first line of wrapped attribution`() {
        overlay.setAttributionText(HUMANITARIAN_ATTRIBUTION)
        val viewWidth = overlay.bounds(10_000, 600).width() / 2
        val box = overlay.bounds(viewWidth, 600)
        var clicked = false
        overlay.onAttributionClickListener = { clicked = true }

        val result = overlay.handleTouch(box.left + 1f, box.top + 1f, viewWidth, 600)

        assertTrue(result)
        assertTrue(clicked)
    }

    private companion object {
        const val HUMANITARIAN_ATTRIBUTION =
            "© OpenStreetMap contributors. Tiles courtesy of Humanitarian OpenStreetMap Team"
    }
}
