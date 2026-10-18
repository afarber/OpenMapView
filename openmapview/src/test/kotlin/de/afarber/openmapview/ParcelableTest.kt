/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.os.Bundle
import android.os.Parcel
import androidx.core.os.BundleCompat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests that the immutable values survive a [Bundle], e.g. in onSaveInstanceState(), an
 * Intent extra or rememberSaveable.
 */
@RunWith(RobolectricTestRunner::class)
class ParcelableTest {
    private val bochum = LatLng(51.4661, 7.2491)
    private val dortmund = LatLng(51.5136, 7.4653)

    // Writes the bundle to a parcel and reads it back, as Android does when it saves state
    private fun marshalled(bundle: Bundle): Bundle {
        val parcel = Parcel.obtain()
        try {
            bundle.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            return Bundle.CREATOR.createFromParcel(parcel).apply { classLoader = LatLng::class.java.classLoader }
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun testLatLng_SurvivesBundle() {
        // A normalized longitude stays normalized
        val wrapped = LatLng(10.0, 190.0)
        val bundle =
            Bundle().apply {
                putParcelable("bochum", bochum)
                putParcelable("wrapped", wrapped)
            }

        val restored = marshalled(bundle)

        assertEquals(bochum, BundleCompat.getParcelable(restored, "bochum", LatLng::class.java))
        assertEquals(LatLng(10.0, -170.0), BundleCompat.getParcelable(restored, "wrapped", LatLng::class.java))
    }

    @Test
    fun testLatLngList_SurvivesBundle() {
        val route = arrayListOf(bochum, dortmund)

        val restored = marshalled(Bundle().apply { putParcelableArrayList("route", route) })

        assertEquals(route, BundleCompat.getParcelableArrayList(restored, "route", LatLng::class.java))
    }

    @Test
    fun testLatLngBounds_SurvivesBundle() {
        val bounds = LatLngBounds(bochum, dortmund)

        val restored = marshalled(Bundle().apply { putParcelable("bounds", bounds) })

        assertEquals(bounds, BundleCompat.getParcelable(restored, "bounds", LatLngBounds::class.java))
    }

    @Test
    fun testCameraPosition_SurvivesBundle() {
        val position = CameraPosition(bochum, 12.5f)

        val restored = marshalled(Bundle().apply { putParcelable("camera", position) })

        assertEquals(position, BundleCompat.getParcelable(restored, "camera", CameraPosition::class.java))
    }
}
