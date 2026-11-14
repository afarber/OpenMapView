/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

/**
 * The shape of a line end, as in the Maps SDK: [ButtCap], [RoundCap] or [SquareCap].
 *
 * Java code passes these to e.g. `PolylineOptions.startCap(new RoundCap())`; Kotlin code can
 * also set the Compose [StrokeCap] properties directly.
 */
public abstract class Cap internal constructor() {
    override fun equals(other: Any?): Boolean = other != null && other::class == this::class

    override fun hashCode(): Int = this::class.hashCode()

    override fun toString(): String = "[${this::class.simpleName}]"
}

/** A line end cut off flat at the end point. */
public class ButtCap : Cap()

/** A line end with a half circle around the end point. */
public class RoundCap : Cap()

/** A line end with a half square around the end point. */
public class SquareCap : Cap()

/**
 * The shape of the corners of a stroke, as int constants like in the Maps SDK,
 * e.g. `PolylineOptions.jointType(JointType.ROUND)`.
 */
public object JointType {
    /** Sharp corners */
    public const val DEFAULT: Int = 0

    /** Corners cut off flat */
    public const val BEVEL: Int = 1

    /** Rounded corners */
    public const val ROUND: Int = 2
}

internal fun Cap.toStrokeCap(): StrokeCap =
    when (this) {
        is RoundCap -> StrokeCap.Round
        is SquareCap -> StrokeCap.Square
        else -> StrokeCap.Butt
    }

internal fun StrokeCap.toCap(): Cap =
    when (this) {
        StrokeCap.Round -> RoundCap()
        StrokeCap.Square -> SquareCap()
        else -> ButtCap()
    }

internal fun jointTypeToStrokeJoin(jointType: Int): StrokeJoin =
    when (jointType) {
        JointType.BEVEL -> StrokeJoin.Bevel
        JointType.ROUND -> StrokeJoin.Round
        else -> StrokeJoin.Miter
    }

internal fun StrokeJoin.toJointType(): Int =
    when (this) {
        StrokeJoin.Bevel -> JointType.BEVEL
        StrokeJoin.Round -> JointType.ROUND
        else -> JointType.DEFAULT
    }
