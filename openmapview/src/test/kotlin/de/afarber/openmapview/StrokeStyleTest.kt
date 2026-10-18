/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.PathDashPathEffect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Tests for the caps, joint types and dot, dash and gap patterns of shapes.
 */
@RunWith(RobolectricTestRunner::class)
class StrokeStyleTest {
    private lateinit var controller: MapController

    private val route = listOf(LatLng(51.49, -0.01), LatLng(51.51, 0.01))
    private val square =
        listOf(LatLng(51.49, -0.01), LatLng(51.51, -0.01), LatLng(51.51, 0.01), LatLng(51.49, 0.01))

    @Before
    fun setUp() {
        controller = MapController(RuntimeEnvironment.getApplication())
        controller.setViewSize(800, 600)
        controller.setCenter(LatLng(51.5, 0.0))
        controller.setZoom(12.0f)
    }

    @After
    fun tearDown() {
        controller.onDestroy()
    }

    @Test
    fun testCaps_MapToStrokeCaps() {
        assertEquals(StrokeCap.Butt, ButtCap().toStrokeCap())
        assertEquals(StrokeCap.Round, RoundCap().toStrokeCap())
        assertEquals(StrokeCap.Square, SquareCap().toStrokeCap())
        assertEquals(RoundCap(), StrokeCap.Round.toCap())
        assertNotEquals(RoundCap() as Any, ButtCap() as Any)
    }

    @Test
    fun testJointTypes_MapToStrokeJoins() {
        assertEquals(StrokeJoin.Miter, jointTypeToStrokeJoin(JointType.DEFAULT))
        assertEquals(StrokeJoin.Bevel, jointTypeToStrokeJoin(JointType.BEVEL))
        assertEquals(StrokeJoin.Round, jointTypeToStrokeJoin(JointType.ROUND))
        assertEquals(JointType.ROUND, StrokeJoin.Round.toJointType())
    }

    @Test
    fun testPolylineOptions_CapsAndJointType() {
        val polyline =
            PolylineOptions()
                .addAll(route)
                .startCap(RoundCap())
                .endCap(SquareCap())
                .jointType(JointType.BEVEL)
                .build()

        assertEquals(StrokeCap.Round, polyline.startCap)
        assertEquals(StrokeCap.Square, polyline.endCap)
        assertEquals(StrokeJoin.Bevel, polyline.strokeJoin)
        assertEquals(RoundCap(), polyline.getStartCap())
        assertEquals(JointType.BEVEL, polyline.getJointType())

        polyline.setEndCap(ButtCap())
        polyline.setJointType(JointType.ROUND)
        assertEquals(StrokeCap.Butt, polyline.endCap)
        assertEquals(StrokeJoin.Round, polyline.strokeJoin)
    }

    @Test
    fun testPolygonAndCircle_CapsAndJointTypes() {
        val polygon =
            PolygonOptions()
                .addAll(square)
                .strokeCap(RoundCap())
                .strokeJointType(JointType.BEVEL)
                .build()
        val circle =
            CircleOptions()
                .center(square[0])
                .radius(100f)
                .strokeCap(ButtCap())
                .strokeJointType(JointType.DEFAULT)
                .build()

        assertEquals(StrokeCap.Round, polygon.strokeCap)
        assertEquals(JointType.BEVEL, polygon.getStrokeJointType())
        assertEquals(StrokeCap.Butt, circle.strokeCap)
        assertEquals(StrokeJoin.Miter, circle.strokeJoin)

        polygon.setStrokeCap(SquareCap())
        circle.setStrokeJointType(JointType.ROUND)
        assertEquals(SquareCap(), polygon.getStrokeCap())
        assertEquals(StrokeJoin.Round, circle.strokeJoin)
    }

    @Test
    fun testPattern_DashesAndGapsBecomeDashPathEffect() {
        assertTrue(listOf(Dash(30f), Gap(20f)).toAndroidPathEffect(10f) is DashPathEffect)
        // A leading gap and an odd count are moved around to start with a dash
        assertTrue(listOf(Gap(10f), Dash(30f)).toAndroidPathEffect(10f) is DashPathEffect)
        assertTrue(listOf(Dash(10f), Gap(10f), Dash(20f)).toAndroidPathEffect(10f) is DashPathEffect)
    }

    @Test
    fun testPattern_DotsBecomePathDashPathEffect() {
        assertTrue(listOf(Dot(), Gap(20f)).toAndroidPathEffect(10f) is PathDashPathEffect)
        assertTrue(listOf(Dash(30f), Gap(10f), Dot(), Gap(10f)).toAndroidPathEffect(10f) is PathDashPathEffect)
    }

    @Test
    fun testPattern_SolidLinesHaveNoEffect() {
        assertNull(emptyList<PatternItem>().toAndroidPathEffect(10f))
        assertNull(listOf(Dash(30f), Dash(10f)).toAndroidPathEffect(10f))
        // Empty gaps are dropped, which leaves a solid line
        assertNull(listOf(Dash(30f), Gap(0f)).toAndroidPathEffect(10f))
    }

    @Test
    fun testPatternItems_EqualByValue() {
        assertEquals(Dash(20f), Dash(20f))
        assertEquals(Gap(20f), Gap(20f))
        assertEquals(Dot(), Dot())
        assertNotEquals(Dash(20f) as Any, Gap(20f) as Any)
        assertEquals(listOf(Dot(), Gap(5f)), listOf(Dot(), Gap(5f)))
    }

    @Test
    fun testOptions_PatternAndAndroidPathEffect() {
        val dots = listOf(Dot(), Gap(20f))
        val dashed = DashPathEffect(floatArrayOf(20f, 10f), 0f)

        assertEquals(
            dots,
            PolylineOptions()
                .addAll(route)
                .pattern(dots)
                .build()
                .pattern,
        )
        assertEquals(
            dots,
            PolygonOptions()
                .addAll(square)
                .strokePattern(dots)
                .build()
                .pattern,
        )
        assertEquals(
            dots,
            CircleOptions()
                .center(square[0])
                .radius(100f)
                .strokePattern(dots)
                .build()
                .pattern,
        )
        assertNotNull(
            PolylineOptions()
                .addAll(route)
                .strokePattern(dashed)
                .build()
                .strokePattern,
        )

        val polygon = Polygon(points = square)
        polygon.setStrokePattern(dots)
        polygon.setStrokePattern(dashed)
        assertEquals(dots, polygon.pattern)
        assertNotNull(polygon.strokePattern)
    }

    @Test
    fun testDraw_PatternWinsOverStrokePattern() {
        val canvas = mockk<Canvas>(relaxed = true)
        val dashed = PathEffect.dashPathEffect(floatArrayOf(20f, 10f))
        controller.addPolyline(Polyline(points = route, strokePattern = dashed, pattern = listOf(Dot(), Gap(10f))))

        controller.draw(canvas)

        verify { canvas.drawPath(any(), match { it.pathEffect is PathDashPathEffect }) }
    }

    @Test
    fun testDraw_KeepsConvertedPatterns() {
        val canvas = mockk<Canvas>(relaxed = true)
        val effects = mutableListOf<android.graphics.PathEffect?>()
        val pattern = mutableListOf<PatternItem>(Dash(20f), Gap(10f))
        controller.addPolyline(Polyline(points = route, pattern = pattern))
        every { canvas.drawPath(any(), any()) } answers { effects.add(secondArg<android.graphics.Paint>().pathEffect) }

        controller.draw(canvas)
        controller.draw(canvas)
        // A changed list is a new pattern
        pattern.add(Dot())
        controller.draw(canvas)

        assertEquals(3, effects.size)
        assertSame(effects[0], effects[1])
        assertTrue(effects[0] is DashPathEffect)
        assertTrue(effects[2] is PathDashPathEffect)
    }
}
