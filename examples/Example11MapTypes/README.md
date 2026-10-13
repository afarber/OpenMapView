# Example11MapTypes - Switching Between Map Types

[Back to README](../../README.md)

This example demonstrates switching between different map types in OpenMapView, showing all OpenStreetMap-based tile sources built into the library with automatic attribution updates.

## Features Demonstrated

- All 15 map types (`MapType.NONE` to `MapType.STAMEN_WATERCOLOR`), one button each
- Automatic attribution text updates
- Each map type keeps its own tile cache, so switching back shows cached tiles right away
- Fallback to the standard map with an "API key required" overlay when a key is missing
- Material3 UI with button controls

## Screenshot

![Example11MapTypes Demo](screenshot.gif)

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example11MapTypes` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to your device or emulator

### Option 2: Build and Install from Command Line

```bash
# From project root - build, install, and launch
./gradlew :examples:Example11MapTypes:installDebug

# Launch the app
adb shell am start -n de.afarber.openmapview.example11maptypes/.MainActivity
```

## Code Highlights

### Setting Map Type

```kotlin
val mapView = OpenMapView(context)

// Set map type
mapView.setMapType(MapType.OPENTOPOMAP)

// Get current map type
val currentType = mapView.getMapType()
```

### Handle Invalid Types

`setMapType()` throws for values outside the `MapType` constants:

```kotlin
try {
    mapView.setMapType(99)
} catch (e: IllegalArgumentException) {
    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
}
```

## Map Types Reference

| Button | Constant | Tile Source | API Key |
| ------ | -------- | ----------- | ------- |
| None | `MapType.NONE` | No tiles | |
| Standard | `MapType.STANDARD` | OpenStreetMap | |
| CyclOSM | `MapType.CYCLOSM` | CyclOSM | |
| Cycle Map | `MapType.CYCLEMAP` | Thunderforest | Thunderforest |
| Transport | `MapType.TRANSPORT` | Thunderforest | Thunderforest |
| Transport Dark | `MapType.TRANSPORT_DARK` | Thunderforest | Thunderforest |
| Tracestrack Topo | `MapType.TRACESTRACK_TOPO` | Tracestrack | Tracestrack |
| Humanitarian | `MapType.HUMANITARIAN` | Humanitarian OSM Team | |
| OPNVKarte | `MapType.OPNVKARTE` | MeMoMaps | |
| OpenTopoMap | `MapType.OPENTOPOMAP` | OpenTopoMap | |
| Carto Light | `MapType.CARTO_LIGHT` | CARTO | |
| Carto Dark | `MapType.CARTO_DARK` | CARTO | |
| Carto Voyager | `MapType.CARTO_VOYAGER` | CARTO | |
| Stamen Toner | `MapType.STAMEN_TONER` | Stadia Maps | Stadia Maps |
| Stamen Watercolor | `MapType.STAMEN_WATERCOLOR` | Stadia Maps | Stadia Maps |

See the [Map Types Guide](../../docs/MAP_TYPES.md) for details on each type and on configuring API keys.

## What to Test

1. **Launch the app** - The standard OSM map of Switzerland loads
2. **Click through the buttons** - The map and the attribution text change for each source
3. **Click a type that needs a key** - Without a key, the standard map stays and an "API key required" overlay appears
4. **Click None** - Removes all base tiles
5. **Switch back** - Tiles of a type you already viewed come from its cache
6. **Pan and zoom** - New tiles load from the current source

## Map Location

**Default Center:** Switzerland (46.8182N, 8.2275E) at zoom 12, a good place to compare the topographic styles.
