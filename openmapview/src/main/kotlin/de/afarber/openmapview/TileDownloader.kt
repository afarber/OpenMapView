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

    suspend fun downloadTile(url: String): Bitmap? =
        try {
            val response =
                client.get(url) {
                    header("User-Agent", "OpenMapView/0.12.0 (https://github.com/afarber/OpenMapView)")
                }
            val bytes = response.readRawBytes()
            // Decode with RGB_565 to reduce memory usage (2 bytes per pixel vs 4 bytes for ARGB_8888)
            val options =
                BitmapFactory.Options().apply {
                    inPreferredConfig = Bitmap.Config.RGB_565
                    inScaled = false
                }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download tile from $url: ${e.javaClass.simpleName}: ${e.message}", e)
            null
        }

    fun close() {
        client.close()
    }
}
