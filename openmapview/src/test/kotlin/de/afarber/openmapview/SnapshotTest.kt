/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertNotNull

/**
 * Unit tests for snapshot functionality.
 *
 * These tests verify that the snapshot methods are correctly defined
 * and can be invoked without errors.
 *
 * Note: Full integration testing of snapshot functionality (bitmap creation,
 * view rendering, etc.) should be done with instrumentation tests on a real
 * device or emulator, as Robolectric has limitations with View rendering and
 * the post() mechanism on detached views.
 */
@RunWith(RobolectricTestRunner::class)
class SnapshotTest {
    private lateinit var context: Context
    private lateinit var mapView: OpenMapView

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mapView = OpenMapView(context)
    }

    @Test
    fun testSnapshotMethodExists() {
        // Verify snapshot method can be called
        mapView.snapshot { bitmap ->
            // Callback exists
        }
    }

    @Test
    fun testSnapshotWithBitmapMethodExists() {
        // Verify snapshot method with bitmap parameter can be called
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        mapView.snapshot({ result ->
            // Callback exists
        }, bitmap)
    }

    @Test
    fun testSnapshotReadyCallbackInterface() {
        // Verify SnapshotReadyCallback interface exists and can be instantiated
        val callback =
            SnapshotReadyCallback { bitmap ->
                // Interface works
            }
        assertNotNull(callback)
    }

    @Test
    fun testSnapshotLambdaSyntax() {
        // Verify lambda syntax works (fun interface)
        mapView.snapshot { bitmap ->
            // Lambda syntax works
        }
    }

    @Test
    fun testSnapshotTraditionalSyntax() {
        // Verify traditional object syntax works
        mapView.snapshot(
            object : SnapshotReadyCallback {
                override fun onSnapshotReady(bitmap: Bitmap?) {
                    // Traditional syntax works
                }
            },
        )
    }

    @Test
    fun testSnapshotWithBitmapLambdaSyntax() {
        // Verify lambda syntax works with bitmap parameter
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        mapView.snapshot({ result ->
            // Lambda works
        }, bitmap)
    }

    @Test
    fun testBitmapParameterTypes() {
        // Verify various bitmap configurations work
        val bitmaps =
            listOf(
                Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888),
                Bitmap.createBitmap(200, 200, Bitmap.Config.RGB_565),
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
            )

        for (bitmap in bitmaps) {
            mapView.snapshot({ result ->
                // Each bitmap type works
            }, bitmap)
        }
    }

    @Test
    fun testMultipleSnapshotCallsDontCrash() {
        // Verify multiple calls don't crash
        mapView.snapshot { bitmap1 -> }
        mapView.snapshot { bitmap2 -> }
        mapView.snapshot { bitmap3 -> }
    }
}
