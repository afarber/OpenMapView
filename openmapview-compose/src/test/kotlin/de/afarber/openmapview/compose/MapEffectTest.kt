/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapEffectTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testMapEffect_RunsWithTheMapAgainWhenAKeyChanges() {
        var created: OpenMapView? = null
        val seen = mutableListOf<Pair<OpenMapView, Int>>()
        var key by mutableIntStateOf(1)
        composeRule.setContent {
            OpenMap(modifier = FillTestWindow, onMapCreated = { created = it }) {
                MapEffect(key) { map -> seen.add(map to key) }
            }
        }
        composeRule.waitForIdle()

        key = 2
        composeRule.waitForIdle()

        assertEquals(listOf(1, 2), seen.map { it.second })
        seen.forEach { assertSame(created, it.first) }
    }

    @Test
    fun testMapEffect_LeavingTheCompositionCancelsIt() {
        var show by mutableStateOf(true)
        var canceled = false
        composeRule.setContent {
            OpenMap(modifier = FillTestWindow) {
                if (show) {
                    MapEffect {
                        try {
                            kotlinx.coroutines.awaitCancellation()
                        } finally {
                            canceled = true
                        }
                    }
                }
            }
        }
        composeRule.waitForIdle()

        show = false
        composeRule.waitForIdle()

        assertEquals(true, canceled)
    }

    @Test
    fun testMapEffect_OutsideOfOpenMapDoesNothing() {
        var runs = 0
        composeRule.setContent { MapEffect { runs++ } }
        composeRule.waitForIdle()

        assertEquals(0, runs)
    }
}
