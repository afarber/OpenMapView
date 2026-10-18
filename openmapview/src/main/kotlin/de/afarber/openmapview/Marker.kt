/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Bitmap
import java.util.concurrent.atomic.AtomicLong

/**
 * A marker on the map at a specific geographic location.
 *
 * The marker returned by [OpenMapView.addMarker] is a live handle, as in the Maps SDK: setting a
 * property redraws the map right away, and [remove] takes the marker off the map.
 *
 * @param position The geographic location of the marker
 * @param title Optional title text displayed when marker is clicked
 * @param snippet Optional snippet text displayed below the title
 * @param icon Custom icon descriptor. If null, a default red marker icon will be used
 * @param anchor Anchor point for the marker icon. Default (0.5f, 1.0f) means
 *               the marker is centered horizontally and anchored at the bottom
 * @param visible Whether the marker is visible. Default is true
 * @param alpha Opacity of the marker from 0.0 (transparent) to 1.0 (opaque). Default is 1.0
 * @param draggable Whether the marker can be dragged. Default is false
 * @param zIndex Draw order. Markers with higher zIndex are drawn on top. Default is 0.0
 * @param tag Optional user data associated with the marker
 */
class Marker(
    position: LatLng,
    title: String? = null,
    snippet: String? = null,
    icon: BitmapDescriptor? = null,
    anchor: Pair<Float, Float> = Pair(0.5f, 1.0f),
    visible: Boolean = true,
    alpha: Float = 1.0f,
    draggable: Boolean = false,
    zIndex: Float = 0f,
    tag: Any? = null,
) {
    /**
     * Reference to the parent map view.
     * Set when the marker is added to a map, cleared when removed.
     */
    internal var mapView: OpenMapView? = null

    private fun <T> property(
        initial: T,
        check: (T) -> Unit = {},
    ) = ShapeProperty(initial, check) { mapView?.invalidate() }

    /** The geographic location of the marker, also changed while it is dragged */
    var position: LatLng by property(position)

    /** Title text shown in the info window */
    var title: String? by property(title)

    /** Snippet text shown below the title in the info window */
    var snippet: String? by property(snippet)

    /** Custom icon, or null for the default red marker */
    var icon: BitmapDescriptor? by property(icon)

    /** The point of the icon at [position], as fractions of its width and height */
    var anchor: Pair<Float, Float> by property(anchor)

    /** Whether the marker is visible */
    var visible: Boolean by property(visible)

    /** Opacity from 0.0 (transparent) to 1.0 (opaque) */
    var alpha: Float by property(alpha) { require(it in 0.0f..1.0f) { "Alpha must be between 0.0 and 1.0" } }

    /** Whether the marker can be dragged */
    var draggable: Boolean by property(draggable)

    /** Draw order: markers with a higher zIndex are drawn on top */
    var zIndex: Float by property(zIndex)

    /** Optional user data */
    var tag: Any? by property(tag)

    /**
     * The unique id of this marker, e.g. "m0", which stays the same for its lifetime.
     * Useful as a key to find the marker again, e.g. in a listener. [copy] returns a
     * marker with a new id.
     */
    val id: String = "m${nextId.getAndIncrement()}"

    /** Sets the [anchor] from its two fractions, like `setAnchor(float, float)` of the Maps SDK. */
    fun setAnchor(
        anchorU: Float,
        anchorV: Float,
    ) {
        anchor = Pair(anchorU, anchorV)
    }

    /** Removes the marker from the map that shows it, closing its info window. */
    fun remove() {
        mapView?.removeMarker(this)
    }

    /** Returns a new marker with the given properties changed, not added to any map. */
    fun copy(
        position: LatLng = this.position,
        title: String? = this.title,
        snippet: String? = this.snippet,
        icon: BitmapDescriptor? = this.icon,
        anchor: Pair<Float, Float> = this.anchor,
        visible: Boolean = this.visible,
        alpha: Float = this.alpha,
        draggable: Boolean = this.draggable,
        zIndex: Float = this.zIndex,
        tag: Any? = this.tag,
    ): Marker = Marker(position, title, snippet, icon, anchor, visible, alpha, draggable, zIndex, tag)

    override fun toString(): String = "Marker(id=$id, position=$position, title=$title)"

    /**
     * Whether the info window is currently shown for this marker.
     *
     * Use [showInfoWindow] and [hideInfoWindow] to change this state.
     */
    var isInfoWindowShown: Boolean
        get() = _isInfoWindowShown
        private set(value) {
            _isInfoWindowShown = value
        }

    private var _isInfoWindowShown: Boolean = false

    /**
     * Shows the info window for this marker.
     *
     * The info window displays the marker's title and snippet text.
     * If [UiSettings.infoWindowAutoDismiss] is set to a positive duration on the map,
     * the info window will be automatically hidden after that duration.
     *
     * Only one info window can be shown at a time - showing this marker's info window
     * will hide any other currently shown info window.
     */
    fun showInfoWindow() {
        mapView?.showInfoWindow(this) ?: run { _isInfoWindowShown = true }
    }

    /**
     * Hides the info window for this marker.
     *
     * Also cancels any pending auto-dismiss timer.
     */
    fun hideInfoWindow() {
        mapView?.hideInfoWindow(this) ?: run { _isInfoWindowShown = false }
    }

    /**
     * Internal method to set the info window shown state without triggering map updates.
     * Used by OpenMapView to avoid infinite recursion.
     */
    internal fun setInfoWindowShownInternal(shown: Boolean) {
        _isInfoWindowShown = shown
        if (!shown) infoWindowImage = null
    }

    /**
     * The info window drawn from an [InfoWindowAdapter] view while it is shown,
     * or null for the default title and snippet window.
     */
    internal var infoWindowImage: InfoWindowImage? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Marker) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()

    private companion object {
        // Shared by all maps, so ids are unique in the app
        val nextId = AtomicLong()
    }
}

/**
 * An info window view drawn into a bitmap.
 *
 * @property bitmap The drawn view
 * @property framed Whether the default frame is drawn around it, see [InfoWindowAdapter.getInfoContents]
 */
internal class InfoWindowImage(
    val bitmap: Bitmap,
    val framed: Boolean,
)
