/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving camera movement events.
 *
 * Implement this interface and set it using [OpenMapView.setOnCameraMoveListener]
 * to receive callbacks while the camera is moving.
 *
 * This callback is invoked repeatedly during camera movement (gestures, animations).
 */
fun interface OnCameraMoveListener {
    /**
     * Called repeatedly while the camera is moving.
     *
     * This is called frequently during animations and gestures.
     */
    fun onCameraMove()
}
