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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserAgentTest {
    @Test
    fun testBuild_WithPackageName_IdentifiesApp() {
        assertEquals(
            "OpenMapView/$LIBRARY_VERSION (com.example.app; https://github.com/afarber/OpenMapView)",
            UserAgent.build("com.example.app"),
        )
    }

    @Test
    fun testBuild_WithoutPackageName_IdentifiesLibraryOnly() {
        assertEquals("OpenMapView/$LIBRARY_VERSION (https://github.com/afarber/OpenMapView)", UserAgent.build(null))
    }

    @Test
    fun testMapController_SetsPackageName() {
        val context: Context = ApplicationProvider.getApplicationContext()

        MapController(context)

        assertEquals(UserAgent.build(context.packageName), UserAgent.value)
    }
}
