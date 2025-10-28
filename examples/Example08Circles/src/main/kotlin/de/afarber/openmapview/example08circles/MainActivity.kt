/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */

package de.afarber.openmapview.example08circles

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import de.afarber.openmapview.CircleOptions
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.OpenMapView
import kotlin.random.Random

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

@Composable
fun MapViewScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var mapView by remember { mutableStateOf<OpenMapView?>(null) }
    var circleCount by remember { mutableStateOf(0) }

    val bochumCenter = LatLng(51.4661, 7.2491)

    // Show initial instruction toast
    LaunchedEffect(Unit) {
        Toast.makeText(
            context,
            "Click circles to see their properties.\nUse FABs to add random circles or clear all",
            Toast.LENGTH_LONG,
        ).show()
    }

    fun addDemoCircles() {
        mapView?.let { map ->
            // Circle 1: Small red circle with high z-index
            map.addCircle(
                CircleOptions()
                    .center(bochumCenter)
                    .radius(500f)
                    .strokeColor(Color.RED)
                    .strokeWidth(5f)
                    .fillColor(Color.argb(64, 255, 0, 0))
                    .clickable(true)
                    .zIndex(2f)
                    .tag("Small Red Circle - 500m"),
            )

            // Circle 2: Medium blue circle with mid z-index
            val offset1 = LatLng(bochumCenter.latitude + 0.01, bochumCenter.longitude + 0.01)
            map.addCircle(
                CircleOptions()
                    .center(offset1)
                    .radius(1000f)
                    .strokeColor(Color.BLUE)
                    .strokeWidth(8f)
                    .fillColor(Color.argb(64, 0, 0, 255))
                    .clickable(true)
                    .zIndex(1f)
                    .tag("Medium Blue Circle - 1000m"),
            )

            // Circle 3: Large green circle with low z-index
            val offset2 = LatLng(bochumCenter.latitude - 0.01, bochumCenter.longitude - 0.01)
            map.addCircle(
                CircleOptions()
                    .center(offset2)
                    .radius(1500f)
                    .strokeColor(Color.GREEN)
                    .strokeWidth(10f)
                    .fillColor(Color.argb(64, 0, 255, 0))
                    .clickable(true)
                    .zIndex(0f)
                    .tag("Large Green Circle - 1500m"),
            )

            circleCount += 3
            Toast.makeText(context, "Added 3 demonstration circles", Toast.LENGTH_SHORT).show()
        }
    }

    fun addRandomCircle() {
        mapView?.let { map ->
            val randomLat = bochumCenter.latitude + (Random.nextDouble() - 0.5) * 0.03
            val randomLng = bochumCenter.longitude + (Random.nextDouble() - 0.5) * 0.06
            val randomRadius = Random.nextInt(300, 1500).toFloat()
            val randomColor = Color.rgb(Random.nextInt(256), Random.nextInt(256), Random.nextInt(256))

            map.addCircle(
                CircleOptions()
                    .center(LatLng(randomLat, randomLng))
                    .radius(randomRadius)
                    .strokeColor(randomColor)
                    .strokeWidth(Random.nextInt(3, 12).toFloat())
                    .fillColor(Color.argb(64, Color.red(randomColor), Color.green(randomColor), Color.blue(randomColor)))
                    .clickable(true)
                    .zIndex(Random.nextFloat() * 5)
                    .tag("Random Circle ${++circleCount} - ${randomRadius.toInt()}m"),
            )

            Toast.makeText(context, "Added random circle", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    mapView = this
                    lifecycleOwner.lifecycle.addObserver(this)

                    setCenter(bochumCenter)
                    setZoom(12.0)

                    // Add initial demonstration circles
                    addDemoCircles()

                    // Set circle click listener
                    setOnCircleClickListener { circle ->
                        val tagStr = circle.tag?.toString() ?: "Unknown Circle"
                        val coordStr = "%.4f, %.4f".format(circle.center.latitude, circle.center.longitude)
                        Toast.makeText(
                            context,
                            "$tagStr\nCenter: $coordStr\nZ-Index: ${circle.zIndex}",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }

                    // Set attribution click listener
                    setOnAttributionClickListener {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/copyright"))
                        context.startActivity(intent)
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Floating Action Buttons
        Column(
            modifier =
            Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End,
        ) {
            // Add random circle FAB
            FloatingActionButton(
                onClick = { addRandomCircle() },
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add random circle",
                )
            }

            // Clear all circles FAB
            FloatingActionButton(
                onClick = {
                    mapView?.clear()
                    circleCount = 0
                    Toast.makeText(
                        context,
                        "All circles cleared",
                        Toast.LENGTH_SHORT,
                    ).show()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear all circles",
                )
            }
        }
    }
}
