# Migrating to OpenMapView

[Back to README](../README.md) | [API Compatibility](PUBLIC_API.md) | [Architecture](ARCHITECTURE.md) | [Lifecycle](LIFECYCLE.md)

This guide helps developers migrate an app from the Google Maps SDK for Android (below: the Maps SDK) to OpenMapView, an open-source map view powered by OpenStreetMap. Whether you're building consumer apps, creating AOSP devices, or developing for Android Automotive OS (AAOS), it shows the changes step by step.

---

## Who Should Consider OpenMapView?

### Android App Developers

Use a map view without vendor lock-in, API key requirements, or usage restrictions. Useful for apps that need:

- Freedom from rate limits and usage quotas
- Predictable costs (no per-request billing)
- Privacy-focused mapping without third-party data collection
- Full control over map rendering and tile sources
- MIT licensed code for commercial use

### AOSP Device Manufacturers

Map SDKs that are not part of the Android Open Source Project (AOSP) may come with their own license terms and certification requirements.

**OpenMapView provides an alternative that:**

- Is fully open-source (MIT licensed)
- Requires no licensing agreements or certifications
- Has no app bundling requirements
- Allows complete customization and white-labeling
- Works on any Android device without Play Services

Reference: [Android Open Source Project documentation](https://source.android.com/)

### Android Automotive OS (AAOS) Manufacturers

Android Automotive OS itself is open-source, and automotive manufacturers often want:

- **Data Privacy**: Full control over customer location data and usage analytics
- **Customization**: No restrictions on UI/UX modifications or branding
- **Vendor Independence**: No service-level agreements for the map view
- **Cost Predictability**: No per-vehicle or per-activation licensing fees

OpenMapView allows automotive OEMs to integrate mapping functionality with complete control over the user experience and data handling.

Reference: [Android Automotive OS documentation](https://source.android.com/devices/automotive)

---

## Privacy and Data Control

OpenMapView gives you complete control:

- **No third-party data transmission**: User location data never leaves your app unless you explicitly send it
- **Choose your tile server**: Use OpenStreetMap's public tiles, self-host tiles, or use commercial OSM providers
- **Transparent codebase**: Fully auditable open-source code
- **Customizable telemetry**: You decide what analytics to collect and where they go

Note: Privacy benefits depend on implementation. Developers are responsible for their own data collection practices and compliance with privacy regulations.

---

## Quick Comparison: Maps SDK vs OpenMapView

| Feature | Maps SDK | OpenMapView |
|---------|----------|-------------|
| **Licensing** | Proprietary, requires API key | MIT licensed, no keys needed |
| **Architecture** | Two classes (MapView + GoogleMap) | Single unified class |
| **Initialization** | Async callback (`getMapAsync`) | Immediate availability |
| **Lifecycle** | Manual forwarding (6 methods) | Automatic |
| **API Compatibility** | N/A | See [PUBLIC_API.md](PUBLIC_API.md) for detailed coverage |
| **Core Features** | Full support | Full support (markers, shapes, camera, overlays) |
| **Traffic, indoor maps, POI data** | Available | Not available |
| **Tile Sources** | The SDK's own tiles | OSM tiles, custom servers, offline tiles |

---

## Migration Guide

### Step 1: Update Dependencies

**Remove the Maps SDK:**

```kotlin
// Remove from build.gradle.kts
dependencies {
    implementation("com.google.android.gms:play-services-maps:18.x.x") // REMOVE
}
```

**Add OpenMapView:**

```kotlin
// Add to build.gradle.kts
dependencies {
    implementation("de.afarber:openmapview:0.23.0")
}
```

Available on [Maven Central](https://central.sonatype.com/artifact/de.afarber/openmapview).

**Requirements:** minSdk 23, compileSdk 37 and Android Gradle Plugin 9.1 or newer (since 0.14.0, because androidx.core 1.19 and Compose UI 1.12 require them). Projects still on compileSdk 36 or AGP 8 can stay on 0.13.3.

### Step 2: Update Imports

**Before (Maps SDK):**

```kotlin
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
```

**After (OpenMapView):**

```kotlin
import de.afarber.openmapview.OpenMapView
import de.afarber.openmapview.LatLng
import de.afarber.openmapview.Marker
import de.afarber.openmapview.MarkerOptions
// Note: No separate GoogleMap import needed
```

### Step 3: Update XML Layouts

**Before (Maps SDK):**

```xml
<com.google.android.gms.maps.MapView
    android:id="@+id/mapView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

**After (OpenMapView):**

```xml
<de.afarber.openmapview.OpenMapView
    android:id="@+id/mapView"
    android:layout_width="match_parent"
    android:layout_height="match_parent" />
```

### Step 4: Simplify Lifecycle Management

**Before (Maps SDK) - Manual Forwarding:**

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)  // Required
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()  // Required
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()  // Required
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()  // Required
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)  // Required
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()  // Required
    }
}
```

**After (OpenMapView) - Automatic Lifecycle:**

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mapView = findViewById<OpenMapView>(R.id.mapView)
    }
    // No lifecycle methods needed: the view observes the Activity's lifecycle
    // and cleans up when it is detached
}
```

See [LIFECYCLE.md](LIFECYCLE.md) for detailed comparison.

### Step 5: Replace Async Map Initialization

**Before (Maps SDK) - Async Pattern:**

```kotlin
mapView.onCreate(savedInstanceState)
mapView.getMapAsync { googleMap ->
    // Map is ready now
    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
        LatLng(51.4661, 7.2491), 14f
    ))
    googleMap.addMarker(MarkerOptions()
        .position(LatLng(51.4661, 7.2491))
        .title("My Location"))
}
```

**After (OpenMapView) - Immediate Availability:**

```kotlin
// Map is ready immediately after construction
val mapView = findViewById<OpenMapView>(R.id.mapView)

// Use right away, no callback needed
mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(
    LatLng(51.4661, 7.2491), 14.0
))
mapView.addMarker(MarkerOptions()
    .position(LatLng(51.4661, 7.2491))
    .title("My Location"))
```

### Step 6: Update Marker Code

OpenMapView supports both Kotlin direct instantiation and the builder pattern:

**Builder Style (still works):**

```kotlin
val marker = mapView.addMarker(
    MarkerOptions()
        .position(LatLng(51.4661, 7.2491))
        .title("Bochum")
        .snippet("City Center")
        .draggable(true)
)
```

**Or Use Kotlin Style (recommended):**

```kotlin
val marker = mapView.addMarker(
    Marker(
        position = LatLng(51.4661, 7.2491),
        title = "Bochum",
        snippet = "City Center",
        draggable = true
    )
)
```

### Step 7: Migrate Color and Stroke Styling

OpenMapView uses Jetpack Compose graphics primitives instead of Android graphics APIs. This provides better color space support and is the modern Android standard.

#### Color Migration

**Before (Maps SDK):**

```kotlin
import android.graphics.Color

// Using predefined colors
val polyline = PolylineOptions()
    .color(Color.RED)
    .width(5f)

// Using custom ARGB colors
val polygon = PolygonOptions()
    .strokeColor(Color.BLUE)
    .fillColor(Color.argb(128, 255, 0, 0))  // Semi-transparent red
```

**After (OpenMapView):**

```kotlin
import androidx.compose.ui.graphics.Color

// Using predefined colors (capitalized names)
val polyline = PolylineOptions()
    .color(Color.Red)
    .width(5f)

// Using custom colors (named parameters)
val polygon = PolygonOptions()
    .strokeColor(Color.Blue)
    .fillColor(Color(red = 255, green = 0, blue = 0, alpha = 128))
```

**Key Differences:**
- Import `androidx.compose.ui.graphics.Color` instead of `android.graphics.Color`
- Predefined colors use capital case: `Color.Red`, `Color.Blue`, `Color.Green`
- Custom colors use named parameters: `Color(red = 255, green = 0, blue = 0, alpha = 128)`
- Hex colors still work: `Color(0xFF0066CC)` or `Color(0x800066CC)` with alpha

In Java code, or to keep the change minimal, the `*Options` builders also take ARGB ints, so `.color(android.graphics.Color.RED)` and `.fillColor(0x80FF0000)` compile unchanged. See [Using OpenMapView from Java](PUBLIC_API.md#using-openmapview-from-java).

#### Pattern/PathEffect Migration

The Maps SDK uses `PatternItem` lists, OpenMapView uses Compose `PathEffect`.

**Before (Maps SDK):**

```kotlin
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.Dot

// Dashed line
val pattern = listOf(Dash(20f), Gap(10f))
polylineOptions.pattern(pattern)

// Dotted line
val dottedPattern = listOf(Dot(), Gap(10f))
polylineOptions.pattern(dottedPattern)
```

**After (OpenMapView):**

```kotlin
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap

// Dashed line (20px dash, 10px gap)
val pattern = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
polylineOptions.strokePattern(pattern)

// Dotted line (2px dot, 8px gap) - use Round cap for circular dots
val dottedPattern = PathEffect.dashPathEffect(floatArrayOf(2f, 8f), 0f)
polylineOptions
    .strokePattern(dottedPattern)
    .strokeCap(StrokeCap.Round)
```

**Key Differences:**
- Use `PathEffect.dashPathEffect(intervals, phase)` instead of `PatternItem` lists
- Intervals array: `[dash1, gap1, dash2, gap2, ...]`
- For dots: use very short dashes (2px) with `StrokeCap.Round`
- Phase parameter (usually 0f): offset into the pattern array

#### Stroke Cap/Join Migration

**Before (Maps SDK):**

```kotlin
import com.google.android.gms.maps.model.Cap
import com.google.android.gms.maps.model.RoundCap
import com.google.android.gms.maps.model.ButtCap
import com.google.android.gms.maps.model.SquareCap
import com.google.android.gms.maps.model.JointType

polylineOptions
    .startCap(RoundCap())
    .endCap(RoundCap())
    .jointType(JointType.ROUND)
```

**After (OpenMapView):**

```kotlin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

polylineOptions
    .strokeCap(StrokeCap.Round)     // Single property for both ends
    .strokeJoin(StrokeJoin.Round)
```

**Key Differences:**
- `StrokeCap`: Single property for both start and end (OpenMapView doesn't support different caps)
- `StrokeCap.Butt` ≈ `ButtCap()`
- `StrokeCap.Round` ≈ `RoundCap()`
- `StrokeCap.Square` ≈ `SquareCap()`
- `StrokeJoin.Miter` ≈ `JointType.DEFAULT`
- `StrokeJoin.Round` ≈ `JointType.ROUND`
- `StrokeJoin.Bevel` ≈ `JointType.BEVEL`

#### Complete Migration Example

**Before (Maps SDK):**

```kotlin
import android.graphics.Color
import com.google.android.gms.maps.model.*

val polyline = googleMap.addPolyline(
    PolylineOptions()
        .add(LatLng(51.5, 0.0))
        .add(LatLng(51.6, 0.1))
        .color(Color.BLUE)
        .width(8f)
        .pattern(listOf(Dash(20f), Gap(10f)))
        .startCap(RoundCap())
        .endCap(RoundCap())
        .jointType(JointType.BEVEL)
        .clickable(true)
)
```

**After (OpenMapView):**

```kotlin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin

val polyline = mapView.addPolyline(
    Polyline(
        points = listOf(LatLng(51.5, 0.0), LatLng(51.6, 0.1)),
        strokeColor = Color.Blue,
        strokeWidth = 8f,
        strokePattern = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f),
        strokeCap = StrokeCap.Round,
        strokeJoin = StrokeJoin.Bevel,
        clickable = true
    )
)
```

### Step 8: Update Camera Animations

Camera APIs are nearly identical:

**Maps SDK:**

```kotlin
googleMap.animateCamera(
    CameraUpdateFactory.newLatLngZoom(LatLng(51.5, 0.0), 10f),
    2000,  // duration in milliseconds
    object : GoogleMap.CancelableCallback {
        override fun onFinish() { /* animation done */ }
        override fun onCancel() { /* animation cancelled */ }
    }
)
```

**OpenMapView:**

```kotlin
mapView.animateCamera(
    CameraUpdateFactory.newLatLngZoom(LatLng(51.5, 0.0), 10.0),
    2000,  // duration in milliseconds
    object : CancelableCallback {
        override fun onFinish() { /* animation done */ }
        override fun onCancel() { /* animation cancelled */ }
    }
)
```

---

## API Compatibility

OpenMapView implements the vast majority of the public MapView API. For a complete method-by-method compatibility matrix, see [PUBLIC_API.md](PUBLIC_API.md).

**Fully Implemented:**

- Camera control (move, animate, zoom)
- Markers (custom icons, draggable, click listeners)
- Polylines and Polygons (with holes)
- Circles with radius and styling
- Ground overlays with rotation
- Tile overlays (including predefined OSM services)
- Projection API (coordinate conversion)
- UI settings and gesture controls
- Click and long-click listeners
- My-location layer (`setMyLocationEnabled`, `LocationSource`, my-location button). Without Play Services the default source is the platform `LocationManager`. As with the Maps SDK, declare `ACCESS_FINE_LOCATION` or `ACCESS_COARSE_LOCATION` in your manifest and request it at runtime first.

**Not Implemented:**

- Traffic layers (requires traffic data)
- Indoor maps (requires indoor data)
- Street View (requires street-level imagery)
- POI (Places of Interest) data

---

## Architecture Differences

OpenMapView uses a simpler architecture than the Maps SDK. Instead of two classes (`MapView` + `GoogleMap`), everything is in one unified `OpenMapView` class.

**Benefits:**

- No async initialization complexity
- Simpler API surface
- Direct method access without controller object
- Automatic lifecycle management, also in Compose

For architectural deep dive, see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## What You Gain

### No API Keys or Registration

Start using maps immediately without:

- Creating a cloud project
- Enabling the Maps SDK
- Generating API keys
- Configuring billing
- Setting up usage restrictions

### No Usage Limits or Costs

- No request quotas
- No per-map-load charges
- No monthly billing
- No credit card required

Note: If using public OpenStreetMap tile servers, follow their [Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/). OpenMapView sends a User-Agent containing your app package name, so the tile servers can tell apps apart. For high-volume applications, consider self-hosting tiles or using commercial OSM providers.

### Complete Data Control

- User location data stays in your app
- No tracking or profiling by third parties
- Choose where tile requests go
- Implement your own analytics
- Full GDPR/privacy compliance control

### Customization Freedom

- Modify any part of the codebase
- White-label without restrictions
- Custom tile sources (OSM, offline, proprietary)
- No Terms of Service limitations on UI modifications
- Fork and extend as needed (MIT license)

---

## Migration Checklist

Use this checklist to track your migration progress:

- [ ] Remove the Maps SDK dependency
- [ ] Add OpenMapView dependency
- [ ] Update all imports (replace `com.google.android.gms.maps.*` with `de.afarber.openmapview.*`)
- [ ] Update XML layouts (replace MapView with OpenMapView)
- [ ] Remove manual lifecycle forwarding (`onCreate`, `onResume`, `onDestroy`, ...)
- [ ] Remove `getMapAsync()` callbacks (use map immediately)
- [ ] Update marker creation code
- [ ] Update camera animation code
- [ ] Test map initialization
- [ ] Test marker placement and interaction
- [ ] Test camera movements and zoom
- [ ] Test gesture handling (pan, zoom, rotate)
- [ ] Verify lifecycle behavior (pause, resume, destroy)
- [ ] Review API compatibility for any advanced features (see [PUBLIC_API.md](PUBLIC_API.md))
- [ ] Update ProGuard/R8 rules if needed
- [ ] Test on multiple Android versions
- [ ] Performance testing with many markers/overlays

---

## Common Migration Issues

### Issue: Map shows blank tiles

**Cause:** Network connectivity or tile server issues

**Solution:**
- Check internet permissions in AndroidManifest.xml
- Verify device has network connectivity
- Check logcat for tile download errors
- Consider implementing error handling for tile failures

### Issue: Markers don't appear

**Cause:** Markers added before view is laid out

**Solution:**
```kotlin
mapView.post {
    // View is laid out now, add markers
    mapView.addMarker(marker)
}
```

### Issue: App crashes on rotation

**Cause:** A reference to the map view outlives its Activity

**Solution:** Don't keep the map view in a ViewModel or a static field; recreate it with the Activity

### Issue: Memory leaks

**Cause:** A reference to the map view is kept after it leaves the screen

**Solution:** OpenMapView releases its resources and stops observing the lifecycle when it is detached from its window. Make sure nothing else, such as a ViewModel or a static field, keeps a reference to it.

---

## Code Examples

See working example applications in the `examples/` directory of the repository, covering features like basic panning, zoom controls, markers, polylines, camera animations, click listeners, draggable markers, circles, tile overlays, ground overlays, and map type switching.

---

## Next Steps

1. **Read the API compatibility matrix**: [PUBLIC_API.md](PUBLIC_API.md)
2. **Understand the architecture**: [ARCHITECTURE.md](ARCHITECTURE.md)
3. **Learn lifecycle management**: [LIFECYCLE.md](LIFECYCLE.md)
4. **Explore map types**: [MAP_TYPES.md](MAP_TYPES.md)
5. **Review API documentation**: [https://afarber.github.io/OpenMapView/](https://afarber.github.io/OpenMapView/)
6. **Try the examples**: Clone the repository and run example apps in `examples/` directory
7. **Report issues**: [GitHub Issues](https://github.com/afarber/OpenMapView/issues)

---

## Resources

- [GitHub Repository](https://github.com/afarber/OpenMapView)
- [API Documentation (KDoc)](https://afarber.github.io/OpenMapView/)
- [Maven Central](https://central.sonatype.com/artifact/de.afarber/openmapview)
- [OpenStreetMap](https://www.openstreetmap.org/)
- [OSM Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/)
- [Android Open Source Project](https://source.android.com/)
- [Android Automotive OS](https://source.android.com/devices/automotive)

---

[Back to README](../README.md)
