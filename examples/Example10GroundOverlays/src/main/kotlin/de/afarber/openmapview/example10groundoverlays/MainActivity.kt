/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example10groundoverlays

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import de.afarber.openmapview.BitmapDescriptor
import de.afarber.openmapview.GroundOverlay
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.LatLngBounds
import de.afarber.openmapview.OpenMapView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MapViewScreen()
                }
            }
        }
    }
}

data class OverlayState(
    val name: String,
    var overlay: GroundOverlay,
    var isVisible: Boolean = false,
)

@Composable
fun MapViewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<OpenMapView?>(null) }

    val bochumCenter = LatLng(51.4661, 7.2491)

    val overlays by remember {
        mutableStateOf(
            listOf(
                OverlayState(
                    "Position Mode",
                    GroundOverlay(
                        image = BitmapDescriptor.BitmapMarker(createSampleBitmap(Color.argb(180, 255, 0, 0), "POS")),
                        position = LatLng(51.47, 7.25),
                        width = 2000f,
                        bearing = 0f,
                        transparency = 0f,
                        visible = false,
                        clickable = true,
                        zIndex = 1f,
                        tag = "Position-based overlay",
                    ),
                    false,
                ),
                OverlayState(
                    "Bounds Mode",
                    GroundOverlay(
                        image = BitmapDescriptor.BitmapMarker(createSampleBitmap(Color.argb(180, 0, 255, 0), "BOUNDS")),
                        bounds =
                        LatLngBounds(
                            southwest = LatLng(51.46, 7.24),
                            northeast = LatLng(51.47, 7.26),
                        ),
                        transparency = 0f,
                        visible = false,
                        clickable = true,
                        zIndex = 2f,
                        tag = "Bounds-based overlay",
                    ),
                    false,
                ),
                OverlayState(
                    "Rotated 45deg",
                    GroundOverlay(
                        image = BitmapDescriptor.BitmapMarker(createSampleBitmap(Color.argb(180, 0, 0, 255), "ROT")),
                        position = LatLng(51.465, 7.255),
                        width = 1500f,
                        bearing = 45f,
                        transparency = 0f,
                        visible = false,
                        clickable = true,
                        zIndex = 3f,
                        tag = "Rotated 45 degrees",
                    ),
                    false,
                ),
            ),
        )
    }

    var transparency by remember { mutableFloatStateOf(0.5f) }

    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "Toggle ground overlays and adjust transparency",
            Toast.LENGTH_LONG,
        ).show()
    }

    fun toggleOverlay(
        index: Int,
        enabled: Boolean,
    ) {
        mapView?.let { map ->
            val state = overlays[index]

            if (enabled) {
                val newOverlay =
                    state.overlay.copy(
                        visible = true,
                        transparency = transparency,
                    )
                map.addGroundOverlay(newOverlay)
                overlays[index].overlay = newOverlay
                overlays[index].isVisible = true
            } else {
                map.removeGroundOverlay(state.overlay)
                overlays[index].isVisible = false
            }
        }
    }

    fun updateTransparency(newTransparency: Float) {
        transparency = newTransparency
        mapView?.let { map ->
            overlays.forEachIndexed { index, state ->
                if (state.isVisible) {
                    map.removeGroundOverlay(state.overlay)
                    val newOverlay =
                        state.overlay.copy(
                            transparency = newTransparency,
                        )
                    map.addGroundOverlay(newOverlay)
                    overlays[index].overlay = newOverlay
                }
            }
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    OpenMapView(ctx).apply {
                        mapView = this
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(bochumCenter)
                        setZoom(13.0f)
                        setOnGroundOverlayClickListener { groundOverlay ->
                            Toast.makeText(context, "Clicked: ${groundOverlay.tag}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(250.dp)
                    .fillMaxHeight(),
            ) {
                GroundOverlayControls(
                    overlays = overlays,
                    transparency = transparency,
                    onToggleOverlay = { index, enabled -> toggleOverlay(index, enabled) },
                    onTransparencyChange = { newValue -> updateTransparency(newValue) },
                    onClearAll = {
                        mapView?.clearGroundOverlays()
                        overlays.forEach { it.isVisible = false }
                        transparency = 0.5f
                        Toast.makeText(context, "All overlays cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    OpenMapView(ctx).apply {
                        mapView = this
                        lifecycleOwner.lifecycle.addObserver(this)
                        setCenter(bochumCenter)
                        setZoom(13.0f)
                        setOnGroundOverlayClickListener { groundOverlay ->
                            Toast.makeText(context, "Clicked: ${groundOverlay.tag}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).fillMaxSize(),
            )

            Surface(
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                GroundOverlayControls(
                    overlays = overlays,
                    transparency = transparency,
                    onToggleOverlay = { index, enabled -> toggleOverlay(index, enabled) },
                    onTransparencyChange = { newValue -> updateTransparency(newValue) },
                    onClearAll = {
                        mapView?.clearGroundOverlays()
                        overlays.forEach { it.isVisible = false }
                        transparency = 0.5f
                        Toast.makeText(context, "All overlays cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
fun GroundOverlayControls(
    overlays: List<OverlayState>,
    transparency: Float,
    onToggleOverlay: (Int, Boolean) -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Ground Overlays",
            style = MaterialTheme.typography.titleMedium,
        )

        overlays.forEachIndexed { index, state ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(
                    checked = state.isVisible,
                    onCheckedChange = { enabled ->
                        onToggleOverlay(index, enabled)
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Transparency: ${(transparency * 100).toInt()}%",
            style = MaterialTheme.typography.bodyMedium,
        )
        Slider(
            value = transparency,
            onValueChange = onTransparencyChange,
            valueRange = 0f..1f,
        )

        Button(
            onClick = onClearAll,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
            )
            Text("Clear All")
        }
    }
}

private fun createSampleBitmap(
    color: Int,
    label: String,
): Bitmap {
    val size = 256
    val bitmap = createBitmap(size, size)
    val canvas = Canvas(bitmap)

    val paint =
        Paint().apply {
            this.color = color
            style = Paint.Style.FILL
        }
    canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

    val textPaint =
        Paint().apply {
            this.color = Color.WHITE
            textSize = 48f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
    canvas.drawText(label, size / 2f, size / 2f + 16f, textPaint)

    return bitmap
}
