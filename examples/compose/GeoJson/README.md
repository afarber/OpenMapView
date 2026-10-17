# GeoJson (Compose)

[Back to README](../../../README.md) | Same app with Views and Java: [GeoJson (Views)](../../views/GeoJson)

Load landmarks, routes and areas in Bochum from a GeoJSON file in the app assets, with the colors and widths stored in the file. Tap a feature to read its name and description, hide points, lines or areas, and fit everything into view again.
The map is the `OpenMap` composable from `de.afarber:openmapview-compose`.

## Features

- `GeoJsonParser.parse()` turns Points into markers, LineStrings into polylines and Polygons (also with holes) into polygons
- Styles from the simplestyle properties `stroke`, `stroke-width`, `fill` and `fill-opacity`
- The parsed features are drawn with the `Marker`, `Polyline` and `Polygon` composables
- Each feature's properties end up in the tag of its marker or shape, shown in the status card on tap
- `GeoJsonResult.bounds` fits all features below the status card and next to the panel with `contentPadding`

## Screenshot

![GeoJson (Compose)](screenshot.gif)

## Run

Select `examples.compose.GeoJson` in Android Studio, or:

```bash
./gradlew :examples:compose:GeoJson:installDebug
adb shell am start -n de.afarber.openmapview.examples.compose.geojson/.MainActivity
```

## Code Highlights

```kotlin
// Points become markers, LineStrings polylines and Polygons polygons
val result = remember { GeoJsonParser.parse(context.assets.open("bochum.geojson").bufferedReader().use { it.readText() }) }

OpenMap(cameraPositionState = cameraPositionState) {
    result.polygons.forEach { area ->
        Polygon(
            points = area.points,
            holes = area.holes,
            strokeColor = area.strokeColor,
            fillColor = area.fillColor,
            visible = showAreas,
            clickable = true,
            // The tag holds the properties of the GeoJSON feature
            tag = area.tag,
            onClick = { selected = it.tag.toFeature() },
        )
    }
}

// The bounds contain all features
result.bounds?.let { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(it, 64), 800) }
```

## What to Try

1. Tap the green Stadtpark, then the blue cycle path
2. Tap the Ruhr University marker for its info window
3. Turn off Points and Lines, then turn them on again and tap Show all
