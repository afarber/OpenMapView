# Public API Compatibility

[Back to README](../README.md) | [Architecture](ARCHITECTURE.md) | [API Documentation (KDoc)](https://afarber.github.io/OpenMapView/)

This document lists all public non-deprecated methods of the `MapView` and `GoogleMap` classes of the Google Maps SDK for Android (below: Maps SDK) and notes any differences in OpenMapView's implementation.

---

## MapView Class

| Method                            | Return Type | Notes                                                              |
| --------------------------------- | ----------- | ------------------------------------------------------------------ |
| `onCreate(Bundle)`                | `void`      | Not needed - OpenMapView manages its lifecycle automatically       |
| `onStart()`                       | `void`      | Not needed - OpenMapView manages its lifecycle automatically       |
| `onResume()`                      | `void`      |                                                                    |
| `onPause()`                       | `void`      |                                                                    |
| `onStop()`                        | `void`      | Not needed - OpenMapView manages its lifecycle automatically       |
| `onDestroy()`                     | `void`      | Optional - called automatically when the view is detached          |
| `onSaveInstanceState(Bundle)`     | `void`      | Not implemented                                                     |
| `onLowMemory()`                   | `void`      | Not implemented                                                     |
| `getMapAsync(OnMapReadyCallback)` | `void`      | Not needed - map is immediately available after view creation      |
| `onEnterAmbient(Bundle)`          | `void`      | Not implemented - wearable-specific feature                         |
| `onExitAmbient()`                 | `void`      | Not implemented - wearable-specific feature                         |

---

## Camera Movement

| Method                                                 | Return Type      | Notes |
| ------------------------------------------------------ | ---------------- | ----- |
| `animateCamera(CameraUpdate)`                          | `void`           |       |
| `animateCamera(CameraUpdate, CancelableCallback)`      | `void`           |       |
| `animateCamera(CameraUpdate, int, CancelableCallback)` | `void`           |       |
| `moveCamera(CameraUpdate)`                             | `void`           |       |
| `stopAnimation()`                                      | `void`           |       |
| `getCameraPosition()`                                  | `CameraPosition` |       |

---

## Marker Management

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

## Shapes & Overlays

| Method                                   | Return Type     | Notes                                                                                    |
| ---------------------------------------- | --------------- | ---------------------------------------------------------------------------------------- |
| `addPolyline(PolylineOptions)`           | `Polyline`      |                                                                                          |
| `addPolygon(PolygonOptions)`             | `Polygon`       |                                                                                          |
| `addCircle(CircleOptions)`               | `Circle`        |                                                                                          |
| `addGroundOverlay(GroundOverlayOptions)` | `GroundOverlay` |                                                                                          |
| `addTileOverlay(TileOverlayOptions)`     | `TileOverlay`   | Includes predefined providers for public OSM services (OpenSeaMap, OpenRailwayMap, etc.) |

### Polyline, Polygon and Circle Handles

The returned `Polyline`, `Polygon` and `Circle` are live handles, as in the Maps SDK: setting a property redraws the map right away, and `remove()` takes the shape off the map.

| Maps SDK | OpenMapView | Notes |
|---|---|---|
| `Polyline.setPoints`, `setGeodesic`, `setVisible`, `setClickable`, `setZIndex`, `setTag` and getters | Same | Kotlin properties `points`, `geodesic`, `visible`, ... |
| `Polyline.setColor(int)`, `getColor()`, `setWidth(float)`, `getWidth()` | Same | Kotlin: `strokeColor` (Compose `Color`), `strokeColorArgb`, `strokeWidth` |
| `Polygon` / `Circle` `setStrokeColor(int)`, `getStrokeColor()`, `setFillColor(int)`, `getFillColor()`, `setStrokeWidth(float)` | Same | Kotlin: `strokeColor`, `fillColor` (Compose `Color`), `strokeColorArgb`, `fillColorArgb`, `strokeWidth` |
| `Polygon.setHoles`, `Circle.setCenter`, `setRadius` | Same | |
| `setPattern(List<PatternItem>)`, `setStartCap`, `setEndCap`, `setJointType` | `strokePattern`, `startCap`, `endCap`, `strokeJoin` | Compose `PathEffect`, `StrokeCap`, `StrokeJoin`, see below |
| `remove()` | Same | |
| `PolylineOptions.add(LatLng...)`, `PolygonOptions.add(LatLng...)` | Same | |

`GroundOverlay` is a live handle too: `setBearing(float)`, `setTransparency(float)`, `setImage(BitmapDescriptor)`, `setZIndex(float)`, `setVisible(boolean)`, `setClickable(boolean)`, `setTag(Object)`, `setPosition(LatLng)`, `setDimensions(float)`, `setDimensions(float, float)`, `setPositionFromBounds(LatLngBounds)` and `remove()`, as in the Maps SDK.

`TileOverlay` is a live handle too: `setTransparency(float)`, `setZIndex(float)`, `setVisible(boolean)`, `setFadeIn(boolean)`, `setTag(Object)`, `clearTileCache()` and `remove()`, as in the Maps SDK. A changed property keeps the overlay's downloaded tiles.

A setter that gets an invalid value (fewer than 2 polyline points or 3 polygon points, a radius of 0 or less) throws `IllegalArgumentException` and keeps the old value. In Kotlin, `copy(...)` returns a new shape with some properties changed, which is not on any map.

---

## Polyline, Polygon, and Circle Styling

OpenMapView uses Jetpack Compose graphics primitives for advanced stroke styling. This provides better color space support, modern Android API integration, and more flexible styling options.

### Color API

| Maps SDK API                   | OpenMapView API                                   | Notes                                                 |
| ------------------------------ | ------------------------------------------------- | ----------------------------------------------------- |
| `setColor(int)`                | `strokeColor: androidx.compose.ui.graphics.Color` | Uses Compose Color instead of Android Color           |
| `setFillColor(int)`            | `fillColor: androidx.compose.ui.graphics.Color`   | Uses Compose Color instead of Android Color           |
| `Color.RED`                    | `Color.Red`                                       | Compose uses capitalized color names                  |
| `Color.argb(128, 255, 0, 0)`   | `Color(red = 255, green = 0, blue = 0, alpha = 128)` | Named parameters instead of positional            |

**Migration Example:**

```kotlin
// Maps SDK
import android.graphics.Color
val polyline = PolylineOptions()
    .color(Color.RED)

// OpenMapView
import androidx.compose.ui.graphics.Color
val polyline = PolylineOptions()
    .color(Color.Red)
```

The `*Options` builders also accept ARGB color ints: `PolylineOptions.color(int)`, and `strokeColor(int)` and `fillColor(int)` on `PolygonOptions` and `CircleOptions`. So `.color(Color.RED)` with `android.graphics.Color` keeps working unchanged. The shapes report their colors as ints through `strokeColorArgb` and `fillColorArgb`.

### Using OpenMapView from Java

Java code calls the same API directly:

- Factory methods are static: `CameraUpdateFactory.newLatLngZoom(...)`, `BitmapDescriptorFactory.defaultMarker(...)`, `LatLngBounds.builder()`, `PredefinedTileProviders.openSeaMap()`, `GeoJsonParser.parse(...)`, `TileSource.fromMapType(...)`
- Shapes are built with the `*Options` builders and int colors, as shown above
- Listeners are functional interfaces, so Java lambdas work: `mapView.setOnMapClickListener(latLng -> ...)`
- `UiSettings.setInfoWindowAutoDismissMillis(long)` replaces the Kotlin `Duration` property

Compose types without a Java-friendly form remain Kotlin-only: the shape constructors and `copy()` (because of their `Color` parameters), and the stroke caps and joins (`StrokeCap`, `StrokeJoin`). Java changes a shape on the map through its setters, e.g. `polyline.setWidth(16f)` and `polygon.setFillColor(0x400000FF)`. `PathEffect` works from Java through `PathEffect.Companion.dashPathEffect(...)`.

### Stroke Styling API

| Maps SDK Property | OpenMapView Equivalent | Notes |
|-------------------|------------------------|-------|
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

## View Information

| Method              | Return Type  | Notes |
| ------------------- | ------------ | ----- |
| `getProjection()`   | `Projection` |       |
| `getMaxZoomLevel()` | `float`      |       |
| `getMinZoomLevel()` | `float`      |       |

---

## Map Configuration

| Method                         | Return Type | Notes                                                            |
| ------------------------------ | ----------- | ---------------------------------------------------------------- |
| `setCenter(LatLng)`            | `void`      | Direct method on OpenMapView (not via a separate map object) |
| `setZoom(float)`               | `void`      | Direct method on OpenMapView (not via a separate map object) |
| `getZoom()`                    | `float`     | Direct method on OpenMapView (not via a separate map object) |
| `setMapType(int)`              | `void`      | Supports all 15 `MapType` constants, see MAP_TYPES.md            |
| `getMapType()`                 | `int`       |                                                                  |
| `setMapStyle(MapStyleOptions)` | `boolean`   | Not implemented                                                   |

---

## Zoom Preferences

| Method                        | Return Type | Notes |
| ----------------------------- | ----------- | ----- |
| `setMaxZoomPreference(float)` | `void`      |       |
| `setMinZoomPreference(float)` | `void`      |       |
| `resetMinMaxZoomPreference()` | `void`      |       |

---

## UI Settings

| Method                           | Return Type  | Notes                                                                   |
| -------------------------------- | ------------ | ----------------------------------------------------------------------- |
| `getUiSettings()`                | `UiSettings` |                                                                         |
| `setPadding(int, int, int, int)` | `void`       | Implemented as setMapPadding() to avoid conflict with View.setPadding(). The camera target is the center of the area inside the padding, for all camera updates, animations and getCameraPosition(); the zoom buttons, my-location button and attribution stay inside it |
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
| `setMyLocationButtonEnabled(boolean)`                 | `void`      | Shown while the my-location layer is enabled; default true |
| `isMyLocationButtonEnabled()`                         | `boolean`   |  |
| `setIndoorLevelPickerEnabled(boolean)`                | `void`      | Not implemented                                                                           |
| `isIndoorLevelPickerEnabled()`                        | `boolean`   | Always returns false                                                                     |
| `setMapToolbarEnabled(boolean)`                       | `void`      | Not implemented - use openInExternalApp() instead (see External Map Integration section) |
| `isMapToolbarEnabled()`                               | `boolean`   | Always returns false                                                                     |
| `setAllGesturesEnabled(boolean)`                      | `void`      |                                                                                          |
| `infoWindowAutoDismiss`                               | `Duration`  | OpenMapView-specific: auto-dismiss info windows after duration (ZERO = disabled)         |

---

## Feature Toggles

| Method                         | Return Type | Notes                                                |
| ------------------------------ | ----------- | ---------------------------------------------------- |
| `setTrafficEnabled(boolean)`   | `void`      | Not implemented                                       |
| `isTrafficEnabled()`           | `boolean`   | Always returns false                                 |
| `setBuildingsEnabled(boolean)` | `void`      | Not implemented - OSM tiles include buildings by default |
| `isBuildingsEnabled()`         | `boolean`   | Always returns true                                  |
| `setIndoorEnabled(boolean)`    | `void`      | Not implemented                                       |
| `isIndoorEnabled()`            | `boolean`   | Always returns false                                 |

---

## Location Layer

| Method                              | Return Type | Notes                                                                               |
| ----------------------------------- | ----------- | ----------------------------------------------------------------------------------- |
| `setMyLocationEnabled(boolean)`     | `void`      | Blue dot with accuracy circle; throws `SecurityException` without location permission, unless a custom source is set |
| `isMyLocationEnabled()`             | `boolean`   |  |
| `setLocationSource(LocationSource)` | `void`      | Default: platform `LocationManager` (GPS and network), no Play Services needed |

---

## Camera Constraints

| Method                                         | Return Type | Notes |
| ---------------------------------------------- | ----------- | ----- |
| `setLatLngBoundsForCameraTarget(LatLngBounds)` | `void`      |       |

---

## Snapshots

| Method                                    | Return Type | Notes |
| ----------------------------------------- | ----------- | ----- |
| `snapshot(SnapshotReadyCallback)`         | `void`      |       |
| `snapshot(SnapshotReadyCallback, Bitmap)` | `void`      |       |

---

## Event Listeners

OpenMapView provides comprehensive event listener support using Kotlin `fun interface` for single-method listeners (enabling lambda syntax) and regular `interface` for multi-method listeners. All callbacks execute on the UI thread.

| Method                                                                | Return Type      | Notes                                                                |
| --------------------------------------------------------------------- | ---------------- | -------------------------------------------------------------------- |
| `setOnMapClickListener(OnMapClickListener)`                           | `void`           |                                                                      |
| `setOnMapLongClickListener(OnMapLongClickListener)`                   | `void`           |                                                                      |
| `setOnMarkerClickListener(OnMarkerClickListener)`                     | `void`           | Runs first; unless it returns true, the info window opens and the camera moves to the marker |
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
| `setOnMapLoadedCallback(OnMapLoadedCallback)`                         | `void`           | Fires once when all visible base tiles are drawn (tile overlays are not awaited) |
| `setInfoWindowAdapter(InfoWindowAdapter)`                             | `void`           | Views are drawn into an image when the window opens |
| `setOnInfoWindowClickListener(OnInfoWindowClickListener)`             | `void`           |                                                                      |
| `setOnInfoWindowCloseListener(OnInfoWindowCloseListener)`             | `void`           | Called when info window is closed (manual or auto-dismiss)           |
| `setOnInfoWindowLongClickListener(OnInfoWindowLongClickListener)`     | `void`           | Not implemented                                                      |
| `setOnMyLocationButtonClickListener(OnMyLocationButtonClickListener)` | `void`           | Return true to keep the camera where it is |
| `setOnMyLocationClickListener(OnMyLocationClickListener)`             | `void`           |  |
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

**Camera Move Reasons:** `REASON_GESTURE` for panning and pinch zooming, `REASON_DEVELOPER_ANIMATION` for `moveCamera()` and `animateCamera()` called by the app, `REASON_API_ANIMATION` for the map's own zoom and my-location buttons.

**Kotlin Lambda Syntax Examples:**

```kotlin
// Single-method listeners support lambda syntax
mapView.setOnMapClickListener { latLng ->
    Log.d("Map", "Clicked: ${latLng.latitude}, ${latLng.longitude}")
}

mapView.setOnMarkerClickListener { marker ->
    Toast.makeText(context, marker.title, Toast.LENGTH_SHORT).show()
    true  // Consume the click: no info window, no camera move
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
| `addGeoJson(String)` | `GeoJsonResult` | OpenMapView-specific feature. Returns the added markers, polylines and polygons and their `bounds` |
| `GeoJsonParser.parse(String)` | `GeoJsonResult` | Parses without adding to a map |

Features use the [simplestyle](https://github.com/mapbox/simplestyle-spec) properties `name` or `title` and `description` for markers, and `stroke`, `stroke-width`, `stroke-opacity`, `fill` and `fill-opacity` for lines and polygons. Each marker, polyline and polygon gets its feature's `properties` as its `tag`.

---

## External Map Integration

| Feature                     | Return Type | Notes                                                                                                                                                  |
| --------------------------- | ----------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `openInExternalApp(String)` | `void`      | OpenMapView-specific feature. Opens location in external apps via geo: URI. See [MAP_TOOLBAR.md](MAP_TOOLBAR.md) for details. |

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

| Method                 | Return Type        | Notes                         |
| ---------------------- | ------------------ | ----------------------------- |
| `defaultMarker()`      | `BitmapDescriptor` |                               |
| `defaultMarker(float)` | `BitmapDescriptor` |                               |
| `fromAsset(String)`    | `BitmapDescriptor` |                               |
| `fromBitmap(Bitmap)`   | `BitmapDescriptor` |                               |
| `fromFile(String)`     | `BitmapDescriptor` |                               |
| `fromPath(String)`     | `BitmapDescriptor` |                               |
| `fromResource(int)`    | `BitmapDescriptor` | Also accepts vector drawables |

---

## Summary

**Total Methods Reviewed:** 102 (includes UiSettings class methods)

**Implementation Summary:**

- 83 methods fully implemented (81.4%)
- 19 methods not planned (18.6%) - mostly features requiring proprietary data (traffic, indoor maps, POI) or unsupported map capabilities (rotation, tilt)

**OpenMapView-Specific Features:**

In addition to the compatible API, OpenMapView provides:

- **GeoJSON import** (`addGeoJson`) - supports Point, LineString, Polygon, Multi\* variants, FeatureCollection
- **External map integration** (`openInExternalApp`) - opens location in external map apps via geo: URI with OpenStreetMap browser fallback
- **Built-in zoom controls** (`UiSettings.isZoomControlsEnabled`) - circular +/- buttons rendered automatically
- **Map types** - 5 types: NONE, NORMAL, TERRAIN, HUMANITARIAN, CYCLE (uses different OSM tile sources)

**Key Architectural Differences:**

OpenMapView uses a **single-class architecture** (vs. the MapView + GoogleMap pattern of the Maps SDK):

- Map methods called directly on `OpenMapView` instance (e.g., `mapView.setCenter()`, `mapView.getZoom()`)
- No async initialization needed - map ready immediately after view creation
- No manual lifecycle forwarding required: while attached to a window, the view observes the lifecycle of its view tree, and it releases its resources when detached

---

## Marker Clustering

The `de.afarber.openmapview.clustering` package follows the clustering of [android-maps-utils](https://github.com/googlemaps/android-maps-utils), with no extra artifact:

| android-maps-utils | OpenMapView | Notes |
|---|---|---|
| `ClusterItem` (`getPosition`, `getTitle`, `getSnippet`, `getZIndex`) | Same | |
| `Cluster` (`getPosition`, `getItems`, `getSize`) | Same, as Kotlin properties | |
| `ClusterManager(context, map)`, `addItem(s)`, `removeItem(s)`, `clearItems`, `cluster()` | Same | Set it as the map's camera idle and marker click listener |
| `setOnClusterClickListener`, `setOnClusterItemClickListener` | Same | Without a consuming cluster listener, a tap zooms in to the cluster's items |
| `NonHierarchicalDistanceBasedAlgorithm` | Default algorithm | 100 dp, items join the closest cluster |
| `DefaultClusterRenderer` | Colored circles with bucketed counts (10+, 20+, 50+, ...) | `minClusterSize` (default 4); custom icons via `clusterIconProvider` and `clusterItemIconProvider` |

---

## Jetpack Compose

The optional `openmapview-compose` artifact follows the API of the [maps-compose](https://github.com/googlemaps/android-maps-compose) library:

| maps-compose | openmapview-compose | Notes |
|---|---|---|
| `GoogleMap(cameraPositionState, properties, uiSettings, onMapClick, onMapLongClick, onMapLoaded)` | `OpenMap(...)` with the same parameters | Plus `onMapCreated` for direct access to the `OpenMapView` |
| `GoogleMap(locationSource, onMyLocationButtonClick, onMyLocationClick)`, `MapProperties(isMyLocationEnabled)`, `MapUiSettings(myLocationButtonEnabled)` | Same | |
| `MapProperties(mapType, minZoomPreference, maxZoomPreference, latLngBoundsForCameraTarget)` | Same | `mapType` is one of the `MapType` constants |
| `MapUiSettings(zoomControlsEnabled, zoomGesturesEnabled, scrollGesturesEnabled)` | Same, plus `infoWindowAutoDismiss` | |
| `GoogleMap(contentPadding)` | `OpenMap(contentPadding)` | Calls `setMapPadding()` with the padding in pixels |
| `rememberCameraPositionState`, `CameraPositionState.animate`, `CameraPositionState.move` | Same | `move` before the map is laid out is applied once it is, so `newLatLngBounds` can fit the map |
| `CameraPositionState.isMoving`, `cameraMoveStartedReason`, `CameraMoveStartedReason` | Same | `isMoving` also becomes false when an animation is stopped |
| `ScaleBar`, `DisappearingScaleBar` (maps-compose-widgets) | Same, in `openmapview-compose` | Metric and imperial bars for the camera's latitude and zoom |
| `Marker(state, title, snippet, onClick, onInfoWindowClick, onInfoWindowClose)` | Same | |
| `MarkerState.showInfoWindow()`, `hideInfoWindow()`, `isInfoWindowShown` | Same | |
| `MarkerState.isDragging` | Same | |
| `MarkerInfoWindow(state, ...) { marker -> }`, `MarkerInfoWindowContent(state, ...) { marker -> }` | Same | The content is drawn into an image when the window opens |
| `Polyline`, `Polygon`, `Circle`, `GroundOverlay`, `TileOverlay` | Same | Parameters match the OpenMapView model classes |
| `MarkerComposable(vararg keys, state, ...) { }` | Same | The content is drawn into the icon again when a key changes |
| `Clustering(items, onClusterClick, onClusterItemClick, clusterContent, clusterItemContent)` (maps-compose-utils) | Same, in `openmapview-compose` | Uses the library's `ClusterManager`, see Marker Clustering above |

---

For an in-depth explanation of OpenMapView's architectural design and how it compares to the Maps SDK, see [ARCHITECTURE.md](ARCHITECTURE.md).
