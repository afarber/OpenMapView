# Example04Polylines - Polylines and Polygons

[Back to README](../../README.md)

This example demonstrates drawing vector shapes (polylines and polygons) on OpenMapView with navigation controls, click handling, and overlay highlighting.

## Features Demonstrated

- Polylines with custom stroke colors and widths
- Geodesic polylines (great-circle paths for long distances)
- Filled polygons with stroke and fill colors
- Polygons with holes (donut shapes)
- Navigation between overlays with prev/next buttons
- Click handling on polylines and polygons
- Highlight toggle for selected overlay (thicker stroke with dashed pattern)
- Camera animation when selecting overlays
- Real-time status display showing selection and camera state

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

### Overlay Data Model

The example uses an interface for overlay data with two implementations:

```kotlin
interface OverlayData {
    val title: String
    val snippet: String
    val points: List<LatLng>
}

data class PolylineData(
    override val title: String,
    override val snippet: String,
    override val points: List<LatLng>,
    val color: Color,
    val width: Float,
    val geodesic: Boolean = false,  // Draw as great-circle path
) : OverlayData

data class PolygonData(
    override val title: String,
    override val snippet: String,
    override val points: List<LatLng>,
    val holes: List<List<LatLng>> = emptyList(),
    val strokeColor: Color,
    val fillColor: Color,
) : OverlayData
```

### Adding Overlays from Data

```kotlin
poiOverlays.forEach { data ->
    when (data) {
        is PolylineData -> {
            mapView.addPolyline(
                Polyline(
                    points = data.points,
                    strokeColor = data.color,
                    strokeWidth = data.width,
                    geodesic = data.geodesic,
                    clickable = true,
                    tag = data.title,
                )
            )
        }
        is PolygonData -> {
            mapView.addPolygon(
                Polygon(
                    points = data.points,
                    holes = data.holes,
                    strokeColor = data.strokeColor,
                    fillColor = data.fillColor,
                    clickable = true,
                    tag = data.title,
                )
            )
        }
    }
}
```

### Click Handling

```kotlin
setOnPolylineClickListener { polyline ->
    val title = polyline.tag as? String ?: return@setOnPolylineClickListener
    selectedIndex = findOverlayIndexByTitle(title)
    animateCamera(CameraUpdateFactory.newLatLng(getOverlayCenter(overlay)), 500)
}

setOnPolygonClickListener { polygon ->
    val title = polygon.tag as? String ?: return@setOnPolygonClickListener
    selectedIndex = findOverlayIndexByTitle(title)
    animateCamera(CameraUpdateFactory.newLatLng(getOverlayCenter(overlay)), 500)
}
```

### Highlight Feature

Toggle highlight by replacing overlays with thicker stroke:

```kotlin
fun updateHighlight(map: OpenMapView, highlighted: Boolean, highlightIndex: Int) {
    // Remove existing overlays
    createdPolylines.forEach { map.removePolyline(it) }
    createdPolygons.forEach { map.removePolygon(it) }

    // Recreate with highlight (2x stroke width when highlighted)
    val strokeMultiplier = if (highlighted && index == highlightIndex) 2.0f else 1.0f
    // ... create overlays with multiplied stroke width
}
```

## What to Test

1. **Launch the app** - you should see 4 polylines (including 1 geodesic) and 3 polygons
2. **Navigate overlays** - use prev/next buttons at the bottom
3. **Click overlays** - tap any polyline or polygon to select it
4. **Toggle highlight** - tap FAB to highlight selected overlay (thicker stroke)
5. **Observe status** - left panel shows overlay info and camera state
6. **Pan and zoom** - shapes stay at correct geographic positions

## UI Components

- **StatusToolbar** - Displays overlay index, title, type, camera state, highlight status
- **OverlayToolbar** - Prev/next navigation buttons
- **FAB** - Toggle highlight (changes color when active)

## Key Concepts

- **Polyline**: Connected line segments defined by a list of LatLng points
- **Polygon**: Closed shape with fill color, automatically closed between last and first point
- **Holes**: Polygons can have interior cutouts (donut shapes)
- **Click handling**: Use `setOnPolylineClickListener` and `setOnPolygonClickListener`
- **Tag property**: Store metadata (like title) for identification in click handlers

## Styling Options

### Polyline Properties

- **points**: List of LatLng coordinates (minimum 2 points)
- **strokeColor**: Line color (Compose Color)
- **strokeWidth**: Line width in pixels (Float)
- **geodesic**: Draw as great-circle path instead of straight Mercator line (Boolean)
- **startCap/endCap**: Shape of line endpoints (StrokeCap: Butt, Round, Square)
- **spans**: List of StyleSpan for multi-colored segments
- **clickable**: Enable click handling (Boolean)
- **tag**: Optional user data (Any?)

### Polygon Properties

- **points**: List of LatLng coordinates (minimum 3 points)
- **strokeColor**: Outline color (Compose Color)
- **strokeWidth**: Outline width in pixels (Float)
- **fillColor**: Interior fill color with alpha channel support
- **holes**: List of hole definitions, each a List<LatLng> (minimum 3 points per hole)
- **geodesic**: Draw edges as great-circle paths (Boolean)
- **clickable**: Enable click handling (Boolean)
- **tag**: Optional user data (Any?)

## Technical Details

### Rendering Order (Z-Index)

Shapes are drawn in this order (bottom to top):

1. Map tiles (base layer)
2. Polygons (filled areas)
3. Polylines (lines)
4. Markers (icons)
5. Attribution overlay

### Immutable Overlays

Polyline and Polygon are immutable data classes. To modify an overlay (like changing stroke width for highlight), you must:

1. Remove the original overlay
2. Add a new overlay with modified properties
