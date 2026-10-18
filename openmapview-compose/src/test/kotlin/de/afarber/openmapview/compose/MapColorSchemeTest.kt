/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import de.afarber.openmapview.MapColorScheme
import de.afarber.openmapview.OpenMapView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MapColorSchemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var map: OpenMapView

    @Test
    fun testDefault_FollowsSystem() {
        composeRule.setContent { OpenMap(modifier = FillTestWindow, onMapCreated = { map = it }) }
        composeRule.waitForIdle()

        assertEquals(MapColorScheme.FOLLOW_SYSTEM, map.getMapColorScheme())
    }

    @Test
    fun testChangedScheme_UpdatesTheMap() {
        var scheme by mutableStateOf(ComposeMapColorScheme.DARK)
        composeRule.setContent {
            OpenMap(modifier = FillTestWindow, mapColorScheme = scheme, onMapCreated = { map = it })
        }
        composeRule.waitForIdle()
        assertEquals(MapColorScheme.DARK, map.getMapColorScheme())

        scheme = ComposeMapColorScheme.LIGHT
        composeRule.waitForIdle()

        assertEquals(MapColorScheme.LIGHT, map.getMapColorScheme())
    }

    @Test
    fun testNullScheme_KeepsSchemeSetOnTheMap() {
        composeRule.setContent {
            OpenMap(
                modifier = FillTestWindow,
                mapColorScheme = null,
                onMapCreated = {
                    map = it
                    it.setMapColorScheme(MapColorScheme.DARK)
                },
            )
        }
        composeRule.waitForIdle()

        assertEquals(MapColorScheme.DARK, map.getMapColorScheme())
    }
}
