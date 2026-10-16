# Markers (Views)

[Back to README](../../../README.md) | Same app in Jetpack Compose: [Markers (Compose)](../../compose/Markers)

Eight places in Bochum as colored markers with info windows. Step through them with Previous and Next, tap them, or show all.
The map padding keeps the selected marker in the free area next to the button panel.
The map is declared in an XML layout and driven from a Java `AppCompatActivity`.

## Features

- Markers in eight colors with `MarkerOptions` and `BitmapDescriptorFactory.defaultMarker(hue)`; title and snippet appear in the info window
- Tapping a marker opens its info window and moves the camera there; the click listener returns false to keep this default
- Previous and Next call `Marker.showInfoWindow()` and `animateCamera(CameraUpdateFactory.newLatLng(...))`
- Show all hides the info window and fits all places with `newLatLngBounds()`
- `setMapPadding()` from the panel width, so the camera centers in the area left of the panel
- A status card with the selection and the last `setOnInfoWindowClickListener` / `setOnInfoWindowCloseListener` event

## Screenshot

![Markers (Views)](screenshot.gif)

## Run

Select `examples.views.Markers` in Android Studio, or:

```bash
./gradlew :examples:views:Markers:installDebug
adb shell am start -n de.afarber.openmapview.examples.views.markers/.MainActivity
```

## Code Highlights

```java
// Title and snippet are shown in the marker's info window
markers.add(mapView.addMarker(
        new MarkerOptions()
                .position(place.position)
                .title(place.title)
                .snippet(place.snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(place.hue))));

// The camera centers in the area left of the panel, so the selected marker isn't hidden
panel.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
        mapView.setMapPadding(0, 0, mapView.getWidth() - left, 0));

// Returning false keeps the default: the info window opens and the camera moves there
mapView.setOnMarkerClickListener(marker -> {
    selected = markers.indexOf(marker);
    showSelection();
    return false;
});
```

## What to Try

1. Tap Next a few times: each marker's info window opens left of the panel
2. Tap the open info window, then tap Next again: the status shows both events
3. Tap Show all, then tap a marker on the map
