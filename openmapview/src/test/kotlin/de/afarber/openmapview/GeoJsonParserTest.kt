/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeoJsonParserTest {
    @Test
    fun testParsePoint() {
        val geoJson =
            """
            {
                "type": "Point",
                "coordinates": [7.2491, 51.4661]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(1, result.markers.size)
        assertEquals(0, result.polylines.size)
        assertEquals(0, result.polygons.size)

        val marker = result.markers[0]
        assertEquals(51.4661, marker.position.latitude, 0.0001)
        assertEquals(7.2491, marker.position.longitude, 0.0001)
    }

    @Test
    fun testParseLineString() {
        val geoJson =
            """
            {
                "type": "LineString",
                "coordinates": [
                    [7.2400, 51.4700],
                    [7.2450, 51.4680],
                    [7.2500, 51.4650]
                ]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(0, result.markers.size)
        assertEquals(1, result.polylines.size)
        assertEquals(0, result.polygons.size)

        val polyline = result.polylines[0]
        assertEquals(3, polyline.points.size)
        assertEquals(51.4700, polyline.points[0].latitude, 0.0001)
        assertEquals(7.2400, polyline.points[0].longitude, 0.0001)
    }

    @Test
    fun testParsePolygon() {
        val geoJson =
            """
            {
                "type": "Polygon",
                "coordinates": [[
                    [7.2380, 51.4640],
                    [7.2380, 51.4660],
                    [7.2420, 51.4660],
                    [7.2420, 51.4640],
                    [7.2380, 51.4640]
                ]]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(0, result.markers.size)
        assertEquals(0, result.polylines.size)
        assertEquals(1, result.polygons.size)

        val polygon = result.polygons[0]
        assertEquals(5, polygon.points.size)
        assertTrue(polygon.holes.isEmpty())
    }

    @Test
    fun testParsePolygonWithHole() {
        val geoJson =
            """
            {
                "type": "Polygon",
                "coordinates": [
                    [
                        [7.2580, 51.4700],
                        [7.2580, 51.4720],
                        [7.2620, 51.4720],
                        [7.2620, 51.4700],
                        [7.2580, 51.4700]
                    ],
                    [
                        [7.2590, 51.4706],
                        [7.2590, 51.4714],
                        [7.2610, 51.4714],
                        [7.2610, 51.4706],
                        [7.2590, 51.4706]
                    ]
                ]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(1, result.polygons.size)
        val polygon = result.polygons[0]
        assertEquals(5, polygon.points.size)
        assertEquals(1, polygon.holes.size)
        assertEquals(5, polygon.holes[0].size)
    }

    @Test
    fun testParseFeature() {
        val geoJson =
            """
            {
                "type": "Feature",
                "properties": {
                    "name": "Test Point",
                    "description": "A test location"
                },
                "geometry": {
                    "type": "Point",
                    "coordinates": [7.2491, 51.4661]
                }
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(1, result.markers.size)
        val marker = result.markers[0]
        assertEquals("Test Point", marker.title)
        assertEquals("A test location", marker.snippet)
    }

    @Test
    fun testParseFeatureCollection() {
        val geoJson =
            """
            {
                "type": "FeatureCollection",
                "features": [
                    {
                        "type": "Feature",
                        "geometry": {
                            "type": "Point",
                            "coordinates": [7.2491, 51.4661]
                        }
                    },
                    {
                        "type": "Feature",
                        "geometry": {
                            "type": "LineString",
                            "coordinates": [
                                [7.2400, 51.4700],
                                [7.2450, 51.4680]
                            ]
                        }
                    },
                    {
                        "type": "Feature",
                        "geometry": {
                            "type": "Polygon",
                            "coordinates": [[
                                [7.2380, 51.4640],
                                [7.2380, 51.4660],
                                [7.2420, 51.4660],
                                [7.2420, 51.4640],
                                [7.2380, 51.4640]
                            ]]
                        }
                    }
                ]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(1, result.markers.size)
        assertEquals(1, result.polylines.size)
        assertEquals(1, result.polygons.size)
    }

    @Test
    fun testParseWithStyleProperties() {
        val geoJson =
            """
            {
                "type": "Feature",
                "properties": {
                    "stroke": "#FF0000",
                    "stroke-width": 8,
                    "fill": "#00FF00"
                },
                "geometry": {
                    "type": "Polygon",
                    "coordinates": [[
                        [7.2380, 51.4640],
                        [7.2380, 51.4660],
                        [7.2420, 51.4660],
                        [7.2380, 51.4640]
                    ]]
                }
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(1, result.polygons.size)
        val polygon = result.polygons[0]
        assertEquals(Color.parseColor("#FF0000"), polygon.strokeColor)
        assertEquals(8f, polygon.strokeWidth, 0.001f)
        assertEquals(Color.parseColor("#00FF00"), polygon.fillColor)
    }

    @Test
    fun testParseMultiPoint() {
        val geoJson =
            """
            {
                "type": "MultiPoint",
                "coordinates": [
                    [7.2491, 51.4661],
                    [7.2550, 51.4700],
                    [7.2430, 51.4620]
                ]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(3, result.markers.size)
        assertEquals(0, result.polylines.size)
        assertEquals(0, result.polygons.size)
    }

    @Test
    fun testParseMultiLineString() {
        val geoJson =
            """
            {
                "type": "MultiLineString",
                "coordinates": [
                    [
                        [7.2400, 51.4700],
                        [7.2450, 51.4680]
                    ],
                    [
                        [7.2500, 51.4650],
                        [7.2550, 51.4620]
                    ]
                ]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(0, result.markers.size)
        assertEquals(2, result.polylines.size)
        assertEquals(0, result.polygons.size)
    }

    @Test
    fun testParseMultiPolygon() {
        val geoJson =
            """
            {
                "type": "MultiPolygon",
                "coordinates": [
                    [[
                        [7.2380, 51.4640],
                        [7.2380, 51.4660],
                        [7.2420, 51.4660],
                        [7.2380, 51.4640]
                    ]],
                    [[
                        [7.2500, 51.4700],
                        [7.2500, 51.4720],
                        [7.2540, 51.4720],
                        [7.2500, 51.4700]
                    ]]
                ]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(0, result.markers.size)
        assertEquals(0, result.polylines.size)
        assertEquals(2, result.polygons.size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseInvalidType() {
        val geoJson =
            """
            {
                "type": "InvalidType",
                "coordinates": []
            }
            """.trimIndent()

        GeoJsonParser.parse(geoJson)
    }

    @Test
    fun testParseEmptyFeatureCollection() {
        val geoJson =
            """
            {
                "type": "FeatureCollection",
                "features": []
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        assertEquals(0, result.markers.size)
        assertEquals(0, result.polylines.size)
        assertEquals(0, result.polygons.size)
    }

    @Test
    fun testParseLineStringTooFewPoints() {
        val geoJson =
            """
            {
                "type": "LineString",
                "coordinates": [
                    [7.2400, 51.4700]
                ]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        // Should return empty result since < 2 points
        assertEquals(0, result.polylines.size)
    }

    @Test
    fun testParsePolygonTooFewPoints() {
        val geoJson =
            """
            {
                "type": "Polygon",
                "coordinates": [[
                    [7.2380, 51.4640],
                    [7.2380, 51.4660]
                ]]
            }
            """.trimIndent()

        val result = GeoJsonParser.parse(geoJson)

        // Should return empty result since < 3 points
        assertEquals(0, result.polygons.size)
    }
}
