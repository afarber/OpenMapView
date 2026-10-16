/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

// A property of a shape: setting a new value checks it and redraws the map that shows the shape
internal class ShapeProperty<T>(
    private var value: T,
    private val check: (T) -> Unit,
    private val redraw: () -> Unit,
) : ReadWriteProperty<Any, T> {
    init {
        check(value)
    }

    override fun getValue(
        thisRef: Any,
        property: KProperty<*>,
    ): T = value

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: T,
    ) {
        // E.g. a recomposition that sets the same value again
        if (value == this.value) return
        check(value)
        this.value = value
        redraw()
    }
}
