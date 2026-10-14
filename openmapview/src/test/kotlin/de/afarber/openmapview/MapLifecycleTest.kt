/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Checks that OpenMapView observes the lifecycle of its view tree while attached to a
 * window and releases its resources when detached.
 */
@RunWith(RobolectricTestRunner::class)
class MapLifecycleTest {
    private lateinit var container: FrameLayout
    private lateinit var lifecycle: LifecycleRegistry
    private lateinit var mapView: OpenMapView

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val owner =
            object : LifecycleOwner {
                override val lifecycle = LifecycleRegistry(this)
            }
        lifecycle = owner.lifecycle
        lifecycle.currentState = Lifecycle.State.RESUMED
        container = FrameLayout(activity)
        container.setViewTreeLifecycleOwner(owner)
        activity.setContentView(container)
        mapView = OpenMapView(activity)
    }

    @Test
    fun testAttachedView_ObservesViewTreeLifecycle() {
        container.addView(mapView)

        assertEquals(1, lifecycle.observerCount)
    }

    @Test
    fun testObserverAddedByApp_IsNotAddedTwice() {
        lifecycle.addObserver(mapView)
        container.addView(mapView)

        assertEquals(1, lifecycle.observerCount)
    }

    @Test
    fun testDetachedView_StopsObservingLifecycle() {
        // Also removes an observer added by the app, so the lifecycle can't keep the view alive
        lifecycle.addObserver(mapView)
        container.addView(mapView)

        container.removeView(mapView)

        assertEquals(0, lifecycle.observerCount)
    }

    @Test
    fun testReattachedView_ObservesLifecycleAgain() {
        container.addView(mapView)
        container.removeView(mapView)

        container.addView(mapView)

        assertEquals(1, lifecycle.observerCount)
    }

    @Test
    fun testDetachedView_CancelsCameraAnimation() {
        var cancels = 0
        mapView.setOnCameraMoveCanceledListener { cancels++ }
        container.addView(mapView)
        mapView.layout(0, 0, 400, 400)
        mapView.animateCamera(CameraUpdateFactory.zoomTo(15f), 10_000)

        container.removeView(mapView)

        assertEquals(1, cancels)
    }

    @Test
    fun testDestroyedLifecycle_ReleasesMapAndOnDestroyCanBeCalledAgain() {
        var cancels = 0
        mapView.setOnCameraMoveCanceledListener { cancels++ }
        container.addView(mapView)
        mapView.layout(0, 0, 400, 400)
        mapView.animateCamera(CameraUpdateFactory.zoomTo(15f), 10_000)

        lifecycle.currentState = Lifecycle.State.DESTROYED
        mapView.onDestroy()

        assertEquals(1, cancels)
    }
}
