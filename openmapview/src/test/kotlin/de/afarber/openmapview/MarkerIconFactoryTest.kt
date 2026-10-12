/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkerIconFactoryTest {
    @Test
    fun getDefaultIcon_SameHue_ReturnsCachedBitmap() {
        MarkerIconFactory.clearCache()

        val icon1 = MarkerIconFactory.getDefaultIcon(0f)
        val icon2 = MarkerIconFactory.getDefaultIcon(0f)

        assertSame(icon1, icon2)
        assertFalse(icon1.isRecycled)
    }

    @Test
    fun clearCache_DoesNotRecycleBitmapInUse() {
        MarkerIconFactory.clearCache()

        val icon = MarkerIconFactory.getDefaultIcon(120f)
        MarkerIconFactory.clearCache()

        assertFalse(icon.isRecycled)
    }

    @Test
    fun getDefaultIcon_AfterClearCache_ReturnsFreshBitmap() {
        MarkerIconFactory.clearCache()

        val first = MarkerIconFactory.getDefaultIcon(240f)
        MarkerIconFactory.clearCache()
        val second = MarkerIconFactory.getDefaultIcon(240f)

        assertNotSame(first, second)
        assertFalse(second.isRecycled)
    }
}
