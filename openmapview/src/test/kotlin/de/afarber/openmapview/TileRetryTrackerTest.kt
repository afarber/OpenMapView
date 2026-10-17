/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TileRetryTrackerTest {
    private val tile = TileCoordinate(1, 2, 3)
    private val tracker = TileRetryTracker()

    @Test
    fun testUnknownTileCanBeRequested() {
        assertTrue(tracker.canRequest(tile, 0L))
    }

    @Test
    fun testFailedTileWaitsForDelay() {
        val delay = tracker.onFailure(tile, 1_000L)

        assertEquals(TileRetryTracker.INITIAL_DELAY_MS, delay)
        assertFalse(tracker.canRequest(tile, 1_000L + delay - 1))
        assertTrue(tracker.canRequest(tile, 1_000L + delay))
        assertTrue("Other tiles are not affected", tracker.canRequest(TileCoordinate(4, 5, 6), 1_000L))
    }

    @Test
    fun testDelayDoublesUpToMaximum() {
        val delays = (1..8).map { tracker.onFailure(tile, 0L) }

        assertEquals(listOf(2_000L, 4_000L, 8_000L, 16_000L, 32_000L, 60_000L, 60_000L, 60_000L), delays)
    }

    @Test
    fun testSuccessResetsBackoff() {
        tracker.onFailure(tile, 0L)
        tracker.onFailure(tile, 0L)

        tracker.onSuccess(tile)

        assertTrue(tracker.canRequest(tile, 0L))
        assertEquals(TileRetryTracker.INITIAL_DELAY_MS, tracker.onFailure(tile, 0L))
    }

    @Test
    fun testNoTileIsNeverRequestedAgain() {
        tracker.onNoTile(tile)

        assertFalse(tracker.canRequest(tile, Long.MAX_VALUE - 1))
        assertTrue("Other tiles are not affected", tracker.canRequest(TileCoordinate(4, 5, 6), 0L))
    }
}
