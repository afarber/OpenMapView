# Example11MapTypes - Switching Between Map Types

[Back to README](../../README.md)

This example demonstrates switching between different map types in OpenMapView, showing various OpenStreetMap-based tile sources with automatic attribution updates.

## Features Demonstrated

- Five map types: Normal, Terrain, Humanitarian, Cycle, None
- Dynamic map type switching with buttons
- Automatic attribution text updates
- Tile cache clearing on map type change
- Material3 UI with button controls
- Error handling for invalid map types

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
mapView.setMapType(MapType.TERRAIN)

// Get current map type
val currentType = mapView.getMapType()
```

### Handle Invalid Types

```kotlin
try {
    mapView.setMapType(99)
} catch (e: IllegalArgumentException) {
    Toast.makeText(context, e.message, Toast.LENGTH_LONG).show()
}
```

### Key Concepts

- **MapType.NORMAL**: Standard OpenStreetMap road map (default)
- **MapType.TERRAIN**: OpenTopoMap with elevation contours
- **MapType.HUMANITARIAN**: HOT OSM emphasizing humanitarian features
- **MapType.CYCLE**: CyclOSM showing cycling infrastructure
- **MapType.NONE**: No base map tiles displayed
- **setMapType()**: Change the map's tile source
- **getMapType()**: Query current map type

## What to Test

1. **Launch the app** - Default Normal map type loads
2. **Click Terrain** - Switches to OpenTopoMap with contour lines
3. **Click Humanitarian** - Switches to red/orange humanitarian style
4. **Click Cycle** - Switches to CyclOSM with bike lanes
5. **Click None** - Removes all base tiles (overlays still visible)
6. **Click Normal** - Returns to standard OSM tiles
7. **Check attribution** - Bottom-right text updates for each source
8. **Pan and zoom** - New tiles load from current source

## Map Types Reference

| Type         | Constant               | Tile Source                   | Best For                                      |
| ------------ | ---------------------- | ----------------------------- | --------------------------------------------- |
| Normal       | `MapType.NORMAL`       | OpenStreetMap Standard        | General purpose street maps                   |
| Terrain      | `MapType.TERRAIN`      | OpenTopoMap                   | Hiking, elevation, topography                 |
| Humanitarian | `MapType.HUMANITARIAN` | Humanitarian OSM              | Emergency response, infrastructure            |
| Cycle        | `MapType.CYCLE`        | CyclOSM                       | Cycling routes, bike infrastructure           |
| None         | `MapType.NONE`         | No tiles                      | Custom overlays only, performance testing     |

## Technical Details

### What Happens on Map Type Change

1. Tile source URL template is updated
2. Memory tile cache is cleared
3. Disk tile cache is cleared
4. Attribution text is updated
5. Map view is invalidated and redraws

### Attribution by Map Type

- **Normal**: Map data from OpenStreetMap
- **Terrain**: Map data from OpenStreetMap | Map style: OpenTopoMap (CC-BY-SA)
- **Humanitarian**: Humanitarian OpenStreetMap Team
- **Cycle**: CyclOSM | Map data from OpenStreetMap

### Performance Notes

- Cache clearing may cause momentary blank tiles
- New tiles download asynchronously
- Each map type has independent cache
- Some tile servers may have rate limits

## Advanced Usage

### Programmatic Map Type Selection

```kotlin
val mapTypes = listOf(
    MapType.NORMAL,
    MapType.TERRAIN,
    MapType.HUMANITARIAN,
    MapType.CYCLE
)

// Cycle through map types
var currentIndex = 0
button.setOnClickListener {
    currentIndex = (currentIndex + 1) % mapTypes.size
    mapView.setMapType(mapTypes[currentIndex])
}
```

### Check Current Map Type

```kotlin
when (mapView.getMapType()) {
    MapType.NORMAL -> showNormalControls()
    MapType.TERRAIN -> showElevationLegend()
    MapType.CYCLE -> showCyclingStats()
    else -> hideSpecialControls()
}
```

## Map Location

**Default Center:** Bochum, Germany (51.4661°N, 7.2491°E) at zoom 13.0

Test different map types at different locations:
- **Terrain**: Best viewed in mountainous areas (Alps, Black Forest)
- **Humanitarian**: Shows infrastructure in any populated area
- **Cycle**: Best in cities with cycling infrastructure
