/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages API keys for third-party tile providers.
 *
 * Some map types (CYCLEMAP, TRANSPORT, TRANSPORT_DARK, TRACESTRACK_TOPO, CARTO_LIGHT,
 * CARTO_DARK, CARTO_VOYAGER, STAMEN_TONER, STAMEN_WATERCOLOR) and the Stamen overlays in [PredefinedTileProviders] require API keys
 * from tile providers. Keys can be configured in two ways:
 *
 * **1. AndroidManifest.xml** (Recommended):
 * ```xml
 * <application>
 *     <meta-data
 *         android:name="de.afarber.openmapview.THUNDERFOREST_API_KEY"
 *         android:value="your_thunderforest_key_here"/>
 *     <meta-data
 *         android:name="de.afarber.openmapview.TRACESTRACK_API_KEY"
 *         android:value="your_tracestrack_key_here"/>
 *     <meta-data
 *         android:name="de.afarber.openmapview.CARTO_API_KEY"
 *         android:value="your_carto_key_here"/>
 *     <meta-data
 *         android:name="de.afarber.openmapview.STADIA_API_KEY"
 *         android:value="your_stadia_key_here"/>
 * </application>
 * ```
 *
 * **2. Programmatically** (Runtime):
 * ```kotlin
 * ApiKeyManager.setApiKey("thunderforest", "your_key_here")
 * ApiKeyManager.setApiKey("tracestrack", "your_key_here")
 * ApiKeyManager.setApiKey("carto", "your_key_here")
 * ApiKeyManager.setApiKey("stadia", "your_key_here")
 * ```
 *
 * API keys set programmatically override keys from the manifest.
 *
 * ## Provider Names
 * - `"thunderforest"` - For CYCLEMAP, TRANSPORT, and TRANSPORT_DARK map types
 * - `"tracestrack"` - For TRACESTRACK_TOPO map type
 * - `"carto"` - For CARTO_LIGHT, CARTO_DARK and CARTO_VOYAGER
 * - `"stadia"` - For STAMEN_TONER, STAMEN_WATERCOLOR and the Stamen overlays
 *
 * ## Obtaining API Keys
 * - **Thunderforest**: https://www.thunderforest.com/pricing/ (Free: 150k tiles/month)
 * - **Tracestrack**: https://www.tracestrack.com/en/signup (Free tier available)
 * - **CARTO**: https://carto.com/basemaps/apikey (Free: 5M tiles/month non-commercial, 1M commercial)
 * - **Stadia Maps**: https://client.stadiamaps.com/signup/ (Free tier for non-commercial use)
 *
 * @see TileSource
 * @see MapType
 */
object ApiKeyManager {
    private const val META_DATA_PREFIX = "de.afarber.openmapview."
    private const val THUNDERFOREST_KEY_NAME = "THUNDERFOREST_API_KEY"
    private const val TRACESTRACK_KEY_NAME = "TRACESTRACK_API_KEY"
    private const val CARTO_KEY_NAME = "CARTO_API_KEY"
    private const val STADIA_KEY_NAME = "STADIA_API_KEY"

    /**
     * Thread-safe storage for API keys set programmatically.
     * Keys set here override keys from AndroidManifest.xml.
     */
    private val programmaticKeys = ConcurrentHashMap<String, String>()

    /**
     * Cached manifest metadata for quick access.
     * Lazy-loaded on first access.
     */
    private var manifestMetadata: Bundle? = null

    /**
     * Whether the manifest metadata has been loaded. The context is only used once
     * for that and is not kept, so this singleton never holds on to a Context.
     */
    @Volatile
    private var initialized = false

    /**
     * Initializes the ApiKeyManager by reading API keys from the app's manifest.
     * Called automatically by OpenMapView on first use.
     *
     * @param appContext Any context of the app
     */
    @Synchronized
    internal fun initialize(appContext: Context) {
        if (!initialized) {
            manifestMetadata = loadManifestMetadata(appContext.applicationContext)
            initialized = true
        }
    }

    /**
     * Loads API keys from AndroidManifest.xml meta-data.
     */
    private fun loadManifestMetadata(context: Context): Bundle? =
        try {
            context.packageManager
                .getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
                .metaData
        } catch (e: PackageManager.NameNotFoundException) {
            // Should never happen, but handle gracefully
            Bundle()
        }

    /**
     * Sets an API key programmatically for the given provider.
     *
     * Keys set this way override keys from AndroidManifest.xml.
     *
     * @param provider Provider name ("thunderforest", "tracestrack", "carto" or "stadia")
     * @param key API key string
     */
    @Synchronized
    fun setApiKey(
        provider: String,
        key: String,
    ) {
        programmaticKeys[provider.lowercase()] = key
    }

    /**
     * Retrieves the API key for the given provider.
     *
     * Checks programmatic keys first, then falls back to manifest meta-data.
     *
     * @param provider Provider name ("thunderforest", "tracestrack", "carto" or "stadia")
     * @return API key string, or null if not configured
     */
    @Synchronized
    fun getApiKey(provider: String): String? {
        val normalizedProvider = provider.lowercase()

        // Check programmatic keys first
        programmaticKeys[normalizedProvider]?.let { return it }

        // Fall back to manifest meta-data
        val metaDataKey =
            when (normalizedProvider) {
                "thunderforest" -> META_DATA_PREFIX + THUNDERFOREST_KEY_NAME
                "tracestrack" -> META_DATA_PREFIX + TRACESTRACK_KEY_NAME
                "carto" -> META_DATA_PREFIX + CARTO_KEY_NAME
                "stadia" -> META_DATA_PREFIX + STADIA_KEY_NAME
                else -> null
            }

        return metaDataKey?.let { manifestMetadata?.getString(it) }
    }

    /**
     * Checks if an API key is configured for the given provider.
     *
     * @param provider Provider name ("thunderforest", "tracestrack", "carto" or "stadia")
     * @return true if a key is available, false otherwise
     */
    fun hasApiKey(provider: String): Boolean = !getApiKey(provider).isNullOrEmpty()

    /**
     * Clears all programmatically-set API keys.
     * Keys from AndroidManifest.xml are not affected.
     *
     * Useful for testing.
     */
    @Synchronized
    fun clearProgrammaticKeys() {
        programmaticKeys.clear()
    }
}
