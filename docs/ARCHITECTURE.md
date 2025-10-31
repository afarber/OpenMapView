# OpenMapView Architecture

[Back to README](../README.md)

This document explains the architectural design of OpenMapView and how it compares to Google Maps SDK.

---

## Google's Two-Class Architecture

Google Maps SDK uses a **two-tier architecture** that separates the view container from the map controller:

### 1. MapView - The Android View Component

- Extends FrameLayout
- Handles lifecycle (onCreate, onResume, etc.)
- Acts as a container
- Has only ~11 methods, mostly lifecycle forwarding

### 2. GoogleMap - The Map Controller Object

- Obtained asynchronously via `getMapAsync(callback)`
- Contains all the actual map functionality (~80 methods)
- Camera control, markers, shapes, settings, listeners
- Not a View, just a controller object

**Google's Usage Pattern:**

```kotlin
// Step 1: Get the view
val mapView = findViewById<MapView>(R.id.mapView)

// Step 2: Forward lifecycle
mapView.onCreate(bundle)
mapView.onResume()

// Step 3: Get the controller asynchronously
mapView.getMapAsync { googleMap ->
    // Now you can use googleMap.addMarker(), googleMap.animateCamera(), etc.
}
```

---

## OpenMapView's Simplified Architecture

OpenMapView uses a **single-class architecture** that combines both roles into one cohesive interface:

### OpenMapView - Unified View and Controller

- Extends FrameLayout (like MapView)
- Implements DefaultLifecycleObserver (simplifies lifecycle)
- Directly exposes map methods (no separate controller needed)
- Map is immediately ready (no async callback needed)

**OpenMapView's Usage Pattern:**

```kotlin
// Step 1: Get the view - that's it!
val mapView = OpenMapView(context)
lifecycle.addObserver(mapView) // Automatic lifecycle

// Step 2: Use immediately, no callback needed
mapView.addMarker(marker)
mapView.animateCamera(update)
```

---

## How OpenMapView Covers Both APIs

OpenMapView consolidates the functionality of both Google classes into a single, simpler interface:

```kotlin
class OpenMapView : FrameLayout, DefaultLifecycleObserver {
    private val controller = MapController(context)  // Internal, not exposed

    // MapView role: lifecycle (automatic via DefaultLifecycleObserver)
    override fun onResume(owner: LifecycleOwner) {
        controller.onResume()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        controller.onDestroy()
    }

    // GoogleMap role: expose map methods directly
    fun addMarker(marker: Marker): Marker {
        val result = controller.addMarker(marker)
        invalidate()
        return result
    }

    fun animateCamera(cameraUpdate: CameraUpdate) {
        controller.animateCamera(cameraUpdate)
    }

    fun moveCamera(cameraUpdate: CameraUpdate) {
        controller.moveCamera(cameraUpdate)
        invalidate()
    }

    // All other GoogleMap methods exposed directly here
}
```

---

## Coverage Analysis

### MapView Methods (11 total)

- Mostly lifecycle boilerplate
- Replaced with DefaultLifecycleObserver pattern
- Simpler for developers (no manual forwarding)

### GoogleMap Methods (80 total)

- The real map functionality
- Exposed directly on OpenMapView
- Internal MapController handles implementation
- No async callback needed

---

## The Math

**Can OpenMapView cover most non-deprecated methods despite being a single class?**

**Yes**, because:

1. MapView's 11 methods are mostly lifecycle management, which OpenMapView handles more elegantly with DefaultLifecycleObserver
2. GoogleMap's 80 methods are the actual functionality, which OpenMapView exposes directly
3. Core features (90% of use cases) achieve 100% coverage:

   - Camera animations
   - Markers with custom icons
   - Polylines and polygons
   - Click listeners
   - GeoJSON import

4. Google-specific features intentionally not planned:
   - Traffic layers (requires Google data)
   - Indoor maps (requires Google data)
   - POI data (not available in OSM tiles)

---

## The Advantages

OpenMapView offers several key advantages over the reference implementation:

### 1. Simpler Architecture

OpenMapView's single-class approach is **simpler** than the two-class pattern:

### Google Maps (complex)

```kotlin
class MyActivity : AppCompatActivity() {
    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)

        // Async - map not ready yet
        mapView.getMapAsync { map ->
            googleMap = map
            googleMap?.addMarker(...)
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()  // Manual forwarding
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()  // Manual forwarding
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()  // Manual forwarding
    }
}
```

### OpenMapView (simple)

```kotlin
class MyActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapView = OpenMapView(context)
        lifecycle.addObserver(mapView)  // Once - automatic lifecycle

        // Immediate - map ready now
        mapView.addMarker(...)
        mapView.animateCamera(...)
    }

    // No manual lifecycle forwarding needed
}
```

### 2. No Vendor Lock-in or Usage Restrictions

OpenMapView uses **OpenStreetMap tiles** instead of proprietary map data:

- **No API keys required** - No registration, no authentication
- **No usage fees** - Free for commercial and personal projects
- **No rate limiting** - Subject only to OSM tile server fair use policies
- **No terms of service restrictions** - MIT licensed, use anywhere
- **No dependency on external services** - Works independently of any company's infrastructure
- **Open data** - Built on community-maintained OpenStreetMap data

This makes OpenMapView particularly valuable for:
- Commercial applications without maps budget
- Projects requiring predictable costs
- Apps that need to work offline or with custom tile servers
- Organizations avoiding vendor dependencies

---

## Conclusion

This architectural design achieves several goals:

1. **Simplicity** - One class instead of two, no async initialization
2. **Lifecycle Safety** - DefaultLifecycleObserver prevents common lifecycle bugs
3. **API Compatibility** - Covers essential functionality for drop-in replacement
4. **Clean Separation** - Internal MapController handles implementation details
5. **No Vendor Lock-in** - Uses free, open OpenStreetMap data without restrictions
6. **Modern Graphics API** - Uses Jetpack Compose graphics primitives (Color, PathEffect, StrokeCap, StrokeJoin) for better integration with modern Android development

The single-class approach combined with OSM tiles and modern Android APIs makes OpenMapView easier to use and more accessible while maintaining the flexibility to implement the features developers expect in a modern map library.
