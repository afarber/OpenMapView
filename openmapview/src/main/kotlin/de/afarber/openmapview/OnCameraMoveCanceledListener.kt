/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving camera movement cancellation events.
 *
 * Implement this interface and set it using [OpenMapView.setOnCameraMoveCanceledListener]
 * to receive callbacks when camera movement is interrupted.
 *
 * This is called when an animation is canceled before completion, such as when
 * a new gesture starts during an animation or [OpenMapView.stopAnimation] is called.
 */
fun interface OnCameraMoveCanceledListener {
    /**
     * Called when camera movement is canceled.
     *
     * This occurs when an ongoing animation is interrupted.
     */
    fun onCameraMoveCanceled()
}
