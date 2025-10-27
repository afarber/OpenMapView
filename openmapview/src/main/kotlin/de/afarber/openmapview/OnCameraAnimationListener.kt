/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Listener for camera animation completion events.
 *
 * Provides callbacks when a camera animation finishes normally or is cancelled.
 */
interface OnCameraAnimationListener {
    /**
     * Called when the camera animation completes successfully.
     */
    fun onFinish()

    /**
     * Called when the camera animation is cancelled before completion.
     *
     * This occurs when [OpenMapView.stopAnimation] is called or when
     * a new camera movement is started.
     */
    fun onCancel()
}
