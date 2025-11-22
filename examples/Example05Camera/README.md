# Example05Camera - Camera Animations

[Back to README](../../README.md)

This example demonstrates smooth camera animations in OpenMapView, showing how to animate map position and zoom level changes with customizable durations and completion callbacks.

## Features Demonstrated

- Animated camera movements to specific locations
- Smooth zoom animations (zoom in/out)
- Different animation durations (fast: 500ms, normal: 1000ms, slow: 2000ms)
- Animation completion callbacks (onFinish, onCancel)
- Animation cancellation with stopAnimation()
- Multiple markers as animation targets

## Screenshot

![Example05Camera Demo](screenshot.gif)

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example05Camera` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to device or emulator

### Option 2: Build and Install from Command Line

```bash
# From project root - build, install, and launch
./gradlew :examples:Example05Camera:installDebug

# Launch the app
adb shell am start -n de.afarber.openmapview.example05camera/.MainActivity
```

## Code Highlights

### Animating to a Specific Location with Zoom

```kotlin
mapView.animateCamera(
    CameraUpdateFactory.newLatLngZoom(targetLocation, 15.0),
    1000, // 1 second duration
    object : CancelableCallback {
        override fun onFinish() {
            Toast.makeText(context, "Animation finished", Toast.LENGTH_SHORT).show()
        }
        override fun onCancel() {
            Toast.makeText(context, "Animation cancelled", Toast.LENGTH_SHORT).show()
        }
    }
)
```

### Animating Zoom In/Out

```kotlin
// Zoom in with 500ms animation
mapView.animateCamera(
    CameraUpdateFactory.zoomIn(),
    500
)

// Zoom out with 500ms animation
mapView.animateCamera(
    CameraUpdateFactory.zoomOut(),
    500
)
```

### Stopping an Animation

```kotlin
// Cancel any running animation
mapView.stopAnimation()
```

### Move Camera Instantly (No Animation)

```kotlin
// Jump to location immediately without animation
mapView.moveCamera(
    CameraUpdateFactory.newLatLngZoom(location, 14.0)
)
```

## Key Concepts

### CameraUpdateFactory

Factory for creating camera update operations:

- `newLatLng(LatLng)` - Move to location, keep current zoom
- `newLatLngZoom(LatLng, Double)` - Move to location with specific zoom
- `newCameraPosition(CameraPosition)` - Full control over camera state
- `zoomIn()` - Increment zoom by 1
- `zoomOut()` - Decrement zoom by 1
- `zoomTo(Double)` - Set specific zoom level
- `zoomBy(Double)` - Adjust zoom by amount (positive or negative)

### animateCamera() Overloads

```kotlin
// Default 250ms duration, no callback
animateCamera(cameraUpdate)

// Custom duration, no callback
animateCamera(cameraUpdate, durationMs)

// Custom duration with callback
animateCamera(cameraUpdate, durationMs, listener)
```

### CancelableCallback

Callback interface for animation lifecycle:

- `onFinish()` - Called when animation completes normally
- `onCancel()` - Called when animation is interrupted by stopAnimation() or new animation

### moveCamera vs animateCamera

- `moveCamera()` - Instant camera update, no animation
- `animateCamera()` - Smooth interpolated transition

## What to Test

1. Location Buttons - Click "Loc 1", "Loc 2", "Loc 3" to animate to different markers
2. Different Durations - Notice Location 3 uses 2 second animation (slower)
3. Zoom Controls - Use + and - buttons for smooth zoom animations
4. Callbacks - Location 1 shows toast messages on animation finish/cancel
5. Stop Button - Click during animation to cancel and see onCancel callback
6. Sequential Animations - Start animation, then immediately click another location (first animation cancels)

## Animation Details

This example sets up three marker locations around Bochum, Germany:

| Location | Coordinates | Animation Duration | Callbacks |
| -------- | ----------- | ------------------ | --------- |
| Location 1 | (51.4700, 7.2400) | 1000ms | Yes (toast) |
| Location 2 | (51.4620, 7.2600) | 1000ms | No |
| Location 3 | (51.4550, 7.2350) | 2000ms | No |

## Technical Details

### Animation Implementation

- Uses Kotlin coroutines for smooth frame-by-frame updates
- Linear interpolation between start and end positions
- 60 FPS target (16ms frame delay)
- Automatically handles map tile loading during animation
- Lifecycle-aware: animations cancelled when activity destroyed

### Lifecycle Integration

- Animations run in MapController's coroutine scope
- Automatic cleanup via existing lifecycle hooks
- No memory leaks when activity is destroyed
- onDestroy() cancels all running animations

### Performance

- Efficient coroutine-based implementation
- Minimal CPU usage during animation
- Smooth rendering on all supported devices
- Tile downloads continue during animation

## Advanced Usage

### Custom Animation Duration Based on Distance

```kotlin
val distance = calculateDistance(currentPosition, targetPosition)
val duration = (distance * 100).toInt().coerceIn(500, 3000)

mapView.animateCamera(
    CameraUpdateFactory.newLatLng(targetPosition),
    duration
)
```

### Chain Animations with Callbacks

```kotlin
mapView.animateCamera(
    CameraUpdateFactory.newLatLng(location1),
    1000,
    object : CancelableCallback {
        override fun onFinish() {
            // Start second animation when first completes
            mapView.animateCamera(
                CameraUpdateFactory.zoomIn(),
                500
            )
        }
        override fun onCancel() {
            // Handle cancellation
        }
    }
)
```

### Animate to Show All Markers (Future Feature)

```kotlin
// This will be available in future versions
val bounds = LatLngBounds.builder()
    .include(marker1.position)
    .include(marker2.position)
    .include(marker3.position)
    .build()

mapView.animateCamera(
    CameraUpdateFactory.newLatLngBounds(bounds, padding)
)
```

## Map Location

Default Center: Bochum, Germany (51.4620°N, 7.2480°E) at zoom 13.0

All three marker locations are positioned around Bochum within approximately 2km radius.
