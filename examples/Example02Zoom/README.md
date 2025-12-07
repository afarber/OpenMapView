# Example02Zoom - Zoom Controls and Gestures

[Back to README](../../README.md)

This example demonstrates zoom functionality in OpenMapView, including custom zoom controls and pinch-to-zoom gestures with real-time status display.

## Features Demonstrated

- Map tile rendering with zoom support
- Custom zoom toolbar with +/- buttons
- Pinch-to-zoom gesture detection
- Real-time zoom level display
- Camera state tracking (Idle/Moving)
- Zoom limits (min: 2.0, max: 19.0)

## Screenshot

![Example02Zoom Demo](screenshot.gif)

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example02Zoom` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to your device or emulator

### Option 2: Build and Install from Command Line

```bash
# From project root - build, install, and launch
./gradlew :examples:Example02Zoom:installDebug

# Launch the app
adb shell am start -n de.afarber.openmapview.example02zoom/.MainActivity
```

## Project Structure

```
example02zoom/
├── MainActivity.kt      # Main activity and MapViewScreen composable
├── ZoomToolbar.kt       # Vertical toolbar with +/- zoom buttons
├── StatusToolbar.kt     # Status overlay showing zoom level and camera state
└── Colors.kt            # OSM-inspired colors and shared dimensions
```

## Code Highlights

### MainActivity.kt

```kotlin
@Composable
fun MapViewScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView: OpenMapView? by remember { mutableStateOf(null) }
    var zoomLevel by remember { mutableStateOf("14.0") }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    setCenter(LatLng(51.4661, 7.2491))
                    setZoom(14.0f)
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        StatusToolbar(zoomLevel = zoomLevel, cameraState = cameraState, ...)
        ZoomToolbar(onZoomInClick = { ... }, onZoomOutClick = { ... }, ...)
    }
}
```

### Key Concepts

- **setZoom()**: Sets zoom level (2.0 = world view, 19.0 = street level)
- **getZoom()**: Returns current zoom level
- **Pinch gesture**: Built-in ScaleGestureDetector in OpenMapView
- **Zoom limits**: Automatically enforced to prevent over-zoom
- **OnCameraMoveListener**: Callback for camera changes during zoom

## What to Test

1. **Tap + button** to zoom in, observe zoom level update
2. **Tap - button** to zoom out, observe zoom level update
3. **Pinch to zoom** using two fingers
4. **Check zoom focus** - pinch-to-zoom zooms toward pinch center
5. **Test limits** - zooming beyond min/max stops at the limit
6. **Observe status** - zoom level and camera state update in real-time

## Technical Details

### Zoom Levels

- **2.0** - World view
- **10.0** - Country/state view
- **14.0** - City view (default)
- **17.0** - Street view
- **19.0** - Maximum detail (building level)

### Zoom Implementation

OpenMapView uses:

- `ScaleGestureDetector` for pinch-to-zoom
- Fractional zoom (Float) for smooth transitions
- Web Mercator projection for tile calculation

## Map Location

**Default Center:** Bochum, Germany (51.4661N, 7.2491E) at zoom 14.0

