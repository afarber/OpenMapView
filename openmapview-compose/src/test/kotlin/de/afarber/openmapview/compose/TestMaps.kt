/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import org.robolectric.Shadows.shadowOf
import java.time.Duration

// Fills the test window; without a size, AndroidView measures the map as 0x0
internal val FillTestWindow: Modifier =
    Modifier.layout { measurable, constraints ->
        val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, constraints.maxHeight))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

// Taps the map at the screen position of the given location
internal fun OpenMapView.tapAt(location: LatLng) {
    val point = getProjection().toScreenLocation(location)
    for ((action, time) in listOf(MotionEvent.ACTION_DOWN to 0L, MotionEvent.ACTION_UP to 50L)) {
        val event = MotionEvent.obtain(0, time, action, point.x.toFloat(), point.y.toFloat(), 0)
        onTouchEvent(event)
        event.recycle()
    }
    confirmTap()
}

// With zoom gestures, the map reports a tap once no second tap followed within the
// double-tap timeout: runs the main looper past it
internal fun confirmTap() {
    shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getDoubleTapTimeout().toLong()))
}
