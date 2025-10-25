# Example01Pan - Basic Map Panning

This example demonstrates the core functionality of OpenMapView: displaying OpenStreetMap tiles and responding to touch pan gestures.

## Features Demonstrated

- Map tile rendering from OpenStreetMap
- Touch pan/drag gestures
- Smooth real-time map updates
- Basic OpenMapView setup

## Screenshot

![Example01Pan Demo](screenshot.gif)

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example01Pan` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to your device or emulator

### Option 2: Build and Install from Command Line

```bash
# From project root - build, install, and launch
./gradlew :examples:Example01Pan:installDebug

# Launch the app
adb shell am start -n de.afarber.openmapview.example01pan/.MainActivity
```

## Code Highlights

### MainActivity.kt

```kotlin
@Composable
fun MapViewScreen() {
    AndroidView(
        factory = { context ->
            OpenMapView(context).apply {
                setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
                setZoom(14.0)
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
```

### Key Concepts

- **LatLng**: Represents geographic coordinates (latitude, longitude)
- **setCenter()**: Sets the initial map center position
- **setZoom()**: Sets zoom level (2.0 = world view, 19.0 = street level)
- **Touch handling**: Built-in via OpenMapView's onTouchEvent()

## What to Test

1. **Pan the map** by dragging with your finger/mouse
2. **Observe tiles loading** as you pan to new areas
3. **Check smooth rendering** - map should update in real-time without lag

## Next Steps

- Try **Example02Zoom** for zoom controls and pinch-to-zoom gestures
- Try **Example03Markers** for marker overlays and click handling

## Map Location

**Default Center:** Bochum, Germany (51.4661°N, 7.2491°E)
