/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri

/**
 * Opens a location in an external map application with automatic fallback to browser.
 *
 * Attempts to open the location using the geo: URI scheme, which is supported by
 * most map applications (Google Maps, OsmAnd, Maps.me, HERE WeGo, Waze, etc.).
 * If no map app is installed, falls back to opening OpenStreetMap.org in the
 * device's browser.
 *
 * The geo: URI format follows RFC 5870 and Android's common intents specification.
 *
 * @param latLng The geographic coordinates to display
 * @param zoom The zoom level (2.0 to 19.0)
 * @param context Android context for launching intents
 * @param label Optional label/title for the location (shown in some apps as pin title)
 * @return true if intent was successfully launched, false if an error occurred
 * @see [Android Common Intents - Maps](https://developer.android.com/guide/components/intents-common#Maps)
 * @see [RFC 5870 - Geo URI](https://datatracker.ietf.org/doc/html/rfc5870)
 */
fun openLocationInExternalApp(
    latLng: LatLng,
    zoom: Float,
    context: Context,
    label: String? = null,
): Boolean =
    try {
        // Clamp zoom to valid range for map applications
        val clampedZoom = zoom.coerceIn(2.0f, 19.0f).toInt()

        // Build geo URI based on whether label is provided
        val geoUri =
            if (label != null) {
                // Format with query parameter shows a labeled pin
                // geo:0,0?q=latitude,longitude(label)
                "geo:0,0?q=${latLng.latitude},${latLng.longitude}($label)".toUri()
            } else {
                // Format with zoom parameter centers the view
                // geo:latitude,longitude?z=zoom
                "geo:${latLng.latitude},${latLng.longitude}?z=$clampedZoom".toUri()
            }

        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)

        // Start the geo intent directly instead of querying the package manager first:
        // since Android 11 such queries need a <queries> manifest entry, without which
        // installed map apps stay invisible and the browser fallback would always be used
        try {
            context.startActivity(geoIntent)
            Log.d("OpenMapView", "Launched geo intent: $geoUri")
        } catch (e: ActivityNotFoundException) {
            // No map app installed: fall back to the browser with OpenStreetMap
            val osmUrl = "https://www.openstreetmap.org/#map=$clampedZoom/${latLng.latitude}/${latLng.longitude}"
            val browserIntent = Intent(Intent.ACTION_VIEW, osmUrl.toUri())
            context.startActivity(browserIntent)
            Log.d("OpenMapView", "Launched browser fallback: $osmUrl")
        }
        true
    } catch (e: Exception) {
        Log.e("OpenMapView", "Failed to open location in external app", e)
        false
    }
