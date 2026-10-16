# Pan (Views)

[Back to README](../../../README.md) | Same app in Jetpack Compose: [Pan (Compose)](../../compose/Pan)

Pan the map by finger, with an arrow pad, or fly to places in Bochum. A switch keeps the camera inside Bochum.
The map is declared in an XML layout and driven from a Java `AppCompatActivity`.

## Features

- `<de.afarber.openmapview.OpenMapView>` in `activity_main.xml`, usable right after `setContentView()`
- Markers in three colors, added with `MarkerOptions` and `BitmapDescriptorFactory.defaultMarker(hue)`
- Buttons that fly to a place with `animateCamera(CameraUpdateFactory.newLatLng(...), 1000)`
- An arrow pad that pans by pixels with `CameraUpdateFactory.scrollBy(x, y)`
- "Stay in Bochum": `setLatLngBoundsForCameraTarget()` limits the camera, a `Polygon` outlines the area and `newLatLngBounds()` zooms out to show it
- A status card updated from `setOnCameraMoveListener` and `setOnCameraIdleListener`

## Screenshot

![Pan (Views)](screenshot.gif)

## Run

Select `examples.views.Pan` in Android Studio, or:

```bash
./gradlew :examples:views:Pan:installDebug
adb shell am start -n de.afarber.openmapview.examples.views.pan/.MainActivity
```

## Code Highlights

```xml
<de.afarber.openmapview.OpenMapView
    android:id="@+id/map_view"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

```java
// The map is ready right away: no getMapAsync() and no lifecycle forwarding needed
mapView = findViewById(R.id.map_view);
mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(CITY_HALL, 14f));

// Panning by pixels, e.g. for a d-pad or rotary controller in a car
findViewById(R.id.button_left).setOnClickListener(
        v -> mapView.animateCamera(CameraUpdateFactory.scrollBy(-PAN_STEP, 0f), 300));

// A null area lets the camera go anywhere again
mapView.setLatLngBoundsForCameraTarget(stay ? BOCHUM_AREA : null);
```

## What to Try

1. Drag the map and watch the coordinates in the status card
2. Tap Mining Museum, Main Station and Ruhr University
3. Pan with the arrow buttons
4. Turn on "Stay in Bochum" and try to drag the map center out of the blue rectangle
