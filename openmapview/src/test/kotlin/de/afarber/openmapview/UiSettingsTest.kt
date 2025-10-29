/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiSettingsTest {
    @Test
    fun testDefaultZoomGesturesEnabled() {
        val uiSettings = UiSettings()
        assertTrue(uiSettings.isZoomGesturesEnabled)
    }

    @Test
    fun testDefaultScrollGesturesEnabled() {
        val uiSettings = UiSettings()
        assertTrue(uiSettings.isScrollGesturesEnabled)
    }

    @Test
    fun testRotateGesturesNotImplemented() {
        val uiSettings = UiSettings()
        assertFalse(uiSettings.isRotateGesturesEnabled)
    }

    @Test
    fun testTiltGesturesNotImplemented() {
        val uiSettings = UiSettings()
        assertFalse(uiSettings.isTiltGesturesEnabled)
    }

    @Test
    fun testSetZoomGesturesEnabled() {
        val uiSettings = UiSettings()
        uiSettings.isZoomGesturesEnabled = false
        assertFalse(uiSettings.isZoomGesturesEnabled)

        uiSettings.isZoomGesturesEnabled = true
        assertTrue(uiSettings.isZoomGesturesEnabled)
    }

    @Test
    fun testSetScrollGesturesEnabled() {
        val uiSettings = UiSettings()
        uiSettings.isScrollGesturesEnabled = false
        assertFalse(uiSettings.isScrollGesturesEnabled)

        uiSettings.isScrollGesturesEnabled = true
        assertTrue(uiSettings.isScrollGesturesEnabled)
    }

    @Test
    fun testSetAllGesturesEnabled_True() {
        val uiSettings = UiSettings()
        uiSettings.isZoomGesturesEnabled = false
        uiSettings.isScrollGesturesEnabled = false

        uiSettings.setAllGesturesEnabled(true)

        assertTrue(uiSettings.isZoomGesturesEnabled)
        assertTrue(uiSettings.isScrollGesturesEnabled)
    }

    @Test
    fun testSetAllGesturesEnabled_False() {
        val uiSettings = UiSettings()
        assertTrue(uiSettings.isZoomGesturesEnabled)
        assertTrue(uiSettings.isScrollGesturesEnabled)

        uiSettings.setAllGesturesEnabled(false)

        assertFalse(uiSettings.isZoomGesturesEnabled)
        assertFalse(uiSettings.isScrollGesturesEnabled)
    }

    @Test
    fun testDefaultZoomControlsDisabled() {
        val uiSettings = UiSettings()
        assertFalse(uiSettings.isZoomControlsEnabled)
    }

    @Test
    fun testZoomControlsEnabledProperty() {
        val uiSettings = UiSettings()
        uiSettings.isZoomControlsEnabled = true
        assertTrue(uiSettings.isZoomControlsEnabled)

        uiSettings.isZoomControlsEnabled = false
        assertFalse(uiSettings.isZoomControlsEnabled)
    }

    @Test
    fun testDefaultScrollGesturesEnabledDuringRotateOrZoom() {
        val uiSettings = UiSettings()
        assertTrue(uiSettings.isScrollGesturesEnabledDuringRotateOrZoom)
    }

    @Test
    fun testScrollGesturesEnabledDuringRotateOrZoomProperty() {
        val uiSettings = UiSettings()
        uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = false
        assertFalse(uiSettings.isScrollGesturesEnabledDuringRotateOrZoom)

        uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = true
        assertTrue(uiSettings.isScrollGesturesEnabledDuringRotateOrZoom)
    }

    @Test
    fun testSetAllGesturesEnabled_IncludesScrollDuringZoom() {
        val uiSettings = UiSettings()
        uiSettings.isScrollGesturesEnabledDuringRotateOrZoom = false

        uiSettings.setAllGesturesEnabled(true)

        assertTrue(uiSettings.isZoomGesturesEnabled)
        assertTrue(uiSettings.isScrollGesturesEnabled)
        assertTrue(uiSettings.isScrollGesturesEnabledDuringRotateOrZoom)
    }

    @Test
    fun testSetAllGesturesEnabled_False_IncludesScrollDuringZoom() {
        val uiSettings = UiSettings()

        uiSettings.setAllGesturesEnabled(false)

        assertFalse(uiSettings.isZoomGesturesEnabled)
        assertFalse(uiSettings.isScrollGesturesEnabled)
        assertFalse(uiSettings.isScrollGesturesEnabledDuringRotateOrZoom)
    }

    @Test
    fun testSetAllGesturesEnabled_DoesNotAffectZoomControls() {
        val uiSettings = UiSettings()
        uiSettings.isZoomControlsEnabled = true

        uiSettings.setAllGesturesEnabled(false)

        // Zoom controls visibility should not be affected by setAllGesturesEnabled
        assertTrue(uiSettings.isZoomControlsEnabled)
    }
}
