[![CI](https://github.com/afarber/OpenMapView/actions/workflows/ci.yml/badge.svg)](https://github.com/afarber/OpenMapView/actions/workflows/ci.yml)
[![Daily Tests](https://github.com/afarber/OpenMapView/actions/workflows/daily.yml/badge.svg)](https://github.com/afarber/OpenMapView/actions/workflows/daily.yml)
[![Release](https://github.com/afarber/OpenMapView/actions/workflows/release.yml/badge.svg)](https://github.com/afarber/OpenMapView/actions/workflows/release.yml)
[![codecov](https://codecov.io/gh/afarber/OpenMapView/branch/main/graph/badge.svg)](https://codecov.io/gh/afarber/OpenMapView)
[![API Documentation](https://img.shields.io/badge/API-Documentation-blue)](https://afarber.github.io/OpenMapView/)

# OpenMapView

A modern, Kotlin-first MapView replacement for Android — powered by [OpenStreetMap](https://www.openstreetmap.org/).

## Installation

Add to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("de.afarber:openmapview:0.9.0")
}
```

The library is available on [Maven Central](https://central.sonatype.com/artifact/de.afarber/openmapview). Alternative distribution via [JitPack](https://jitpack.io/#afarber/OpenMapView) is also supported.

## Features

- Drop-in compatible with Google `MapView` (non-deprecated methods only)
- Lightweight, pure Kotlin implementation with zero Google dependencies
- OSM tiles via standard APIs (free, no API keys required)
- Smooth camera animations with customizable durations and callbacks
- Multiple overlay types: Markers, Polylines, Polygons, Circles, Ground Overlays, Tile Overlays, Info Windows
- Multiple map types (see [Map Types Guide](docs/MAP_TYPES.md))
- Built-in zoom controls (+/- buttons) via UiSettings
- External app integration (open in Google Maps, OsmAnd, etc. via geo: URI)
- GeoJSON import support
- Extensible marker, overlay, and gesture handling
- MIT licensed (use freely in commercial apps)

## API Documentation

Full API reference documentation is available at [afarber.github.io/OpenMapView](https://afarber.github.io/OpenMapView/).

The documentation is automatically generated from KDoc comments and updated with every commit to main.

## Examples

Explore the example applications to see OpenMapView in action:

- [Example01Pan](examples/Example01Pan) - Basic map panning with touch gestures
- [Example02Zoom](examples/Example02Zoom) - Built-in zoom controls (+/- buttons) and zoom level display
- [Example03Markers](examples/Example03Markers) - Marker overlays with custom colors and info windows
- [Example04Polylines](examples/Example04Polylines) - Polylines, polygons, and polygons with holes (donut shapes)
- [Example05Camera](examples/Example05Camera) - Camera animations with callbacks, custom durations, and built-in zoom controls
- [Example06Clicks](examples/Example06Clicks) - Map click and long-click listeners with coordinate display
- [Example07DraggableMarkers](examples/Example07DraggableMarkers) - Draggable markers with drag event listeners
- [Example08Circles](examples/Example08Circles) - Circles with radius, styling, z-index ordering, and click listeners
- [Example09Overlays](examples/Example09Overlays) - Tile overlays with transparency control (OpenSeaMap, Railways, Trails, Snow)
- [Example10GroundOverlays](examples/Example10GroundOverlays) - Ground overlays with position/bounds modes, rotation, and transparency
- [Example11MapTypes](examples/Example11MapTypes) - Switching between 5 map types (Normal, Terrain, Humanitarian, Cycle, None)
- [Example12Toolbar](examples/Example12Toolbar) - Open location in external map apps via geo: URI with OpenStreetMap browser fallback

![Example05Camera Demo](examples/Example05Camera/screenshot.gif)

## Documentation

- [Public API Compatibility](docs/PUBLIC_API.md) - Google MapView API implementation status and compatibility matrix
- [Architecture](docs/ARCHITECTURE.md) - Architectural design and comparison with Google Maps SDK
- [Map Toolbar](docs/MAP_TOOLBAR.md) - External map app integration using geo: URIs with browser fallback
- [Contributing Guide](docs/CONTRIBUTING.md) - Code quality requirements, formatting, git hooks, and PR process
- [Lifecycle Management](docs/LIFECYCLE.md) - How OpenMapView handles Android lifecycle events
- [Publishing Guide](docs/PUBLISHING.md) - Publishing to Maven Central and JitPack
- [GitHub Workflows](docs/GITHUB_WORKFLOWS.md) - CI/CD pipeline and workflow architecture
- [Unit Testing](docs/TESTING_UNIT.md) - JVM unit tests with Robolectric for Android framework APIs
- [Instrumented Testing](docs/TESTING_INSTRUMENTED.md) - On-device (phone and auto) testing with Android emulator

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

                // Add markers - Kotlin style (direct instantiation)
                addMarker(Marker(
                    position = LatLng(51.4661, 7.2491),
                    title = "Bochum City Center"
                ))

                // Or Google Maps API style (builder pattern)
                addMarker(
                    MarkerOptions()
                        .position(LatLng(51.4700, 7.2550))
                        .title("North Location")
                        .draggable(true)
                )
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
