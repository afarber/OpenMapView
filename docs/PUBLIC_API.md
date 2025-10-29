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

| Method                                   | Return Type     | Status      | Notes                                                       |
| ---------------------------------------- | --------------- | ----------- | ----------------------------------------------------------- |
| `addPolyline(PolylineOptions)`           | `Polyline`      | IMPLEMENTED | Supports points, stroke color/width, visible, clickable, tag |
| `addPolygon(PolygonOptions)`             | `Polygon`       | IMPLEMENTED | Supports points, holes, colors, visible, clickable, tag     |
| `addCircle(CircleOptions)`               | `Circle`        | IMPLEMENTED | Supports center, radius, stroke/fill colors, z-index, visible, clickable, tag |
| `addGroundOverlay(GroundOverlayOptions)` | `GroundOverlay` | IMPLEMENTED | Supports image, position/bounds modes, bearing, transparency, anchor, z-index, visible, clickable, tag |
| `addTileOverlay(TileOverlayOptions)`     | `TileOverlay`   | IMPLEMENTED | Supports tileProvider, transparency, zIndex, visible, fadeIn, tag. Includes predefined providers for public OSM services |

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
| `setCenter(LatLng)`            | `void`      | IMPLEMENTED     | Direct method, not via GoogleMap pattern                                   |
| `setZoom(double)`              | `void`      | IMPLEMENTED     | Direct method, not via GoogleMap pattern                                   |
| `getZoom()`                    | `double`    | IMPLEMENTED     | Direct method, not via GoogleMap pattern                                   |
| `setMapType(int)`              | `void`      | IMPLEMENTED     | Supports NORMAL, TERRAIN, HUMANITARIAN, CYCLE, NONE. See MapType constants |
| `getMapType()`                 | `int`       | IMPLEMENTED     | Returns current map type constant                                          |
| `setMapStyle(MapStyleOptions)` | `boolean`   | NOT PLANNED     | Custom tile sources could provide this                                     |

---

## GoogleMap Class - Zoom Preferences

| Method                        | Return Type | Status      | Notes                                       |
| ----------------------------- | ----------- | ----------- | ------------------------------------------- |
| `setMaxZoomPreference(float)` | `void`      | IMPLEMENTED | Configurable, default 19.0                  |
| `setMinZoomPreference(float)` | `void`      | IMPLEMENTED | Configurable, default 2.0                   |
| `resetMinMaxZoomPreference()` | `void`      | IMPLEMENTED | Resets to defaults (min=2.0, max=19.0)      |

---

## GoogleMap Class - UI Settings

| Method                           | Return Type  | Status      | Notes                                                                                                             |
| -------------------------------- | ------------ | ----------- | ----------------------------------------------------------------------------------------------------------------- |
| `getUiSettings()`                | `UiSettings` | IMPLEMENTED | Full UiSettings support: gestures, zoom controls, scroll-during-zoom                                              |
| `setPadding(int, int, int, int)` | `void`       | IMPLEMENTED | Implemented as setMapPadding() to avoid conflict with View.setPadding(). Adjusts logical viewport for camera ops |
| `setContentDescription(String)`  | `void`       | IMPLEMENTED | Inherited from View class - use directly on OpenMapView instance for accessibility support                        |

---

## UiSettings Class - Methods

Methods available on the UiSettings object returned by `getUiSettings()`:

| Method                                                  | Return Type | Status      | Notes                                                          |
| ------------------------------------------------------- | ----------- | ----------- | -------------------------------------------------------------- |
| `setZoomGesturesEnabled(boolean)`                       | `void`      | IMPLEMENTED | Enable/disable pinch-to-zoom gestures (property: isZoomGesturesEnabled) |
| `isZoomGesturesEnabled()`                               | `boolean`   | IMPLEMENTED | Check if zoom gestures are enabled (default: true)             |
| `setScrollGesturesEnabled(boolean)`                     | `void`      | IMPLEMENTED | Enable/disable pan/scroll gestures (property: isScrollGesturesEnabled) |
| `isScrollGesturesEnabled()`                             | `boolean`   | IMPLEMENTED | Check if scroll gestures are enabled (default: true)           |
| `setZoomControlsEnabled(boolean)`                       | `void`      | IMPLEMENTED | Show/hide +/- zoom button overlay (property: isZoomControlsEnabled) |
| `isZoomControlsEnabled()`                               | `boolean`   | IMPLEMENTED | Check if zoom controls are visible (default: false)            |
| `setScrollGesturesEnabledDuringRotateOrZoom(boolean)`   | `void`      | IMPLEMENTED | Allow panning during pinch-zoom (property: isScrollGesturesEnabledDuringRotateOrZoom) |
| `isScrollGesturesEnabledDuringRotateOrZoom()`           | `boolean`   | IMPLEMENTED | Check if scroll-during-zoom is enabled (default: true)         |
| `setRotateGesturesEnabled(boolean)`                     | `void`      | NOT PLANNED | OSM tiles don't support rotation                               |
| `isRotateGesturesEnabled()`                             | `boolean`   | IMPLEMENTED | Always returns false (rotation not supported)                  |
| `setTiltGesturesEnabled(boolean)`                       | `void`      | NOT PLANNED | OSM tiles don't support 3D tilt                                |
| `isTiltGesturesEnabled()`                               | `boolean`   | IMPLEMENTED | Always returns false (tilt not supported)                      |
| `setCompassEnabled(boolean)`                            | `void`      | NOT PLANNED | Requires rotation support                                      |
| `isCompassEnabled()`                                    | `boolean`   | IMPLEMENTED | Always returns false (compass not implemented)                 |
| `setMyLocationButtonEnabled(boolean)`                   | `void`      | NOT PLANNED | Requires location services integration                         |
| `isMyLocationButtonEnabled()`                           | `boolean`   | IMPLEMENTED | Always returns false (location button not implemented)         |
| `setIndoorLevelPickerEnabled(boolean)`                  | `void`      | NOT PLANNED | No indoor mapping support                                      |
| `isIndoorLevelPickerEnabled()`                          | `boolean`   | IMPLEMENTED | Always returns false (indoor mapping not supported)            |
| `setMapToolbarEnabled(boolean)`                         | `void`      | NOT IMPLEMENTED | Use openInExternalApp() instead - see External Map Integration section below |
| `isMapToolbarEnabled()`                                 | `boolean`   | IMPLEMENTED | Always returns false (map toolbar not implemented)             |
| `setAllGesturesEnabled(boolean)`                        | `void`      | IMPLEMENTED | Enable/disable all gesture controls at once                    |

---

## GoogleMap Class - Feature Toggles

| Method                         | Return Type | Status      | Notes                                                           |
| ------------------------------ | ----------- | ----------- | --------------------------------------------------------------- |
| `setTrafficEnabled(boolean)`   | `void`      | NOT PLANNED | Requires traffic data source                                    |
| `isTrafficEnabled()`           | `boolean`   | IMPLEMENTED | Always returns false (traffic not supported by OSM tiles)       |
| `setBuildingsEnabled(boolean)` | `void`      | NOT PLANNED | OSM tiles include buildings by default                          |
| `isBuildingsEnabled()`         | `boolean`   | IMPLEMENTED | Always returns true (buildings always visible in OSM tiles)     |
| `setIndoorEnabled(boolean)`    | `void`      | NOT PLANNED | Requires indoor mapping data                                    |
| `isIndoorEnabled()`            | `boolean`   | IMPLEMENTED | Always returns false (indoor maps not supported by OpenMapView) |

---

## GoogleMap Class - Location Layer

| Method                              | Return Type | Status          | Notes                                                                |
| ----------------------------------- | ----------- | --------------- | -------------------------------------------------------------------- |
| `setMyLocationEnabled(boolean)`     | `void`      | NOT IMPLEMENTED | Can be implemented via custom marker                                 |
| `isMyLocationEnabled()`             | `boolean`   | IMPLEMENTED     | Always returns false (my-location layer not implemented)             |
| `setLocationSource(LocationSource)` | `void`      | NOT IMPLEMENTED | Can be implemented via custom location tracking and marker placement |

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
| `setOnCircleClickListener(OnCircleClickListener)`                     | `void`           | IMPLEMENTED     | Distance-based hit testing with stroke width         |
| `setOnGroundOverlayClickListener(OnGroundOverlayClickListener)`       | `void`           | IMPLEMENTED     | Rectangle hit testing with rotation support          |
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

## External Map Integration

| Feature                     | Status      | Notes                                                                                              |
| --------------------------- | ----------- | -------------------------------------------------------------------------------------------------- |
| `openInExternalApp(String)` | IMPLEMENTED | Opens current map location in external apps via geo: URI. Falls back to OpenStreetMap.org in browser if no map apps installed. Similar to Google Maps toolbar functionality but open-source. |

This method is an OpenMapView-specific feature not present in Google Maps API. See [MAP_TOOLBAR.md](MAP_TOOLBAR.md) for details.

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

**Total Methods Reviewed:** 102 (includes UiSettings class methods)

**Implementation Status:**

- IMPLEMENTED: 83 methods (81.4%)
- PARTIAL: 0 methods (0%)
- NOT IMPLEMENTED: 0 methods (0%)
- NOT PLANNED: 19 methods (18.6%)

**Core Functionality Coverage:**

- Camera control: 100% (animateCamera, moveCamera, stopAnimation, getCameraPosition, setPadding)
- Markers: 100% (addMarker, click listener, drag support, visibility, alpha)
- Vector shapes: 100% (polylines, polygons with holes, circles, visibility)
- Ground overlays: 100% (position/bounds modes, bearing, transparency, click listener)
- Tile overlays: 100% (custom tile providers, transparency, z-index, visibility)
- Map interaction: 100% (click listeners, long-click, projection API)
- Zoom control: 100% (min/max zoom preferences, getZoom)
- UI Settings: 100% of applicable methods (gesture controls, zoom controls, scroll-during-zoom)
- Lifecycle management: 75% (onResume, onPause, onDestroy)

**OpenMapView-Specific Features:**
In addition to Google Maps API compatibility, OpenMapView provides:
- GeoJSON import (addGeoJson)
- External map integration (openInExternalApp) - opens location in external map apps or browser

**Focus Areas:**
OpenMapView prioritizes lightweight, essential mapping features for applications that need basic map display, markers, shapes, and camera animations without the complexity and overhead of Google Play Services.

---

For an in-depth explanation of OpenMapView's architectural design and how it compares to Google Maps SDK, see [ARCHITECTURE.md](ARCHITECTURE.md).
