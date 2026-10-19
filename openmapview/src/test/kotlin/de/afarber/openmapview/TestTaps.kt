/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.os.Looper
import android.view.ViewConfiguration
import org.robolectric.Shadows.shadowOf
import java.time.Duration

// With zoom gestures, the map reports a tap once no second tap followed within the
// double-tap timeout: runs the main looper past it
internal fun confirmTap() {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getDoubleTapTimeout().toLong()))
}
