/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving camera idle events.
 *
 * Implement this interface and set it using [OpenMapView.setOnCameraIdleListener]
 * to receive callbacks when the camera stops moving.
 *
 * This is called after all camera movements (gestures, animations) have completed.
 */
fun interface OnCameraIdleListener {
    /**
     * Called when the camera stops moving.
     *
     * This is invoked after animations complete or gestures end.
     */
    fun onCameraIdle()
}
