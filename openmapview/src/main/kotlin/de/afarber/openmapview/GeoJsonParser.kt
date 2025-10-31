/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview

import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser for GeoJSON data to convert to map overlays.
 *
 * Supports GeoJSON Feature, FeatureCollection, and direct geometries:
 * - Point -> Marker
 * - LineString -> Polyline
 * - Polygon -> Polygon
 * - MultiPoint -> Multiple Markers
 * - MultiLineString -> Multiple Polylines
 * - MultiPolygon -> Multiple Polygons
 */
object GeoJsonParser {
    /**
     * Parse GeoJSON string and return parsed features.
     *
     * @param geoJsonString Valid GeoJSON string
     * @return GeoJsonResult containing markers, polylines, and polygons
     */
    fun parse(geoJsonString: String): GeoJsonResult {
        val json = JSONObject(geoJsonString)
        return parseGeoJson(json)
    }

    private fun parseGeoJson(json: JSONObject): GeoJsonResult {
        val type = json.optString("type", "")

        return when (type) {
            "FeatureCollection" -> parseFeatureCollection(json)
            "Feature" -> parseFeature(json)
            "Point", "MultiPoint" -> parseGeometry(json, null)
            "LineString", "MultiLineString" -> parseGeometry(json, null)
            "Polygon", "MultiPolygon" -> parseGeometry(json, null)
            else -> throw IllegalArgumentException("Unsupported GeoJSON type: $type")
        }
    }

    private fun parseFeatureCollection(json: JSONObject): GeoJsonResult {
        val features = json.optJSONArray("features") ?: return GeoJsonResult()
        val markers = mutableListOf<Marker>()
        val polylines = mutableListOf<Polyline>()
        val polygons = mutableListOf<Polygon>()

        for (i in 0 until features.length()) {
            val feature = features.getJSONObject(i)
            val result = parseFeature(feature)
            markers.addAll(result.markers)
            polylines.addAll(result.polylines)
            polygons.addAll(result.polygons)
        }

        return GeoJsonResult(markers, polylines, polygons)
    }

    private fun parseFeature(json: JSONObject): GeoJsonResult {
        val geometry = json.optJSONObject("geometry") ?: return GeoJsonResult()
        val properties = json.optJSONObject("properties")

        return parseGeometry(geometry, properties)
    }

    private fun parseGeometry(
        geometry: JSONObject,
        properties: JSONObject?,
    ): GeoJsonResult {
        val type = geometry.optString("type", "")
        val coordinates = geometry.optJSONArray("coordinates") ?: return GeoJsonResult()

        return when (type) {
            "Point" -> parsePoint(coordinates, properties)
            "MultiPoint" -> parseMultiPoint(coordinates, properties)
            "LineString" -> parseLineString(coordinates, properties)
            "MultiLineString" -> parseMultiLineString(coordinates, properties)
            "Polygon" -> parsePolygon(coordinates, properties)
            "MultiPolygon" -> parseMultiPolygon(coordinates, properties)
            else -> throw IllegalArgumentException("Unsupported geometry type: $type")
        }
    }

    private fun parsePoint(
        coordinates: JSONArray,
        properties: JSONObject?,
    ): GeoJsonResult {
        val latLng = parseCoordinate(coordinates)
        val marker =
            Marker(
                position = latLng,
                title = properties?.optString("name") ?: properties?.optString("title"),
                snippet = properties?.optString("description"),
            )
        return GeoJsonResult(markers = listOf(marker))
    }

    private fun parseMultiPoint(
        coordinates: JSONArray,
        properties: JSONObject?,
    ): GeoJsonResult {
        val markers = mutableListOf<Marker>()
        for (i in 0 until coordinates.length()) {
            val coord = coordinates.getJSONArray(i)
            val latLng = parseCoordinate(coord)
            val marker =
                Marker(
                    position = latLng,
                    title = properties?.optString("name") ?: properties?.optString("title"),
                    snippet = properties?.optString("description"),
                )
            markers.add(marker)
        }
        return GeoJsonResult(markers = markers)
    }

    private fun parseLineString(
        coordinates: JSONArray,
        properties: JSONObject?,
    ): GeoJsonResult {
        val points = parseCoordinateArray(coordinates)
        if (points.size < 2) return GeoJsonResult()

        val polyline =
            Polyline(
                points = points,
                strokeColor = parseColor(properties?.optString("stroke")) ?: Color.Blue,
                strokeWidth = properties?.optDouble("stroke-width", 5.0)?.toFloat() ?: 5f,
                tag = properties,
            )
        return GeoJsonResult(polylines = listOf(polyline))
    }

    private fun parseMultiLineString(
        coordinates: JSONArray,
        properties: JSONObject?,
    ): GeoJsonResult {
        val polylines = mutableListOf<Polyline>()
        for (i in 0 until coordinates.length()) {
            val lineCoords = coordinates.getJSONArray(i)
            val points = parseCoordinateArray(lineCoords)
            if (points.size >= 2) {
                val polyline =
                    Polyline(
                        points = points,
                        strokeColor = parseColor(properties?.optString("stroke")) ?: Color.Blue,
                        strokeWidth = properties?.optDouble("stroke-width", 5.0)?.toFloat() ?: 5f,
                        tag = properties,
                    )
                polylines.add(polyline)
            }
        }
        return GeoJsonResult(polylines = polylines)
    }

    private fun parsePolygon(
        coordinates: JSONArray,
        properties: JSONObject?,
    ): GeoJsonResult {
        if (coordinates.length() == 0) return GeoJsonResult()

        // First array is outer ring
        val outerRing = parseCoordinateArray(coordinates.getJSONArray(0))
        if (outerRing.size < 3) return GeoJsonResult()

        // Remaining arrays are holes
        val holes = mutableListOf<List<LatLng>>()
        for (i in 1 until coordinates.length()) {
            val hole = parseCoordinateArray(coordinates.getJSONArray(i))
            if (hole.size >= 3) {
                holes.add(hole)
            }
        }

        val polygon =
            Polygon(
                points = outerRing,
                holes = holes,
                strokeColor = parseColor(properties?.optString("stroke")) ?: Color.Black,
                strokeWidth = properties?.optDouble("stroke-width", 3.0)?.toFloat() ?: 3f,
                fillColor = parseColor(properties?.optString("fill")) ?: Color(red = 128, green = 128, blue = 128, alpha = 100),
                tag = properties,
            )
        return GeoJsonResult(polygons = listOf(polygon))
    }

    private fun parseMultiPolygon(
        coordinates: JSONArray,
        properties: JSONObject?,
    ): GeoJsonResult {
        val polygons = mutableListOf<Polygon>()
        for (i in 0 until coordinates.length()) {
            val polygonCoords = coordinates.getJSONArray(i)
            val result = parsePolygon(polygonCoords, properties)
            polygons.addAll(result.polygons)
        }
        return GeoJsonResult(polygons = polygons)
    }

    private fun parseCoordinate(coordinates: JSONArray): LatLng {
        val lng = coordinates.getDouble(0)
        val lat = coordinates.getDouble(1)
        return LatLng(lat, lng)
    }

    private fun parseCoordinateArray(coordinates: JSONArray): List<LatLng> {
        val points = mutableListOf<LatLng>()
        for (i in 0 until coordinates.length()) {
            val coord = coordinates.getJSONArray(i)
            points.add(parseCoordinate(coord))
        }
        return points
    }

    private fun parseColor(colorString: String?): Color? {
        if (colorString == null) return null
        return colorString.toComposeColor()
    }
}

/**
 * Result of parsing GeoJSON data.
 */
data class GeoJsonResult(
    val markers: List<Marker> = emptyList(),
    val polylines: List<Polyline> = emptyList(),
    val polygons: List<Polygon> = emptyList(),
)
