/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.DashPathEffect
import android.graphics.Path
import android.graphics.PathDashPathEffect
import android.graphics.PathEffect

/**
 * One element of a stroke pattern, as in the Maps SDK: a [Dot], a [Dash] or a [Gap].
 *
 * A pattern repeats along the stroke, e.g. `pattern(Arrays.asList(new Dash(30f), new Gap(20f)))`.
 */
abstract class PatternItem internal constructor()

/** A dot as wide as the stroke. */
class Dot : PatternItem() {
    override fun equals(other: Any?): Boolean = other is Dot

    override fun hashCode(): Int = Dot::class.hashCode()

    override fun toString(): String = "[Dot]"
}

/**
 * A dash, drawn as a stroke of the given length.
 *
 * @property length the length in pixels
 */
class Dash(
    @JvmField val length: Float,
) : PatternItem() {
    override fun equals(other: Any?): Boolean = other is Dash && other.length == length

    override fun hashCode(): Int = length.hashCode()

    override fun toString(): String = "[Dash: length=$length]"
}

/**
 * A gap, where nothing is drawn.
 *
 * @property length the length in pixels
 */
class Gap(
    @JvmField val length: Float,
) : PatternItem() {
    override fun equals(other: Any?): Boolean = other is Gap && other.length == length

    override fun hashCode(): Int = 31 + length.hashCode()

    override fun toString(): String = "[Gap: length=$length]"
}

/**
 * Converts a stroke pattern to a path effect for a stroke of [strokeWidth] pixels,
 * or returns null if the pattern draws a solid line.
 *
 * Dashes and gaps only become a [DashPathEffect]; a pattern with dots stamps one
 * period of dashes and dots along the stroke with a [PathDashPathEffect].
 */
internal fun List<PatternItem>.toAndroidPathEffect(strokeWidth: Float): PathEffect? {
    // Merge neighbors of the same kind, dots excepted, and drop empty dashes and gaps
    val items = mutableListOf<PatternItem>()
    for (item in this) {
        val last = items.lastOrNull()
        when {
            item is Dash && item.length <= 0f -> {}

            item is Gap && item.length <= 0f -> {}

            item is Dash && last is Dash -> {
                items[items.lastIndex] = Dash(last.length + item.length)
            }

            item is Gap && last is Gap -> {
                items[items.lastIndex] = Gap(last.length + item.length)
            }

            else -> {
                items.add(item)
            }
        }
    }
    if (items.isEmpty() || items.all { it is Dash }) return null

    return if (items.any { it is Dot }) {
        stampedPathEffect(items, strokeWidth)
    } else {
        dashPathEffect(items)
    }
}

private fun dashPathEffect(items: List<PatternItem>): PathEffect {
    val lengths = items.map { if (it is Dash) it.length else (it as Gap).length }.toMutableList()
    // Where the pattern starts within the intervals given to DashPathEffect
    var phase = 0f

    // The pattern repeats, so a last item of the same kind as the first joins it
    if (lengths.size > 1 && items.first()::class == items.last()::class) {
        phase = lengths.removeAt(lengths.lastIndex)
        lengths[0] += phase
    }
    // DashPathEffect starts with a dash: move a leading gap to the end
    if (items.first() is Gap) {
        if (lengths.size == 1) return DashPathEffect(floatArrayOf(0f, lengths[0]), 0f)
        val gap = lengths.removeAt(0)
        lengths.add(gap)
        phase += lengths.sum() - gap
    }
    return DashPathEffect(lengths.toFloatArray(), phase % lengths.sum())
}

private fun stampedPathEffect(
    items: List<PatternItem>,
    strokeWidth: Float,
): PathEffect? {
    val width = strokeWidth.coerceAtLeast(1f)
    val half = width / 2
    val shape = Path()
    var x = 0f
    for (item in items) {
        when (item) {
            is Dot -> {
                shape.addCircle(x + half, 0f, half, Path.Direction.CW)
                x += width
            }

            is Dash -> {
                shape.addRect(x, -half, x + item.length, half, Path.Direction.CW)
                x += item.length
            }

            is Gap -> {
                x += item.length
            }
        }
    }
    return PathDashPathEffect(shape, x, 0f, PathDashPathEffect.Style.MORPH)
}
