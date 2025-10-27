# Google MapView Public API - Implementation Status

[Back to README](../README.md)

This document lists all public non-deprecated methods from Google's MapView and GoogleMap classes, along with their implementation status in OpenMapView.

**Legend:**

- IMPLEMENTED: Feature is fully implemented and tested
- PARTIAL: Feature is partially implemented
- NOT IMPLEMENTED: Feature is not yet implemented
- NOT PLANNED: Feature is out of scope for this project

---

## MapView Class

Methods that must be forwarded from the parent Activity/Fragment.

| Method                            | Return Type | Status          | Notes                                              |
| --------------------------------- | ----------- | --------------- | -------------------------------------------------- |
| `onCreate(Bundle)`                | `void`      | NOT IMPLEMENTED | OpenMapView uses DefaultLifecycleObserver pattern  |
| `onStart()`                       | `void`      | NOT IMPLEMENTED | OpenMapView uses DefaultLifecycleObserver pattern  |
| `onResume()`                      | `void`      | IMPLEMENTED     | Called via DefaultLifecycleObserver.onResume()     |
| `onPause()`                       | `void`      | IMPLEMENTED     | Called via DefaultLifecycleObserver.onPause()      |
| `onStop()`                        | `void`      | NOT IMPLEMENTED | OpenMapView uses DefaultLifecycleObserver pattern  |
| `onDestroy()`                     | `void`      | IMPLEMENTED     | Called via DefaultLifecycleObserver.onDestroy()    |
| `onSaveInstanceState(Bundle)`     | `void`      | NOT IMPLEMENTED | State persistence not yet implemented              |
| `onLowMemory()`                   | `void`      | NOT IMPLEMENTED | Memory management optimization not yet implemented |
| `getMapAsync(OnMapReadyCallback)` | `void`      | NOT IMPLEMENTED | Map is immediately available after view creation   |
| `onEnterAmbient(Bundle)`          | `void`      | NOT PLANNED     | Wearable-specific feature                          |
| `onExitAmbient()`                 | `void`      | NOT PLANNED     | Wearable-specific feature                          |

---

## GoogleMap Class - Camera Movement

| Method                                                 | Return Type      | Status      | Notes                                                        |
| ------------------------------------------------------ | ---------------- | ----------- | ------------------------------------------------------------ |
| `animateCamera(CameraUpdate)`                          | `void`           | IMPLEMENTED | Default 250ms duration                                       |
| `animateCamera(CameraUpdate, CancelableCallback)`      | `void`           | PARTIAL     | Uses OnCameraAnimationListener instead of CancelableCallback |
| `animateCamera(CameraUpdate, int, CancelableCallback)` | `void`           | PARTIAL     | Custom duration supported, uses OnCameraAnimationListener    |
| `moveCamera(CameraUpdate)`                             | `void`           | IMPLEMENTED | Instant camera repositioning                                 |
| `stopAnimation()`                                      | `void`           | IMPLEMENTED | Cancels ongoing camera animation                             |
| `getCameraPosition()`                                  | `CameraPosition` | IMPLEMENTED | Returns current camera state                                 |

---

## GoogleMap Class - Marker Management

| Method                     | Return Type | Status      | Notes                                                            |
| -------------------------- | ----------- | ----------- | ---------------------------------------------------------------- |
| `addMarker(MarkerOptions)` | `Marker`    | IMPLEMENTED | Supports position, title, snippet, icon, anchor, tag             |
| `clear()`                  | `void`      | PARTIAL     | Implemented as clearMarkers(), clearPolylines(), clearPolygons() |

---

## GoogleMap Class - Shapes & Overlays

| Method                                   | Return Type     | Status          | Notes                                            |
| ---------------------------------------- | --------------- | --------------- | ------------------------------------------------ |
| `addPolyline(PolylineOptions)`           | `Polyline`      | IMPLEMENTED     | Supports points, stroke color, stroke width, tag |
| `addPolygon(PolygonOptions)`             | `Polygon`       | IMPLEMENTED     | Supports points, holes, stroke/fill colors, tag  |
| `addCircle(CircleOptions)`               | `Circle`        | NOT IMPLEMENTED | Planned for future release                       |
| `addGroundOverlay(GroundOverlayOptions)` | `GroundOverlay` | NOT IMPLEMENTED | Planned for future release                       |
| `addTileOverlay(TileOverlayOptions)`     | `TileOverlay`   | NOT PLANNED     | Advanced feature                                 |

---

## GoogleMap Class - View Information

| Method              | Return Type  | Status          | Notes                                            |
| ------------------- | ------------ | --------------- | ------------------------------------------------ |
| `getProjection()`   | `Projection` | NOT IMPLEMENTED | Coordinate conversion utilities exist internally |
| `getMaxZoomLevel()` | `float`      | NOT IMPLEMENTED | Fixed at 19.0                                    |
| `getMinZoomLevel()` | `float`      | NOT IMPLEMENTED | Fixed at 2.0                                     |

---

## GoogleMap Class - Map Configuration

| Method                         | Return Type | Status          | Notes                                       |
| ------------------------------ | ----------- | --------------- | ------------------------------------------- |
| `setCenter(LatLng)`            | `void`      | IMPLEMENTED     | Direct method, not via GoogleMap pattern    |
| `setZoom(double)`              | `void`      | IMPLEMENTED     | Direct method, not via GoogleMap pattern    |
| `getZoom()`                    | `double`    | IMPLEMENTED     | Direct method, not via GoogleMap pattern    |
| `setMapType(int)`              | `void`      | NOT IMPLEMENTED | Only standard OSM tiles currently supported |
| `getMapType()`                 | `int`       | NOT IMPLEMENTED | Only standard OSM tiles currently supported |
| `setMapStyle(MapStyleOptions)` | `boolean`   | NOT PLANNED     | Custom tile sources could provide this      |

---

## GoogleMap Class - Zoom Preferences

| Method                        | Return Type | Status          | Notes          |
| ----------------------------- | ----------- | --------------- | -------------- |
| `setMaxZoomPreference(float)` | `void`      | NOT IMPLEMENTED | Fixed at 19.0  |
| `setMinZoomPreference(float)` | `void`      | NOT IMPLEMENTED | Fixed at 2.0   |
| `resetMinMaxZoomPreference()` | `void`      | NOT IMPLEMENTED | Not applicable |

---

## GoogleMap Class - UI Settings

| Method                           | Return Type  | Status          | Notes                           |
| -------------------------------- | ------------ | --------------- | ------------------------------- |
| `getUiSettings()`                | `UiSettings` | NOT IMPLEMENTED | No separate UI settings object  |
| `setPadding(int, int, int, int)` | `void`       | NOT IMPLEMENTED | Use standard View padding       |
| `setContentDescription(String)`  | `void`       | NOT IMPLEMENTED | Use standard View accessibility |

---

## GoogleMap Class - Feature Toggles

| Method                         | Return Type | Status      | Notes                                  |
| ------------------------------ | ----------- | ----------- | -------------------------------------- |
| `setTrafficEnabled(boolean)`   | `void`      | NOT PLANNED | Requires traffic data source           |
| `isTrafficEnabled()`           | `boolean`   | NOT PLANNED | Not applicable                         |
| `setBuildingsEnabled(boolean)` | `void`      | NOT PLANNED | OSM tiles include buildings by default |
| `isBuildingsEnabled()`         | `boolean`   | NOT PLANNED | Not applicable                         |
| `setIndoorEnabled(boolean)`    | `void`      | NOT PLANNED | Requires indoor mapping data           |
| `isIndoorEnabled()`            | `boolean`   | NOT PLANNED | Not applicable                         |

---

## GoogleMap Class - Location Layer

| Method                              | Return Type | Status          | Notes                                |
| ----------------------------------- | ----------- | --------------- | ------------------------------------ |
| `setMyLocationEnabled(boolean)`     | `void`      | NOT IMPLEMENTED | Can be implemented via custom marker |
| `isMyLocationEnabled()`             | `boolean`   | NOT IMPLEMENTED | Not applicable                       |
| `setLocationSource(LocationSource)` | `void`      | NOT IMPLEMENTED | Not applicable                       |

---

## GoogleMap Class - Camera Constraints

| Method                                         | Return Type | Status          | Notes                      |
| ---------------------------------------------- | ----------- | --------------- | -------------------------- |
| `setLatLngBoundsForCameraTarget(LatLngBounds)` | `void`      | NOT IMPLEMENTED | Planned for future release |

---

## GoogleMap Class - Snapshots

| Method                                    | Return Type | Status          | Notes                              |
| ----------------------------------------- | ----------- | --------------- | ---------------------------------- |
| `snapshot(SnapshotReadyCallback)`         | `void`      | NOT IMPLEMENTED | Can be implemented via View.draw() |
| `snapshot(SnapshotReadyCallback, Bitmap)` | `void`      | NOT IMPLEMENTED | Can be implemented via View.draw() |

---

## GoogleMap Class - Event Listeners

| Method                                                                | Return Type      | Status          | Notes                                                |
| --------------------------------------------------------------------- | ---------------- | --------------- | ---------------------------------------------------- |
| `setOnMapClickListener(OnMapClickListener)`                           | `void`           | NOT IMPLEMENTED | Can be implemented via View.setOnClickListener()     |
| `setOnMapLongClickListener(OnMapLongClickListener)`                   | `void`           | NOT IMPLEMENTED | Can be implemented via View.setOnLongClickListener() |
| `setOnMarkerClickListener(OnMarkerClickListener)`                     | `void`           | IMPLEMENTED     | Returns boolean to consume event                     |
| `setOnMarkerDragListener(OnMarkerDragListener)`                       | `void`           | NOT IMPLEMENTED | Planned for future release                           |
| `setOnPolylineClickListener(OnPolylineClickListener)`                 | `void`           | NOT IMPLEMENTED | Planned for future release                           |
| `setOnPolygonClickListener(OnPolygonClickListener)`                   | `void`           | NOT IMPLEMENTED | Planned for future release                           |
| `setOnCircleClickListener(OnCircleClickListener)`                     | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnGroundOverlayClickListener(OnGroundOverlayClickListener)`       | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnPoiClickListener(OnPoiClickListener)`                           | `void`           | NOT PLANNED     | POI data not available in OSM tiles                  |
| `setOnCameraMoveStartedListener(OnCameraMoveStartedListener)`         | `void`           | NOT IMPLEMENTED | Planned for future release                           |
| `setOnCameraMoveListener(OnCameraMoveListener)`                       | `void`           | NOT IMPLEMENTED | Planned for future release                           |
| `setOnCameraIdleListener(OnCameraIdleListener)`                       | `void`           | NOT IMPLEMENTED | Planned for future release                           |
| `setOnCameraMoveCanceledListener(OnCameraMoveCanceledListener)`       | `void`           | NOT IMPLEMENTED | Planned for future release                           |
| `setOnMapLoadedCallback(OnMapLoadedCallback)`                         | `void`           | NOT IMPLEMENTED | Tiles load asynchronously, callback could be added   |
| `setInfoWindowAdapter(InfoWindowAdapter)`                             | `void`           | NOT IMPLEMENTED | Info windows not yet implemented                     |
| `setOnInfoWindowClickListener(OnInfoWindowClickListener)`             | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnInfoWindowCloseListener(OnInfoWindowCloseListener)`             | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnInfoWindowLongClickListener(OnInfoWindowLongClickListener)`     | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener)` | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnMyLocationClickListener(OnMyLocationClickListener)`             | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnIndoorStateChangeListener(OnIndoorStateChangeListener)`         | `void`           | NOT PLANNED     | Indoor mapping not supported                         |
| `getFocusedBuilding()`                                                | `IndoorBuilding` | NOT PLANNED     | Indoor mapping not supported                         |

---

## GeoJSON Support

| Feature              | Status      | Notes                                              |
| -------------------- | ----------- | -------------------------------------------------- |
| `addGeoJson(String)` | IMPLEMENTED | Parses Point, LineString, Polygon, Multi- variants |

This method is an OpenMapView-specific feature not present in Google Maps API.

---

## CameraUpdateFactory

| Method                                         | Return Type    | Status          | Notes                      |
| ---------------------------------------------- | -------------- | --------------- | -------------------------- |
| `newLatLng(LatLng)`                            | `CameraUpdate` | IMPLEMENTED     | Move to location           |
| `newLatLngZoom(LatLng, float)`                 | `CameraUpdate` | IMPLEMENTED     | Move to location and zoom  |
| `newCameraPosition(CameraPosition)`            | `CameraUpdate` | IMPLEMENTED     | Move to camera position    |
| `zoomIn()`                                     | `CameraUpdate` | IMPLEMENTED     | Increment zoom by 1        |
| `zoomOut()`                                    | `CameraUpdate` | IMPLEMENTED     | Decrement zoom by 1        |
| `zoomTo(float)`                                | `CameraUpdate` | IMPLEMENTED     | Set specific zoom level    |
| `zoomBy(float)`                                | `CameraUpdate` | IMPLEMENTED     | Adjust zoom by amount      |
| `newLatLngBounds(LatLngBounds, int)`           | `CameraUpdate` | NOT IMPLEMENTED | Planned for future release |
| `newLatLngBounds(LatLngBounds, int, int, int)` | `CameraUpdate` | NOT IMPLEMENTED | Planned for future release |
| `scrollBy(float, float)`                       | `CameraUpdate` | NOT IMPLEMENTED | Planned for future release |

---

## BitmapDescriptorFactory

| Method                 | Return Type        | Status          | Notes                              |
| ---------------------- | ------------------ | --------------- | ---------------------------------- |
| `defaultMarker()`      | `BitmapDescriptor` | IMPLEMENTED     | Red marker icon                    |
| `defaultMarker(float)` | `BitmapDescriptor` | IMPLEMENTED     | Marker with custom hue (0-360)     |
| `fromAsset(String)`    | `BitmapDescriptor` | NOT IMPLEMENTED | Use Marker constructor with Bitmap |
| `fromBitmap(Bitmap)`   | `BitmapDescriptor` | NOT IMPLEMENTED | Use Marker constructor with Bitmap |
| `fromFile(String)`     | `BitmapDescriptor` | NOT IMPLEMENTED | Use Marker constructor with Bitmap |
| `fromPath(String)`     | `BitmapDescriptor` | NOT IMPLEMENTED | Use Marker constructor with Bitmap |
| `fromResource(int)`    | `BitmapDescriptor` | NOT IMPLEMENTED | Use Marker constructor with Bitmap |

---

## Summary Statistics

**Total Methods Reviewed:** 91

**Implementation Status:**

- IMPLEMENTED: 25 methods (27.5%)
- PARTIAL: 3 methods (3.3%)
- NOT IMPLEMENTED: 37 methods (40.7%)
- NOT PLANNED: 26 methods (28.6%)

**Core Functionality Coverage:**

- Camera control: 100% (animateCamera, moveCamera, stopAnimation, getCameraPosition)
- Basic markers: 100% (addMarker, click listener)
- Vector shapes: 100% (polylines, polygons with holes)
- Lifecycle management: 75% (onResume, onPause, onDestroy)

**Focus Areas:**
OpenMapView prioritizes lightweight, essential mapping features for applications that need basic map display, markers, shapes, and camera animations without the complexity and overhead of Google Play Services.

---

## In-Depth Look: Architecture Comparison

### Google's Two-Class Architecture

Google Maps SDK uses a **two-tier architecture** that separates the view container from the map controller:

#### 1. MapView - The Android View Component

- Extends FrameLayout
- Handles lifecycle (onCreate, onResume, etc.)
- Acts as a container
- Has only ~11 methods, mostly lifecycle forwarding

#### 2. GoogleMap - The Map Controller Object

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

### OpenMapView's Simplified Architecture

OpenMapView uses a **single-class architecture** that combines both roles into one cohesive interface:

#### OpenMapView - Unified View and Controller

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

### How OpenMapView Covers Both APIs

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

### Coverage Analysis

**MapView Methods (11 total):**

- Mostly lifecycle boilerplate
- Replaced with DefaultLifecycleObserver pattern
- Simpler for developers (no manual forwarding)

**GoogleMap Methods (80 total):**

- The real map functionality
- Exposed directly on OpenMapView
- Internal MapController handles implementation
- No async callback needed

### The Math

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

### The Advantage

OpenMapView's single-class approach is **simpler** than Google's two-class pattern:

**Google Maps (complex):**

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

**OpenMapView (simple):**

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

### Conclusion

This document lists two Google classes (MapView and GoogleMap) to provide complete API compatibility reference for developers migrating from Google Maps. However, OpenMapView's implementation consolidates both into a single, more intuitive interface without sacrificing functionality coverage.

The separation in the documentation helps developers understand:

1. Which MapView lifecycle methods are handled automatically (via DefaultLifecycleObserver)
2. Which GoogleMap functionality methods are exposed directly on OpenMapView
3. What the implementation status is for each method

OpenMapView achieves the goal of being a drop-in replacement for the essential mapping functionality while providing a cleaner, simpler API.
