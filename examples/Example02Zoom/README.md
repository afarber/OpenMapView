# Example02Zoom - Zoom Controls and Gestures

This example demonstrates zoom functionality in OpenMapView, including both programmatic zoom controls and pinch-to-zoom gestures.

## Features Demonstrated

- Map tile rendering with zoom support
- Floating Action Button (FAB) zoom controls (+/-)
- Pinch-to-zoom gesture detection
- Real-time zoom level display
- Smooth zoom animations
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

## Code Highlights

### MainActivity.kt - FAB Zoom Controls

```kotlin
// Zoom In FAB
FloatingActionButton(
    onClick = {
        mapView?.apply {
            val newZoom = (getZoom() + 1.0).coerceAtMost(19.0)
            setZoom(newZoom)
            zoomLevel = getZoom()
        }
    }
) {
    Icon(Icons.Default.Add, "Zoom In")
}
```

### Zoom Level Display

```kotlin
// Real-time zoom level indicator
Text(
    text = "Zoom: %.1f".format(zoomLevel),
    modifier = Modifier
        .align(Alignment.BottomStart)
        .padding(16.dp)
        .background(Color.White.copy(alpha = 0.8f))
)
```

### Key Concepts

- **setZoom()**: Programmatically set zoom level (2.0-19.0)
- **getZoom()**: Read current zoom level
- **Pinch gesture**: Built-in ScaleGestureDetector in OpenMapView
- **Zoom limits**: Automatically enforced to prevent over-zoom

## What to Test

1. **Click the + button** - map should zoom in, counter updates
2. **Click the - button** - map should zoom out, counter updates
3. **Pinch to zoom** - use two fingers to zoom in/out
4. **Check zoom focus** - pinch-to-zoom should zoom toward pinch center
5. **Test limits** - zooming beyond min/max should stop gracefully
6. **Observe zoom level** - bottom-left display updates in real-time

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
- Fractional zoom (Double) for smooth transitions
- Web Mercator projection for tile calculation

## Next Steps

- Try **Example01Pan** for basic panning without zoom controls
- Try **Example03Markers** for marker overlays with zoom support

## Map Location

**Default Center:** Berlin, Germany (52.52°N, 13.405°E) at zoom 14.0
