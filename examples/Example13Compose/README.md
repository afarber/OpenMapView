# Example13Compose - OpenMap Composable

[Back to README](../../README.md)

This example shows the `OpenMap` composable from the optional `de.afarber:openmapview-compose` artifact in an app with two tabs.

## Features Demonstrated

- `OpenMap` without any lifecycle code or `Modifier.clipToBounds()`
- `rememberCameraPositionState` hoisted above the tabs, so the map keeps its camera when the map tab is left and shown again
- Moving the camera by setting `CameraPositionState.position` from another tab
- Animating the camera with the suspending `CameraPositionState.animate`
- Adding markers in `onMapCreated`

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example13Compose` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to your device or emulator

### Option 2: Build and Install from Command Line

```bash
./gradlew :examples:Example13Compose:installDebug
adb shell am start -n de.afarber.openmapview.example13compose/.MainActivity
```

## Code Highlights

```kotlin
@Composable
fun TabsScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Hoisted above the tabs: the map is created again on every visit of the map tab,
    // but keeps its camera
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition(BOCHUM, 14f)
    }

    Scaffold(bottomBar = { /* Map and Camera tabs */ }) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> MapTab(cameraPositionState)
                else -> CameraTab(cameraPositionState) { selectedTab = 0 }
            }
        }
    }
}

@Composable
fun MapTab(cameraPositionState: CameraPositionState) {
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        OpenMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            onMapCreated = { map -> map.addMarker(Marker(position = BOCHUM, title = "Bochum")) },
        )
        Button(onClick = { scope.launch { cameraPositionState.animate(CameraUpdateFactory.zoomIn()) } }) {
            Text("Zoom in")
        }
    }
}
```

## What to Try

1. Pan and zoom the map, then switch to the Camera tab: it shows the current camera position
2. Switch back to the Map tab: the new map starts at the same position
3. Tap "Show Bochum" on the Camera tab: the map tab opens at the initial position
4. Tap "Zoom in" for an animated zoom

## Notes

`OpenMap` sets the camera move and idle listeners of the map to update the state. Observe the camera through `cameraPositionState.position` (e.g. with `snapshotFlow`) instead of setting those listeners in `onMapCreated`.
