/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TileOverlayTest {
    private val provider = TileProvider { _, _, _ -> null }

    @Test
    fun testDefaults() {
        val overlay = TileOverlay(tileProvider = provider)

        assertSame(provider, overlay.tileProvider)
        assertEquals(0f, overlay.transparency)
        assertEquals(0f, overlay.zIndex)
        assertTrue(overlay.visible)
        assertFalse(overlay.fadeIn)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testTransparencyAboveOneThrows() {
        TileOverlay(tileProvider = provider, transparency = 1.1f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeTransparencyThrows() {
        TileOverlay(tileProvider = provider, transparency = -0.1f)
    }

    @Test
    fun testCopy_ChangesOnlyTheCopy() {
        val overlay = TileOverlay(tileProvider = provider)

        val copy = overlay.copy(transparency = 0.5f, visible = false, zIndex = 3f)

        assertEquals(0.5f, copy.transparency)
        assertFalse(copy.visible)
        assertEquals(3f, copy.zIndex)
        // The original is unchanged
        assertEquals(0f, overlay.transparency)
        assertTrue(overlay.visible)
        assertEquals(0f, overlay.zIndex)
    }

    @Test
    fun testToString() {
        val text = TileOverlay(tileProvider = provider, transparency = 0.25f).toString()

        assertTrue(text.startsWith("TileOverlay(id=tileOverlay_"))
        assertTrue(text.contains("transparency=0.25"))
    }
}
