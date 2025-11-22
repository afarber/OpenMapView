# Example12Toolbar - External Map App Integration

[Back to README](../../README.md)

This example demonstrates how to open the current map location in external map applications using the geo: URI scheme with automatic browser fallback.

## Features Demonstrated

- Open current location in external map apps
- Long-press gesture to select and open location
- Automatic fallback to OpenStreetMap.org browser
- Works with Google Maps, OsmAnd, Maps.me, HERE WeGo, Waze, Organic Maps
- Custom location labels in geo: URI
- Material3 floating action button UI

## Screenshot

![Example12Toolbar Demo](screenshot.gif)

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example12Toolbar` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to your device or emulator

### Option 2: Build and Install from Command Line

```bash
# From project root - build, install, and launch
./gradlew :examples:Example12Toolbar:installDebug

# Launch the app
adb shell am start -n de.afarber.openmapview.example12toolbar/.MainActivity
```

## Code Highlights

### Open Current Map Center

```kotlin
// Open current map center in external app
mapView.openInExternalApp()
```

### Open with Custom Label

```kotlin
// Open with a descriptive label
mapView.openInExternalApp("Coffee Shop")
```

### Long-Press to Select and Open

```kotlin
mapView.setOnMapLongClickListener { latLng ->
    mapView.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    mapView.openInExternalApp("Selected Location")
}
```

### Key Concepts

- **openInExternalApp()**: Launch external map app at current center
- **geo: URI scheme**: Standard Android intent for geographic locations
- **Browser fallback**: Opens OpenStreetMap.org if no map apps installed
- **App picker**: Android shows chooser if multiple map apps available

## What to Test

1. **Tap Open button** - Opens current map center in external app
2. **Long-press on map** - Moves to location and opens in external app
3. **Single map app installed** - Opens directly in that app
4. **Multiple map apps** - Shows Android app picker
5. **No map apps** - Falls back to OpenStreetMap.org in browser
6. **Pan and zoom** - Open button always uses current center

## Behavior Details

### App Selection Logic

```
1. User taps "Open" button
2. App creates geo: intent with current coordinates
3. Android resolves intent:
   - One app: Opens directly
   - Multiple apps: Shows picker dialog
   - No apps: Falls back to OSM browser
```

### geo: URI Format

```
geo:51.4661,7.2491?z=14&q=51.4661,7.2491(Location+Name)
```

Components:
- `geo:lat,lng` - Base coordinates
- `z=14` - Zoom level hint
- `q=lat,lng(label)` - Query with optional label

## Technical Details

### Supported External Apps

| App          | Package                        | Notes                           |
| ------------ | ------------------------------ | ------------------------------- |
| Google Maps  | com.google.android.apps.maps   | Most common                     |
| OsmAnd       | net.osmand                     | Offline OSM maps                |
| Maps.me      | com.mapswithme.maps.pro        | Offline maps                    |
| HERE WeGo    | com.here.app.maps              | Navigation                      |
| Waze         | com.waze                       | Traffic-focused                 |
| Organic Maps | app.organicmaps                | Privacy-focused OSM             |

### Browser Fallback URL

```
https://www.openstreetmap.org/?mlat=51.4661&mlon=7.2491#map=14/51.4661/7.2491
```

### Intent Creation

```kotlin
// Internal implementation
val geoUri = Uri.parse("geo:$lat,$lng?z=$zoom&q=$lat,$lng($label)")
val intent = Intent(Intent.ACTION_VIEW, geoUri)
context.startActivity(intent)
```

## Advanced Usage

### Open Specific Location

```kotlin
// Open a specific location (not current center)
val location = LatLng(48.8584, 2.2945) // Eiffel Tower
val uri = Uri.parse("geo:${location.latitude},${location.longitude}")
val intent = Intent(Intent.ACTION_VIEW, uri)
context.startActivity(intent)
```

### Check for Map Apps

```kotlin
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0"))
val activities = packageManager.queryIntentActivities(intent, 0)
if (activities.isEmpty()) {
    // No map apps installed, use browser fallback
}
```

### Custom App Picker Title

```kotlin
val intent = Intent(Intent.ACTION_VIEW, geoUri)
val chooser = Intent.createChooser(intent, "Open in Maps")
context.startActivity(chooser)
```

## Use Cases

- **Navigation**: Open location in preferred navigation app
- **Sharing**: Let user choose how to view/share location
- **Cross-app workflow**: Start in your app, continue in Google Maps
- **Fallback**: Ensure location is always accessible even without map apps

## Map Location

**Default Center:** Bochum, Germany (51.4661°N, 7.2491°E) at zoom 14.0
