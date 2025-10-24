package de.afarber.openmapview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class OpenMapView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), DefaultLifecycleObserver {

    private val controller = MapController(context)

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        controller.draw(canvas)
    }

    fun setCenter(latLng: LatLng) = controller.setCenter(latLng)
    fun setZoom(zoom: Double) = controller.setZoom(zoom)
    fun getZoom(): Double = controller.getZoom()

    override fun onResume(owner: LifecycleOwner) = controller.onResume()
    override fun onPause(owner: LifecycleOwner) = controller.onPause()
    override fun onDestroy(owner: LifecycleOwner) = controller.onDestroy()
}

