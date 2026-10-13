/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TileTest {
    @Test
    fun testEqualTilesWithSameContent() {
        val tile1 = Tile(256, 256, byteArrayOf(1, 2, 3))
        val tile2 = Tile(256, 256, byteArrayOf(1, 2, 3))

        assertEquals(tile1, tile1)
        assertEquals(tile1, tile2)
        assertEquals(tile1.hashCode(), tile2.hashCode())
    }

    @Test
    fun testDifferentTiles() {
        val tile = Tile(256, 256, byteArrayOf(1, 2, 3))

        assertNotEquals(tile, Tile(512, 256, byteArrayOf(1, 2, 3)))
        assertNotEquals(tile, Tile(256, 512, byteArrayOf(1, 2, 3)))
        assertNotEquals(tile, Tile(256, 256, byteArrayOf(1, 2, 4)))
        assertNotEquals(tile, null)
        assertNotEquals(tile, "not a tile")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroWidthThrows() {
        Tile(0, 256, byteArrayOf(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testZeroHeightThrows() {
        Tile(256, 0, byteArrayOf(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testEmptyDataThrows() {
        Tile(256, 256, byteArrayOf())
    }

    @Test
    fun testToString() {
        assertEquals("Tile(width=256, height=256, dataSize=3 bytes)", Tile(256, 256, byteArrayOf(1, 2, 3)).toString())
    }
}
