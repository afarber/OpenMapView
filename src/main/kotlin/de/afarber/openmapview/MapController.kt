package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas

class MapController(private val context: Context) {

    private var zoom = 10.0
    private var center = LatLng(0.0, 0.0)

    fun setZoom(z: Double) { zoom = z }
    fun getZoom(): Double = zoom

    fun setCenter(latLng: LatLng) { center = latLng }

    fun draw(canvas: Canvas?) {
        // TODO: Implement OSM tile rendering here
    }

    fun onResume() {}
    fun onPause() {}
    fun onDestroy() {}
}

