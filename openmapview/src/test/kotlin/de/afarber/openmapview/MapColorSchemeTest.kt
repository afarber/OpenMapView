/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Tests that the dark color scheme draws the light tiles with [TileColorFilters.invertedDark].
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class MapColorSchemeTest {
    private lateinit var controller: MapController

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        // No tile source: nothing is downloaded, so only the cached tiles are drawn
        controller.setMapType(MapType.NONE)
        val cache = TileCache()
        val field = MapController::class.java.getDeclaredField("tileCache")
        field.isAccessible = true
        field.set(controller, cache)
        controller.setViewSize(SIZE, SIZE)
        controller.setCenter(LatLng(0.0, 0.0))
        controller.setZoom(10f)
        for (x in 511..512) {
            for (y in 511..512) {
                cache.put(TileCoordinate(x, y, 10), createBitmap(256, 256).applyCanvas { drawColor(Color.WHITE) })
            }
        }
    }

    @After
    fun tearDown() {
        controller.onDestroy()
        ApiKeyManager.clearProgrammaticKeys()
    }

    private fun drawPixel(): Int = createBitmap(SIZE, SIZE).also { controller.draw(Canvas(it)) }.getPixel(64, 64)

    // The filter the tiles are drawn with
    private fun tileFilter(): ColorFilter? {
        val field = MapController::class.java.getDeclaredField("tilePaint")
        field.isAccessible = true
        return (field.get(controller) as Paint).colorFilter
    }

    @Test
    fun testDefault_IsLight() {
        assertEquals(MapColorScheme.LIGHT, controller.getMapColorScheme())
        assertEquals(Color.WHITE, drawPixel())
    }

    @Test
    fun testDark_DarkensTheTilesUntilLightAgain() {
        controller.setMapColorScheme(MapColorScheme.DARK)
        assertEquals(MapColorScheme.DARK, controller.getMapColorScheme())
        assertSame(TileColorFilters.invertedDark(), tileFilter())
        // White becomes dark gray
        assertEquals(Color.rgb(20, 20, 20), drawPixel())
        // The app's filter is unchanged
        assertNull(controller.getTileColorFilter())

        controller.setMapColorScheme(MapColorScheme.LIGHT)
        assertEquals(Color.WHITE, drawPixel())
    }

    @Test
    fun testFollowSystem_FollowsNightMode() {
        controller.setMapColorScheme(MapColorScheme.FOLLOW_SYSTEM)
        assertNull(tileFilter())

        controller.setNightMode(true)
        assertSame(TileColorFilters.invertedDark(), tileFilter())

        controller.setNightMode(false)
        assertNull(tileFilter())
    }

    @Test
    @Config(qualifiers = "night")
    fun testFollowSystem_ReadsNightModeOfTheDevice() {
        val nightController = MapController(RuntimeEnvironment.getApplication())
        nightController.setMapColorScheme(MapColorScheme.FOLLOW_SYSTEM)

        val field = MapController::class.java.getDeclaredField("tilePaint")
        field.isAccessible = true
        assertSame(TileColorFilters.invertedDark(), (field.get(nightController) as Paint).colorFilter)
        nightController.onDestroy()
    }

    @Test
    fun testTileColorFilter_TakesPrecedenceOverDark() {
        val black = ColorMatrixColorFilter(ColorMatrix().apply { setScale(0f, 0f, 0f, 1f) })
        controller.setMapColorScheme(MapColorScheme.DARK)

        controller.setTileColorFilter(black)
        assertSame(black, tileFilter())

        controller.setTileColorFilter(null)
        assertSame(TileColorFilters.invertedDark(), tileFilter())
    }

    @Test
    fun testDark_KeepsDarkMapTypes() {
        ApiKeyManager.setApiKey("carto", "test-key")
        ApiKeyManager.setApiKey("thunderforest", "test-key")
        controller.setMapColorScheme(MapColorScheme.DARK)

        controller.setMapType(MapType.CARTO_DARK)
        assertNull(tileFilter())
        controller.setMapType(MapType.TRANSPORT_DARK)
        assertNull(tileFilter())
        controller.setMapType(MapType.CARTO_LIGHT)
        assertSame(TileColorFilters.invertedDark(), tileFilter())
    }

    @Test
    fun testDark_DarkensStandardShownForDarkTypeWithoutKey() {
        // Without a key CARTO_DARK falls back to the light STANDARD tiles
        controller.setMapColorScheme(MapColorScheme.DARK)
        controller.setMapType(MapType.CARTO_DARK)

        assertSame(TileColorFilters.invertedDark(), tileFilter())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testUnknownScheme_IsRejected() {
        controller.setMapColorScheme(MapColorScheme.UNKNOWN)
    }

    @Test
    fun testMapView_FollowsConfigurationChange() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val mapView = OpenMapView(activity)
        mapView.setMapColorScheme(MapColorScheme.FOLLOW_SYSTEM)
        assertEquals(MapColorScheme.FOLLOW_SYSTEM, mapView.getMapColorScheme())

        val night = Configuration(activity.resources.configuration)
        night.uiMode = Configuration.UI_MODE_NIGHT_YES or (night.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv())
        mapView.dispatchConfigurationChanged(night)

        val field = OpenMapView::class.java.getDeclaredField("controller")
        field.isAccessible = true
        val paintField = MapController::class.java.getDeclaredField("tilePaint")
        paintField.isAccessible = true
        assertSame(TileColorFilters.invertedDark(), (paintField.get(field.get(mapView)) as Paint).colorFilter)
    }

    private companion object {
        const val SIZE = 256
    }
}
