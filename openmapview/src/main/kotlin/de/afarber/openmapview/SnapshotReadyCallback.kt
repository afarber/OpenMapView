/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap

/**
 * Callback interface for receiving map snapshots.
 *
 * This callback is invoked when a snapshot of the map has been captured.
 * The callback is always invoked on the Android UI thread.
 *
 * Example usage:
 * ```kotlin
 * // Lambda syntax (fun interface benefit)
 * mapView.snapshot { bitmap ->
 *     if (bitmap != null) {
 *         // Save or display the bitmap
 *         imageView.setImageBitmap(bitmap)
 *     } else {
 *         // Snapshot failed (view not laid out)
 *     }
 * }
 *
 * // Traditional syntax
 * mapView.snapshot(object : SnapshotReadyCallback {
 *     override fun onSnapshotReady(bitmap: Bitmap?) {
 *         // Handle bitmap
 *     }
 * })
 * ```
 *
 * @see OpenMapView.snapshot
 */
fun interface SnapshotReadyCallback {
    /**
     * Called when the snapshot is ready.
     *
     * This method is invoked on the Android UI thread after the map snapshot
     * has been captured.
     *
     * @param bitmap A [Bitmap] containing the map snapshot, or null if the
     *               snapshot could not be taken (e.g., if the view has not been
     *               laid out yet or has zero width/height).
     */
    fun onSnapshotReady(bitmap: Bitmap?)
}
