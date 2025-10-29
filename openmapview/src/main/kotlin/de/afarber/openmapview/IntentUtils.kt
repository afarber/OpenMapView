/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log

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
    zoom: Double,
    context: Context,
    label: String? = null,
): Boolean =
    try {
        // Clamp zoom to valid range for map applications
        val clampedZoom = zoom.coerceIn(2.0, 19.0).toInt()

        // Build geo URI based on whether label is provided
        val geoUri =
            if (label != null) {
                // Format with query parameter shows a labeled pin
                // geo:0,0?q=latitude,longitude(label)
                Uri.parse("geo:0,0?q=${latLng.latitude},${latLng.longitude}($label)")
            } else {
                // Format with zoom parameter centers the view
                // geo:latitude,longitude?z=zoom
                Uri.parse("geo:${latLng.latitude},${latLng.longitude}?z=$clampedZoom")
            }

        val geoIntent = Intent(Intent.ACTION_VIEW, geoUri)

        // Check if any app can handle geo: URI
        if (canHandleIntent(geoIntent, context)) {
            context.startActivity(geoIntent)
            Log.d("OpenMapView", "Launched geo intent: $geoUri")
            true
        } else {
            // Fallback to browser with OpenStreetMap
            val osmUrl = "https://www.openstreetmap.org/#map=$clampedZoom/${latLng.latitude}/${latLng.longitude}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(osmUrl))
            context.startActivity(browserIntent)
            Log.d("OpenMapView", "Launched browser fallback: $osmUrl")
            true
        }
    } catch (e: Exception) {
        Log.e("OpenMapView", "Failed to open location in external app", e)
        false
    }

/**
 * Checks if any app can handle the given intent.
 *
 * Uses PackageManager to query for activities that can handle the intent.
 * Handles API level differences (queryIntentActivities signature change in API 33).
 *
 * @param intent The intent to check
 * @param context Android context
 * @return true if at least one app can handle the intent, false otherwise
 */
private fun canHandleIntent(
    intent: Intent,
    context: Context,
): Boolean {
    val packageManager = context.packageManager
    val activities =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    return activities.isNotEmpty()
}
