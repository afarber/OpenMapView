/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.compose

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import de.afarber.openmapview.CameraPosition
import de.afarber.openmapview.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Checks that a map in a vertically scrolling Column pans on vertical drags instead of the
 * Column scrolling.
 */
@RunWith(RobolectricTestRunner::class)
class OpenMapScrollTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun testVerticalDragOnMap_PansMapInsteadOfScrollingColumn() {
        val start = CameraPosition(LatLng(51.4661, 7.2491), 14f)
        val cameraPositionState = CameraPositionState(start)
        lateinit var scrollState: ScrollState
        composeRule.setContent {
            scrollState = rememberScrollState()
            Column(modifier = Modifier.verticalScroll(scrollState)) {
                OpenMap(
                    modifier = Modifier.fillMaxWidth().height(300.dp).testTag("map"),
                    cameraPositionState = cameraPositionState,
                )
                Spacer(modifier = Modifier.height(3000.dp))
            }
        }

        composeRule.onNodeWithTag("map").performTouchInput { swipeUp(durationMillis = 500) }
        composeRule.waitForIdle()

        assertEquals(0, scrollState.value)
        assertNotEquals(start.target, cameraPositionState.position.target)
    }
}
