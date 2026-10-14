/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class TileDownloader {
    companion object {
        private val TAG = TileDownloader::class.java.simpleName
    }

    private val client =
        HttpClient(Android) {
            engine {
                connectTimeout = 10_000
                socketTimeout = 10_000
            }
        }

    suspend fun downloadTile(url: String): Bitmap? = downloadTileBytes(url)?.let { decodeTile(it) }

    /**
     * Downloads a tile and returns its bytes as sent by the server, or null on failure.
     * Used to store tiles on disk without decoding and re-encoding them.
     */
    suspend fun downloadTileBytes(url: String): ByteArray? =
        try {
            val response =
                client.get(url) {
                    header("User-Agent", UserAgent.value)
                }
            if (!response.status.isSuccess()) {
                Log.w(TAG, "Failed to download tile from $url: HTTP ${response.status.value}")
                null
            } else {
                response.readRawBytes()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download tile from $url: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }

    fun close() {
        client.close()
    }
}

/**
 * Decodes tile image bytes with RGB_565 to reduce memory usage
 * (2 bytes per pixel vs 4 bytes for ARGB_8888).
 */
internal fun decodeTile(
    bytes: ByteArray,
    offset: Int = 0,
    length: Int = bytes.size - offset,
): Bitmap? {
    val options =
        BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.RGB_565
            inScaled = false
        }
    return BitmapFactory.decodeByteArray(bytes, offset, length, options)
}
