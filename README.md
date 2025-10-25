[![Build Status](https://github.com/afarber/OpenMapView/actions/workflows/ci.yml/badge.svg)](https://github.com/afarber/OpenMapView/actions/workflows/ci.yml)

[![Spotless](https://github.com/afarber/OpenMapView/actions/workflows/spotless.yml/badge.svg)](https://github.com/afarber/OpenMapView/actions/workflows/spotless.yml)

# OpenMapView

A modern, Kotlin-first MapView replacement for Android — powered by [OpenStreetMap](https://www.openstreetmap.org/).

## Features

- Drop-in compatible with Google `MapView` (non-deprecated methods only)
- Lightweight, pure Kotlin implementation
- OSM tiles via standard APIs
- Extensible marker, overlay, and gesture handling
- MIT licensed (use freely in commercial apps)

## Examples

Explore the example applications to see OpenMapView in action:

### [Example01Pan](examples/Example01Pan) - Basic Map Panning

![Example01Pan](examples/Example01Pan/screenshot.gif)

Demonstrates basic map tile rendering and touch pan gestures.

### [Example02Zoom](examples/Example02Zoom) - Zoom Controls and Gestures

![Example02Zoom](examples/Example02Zoom/screenshot.gif)

Shows zoom functionality with FAB controls and pinch-to-zoom gestures.

### [Example03Markers](examples/Example03Markers) - Marker Overlays

![Example03Markers](examples/Example03Markers/screenshot.gif)

Demonstrates marker system with custom icons and click handling.

## Getting Started

### With Jetpack Compose

```kotlin
@Composable
fun MapViewScreen() {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            OpenMapView(context).apply {
                // Register lifecycle observer for proper cleanup
                lifecycleOwner.lifecycle.addObserver(this)

                setCenter(LatLng(51.4661, 7.2491))
                setZoom(14.0)

                // Add markers (optional)
                addMarker(Marker(
                    position = LatLng(51.4661, 7.2491),
                    title = "Bochum City Center"
                ))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
```

### With XML Layouts

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapView = findViewById<OpenMapView>(R.id.mapView)
        mapView.setZoom(14.0)
        mapView.setCenter(LatLng(51.4661, 7.2491))
    }
}
```
