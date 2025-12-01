# Example01Pan - Basic Map Panning

[Back to README](../../README.md)

This example demonstrates the core functionality of OpenMapView: displaying OpenStreetMap tiles, responding to touch pan gestures, and using camera controls with status display.

## Features Demonstrated

- Map tile rendering from OpenStreetMap
- Touch pan/drag gestures
- Arrow button toolbar for programmatic panning
- Preset location buttons with animated camera moves
- Camera bounds constraints with visual polyline indicator
- Real-time camera state and position display
- Colored markers at preset locations

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

## Project Structure

```
example01pan/
├── MainActivity.kt      # Main activity and MapViewScreen composable
├── ArrowToolbar.kt      # Horizontal toolbar with pan arrow buttons
├── LocationToolbar.kt   # Vertical toolbar with preset location buttons
├── ControlToolbar.kt    # Vertical toolbar with bounds/reset controls
├── StatusToolbar.kt     # Status overlay showing camera state
└── Colors.kt            # OSM-inspired colors and shared dimensions
```

## Code Highlights

### MainActivity.kt

```kotlin
@Composable
fun MapViewScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    var mapView: OpenMapView? by remember { mutableStateOf(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                OpenMapView(ctx).apply {
                    lifecycleOwner.lifecycle.addObserver(this)
                    setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
                    setZoom(14.0f)
                    mapView = this
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Toolbars overlay the map
        StatusToolbar(...)
        ArrowToolbar(...)
        LocationToolbar(...)
        ControlToolbar(...)
    }
}
```

### OSM-Inspired Colors (Colors.kt)

```kotlin
val OsmParkGreen = Color(0xFFAAD3A2)   // Parks and forests
val OsmHighwayPink = Color(0xFFE892A2)  // Highways and roads
val OsmWaterBlue = Color(0xFFAAD3DF)    // Water areas
```

### Key Concepts

- **LatLng**: Represents geographic coordinates (latitude, longitude)
- **setCenter()**: Sets the initial map center position
- **setZoom()**: Sets zoom level (2.0 = world view, 19.0 = street level)
- **moveCamera()**: Instantly moves the camera (no animation)
- **animateCamera()**: Smoothly animates camera to new position
- **setLatLngBoundsForCameraTarget()**: Constrains camera movement to bounds
- **OnCameraMoveListener**: Callback for camera position changes

## What to Test

1. **Pan the map** by dragging with your finger/mouse
2. **Use arrow buttons** to pan the map programmatically
3. **Tap location buttons** (Loc 1, 2, 3) to animate to preset positions
4. **Toggle bounds** to constrain camera movement (blue polyline shows bounds)
5. **Tap Reset** to return to initial position
6. **Observe status overlay** showing camera state and coordinates

## Map Location

**Default Center:** Bochum, Germany (51.4661N, 7.2491E)

**Preset Locations:**
- Location 1 (Red marker): North-West of center
- Location 2 (Green marker): East of center
- Location 3 (Magenta marker): South-West of center
- Initial (Cyan marker): Center position
