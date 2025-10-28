# Google MapView Public API - Implementation Status

[Back to README](../README.md) | [Architecture](ARCHITECTURE.md) | [API Documentation (KDoc)](https://afarber.github.io/OpenMapView/)

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
| `animateCamera(CameraUpdate, CancelableCallback)`      | `void`           | IMPLEMENTED | Default 250ms duration with callback                         |
| `animateCamera(CameraUpdate, int, CancelableCallback)` | `void`           | IMPLEMENTED | Custom duration with callback support                        |
| `moveCamera(CameraUpdate)`                             | `void`           | IMPLEMENTED | Instant camera repositioning                                 |
| `stopAnimation()`                                      | `void`           | IMPLEMENTED | Cancels ongoing camera animation                             |
| `getCameraPosition()`                                  | `CameraPosition` | IMPLEMENTED | Returns current camera state                                 |

---

## GoogleMap Class - Marker Management

| Method                     | Return Type | Status      | Notes                                                                       |
| -------------------------- | ----------- | ----------- | --------------------------------------------------------------------------- |
| `addMarker(MarkerOptions)` | `Marker`    | IMPLEMENTED | Supports position, title, snippet, icon, anchor, tag, visible, alpha, draggable |
| `clear()`                  | `void`      | IMPLEMENTED | Clears all markers, polylines, and polygons                                 |

---

## GoogleMap Class - Shapes & Overlays

| Method                                   | Return Type     | Status          | Notes                                                       |
| ---------------------------------------- | --------------- | --------------- | ----------------------------------------------------------- |
| `addPolyline(PolylineOptions)`           | `Polyline`      | IMPLEMENTED     | Supports points, stroke color/width, visible, clickable, tag |
| `addPolygon(PolygonOptions)`             | `Polygon`       | IMPLEMENTED     | Supports points, holes, colors, visible, clickable, tag     |
| `addCircle(CircleOptions)`               | `Circle`        | IMPLEMENTED     | Supports center, radius, stroke/fill colors, z-index, visible, clickable, tag |
| `addGroundOverlay(GroundOverlayOptions)` | `GroundOverlay` | NOT IMPLEMENTED | Planned for future release                                  |
| `addTileOverlay(TileOverlayOptions)`     | `TileOverlay`   | NOT PLANNED     | Advanced feature                                            |

---

## GoogleMap Class - View Information

| Method              | Return Type  | Status      | Notes                                                     |
| ------------------- | ------------ | ----------- | --------------------------------------------------------- |
| `getProjection()`   | `Projection` | IMPLEMENTED | Full Projection API with coordinate conversion            |
| `getMaxZoomLevel()` | `float`      | IMPLEMENTED | Returns current max zoom preference (default 19.0)        |
| `getMinZoomLevel()` | `float`      | IMPLEMENTED | Returns current min zoom preference (default 2.0)         |

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

| Method                        | Return Type | Status      | Notes                                       |
| ----------------------------- | ----------- | ----------- | ------------------------------------------- |
| `setMaxZoomPreference(float)` | `void`      | IMPLEMENTED | Configurable, default 19.0                  |
| `setMinZoomPreference(float)` | `void`      | IMPLEMENTED | Configurable, default 2.0                   |
| `resetMinMaxZoomPreference()` | `void`      | IMPLEMENTED | Resets to defaults (min=2.0, max=19.0)      |

---

## GoogleMap Class - UI Settings

| Method                           | Return Type  | Status          | Notes                                                 |
| -------------------------------- | ------------ | --------------- | ----------------------------------------------------- |
| `getUiSettings()`                | `UiSettings` | IMPLEMENTED     | Controls zoom and scroll gestures, rotate/tilt unimplemented |
| `setPadding(int, int, int, int)` | `void`       | NOT IMPLEMENTED | Use standard View padding                             |
| `setContentDescription(String)`  | `void`       | NOT IMPLEMENTED | Use standard View accessibility                       |

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
| `setOnMapClickListener(OnMapClickListener)`                           | `void`           | IMPLEMENTED     | Full implementation with LatLng coordinate callbacks |
| `setOnMapLongClickListener(OnMapLongClickListener)`                   | `void`           | IMPLEMENTED     | GestureDetector-based long-press detection           |
| `setOnMarkerClickListener(OnMarkerClickListener)`                     | `void`           | IMPLEMENTED     | Returns boolean to consume event                     |
| `setOnMarkerDragListener(OnMarkerDragListener)`                       | `void`           | IMPLEMENTED     | Full drag support with start/drag/end callbacks      |
| `setOnPolylineClickListener(OnPolylineClickListener)`                 | `void`           | IMPLEMENTED     | Point-to-line distance hit testing with tolerance    |
| `setOnPolygonClickListener(OnPolygonClickListener)`                   | `void`           | IMPLEMENTED     | Ray casting algorithm with hole support              |
| `setOnCircleClickListener(OnCircleClickListener)`                     | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnGroundOverlayClickListener(OnGroundOverlayClickListener)`       | `void`           | NOT IMPLEMENTED | Not applicable                                       |
| `setOnPoiClickListener(OnPoiClickListener)`                           | `void`           | NOT PLANNED     | POI data not available in OSM tiles                  |
| `setOnCameraMoveStartedListener(OnCameraMoveStartedListener)`         | `void`           | IMPLEMENTED     | Tracks gesture, API, and developer-initiated moves   |
| `setOnCameraMoveListener(OnCameraMoveListener)`                       | `void`           | IMPLEMENTED     | Called repeatedly during camera movement             |
| `setOnCameraIdleListener(OnCameraIdleListener)`                       | `void`           | IMPLEMENTED     | Called when camera stops moving                      |
| `setOnCameraMoveCanceledListener(OnCameraMoveCanceledListener)`       | `void`           | IMPLEMENTED     | Called when animation is interrupted                 |
| `setOnMapLoadedCallback(OnMapLoadedCallback)`                         | `void`           | NOT IMPLEMENTED | Tiles load asynchronously, callback could be added   |
| `setInfoWindowAdapter(InfoWindowAdapter)`                             | `void`           | NOT IMPLEMENTED | Custom adapters not yet implemented                  |
| `setOnInfoWindowClickListener(OnInfoWindowClickListener)`             | `void`           | IMPLEMENTED     | Full support with basic info window rendering        |
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

| Method                 | Return Type        | Status      | Notes                                         |
| ---------------------- | ------------------ | ----------- | --------------------------------------------- |
| `defaultMarker()`      | `BitmapDescriptor` | IMPLEMENTED | Red marker icon                               |
| `defaultMarker(float)` | `BitmapDescriptor` | IMPLEMENTED | Marker with custom hue (0-360)                |
| `fromAsset(String)`    | `BitmapDescriptor` | IMPLEMENTED | Loads bitmap from assets folder               |
| `fromBitmap(Bitmap)`   | `BitmapDescriptor` | IMPLEMENTED | Creates descriptor from bitmap object         |
| `fromFile(String)`     | `BitmapDescriptor` | IMPLEMENTED | Loads bitmap from file path                   |
| `fromPath(String)`     | `BitmapDescriptor` | IMPLEMENTED | Alias for fromFile                            |
| `fromResource(int)`    | `BitmapDescriptor` | IMPLEMENTED | Loads bitmap from drawable resource           |

---

## Summary Statistics

**Total Methods Reviewed:** 91

**Implementation Status:**

- IMPLEMENTED: 62 methods (68.1%)
- PARTIAL: 2 methods (2.2%)
- NOT IMPLEMENTED: 1 method (1.1%)
- NOT PLANNED: 26 methods (28.6%)

**Core Functionality Coverage:**

- Camera control: 100% (animateCamera, moveCamera, stopAnimation, getCameraPosition)
- Markers: 100% (addMarker, click listener, drag support, visibility, alpha)
- Vector shapes: 100% (polylines, polygons with holes, visibility)
- Map interaction: 100% (click listeners, long-click, projection API)
- Zoom control: 100% (min/max zoom preferences, getZoom)
- Lifecycle management: 75% (onResume, onPause, onDestroy)

**Focus Areas:**
OpenMapView prioritizes lightweight, essential mapping features for applications that need basic map display, markers, shapes, and camera animations without the complexity and overhead of Google Play Services.

---

For an in-depth explanation of OpenMapView's architectural design and how it compares to Google Maps SDK, see [ARCHITECTURE.md](ARCHITECTURE.md).
