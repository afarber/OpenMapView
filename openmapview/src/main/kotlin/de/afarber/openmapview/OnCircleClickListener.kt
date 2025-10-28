/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Interface for receiving circle click events.
 *
 * Implement this interface and set it using [OpenMapView.setOnCircleClickListener]
 * to receive callbacks when a circle is clicked on the map.
 */
fun interface OnCircleClickListener {
    /**
     * Called when a circle has been clicked.
     *
     * @param circle The circle that was clicked
     */
    fun onCircleClick(circle: Circle)
}
