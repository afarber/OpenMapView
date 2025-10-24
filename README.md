# OpenMapView

A modern, Kotlin-first MapView replacement for Android — powered by [OpenStreetMap](https://www.openstreetmap.org/).

## Features
- Drop-in compatible with `Google MapView` (non-deprecated methods only)
- Lightweight, pure Kotlin implementation
- OSM tiles via standard APIs
- Extensible marker, overlay, and gesture handling
- MIT licensed (use freely in commercial apps)

[![Spotless](https://github.com/afarber/OpenMapView/actions/workflows/spotless.yml/badge.svg)](https://github.com/afarber/OpenMapView/actions/workflows/spotless.yml)


## Getting Started

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapView = findViewById<OpenMapView>(R.id.mapView)
        mapView.setTileSource(TileSource.STANDARD)
        mapView.setZoom(14.0)
        mapView.setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
    }
}
