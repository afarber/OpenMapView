# Example04Polylines - Polylines and Polygons

This example demonstrates drawing vector shapes (polylines and polygons) on OpenMapView, including styled lines, filled areas, and polygons with holes.

## Features Demonstrated

- Polylines with custom stroke colors and widths
- Filled polygons with stroke and fill colors
- Polygons with holes (donut shapes)
- Multiple overlapping shapes on a single map
- Shapes that properly pan and zoom with the map
- Semi-transparent polygon fills

## Screenshot

![Example04Polylines Demo](screenshot.gif)

## Quick Start

### Option 1: Run in Android Studio

1. Open the OpenMapView project in Android Studio
2. Select `examples.Example04Polylines` from the run configuration dropdown
3. Click Run (green play button)
4. Deploy to your device or emulator

### Option 2: Build and Install from Command Line

```bash
# From project root - build, install, and launch
./gradlew :examples:Example04Polylines:installDebug

# Launch the app
adb shell am start -n de.afarber.openmapview.example04polylines/.MainActivity
```

## Code Highlights

### Adding a Polyline

```kotlin
OpenMapView(context).apply {
    setCenter(LatLng(51.4661, 7.2491)) // Bochum, Germany
    setZoom(14.0)

    // Add a blue route line
    addPolyline(
        Polyline(
            points = listOf(
                LatLng(51.4700, 7.2400),
                LatLng(51.4680, 7.2450),
                LatLng(51.4650, 7.2500),
                LatLng(51.4620, 7.2550),
            ),
            strokeColor = Color.BLUE,
            strokeWidth = 8f,
        )
    )
}
```

### Adding a Polygon

```kotlin
// Add a green area (park)
addPolygon(
    Polygon(
        points = listOf(
            LatLng(51.4640, 7.2380),
            LatLng(51.4660, 7.2380),
            LatLng(51.4660, 7.2420),
            LatLng(51.4640, 7.2420),
        ),
        strokeColor = Color.rgb(0, 128, 0),
        strokeWidth = 4f,
        fillColor = Color.argb(100, 0, 255, 0), // Semi-transparent green
    )
)
```

### Adding a Polygon with a Hole

```kotlin
// Add a donut-shaped polygon
addPolygon(
    Polygon(
        points = listOf(
            LatLng(51.4700, 7.2580),
            LatLng(51.4720, 7.2580),
            LatLng(51.4720, 7.2620),
            LatLng(51.4700, 7.2620),
        ),
        holes = listOf(
            listOf(
                LatLng(51.4706, 7.2590),
                LatLng(51.4714, 7.2590),
                LatLng(51.4714, 7.2610),
                LatLng(51.4706, 7.2610),
            ),
        ),
        strokeColor = Color.CYAN,
        strokeWidth = 4f,
        fillColor = Color.argb(100, 0, 255, 255), // Semi-transparent cyan
    )
)
```

### Key Concepts

- **Polyline**: Connected line segments defined by a list of LatLng points
- **Polygon**: Closed shape with fill color, automatically closed between last and first point
- **Holes**: Polygons can have interior cutouts (donut shapes)
- **addPolyline()**: Add a polyline to the map
- **addPolygon()**: Add a polygon to the map
- **removePolyline()/removePolygon()**: Remove specific shapes
- **clearPolylines()/clearPolygons()**: Remove all shapes of that type

## What to Test

1. **Launch the app** - you should see two polylines and two polygons
2. **Pan the map** - shapes stay at correct geographic positions
3. **Zoom in/out** - shapes scale and remain properly positioned
4. **Observe layering** - polygons render below polylines, polylines below markers
5. **Notice transparency** - polygon fills are semi-transparent

## Shapes in this Example

This example displays 4 shapes around Bochum, Germany:

| Shape Type | Color | Description                      |
| ---------- | ----- | -------------------------------- |
| Polyline   | Blue  | Route path (8px wide)            |
| Polyline   | Red   | Alternative route (6px wide)     |
| Polygon    | Green | Rectangular area (park/zone)     |
| Polygon    | Cyan  | Rectangular area with inner hole |

## Styling Options

### Polyline Properties

- **points**: List of LatLng coordinates (minimum 2 points)
- **strokeColor**: Line color (Int from Color class)
- **strokeWidth**: Line width in pixels (Float)
- **tag**: Optional user data (Any?)

### Polygon Properties

- **points**: List of LatLng coordinates (minimum 3 points)
- **strokeColor**: Outline color (Int from Color class)
- **strokeWidth**: Outline width in pixels (Float)
- **fillColor**: Interior fill color with alpha channel support
- **holes**: List of hole definitions, each a List<LatLng> (minimum 3 points per hole)
- **tag**: Optional user data (Any?)

## Technical Details

### Rendering Order (Z-Index)

Shapes are drawn in this order (bottom to top):

1. Map tiles (base layer)
2. Polygons (filled areas)
3. Polylines (lines)
4. Markers (icons)
5. Attribution overlay

Shapes of the same type are drawn in the order they were added.

### Coordinate System

- Uses Web Mercator projection (EPSG:3857)
- Geographic coordinates (LatLng) automatically converted to screen pixels
- Shapes properly transform during pan and zoom operations

### Performance

- Paths are created on-the-fly during rendering
- Paint objects configured per shape for accurate styling
- Anti-aliasing enabled for smooth lines
- Round caps and joins for professional appearance

## Advanced Usage

### Create Complex Routes

```kotlin
val route = Polyline(
    points = listOf(
        LatLng(51.4700, 7.2400),
        LatLng(51.4680, 7.2450),
        // ... add more waypoints
    ),
    strokeColor = Color.BLUE,
    strokeWidth = 10f,
    tag = "main_route" // Custom metadata
)
addPolyline(route)
```

### Create Multi-Hole Polygons

```kotlin
val complexPolygon = Polygon(
    points = outerBoundary,
    holes = listOf(
        hole1Points,
        hole2Points,
        hole3Points,
    ),
    strokeColor = Color.BLACK,
    strokeWidth = 2f,
    fillColor = Color.argb(80, 255, 255, 0),
)
addPolygon(complexPolygon)
```

### Remove Shapes

```kotlin
val polyline = addPolyline(...)
// Later:
removePolyline(polyline)

// Or remove all:
clearPolylines()
clearPolygons()
```

## Next Steps

- Try [Example01Pan](../Example01Pan) for basic map panning
- Try [Example02Zoom](../Example02Zoom) for zoom controls
- Try [Example03Markers](../Example03Markers) for marker overlays
- Try [Example05Camera](../Example05Camera) for camera animations
- Add your own geographic data (routes, boundaries, zones)
- Experiment with different colors and transparency levels

## Map Location

**Default Center:** Bochum, Germany (51.4661°N, 7.2491°E) at zoom 14.0

All shapes are positioned around Bochum within approximately 1km radius.
