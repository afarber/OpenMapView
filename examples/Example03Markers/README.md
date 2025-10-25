# Example03Markers - Marker Overlays and Click Handling

This example demonstrates the marker system in OpenMapView, including marker rendering, touch detection, and click event handling.

## Features Demonstrated

- Multiple markers at different geographic locations
- Default red teardrop marker icons
- Marker click detection and callbacks
- Toast notifications on marker click
- Markers with title and snippet metadata
- Marker positioning with proper anchor points
- Markers that stay fixed during pan and zoom

## Screenshot

![Example03Markers Demo](screenshot.gif)

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example03Markers` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to your device or emulator

### Option 2: Build and Install from Command Line

```bash
# From project root - build, install, and launch
./gradlew :examples:Example03Markers:installDebug

# Launch the app
adb shell am start -n de.afarber.openmapview.example03markers/.MainActivity
```

## Code Highlights

### Adding Markers

```kotlin
OpenMapView(context).apply {
    setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
    setZoom(14.0)

    // Add marker with title and snippet
    addMarker(
        Marker(
            position = LatLng(51.4661, 7.2491),
            title = "Bochum City Center",
            snippet = "Welcome to Bochum!",
        )
    )
}
```

### Click Listener

```kotlin
setOnMarkerClickListener { marker ->
    val message = buildString {
        append(marker.title ?: "Marker")
        if (marker.snippet != null) {
            append("\n")
            append(marker.snippet)
        }
    }
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    true // Consume the click event
}
```

### Key Concepts

- **Marker**: Data class with position, title, snippet, icon, anchor, and tag
- **addMarker()**: Add a marker to the map
- **removeMarker()**: Remove a specific marker
- **clearMarkers()**: Remove all markers
- **setOnMarkerClickListener()**: Handle marker click events
- **Default icon**: Red teardrop shape generated via MarkerIconFactory
- **Custom icons**: Provide your own Bitmap via the `icon` parameter

## What to Test

1. **Launch the app** - you should see 5 red markers around Bochum
2. **Click a marker** - Toast message shows title and snippet
3. **Pan the map** - markers stay at correct geographic positions
4. **Zoom in/out** - markers remain properly positioned
5. **Click different markers** - each shows its own title/snippet

## Marker Locations

This example displays 5 markers:

| Location | Coordinates         | Description        |
| -------- | ------------------- | ------------------ |
| Center   | 51.4661°N, 7.2491°E | Bochum City Center |
| North    | 51.4700°N, 7.2550°E | North Location     |
| South    | 51.4620°N, 7.2430°E | South Location     |
| West     | 51.4680°N, 7.2380°E | West Location      |
| East     | 51.4640°N, 7.2600°E | East Location      |

**Note on marker positioning:** While the 4 outer markers are placed on N, S, W, E sides of the central marker (by adjusting latitude/longitude), they do not appear strictly above, below, left, right on the screen. This is due to the Web Mercator projection used by OpenStreetMap, which distorts distances and angles, especially at higher latitudes. The further from the equator, the more pronounced this distortion becomes.

## Custom Marker Icons

To use custom marker icons instead of the default red teardrop:

```kotlin
// Create custom bitmap (e.g., from resources)
val customIcon = BitmapFactory.decodeResource(
    resources,
    R.drawable.my_custom_marker
)

// Add marker with custom icon
addMarker(
    Marker(
        position = LatLng(51.4661, 7.2491),
        title = "Custom Marker",
        icon = customIcon,
        anchor = Pair(0.5f, 1.0f) // Center-bottom anchor
    )
)
```

## Technical Details

### Marker Anchor Point

The anchor determines which point of the icon aligns with the geographic position:

- `Pair(0.5f, 1.0f)` - Center horizontally, bottom vertically (default for pins)
- `Pair(0.5f, 0.5f)` - Center of icon
- `Pair(0.0f, 0.0f)` - Top-left corner

### Marker Rendering

Markers are drawn:

1. On top of map tiles (layered correctly)
2. In the order they were added (first added = bottom layer)
3. With proper screen-to-geographic coordinate conversion
4. Using the Web Mercator projection

### Touch Detection

Click detection uses:

- Bounding box hit testing on marker icons
- Reverse z-order checking (top markers checked first)
- Configurable movement threshold (10px) to distinguish clicks from drags

## Next Steps

- Try **Example01Pan** for basic map panning
- Try **Example02Zoom** for zoom controls
- Modify marker positions to show your own locations
- Add custom marker icons from your app resources

## Map Location

**Default Center:** Bochum, Germany (51.4661°N, 7.2491°E) at zoom 14.0

All 5 markers are positioned around Bochum within ~1km radius.
