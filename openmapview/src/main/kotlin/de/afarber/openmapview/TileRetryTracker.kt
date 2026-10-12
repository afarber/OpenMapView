/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

/**
 * Remembers tiles that failed to load and delays the next attempt with exponential backoff.
 *
 * Without it, a failed tile would be requested again on every redraw, which floods the
 * tile server when the device is offline or the server is rate limiting.
 * Not thread-safe: use it from the main thread only.
 */
internal class TileRetryTracker {
    companion object {
        const val INITIAL_DELAY_MS = 2_000L
        const val MAX_DELAY_MS = 60_000L
    }

    private class Failure(
        val count: Int,
        val retryAt: Long,
    )

    private val failures = mutableMapOf<TileCoordinate, Failure>()

    /**
     * Returns true if the tile has not failed or its retry delay has passed.
     *
     * @param now Current time in milliseconds, e.g. SystemClock.elapsedRealtime()
     */
    fun canRequest(
        tile: TileCoordinate,
        now: Long,
    ): Boolean {
        val failure = failures[tile] ?: return true
        return now >= failure.retryAt
    }

    /**
     * Records a failed attempt.
     *
     * @param now Current time in milliseconds
     * @return The delay in milliseconds before the tile may be requested again
     */
    fun onFailure(
        tile: TileCoordinate,
        now: Long,
    ): Long {
        val count = (failures[tile]?.count ?: 0) + 1
        val delay = (INITIAL_DELAY_MS shl (count - 1).coerceAtMost(5)).coerceAtMost(MAX_DELAY_MS)
        failures[tile] = Failure(count, now + delay)
        return delay
    }

    /**
     * Forgets earlier failures of a tile after it loaded successfully.
     */
    fun onSuccess(tile: TileCoordinate) {
        failures.remove(tile)
    }
}
