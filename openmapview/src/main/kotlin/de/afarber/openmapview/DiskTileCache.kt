/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer

/**
 * Persistent disk cache for map tiles.
 *
 * Stores up to 50MB of tiles in the app's cache directory, one file per tile, with the
 * bytes exactly as the tile server sent them. Tiles persist across app restarts, reducing
 * network usage on subsequent launches. When the size limit is reached, the least recently
 * used tiles are deleted.
 *
 * Tiles older than 7 days count as expired: [get] ignores them, so they are downloaded
 * again, but they stay on disk as a fallback for when the download fails (e.g. offline),
 * see the `allowStale` parameter of [get].
 *
 * Tiles are stored under a [namespace] (e.g. the tile source), so tiles of different
 * sources never replace each other. All instances in a process share the size accounting
 * per directory.
 *
 * The cache stays usable after [clear], and a failing cache only results in cache misses.
 *
 * @param context Context used to locate the app's cache directory
 * @param namespace Prefix for the cache keys, e.g. the name of the tile source
 */
class DiskTileCache internal constructor(
    directory: File,
    namespace: String,
    maxSize: Long,
    private val maxAgeMs: Long,
    private val clock: () -> Long,
    legacyDirectory: File? = null,
) {
    constructor(
        context: Context,
        namespace: String = DEFAULT_NAMESPACE,
    ) : this(
        directory = File(context.cacheDir, DIRECTORY_NAME),
        namespace = namespace,
        maxSize = MAX_SIZE,
        maxAgeMs = MAX_AGE_MS,
        clock = System::currentTimeMillis,
        legacyDirectory = File(context.cacheDir, LEGACY_DIRECTORY_NAME),
    )

    private val store = storeFor(directory, maxSize, legacyDirectory)
    private val namespaceDirectory =
        File(directory, namespace.lowercase().replace(INVALID_NAME_CHARS, "_").take(MAX_NAMESPACE_LENGTH))

    @Volatile
    private var closed = false

    companion object {
        private val TAG = DiskTileCache::class.java.simpleName
        private const val DIRECTORY_NAME = "tiles2"

        // Used by DiskLruCache in versions up to 0.15.0, deleted on first use
        private const val LEGACY_DIRECTORY_NAME = "tiles"
        private const val MAX_SIZE = 50L * 1024 * 1024
        private const val MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
        private const val FILE_SUFFIX = ".tile"
        private const val TEMP_SUFFIX = ".tmp"
        private val INVALID_NAME_CHARS = Regex("[^a-z0-9_-]")
        private const val MAX_NAMESPACE_LENGTH = 32

        // File header: format version (1 byte) and download time in epoch milliseconds (8 bytes)
        private const val FORMAT_VERSION: Byte = 1
        private const val HEADER_SIZE = 9

        /** Namespace used when none is given. */
        const val DEFAULT_NAMESPACE = "default"

        private val stores = HashMap<String, Store>()

        private fun storeFor(
            directory: File,
            maxSize: Long,
            legacyDirectory: File?,
        ): Store =
            synchronized(stores) {
                stores.getOrPut(directory.absolutePath) { Store(directory, maxSize, legacyDirectory) }
            }
    }

    /**
     * Size accounting of one cache directory, shared by all DiskTileCache instances.
     * Callers synchronize on the Store before reading or writing files.
     */
    private class Store(
        val directory: File,
        val maxSize: Long,
        private var legacyDirectory: File?,
    ) {
        // Total size of all tile files, -1 until the directory has been scanned
        private var totalSize = -1L

        /** Scans the directory on first use, and again after it was removed (e.g. app cache cleared). */
        fun size(): Long {
            legacyDirectory?.let {
                it.deleteRecursively()
                legacyDirectory = null
            }
            if (totalSize < 0 || !directory.exists()) {
                totalSize = 0
                directory.walkTopDown().filter { it.isFile }.forEach { file ->
                    if (file.name.endsWith(FILE_SUFFIX)) {
                        totalSize += file.length()
                    } else {
                        // Left over from a write that was interrupted
                        file.delete()
                    }
                }
            }
            return totalSize
        }

        fun add(delta: Long) {
            totalSize = size() + delta
        }

        fun reset() {
            totalSize = 0
        }

        /** Deletes the least recently used tiles until the cache uses at most 90% of [maxSize]. */
        fun trim() {
            if (size() <= maxSize) return
            val target = maxSize / 10 * 9
            val files =
                directory
                    .walkTopDown()
                    .filter { it.isFile && it.name.endsWith(FILE_SUFFIX) }
                    .map { it to it.lastModified() }
                    .sortedBy { it.second }
            for ((file, _) in files) {
                if (totalSize <= target) break
                val length = file.length()
                if (file.delete()) totalSize -= length
            }
        }
    }

    /**
     * Returns the cached tile, decoded with RGB_565, or null if it is missing.
     *
     * @param allowStale Also return a tile older than the maximum age. Use it as a fallback
     *   when downloading a fresh tile failed.
     */
    fun get(
        tile: TileCoordinate,
        allowStale: Boolean = false,
    ): Bitmap? {
        val bytes = getBytes(tile, allowStale) ?: return null
        return decodeTile(bytes, HEADER_SIZE)
    }

    /** Returns the stored file contents (header and tile bytes), or null if missing or expired. */
    internal fun getBytes(
        tile: TileCoordinate,
        allowStale: Boolean = false,
    ): ByteArray? {
        if (closed) return null
        val file = fileFor(tile)
        return synchronized(store) {
            try {
                if (!file.exists()) return null
                val bytes = file.readBytes()
                if (bytes.size <= HEADER_SIZE || bytes[0] != FORMAT_VERSION) {
                    store.add(-file.length())
                    file.delete()
                    return null
                }
                val downloadedAt = ByteBuffer.wrap(bytes, 1, 8).long
                if (!allowStale && clock() - downloadedAt > maxAgeMs) return null
                // Remember the access for LRU trimming, the download time stays in the header
                file.setLastModified(clock())
                bytes
            } catch (e: IOException) {
                Log.w(TAG, "Failed to read tile $tile from disk cache", e)
                null
            }
        }
    }

    /**
     * Stores the tile bytes as sent by the tile server.
     */
    fun put(
        tile: TileCoordinate,
        bytes: ByteArray,
    ) {
        if (closed) return
        val file = fileFor(tile)
        synchronized(store) {
            val temp = File(file.parentFile, file.name + TEMP_SUFFIX)
            try {
                val header =
                    ByteBuffer
                        .allocate(HEADER_SIZE)
                        .put(FORMAT_VERSION)
                        .putLong(clock())
                        .array()
                // Scan before writing: the scan deletes temp files and must not count the new file
                store.size()
                file.parentFile?.mkdirs()
                temp.outputStream().use { output ->
                    output.write(header)
                    output.write(bytes)
                }
                val oldLength = if (file.exists()) file.length() else 0L
                if (!temp.renameTo(file)) throw IOException("Failed to rename $temp")
                file.setLastModified(clock())
                store.add(file.length() - oldLength)
                store.trim()
            } catch (e: IOException) {
                Log.w(TAG, "Failed to write tile $tile to disk cache", e)
                temp.delete()
            }
        }
    }

    /**
     * Stores a decoded tile as PNG. Prefer the [put] overload with the original bytes,
     * which avoids re-encoding.
     */
    fun put(
        tile: TileCoordinate,
        bitmap: Bitmap,
    ) {
        if (closed) return
        val output = ByteArrayOutputStream()
        if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            put(tile, output.toByteArray())
        }
    }

    /**
     * Removes all cached tiles of all namespaces.
     */
    fun clear() {
        if (closed) return
        synchronized(store) {
            if (!store.directory.deleteRecursively()) {
                Log.w(TAG, "Failed to clear disk cache")
            }
            store.reset()
        }
    }

    /**
     * Stops using the cache. Later calls to [get] and [put] are treated as cache misses.
     * Other instances on the same directory keep working.
     */
    fun close() {
        closed = true
    }

    // One file per namespace and tile coordinate, e.g. "standard/10_512_384.tile"
    private fun fileFor(tile: TileCoordinate): File = File(namespaceDirectory, "${tile.zoom}_${tile.x}_${tile.y}$FILE_SUFFIX")
}
