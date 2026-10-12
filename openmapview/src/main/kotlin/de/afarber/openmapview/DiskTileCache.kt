/*
 * Copyright (c) 2025-2026 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.jakewharton.disklrucache.DiskLruCache
import java.io.File
import java.io.IOException

/**
 * Persistent disk cache for map tiles using DiskLruCache.
 *
 * Stores up to 50MB of tile images in PNG format in the app's cache directory.
 * Tiles persist across app restarts, reducing network usage on subsequent launches.
 * Uses LRU eviction when cache size limit is reached.
 *
 * Tiles are stored under a [namespace] (e.g. the tile source), so tiles of different
 * sources never replace each other. All instances in a process share one DiskLruCache
 * per directory, because DiskLruCache does not support several instances on the same
 * directory.
 *
 * The cache stays usable after [clear], and a closed or failing cache only results
 * in cache misses.
 *
 * @param context Context used to locate the app's cache directory
 * @param namespace Prefix for the cache keys, e.g. the name of the tile source
 */
class DiskTileCache(
    context: Context,
    namespace: String = DEFAULT_NAMESPACE,
) {
    private val store = storeFor(File(context.cacheDir, "tiles"))
    private val keyPrefix = namespace.lowercase().replace(INVALID_KEY_CHARS, "_").take(MAX_NAMESPACE_LENGTH)

    @Volatile
    private var closed = false

    companion object {
        private val TAG = DiskTileCache::class.java.simpleName
        private const val DISK_CACHE_VERSION = 2
        private const val VALUE_COUNT = 1
        private const val MAX_SIZE = 50L * 1024 * 1024
        private val INVALID_KEY_CHARS = Regex("[^a-z0-9_-]")

        // DiskLruCache keys are limited to 64 characters, leave room for the tile coordinates
        private const val MAX_NAMESPACE_LENGTH = 32

        /** Namespace used when none is given. */
        const val DEFAULT_NAMESPACE = "default"

        private val stores = HashMap<String, Store>()

        private fun storeFor(directory: File): Store =
            synchronized(stores) {
                stores.getOrPut(directory.absolutePath) { Store(directory) }
            }
    }

    /**
     * One DiskLruCache per directory, shared by all DiskTileCache instances.
     * Callers synchronize on the Store before using [cache].
     */
    private class Store(
        private val directory: File,
    ) {
        private var diskCache: DiskLruCache? = null

        // Opens the cache on first use, and again after it was deleted or
        // the directory was removed (e.g. when the user clears the app cache)
        val cache: DiskLruCache?
            get() {
                val current = diskCache
                if (current != null && !current.isClosed && directory.exists()) {
                    return current
                }
                diskCache =
                    try {
                        DiskLruCache.open(directory, DISK_CACHE_VERSION, VALUE_COUNT, MAX_SIZE)
                    } catch (e: IOException) {
                        Log.e(TAG, "Failed to initialize disk cache at $directory", e)
                        null
                    }
                return diskCache
            }
    }

    fun get(tile: TileCoordinate): Bitmap? {
        if (closed) return null
        val key = getTileKey(tile)
        return synchronized(store) {
            try {
                store.cache?.get(key)?.use { snapshot ->
                    val inputStream = snapshot.getInputStream(0)
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: IOException) {
                Log.w(TAG, "Failed to read tile $key from disk cache", e)
                null
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Failed to read tile $key from disk cache", e)
                null
            }
        }
    }

    fun put(
        tile: TileCoordinate,
        bitmap: Bitmap,
    ) {
        if (closed) return
        val key = getTileKey(tile)
        synchronized(store) {
            try {
                store.cache?.edit(key)?.let { editor ->
                    try {
                        editor.newOutputStream(0).use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        editor.commit()
                    } catch (e: IOException) {
                        Log.w(TAG, "Failed to write tile $key to disk cache", e)
                        editor.abort()
                    }
                }
            } catch (e: IOException) {
                Log.w(TAG, "Failed to edit tile $key in disk cache", e)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "Failed to edit tile $key in disk cache", e)
            }
        }
    }

    /**
     * Removes all cached tiles of all namespaces. DiskLruCache.delete() also closes
     * the cache, so it is reopened on the next access.
     */
    fun clear() {
        if (closed) return
        synchronized(store) {
            try {
                store.cache?.delete()
            } catch (e: IOException) {
                Log.w(TAG, "Failed to clear disk cache", e)
            }
        }
    }

    /**
     * Stops using the cache. Later calls to [get] and [put] are treated as cache misses.
     * The shared DiskLruCache stays open for other instances; it flushes its journal
     * after every write, so nothing is lost by not closing it.
     */
    fun close() {
        closed = true
    }

    // Unique key per namespace and tile coordinate (e.g. "standard_10_512_384" for zoom 10, x=512, y=384)
    private fun getTileKey(tile: TileCoordinate): String = "${keyPrefix}_${tile.zoom}_${tile.x}_${tile.y}"
}
