/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * The default [LocationSource]: GPS and network locations from the platform
 * [LocationManager], so no Google Play Services are needed.
 */
internal class PlatformLocationSource(
    context: Context,
) : LocationSource {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager?
    private var locationListener: LocationListener? = null

    // Lint does not see the hasLocationPermission() check, and a permission revoked
    // meanwhile is caught as SecurityException
    @SuppressLint("MissingPermission")
    override fun activate(listener: LocationSource.OnLocationChangedListener) {
        deactivate()
        val manager = locationManager ?: return
        if (!hasLocationPermission(appContext)) return

        val updates = LocationListener { listener.onLocationChanged(it) }
        locationListener = updates
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { manager.isProviderEnabled(it) }
            // Start with the most recent known location, so the dot appears right away
            providers
                .mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
                ?.let { listener.onLocationChanged(it) }
            for (provider in providers) {
                manager.requestLocationUpdates(provider, UPDATE_INTERVAL_MS, 0f, updates, Looper.getMainLooper())
            }
        } catch (_: SecurityException) {
            // The permission was revoked meanwhile
            deactivate()
        }
    }

    override fun deactivate() {
        locationListener?.let { locationManager?.removeUpdates(it) }
        locationListener = null
    }

    companion object {
        private const val UPDATE_INTERVAL_MS = 1000L

        /** Whether the app may read the fine or coarse location. */
        fun hasLocationPermission(context: Context): Boolean =
            listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION).any {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
    }
}
