# Google MapView Public API Compatibility

[Back to README](../README.md) | [Architecture](ARCHITECTURE.md) | [API Documentation (KDoc)](https://afarber.github.io/OpenMapView/)

This document lists all public non-deprecated methods from Google's MapView and GoogleMap classes and notes any differences in OpenMapView's implementation.

---

## MapView Class

| Method                            | Return Type | Notes                                                              |
| --------------------------------- | ----------- | ------------------------------------------------------------------ |
| `onCreate(Bundle)`                | `void`      | Not needed - OpenMapView uses DefaultLifecycleObserver pattern     |
| `onStart()`                       | `void`      | Not needed - OpenMapView uses DefaultLifecycleObserver pattern     |
| `onResume()`                      | `void`      |                                                                    |
| `onPause()`                       | `void`      |                                                                    |
| `onStop()`                        | `void`      | Not needed - OpenMapView uses DefaultLifecycleObserver pattern     |
| `onDestroy()`                     | `void`      |                                                                    |
| `onSaveInstanceState(Bundle)`     | `void`      | Not implemented                                                     |
| `onLowMemory()`                   | `void`      | Not implemented                                                     |
| `getMapAsync(OnMapReadyCallback)` | `void`      | Not needed - map is immediately available after view creation      |
| `onEnterAmbient(Bundle)`          | `void`      | Not implemented - wearable-specific feature                         |
| `onExitAmbient()`                 | `void`      | Not implemented - wearable-specific feature                         |

---

## GoogleMap Class - Camera Movement

| Method                                                 | Return Type      | Notes |
| ------------------------------------------------------ | ---------------- | ----- |
| `animateCamera(CameraUpdate)`                          | `void`           |       |
| `animateCamera(CameraUpdate, CancelableCallback)`      | `void`           |       |
| `animateCamera(CameraUpdate, int, CancelableCallback)` | `void`           |       |
| `moveCamera(CameraUpdate)`                             | `void`           |       |
| `stopAnimation()`                                      | `void`           |       |
| `getCameraPosition()`                                  | `CameraPosition` |       |

---

## GoogleMap Class - Marker Management

| Method                     | Return Type | Notes |
| -------------------------- | ----------- | ----- |
| `addMarker(MarkerOptions)` | `Marker`    |       |
| `clear()`                  | `void`      |       |
| `showInfoWindow(Marker)`   | `void`      | Shows marker's info window with auto-dismiss support |
| `hideInfoWindow(Marker)`   | `void`      | Hides marker's info window and cancels auto-dismiss  |

---

## Marker Class

| Method             | Return Type | Notes                                                                |
| ------------------ | ----------- | -------------------------------------------------------------------- |
| `showInfoWindow()` | `void`      | Shows this marker's info window (auto-dismiss if configured via UiSettings) |
| `hideInfoWindow()` | `void`      | Hides this marker's info window                                      |
| `isInfoWindowShown`| `Boolean`   | Returns whether this marker's info window is currently shown         |

---

## GoogleMap Class - Shapes & Overlays

| Method                                   | Return Type     | Notes                                                                                    |
| ---------------------------------------- | --------------- | ---------------------------------------------------------------------------------------- |
| `addPolyline(PolylineOptions)`           | `Polyline`      |                                                                                          |
| `addPolygon(PolygonOptions)`             | `Polygon`       |                                                                                          |
| `addCircle(CircleOptions)`               | `Circle`        |                                                                                          |
| `addGroundOverlay(GroundOverlayOptions)` | `GroundOverlay` |                                                                                          |
| `addTileOverlay(TileOverlayOptions)`     | `TileOverlay`   | Includes predefined providers for public OSM services (OpenSeaMap, OpenRailwayMap, etc.) |

---

## Polyline, Polygon, and Circle Styling

OpenMapView uses Jetpack Compose graphics primitives for advanced stroke styling. This provides better color space support, modern Android API integration, and more flexible styling options.

### Color API

| Google Maps API                | OpenMapView API                                   | Notes                                                 |
| ------------------------------ | ------------------------------------------------- | ----------------------------------------------------- |
| `setColor(int)`                | `strokeColor: androidx.compose.ui.graphics.Color` | Uses Compose Color instead of Android Color           |
| `setFillColor(int)`            | `fillColor: androidx.compose.ui.graphics.Color`   | Uses Compose Color instead of Android Color           |
| `Color.RED`                    | `Color.Red`                                       | Compose uses capitalized color names                  |
| `Color.argb(128, 255, 0, 0)`   | `Color(red = 255, green = 0, blue = 0, alpha = 128)` | Named parameters instead of positional            |

**Migration Example:**

```kotlin
// Google Maps
import android.graphics.Color
val polyline = PolylineOptions()
    .color(Color.RED)

// OpenMapView
import androidx.compose.ui.graphics.Color
val polyline = PolylineOptions()
    .color(Color.Red)
```

### Stroke Styling API

| Google Maps Property | OpenMapView Equivalent | Notes |
|---------------------|------------------------|-------|
| `setWidth(float)` | `strokeWidth: Float` | Same concept, pixels (not dp) |
| `setPattern(List<PatternItem>)` | `strokePattern: PathEffect?` | Different API: uses Compose PathEffect instead of PatternItem list |
| `setStartCap(Cap)` / `setEndCap(Cap)` | `strokeCap: StrokeCap` | Single property for both start and end (OpenMapView uses same cap for both) |
| `setJointType(int)` | `strokeJoin: StrokeJoin` | Uses Compose StrokeJoin enum instead of int constant |

**PathEffect Examples:**

```kotlin
import androidx.compose.ui.graphics.PathEffect

// Dashed line (20px dash, 10px gap)
val dashed = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
Polyline(points = points, strokePattern = dashed)

// Dotted line (2px dot, 8px gap) - use Round cap for circular dots
val dotted = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)
Polyline(points = points, strokePattern = dotted, strokeCap = StrokeCap.Round)

// Chain effects (e.g., railroad tracks)
val railroad = PathEffect.chainPathEffect(
    outer = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), 0f),
    inner = PathEffect.cornerPathEffect(3f)
)
Polyline(points = points, strokePattern = railroad)
```

**StrokeCap Options:**

- `StrokeCap.Butt` - Flat edge at endpoint (default for precision)
- `StrokeCap.Round` - Rounded cap extending beyond endpoint
- `StrokeCap.Square` - Square cap extending beyond endpoint

**StrokeJoin Options:**

- `StrokeJoin.Miter` - Sharp pointed corners (can extend on acute angles)
- `StrokeJoin.Round` - Rounded corners (default for smooth appearance)
- `StrokeJoin.Bevel` - Flat diagonal cut at corners

**Complete Styling Example:**

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

val styledPolyline = Polyline(
    points = listOf(LatLng(51.5, 0.0), LatLng(51.6, 0.1)),
    strokeColor = Color(0xFF0066CC),  // Custom blue
    strokeWidth = 12f,
    strokePattern = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f),
    strokeCap = StrokeCap.Round,
    strokeJoin = StrokeJoin.Bevel
)
```

These properties apply to:
- `Polyline` - All stroke styling properties
- `Polygon` - All stroke styling properties (applies to outline)
- `Circle` - All stroke styling properties (applies to outline)

---

## GoogleMap Class - View Information

| Method              | Return Type  | Notes |
| ------------------- | ------------ | ----- |
| `getProjection()`   | `Projection` |       |
| `getMaxZoomLevel()` | `float`      |       |
| `getMinZoomLevel()` | `float`      |       |

---

## GoogleMap Class - Map Configuration

| Method                         | Return Type | Notes                                                            |
| ------------------------------ | ----------- | ---------------------------------------------------------------- |
| `setCenter(LatLng)`            | `void`      | Direct method on OpenMapView (not via separate GoogleMap object) |
| `setZoom(double)`              | `void`      | Direct method on OpenMapView (not via separate GoogleMap object) |
| `getZoom()`                    | `double`    | Direct method on OpenMapView (not via separate GoogleMap object) |
| `setMapType(int)`              | `void`      | Supports NORMAL, TERRAIN, HUMANITARIAN, CYCLE, NONE              |
| `getMapType()`                 | `int`       |                                                                  |
| `setMapStyle(MapStyleOptions)` | `boolean`   | Not implemented                                                   |

---

## GoogleMap Class - Zoom Preferences

| Method                        | Return Type | Notes |
| ----------------------------- | ----------- | ----- |
| `setMaxZoomPreference(float)` | `void`      |       |
| `setMinZoomPreference(float)` | `void`      |       |
| `resetMinMaxZoomPreference()` | `void`      |       |

---

## GoogleMap Class - UI Settings

| Method                           | Return Type  | Notes                                                                   |
| -------------------------------- | ------------ | ----------------------------------------------------------------------- |
| `getUiSettings()`                | `UiSettings` |                                                                         |
| `setPadding(int, int, int, int)` | `void`       | Implemented as setMapPadding() to avoid conflict with View.setPadding() |
| `setContentDescription(String)`  | `void`       | Inherited from View class                                               |

---

## UiSettings Class - Methods

Methods available on the UiSettings object returned by `getUiSettings()`:

| Method                                                | Return Type | Notes                                                                                    |
| ----------------------------------------------------- | ----------- | ---------------------------------------------------------------------------------------- |
| `setZoomGesturesEnabled(boolean)`                     | `void`      |                                                                                          |
| `isZoomGesturesEnabled()`                             | `boolean`   |                                                                                          |
| `setScrollGesturesEnabled(boolean)`                   | `void`      |                                                                                          |
| `isScrollGesturesEnabled()`                           | `boolean`   |                                                                                          |
| `setZoomControlsEnabled(boolean)`                     | `void`      |                                                                                          |
| `isZoomControlsEnabled()`                             | `boolean`   |                                                                                          |
| `setScrollGesturesEnabledDuringRotateOrZoom(boolean)` | `void`      |                                                                                          |
| `isScrollGesturesEnabledDuringRotateOrZoom()`         | `boolean`   |                                                                                          |
| `setRotateGesturesEnabled(boolean)`                   | `void`      | Not implemented                                                                           |
| `isRotateGesturesEnabled()`                           | `boolean`   | Always returns false                                                                     |
| `setTiltGesturesEnabled(boolean)`                     | `void`      | Not implemented                                                                           |
| `isTiltGesturesEnabled()`                             | `boolean`   | Always returns false                                                                     |
| `setCompassEnabled(boolean)`                          | `void`      | Not implemented                                                                           |
| `isCompassEnabled()`                                  | `boolean`   | Always returns false                                                                     |
| `setMyLocationButtonEnabled(boolean)`                 | `void`      | Not implemented                                                                           |
| `isMyLocationButtonEnabled()`                         | `boolean`   | Always returns false                                                                     |
| `setIndoorLevelPickerEnabled(boolean)`                | `void`      | Not implemented                                                                           |
| `isIndoorLevelPickerEnabled()`                        | `boolean`   | Always returns false                                                                     |
| `setMapToolbarEnabled(boolean)`                       | `void`      | Not implemented - use openInExternalApp() instead (see External Map Integration section) |
| `isMapToolbarEnabled()`                               | `boolean`   | Always returns false                                                                     |
| `setAllGesturesEnabled(boolean)`                      | `void`      |                                                                                          |
| `infoWindowAutoDismiss`                               | `Duration`  | OpenMapView-specific: auto-dismiss info windows after duration (ZERO = disabled)         |

---

## GoogleMap Class - Feature Toggles

| Method                         | Return Type | Notes                                                |
| ------------------------------ | ----------- | ---------------------------------------------------- |
| `setTrafficEnabled(boolean)`   | `void`      | Not implemented                                       |
| `isTrafficEnabled()`           | `boolean`   | Always returns false                                 |
| `setBuildingsEnabled(boolean)` | `void`      | Not implemented - OSM tiles include buildings by default |
| `isBuildingsEnabled()`         | `boolean`   | Always returns true                                  |
| `setIndoorEnabled(boolean)`    | `void`      | Not implemented                                       |
| `isIndoorEnabled()`            | `boolean`   | Always returns false                                 |

---

## GoogleMap Class - Location Layer

| Method                              | Return Type | Notes                                                                               |
| ----------------------------------- | ----------- | ----------------------------------------------------------------------------------- |
| `setMyLocationEnabled(boolean)`     | `void`      | Not implemented - can be achieved via custom marker                                 |
| `isMyLocationEnabled()`             | `boolean`   | Always returns false                                                                |
| `setLocationSource(LocationSource)` | `void`      | Not implemented - can be achieved via custom location tracking and marker placement |

---

## GoogleMap Class - Camera Constraints

| Method                                         | Return Type | Notes |
| ---------------------------------------------- | ----------- | ----- |
| `setLatLngBoundsForCameraTarget(LatLngBounds)` | `void`      |       |

---

## GoogleMap Class - Snapshots

| Method                                    | Return Type | Notes |
| ----------------------------------------- | ----------- | ----- |
| `snapshot(SnapshotReadyCallback)`         | `void`      |       |
| `snapshot(SnapshotReadyCallback, Bitmap)` | `void`      |       |

---

## GoogleMap Class - Event Listeners

OpenMapView provides comprehensive event listener support using Kotlin `fun interface` for single-method listeners (enabling lambda syntax) and regular `interface` for multi-method listeners. All callbacks execute on the UI thread.

| Method                                                                | Return Type      | Notes                                                                |
| --------------------------------------------------------------------- | ---------------- | -------------------------------------------------------------------- |
| `setOnMapClickListener(OnMapClickListener)`                           | `void`           |                                                                      |
| `setOnMapLongClickListener(OnMapLongClickListener)`                   | `void`           |                                                                      |
| `setOnMarkerClickListener(OnMarkerClickListener)`                     | `void`           |                                                                      |
| `setOnMarkerDragListener(OnMarkerDragListener)`                       | `void`           |                                                                      |
| `setOnPolylineClickListener(OnPolylineClickListener)`                 | `void`           |                                                                      |
| `setOnPolygonClickListener(OnPolygonClickListener)`                   | `void`           |                                                                      |
| `setOnCircleClickListener(OnCircleClickListener)`                     | `void`           |                                                                      |
| `setOnGroundOverlayClickListener(OnGroundOverlayClickListener)`       | `void`           |                                                                      |
| `setOnPoiClickListener(OnPoiClickListener)`                           | `void`           | Not implemented - POI data not available in OSM tiles                 |
| `setOnCameraMoveStartedListener(OnCameraMoveStartedListener)`         | `void`           |                                                                      |
| `setOnCameraMoveListener(OnCameraMoveListener)`                       | `void`           |                                                                      |
| `setOnCameraIdleListener(OnCameraIdleListener)`                       | `void`           |                                                                      |
| `setOnCameraMoveCanceledListener(OnCameraMoveCanceledListener)`       | `void`           |                                                                      |
| `setOnMapLoadedCallback(OnMapLoadedCallback)`                         | `void`           | Not implemented - tiles load asynchronously, callback could be added |
| `setInfoWindowAdapter(InfoWindowAdapter)`                             | `void`           | Not implemented - custom adapters not yet implemented                |
| `setOnInfoWindowClickListener(OnInfoWindowClickListener)`             | `void`           |                                                                      |
| `setOnInfoWindowCloseListener(OnInfoWindowCloseListener)`             | `void`           | Called when info window is closed (manual or auto-dismiss)           |
| `setOnInfoWindowLongClickListener(OnInfoWindowLongClickListener)`     | `void`           | Not implemented                                                      |
| `setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener)` | `void`           | Not implemented                                                      |
| `setOnMyLocationClickListener(OnMyLocationClickListener)`             | `void`           | Not implemented                                                      |
| `setOnIndoorStateChangeListener(OnIndoorStateChangeListener)`         | `void`           | Not implemented - indoor mapping not supported                        |
| `getFocusedBuilding()`                                                | `IndoorBuilding` | Not implemented - indoor mapping not supported                        |

**Event Priority (highest to lowest):**

1. Info window click (if info window is showing)
2. Marker click (returns boolean - true consumes event)
3. Ground overlay click (highest z-index first)
4. Circle click (highest z-index first)
5. Polygon click (highest z-index first)
6. Polyline click (highest z-index first)
7. Map click (fires if nothing else consumed event)

**Camera Event Sequence:**

- **Successful animation:** onCameraMoveStarted(reason) → onCameraMove\* → onCameraIdle
- **Interrupted animation:** onCameraMoveStarted(reason) → onCameraMove\* → onCameraMoveCanceled → onCameraMoveStarted(REASON_GESTURE) → ...

**Kotlin Lambda Syntax Examples:**

```kotlin
// Single-method listeners support lambda syntax
mapView.setOnMapClickListener { latLng ->
    Log.d("Map", "Clicked: ${latLng.latitude}, ${latLng.longitude}")
}

mapView.setOnMarkerClickListener { marker ->
    Toast.makeText(context, marker.title, Toast.LENGTH_SHORT).show()
    true  // Consume event (prevents info window)
}

// Multi-method listeners use object syntax
mapView.setOnMarkerDragListener(object : OnMarkerDragListener {
    override fun onMarkerDragStart(marker: Marker) { }
    override fun onMarkerDrag(marker: Marker) { }
    override fun onMarkerDragEnd(marker: Marker) { }
})
```

---

## GeoJSON Support

| Feature              | Return Type | Notes                                                 |
| -------------------- | ----------- | ----------------------------------------------------- |
| `addGeoJson(String)` | `void`      | OpenMapView-specific feature (not in Google Maps API) |

---

## External Map Integration

| Feature                     | Return Type | Notes                                                                                                                                                  |
| --------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `openInExternalApp(String)` | `void`      | OpenMapView-specific feature (not in Google Maps API). Opens location in external apps via geo: URI. See [MAP_TOOLBAR.md](MAP_TOOLBAR.md) for details. |

---

## CameraUpdateFactory

| Method                                         | Return Type    | Notes |
| ---------------------------------------------- | -------------- | ----- |
| `newLatLng(LatLng)`                            | `CameraUpdate` |       |
| `newLatLngZoom(LatLng, float)`                 | `CameraUpdate` |       |
| `newCameraPosition(CameraPosition)`            | `CameraUpdate` |       |
| `zoomIn()`                                     | `CameraUpdate` |       |
| `zoomOut()`                                    | `CameraUpdate` |       |
| `zoomTo(float)`                                | `CameraUpdate` |       |
| `zoomBy(float)`                                | `CameraUpdate` |       |
| `newLatLngBounds(LatLngBounds, int)`           | `CameraUpdate` |       |
| `newLatLngBounds(LatLngBounds, int, int, int)` | `CameraUpdate` |       |
| `scrollBy(float, float)`                       | `CameraUpdate` |       |

---

## BitmapDescriptorFactory

| Method                 | Return Type        | Notes |
| ---------------------- | ------------------ | ----- |
| `defaultMarker()`      | `BitmapDescriptor` |       |
| `defaultMarker(float)` | `BitmapDescriptor` |       |
| `fromAsset(String)`    | `BitmapDescriptor` |       |
| `fromBitmap(Bitmap)`   | `BitmapDescriptor` |       |
| `fromFile(String)`     | `BitmapDescriptor` |       |
| `fromPath(String)`     | `BitmapDescriptor` |       |
| `fromResource(int)`    | `BitmapDescriptor` |       |

---

## Summary

**Total Methods Reviewed:** 102 (includes UiSettings class methods)

**Implementation Summary:**

- 83 methods fully implemented (81.4%)
- 19 methods not planned (18.6%) - mostly features requiring Google-specific data (traffic, indoor maps, POI) or unsupported map capabilities (rotation, tilt)

**OpenMapView-Specific Features:**

In addition to Google Maps API compatibility, OpenMapView provides:

- **GeoJSON import** (`addGeoJson`) - supports Point, LineString, Polygon, Multi\* variants, FeatureCollection
- **External map integration** (`openInExternalApp`) - opens location in external map apps via geo: URI with OpenStreetMap browser fallback
- **Built-in zoom controls** (`UiSettings.isZoomControlsEnabled`) - circular +/- buttons rendered automatically
- **Map types** - 5 types: NONE, NORMAL, TERRAIN, HUMANITARIAN, CYCLE (uses different OSM tile sources)

**Key Architectural Differences:**

OpenMapView uses a **single-class architecture** (vs. Google Maps' MapView + GoogleMap pattern):

- Map methods called directly on `OpenMapView` instance (e.g., `mapView.setCenter()`, `mapView.getZoom()`)
- No async initialization needed - map ready immediately after view creation
- Uses `DefaultLifecycleObserver` pattern - no manual lifecycle forwarding required
- Just call `lifecycle.addObserver(mapView)` once in Activity/Fragment

---

For an in-depth explanation of OpenMapView's architectural design and how it compares to Google Maps SDK, see [ARCHITECTURE.md](ARCHITECTURE.md).
