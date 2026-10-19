# Rotation (Compose)

[Back to README](../../../README.md) | Same app with Views and Java: [Rotation (Views)](../../views/Rotation)

A bus drives a loop through Bochum. Heading up, the map turns with the bus like a navigation app; north up, it only follows the bus. Turn the map yourself with two fingers or the slider, and back north with the compass.
The map is the `OpenMap` composable from `de.afarber:openmapview-compose`.

## Features

- A camera `bearing`: `CameraPosition(target, zoom, bearing)` turns the map around the camera target
- Heading up with a top `contentPadding`, so the bus drives in the lower part of the map with the road ahead visible
- The bus moves once per display frame with `SphericalUtil.interpolate()` and turns with `SphericalUtil.computeHeading()`
- A flat `Marker` lies on the map and turns with it; a marker that is not flat faces the screen
- Two-finger rotation and the compass, which shows while the map is turned and turns it back north
- `CameraMoveStartedReason.GESTURE` stops following the bus when the user touches the map

## Screenshot

![Rotation (Compose)](screenshot.gif)

## Run

Select `examples.compose.Rotation` in Android Studio, or:

```bash
./gradlew :examples:compose:Rotation:installDebug
adb shell am start -n de.afarber.openmapview.examples.compose.rotation/.MainActivity
```

## Code Highlights

```kotlin
// Once per display frame: the bus moves on, and the camera follows it and turns with it
withFrameNanos { frame ->
    val (position, roadHeading) = routeAt(distance)
    bus.position = position
    busHeading = turnToward(busHeading, roadHeading, (TURN_SPEED * seconds).toFloat())
    cameraPositionState.move(
        CameraUpdateFactory.newCameraPosition(CameraPosition(position, camera.zoom, busHeading)),
    )
}

// A flat icon lies on the map, so its rotation is relative to north
Marker(state = bus, icon = arrow, anchor = 0.5f to 0.5f, rotation = busHeading, flat = true)
```

## What to Try

1. Watch the map turn with the bus at the corners, then tap North up
2. Turn the map with two fingers or the slider, then tap the compass in the top-left corner
3. Turn off Flat bus icon while heading up: the arrow now faces the screen instead of the road
