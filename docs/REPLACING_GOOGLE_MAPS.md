# Replacing Google Maps with OpenStreetMap

[Back to README](../README.md) | [API Compatibility](PUBLIC_API.md) | [Architecture](ARCHITECTURE.md) | [Lifecycle](LIFECYCLE.md)

This guide helps developers migrate from Google Maps SDK to OpenMapView, an open-source alternative powered by OpenStreetMap. Whether you're building consumer apps, creating AOSP devices, or developing for Android Automotive OS (AAOS), this migration guide will help you make the transition.

---

## Who Should Consider OpenMapView?

### Android App Developers

Replace Google Maps in your applications without vendor lock-in, API key requirements, or usage restrictions. Perfect for apps that need:

- Freedom from rate limits and usage quotas
- Predictable costs (no per-request billing)
- Privacy-focused mapping without third-party data collection
- Full control over map rendering and tile sources
- MIT licensed code for commercial use

### AOSP Device Manufacturers

Google Maps is not part of the Android Open Source Project (AOSP). Device manufacturers who want to include mapping functionality typically need to:

- Enter into licensing agreements with Google
- Meet Google Mobile Services (GMS) certification requirements
- Bundle multiple Google applications together as part of GMS
- Accept Google's terms regarding app distribution and updates

**OpenMapView provides an alternative that:**

- Is fully open-source (MIT licensed)
- Requires no licensing agreements or certifications
- Has no app bundling requirements
- Allows complete customization and white-labeling
- Works on any Android device without Google dependencies

Reference: [Android Open Source Project documentation](https://source.android.com/)

### Android Automotive OS (AAOS) Manufacturers

While Android Automotive OS itself is open-source, Google's automotive services including Google Maps require separate licensing agreements. Automotive manufacturers concerned about:

- **Data Privacy**: Full control over customer location data and usage analytics
- **Customization**: No restrictions on UI/UX modifications or branding
- **Vendor Independence**: Freedom from service-level agreements with Google
- **Cost Predictability**: No per-vehicle or per-activation licensing fees

OpenMapView allows automotive OEMs to integrate mapping functionality with complete control over the user experience and data handling.

Reference: [Android Automotive OS documentation](https://source.android.com/devices/automotive)

---

## Privacy and Data Control

### Google Maps Data Collection

Google Maps, as a cloud-connected service, collects various types of user data including location history, search queries, and navigation patterns according to Google's privacy policy. This data is used for service improvements, personalization, and other business purposes.

### OpenMapView Privacy Model

OpenMapView gives you complete control:

- **No third-party data transmission**: User location data never leaves your app unless you explicitly send it
- **Choose your tile server**: Use OpenStreetMap's public tiles, self-host tiles, or use commercial OSM providers
- **Transparent codebase**: Fully auditable open-source code
- **Customizable telemetry**: You decide what analytics to collect and where they go

Note: Privacy benefits depend on implementation. Developers are responsible for their own data collection practices and compliance with privacy regulations.

---

## Quick Comparison: Google Maps SDK vs OpenMapView

| Feature | Google Maps SDK | OpenMapView |
|---------|----------------|-------------|
| **Licensing** | Proprietary, requires API key | MIT licensed, no keys needed |
| **Cost** | Pay per request after free tier | Free (OSM tile servers may have usage policies) |
| **Architecture** | Two classes (MapView + GoogleMap) | Single unified class |
| **Initialization** | Async callback (`getMapAsync`) | Immediate availability |
| **Lifecycle** | Manual forwarding (6 methods) | Automatic (`DefaultLifecycleObserver`) |
| **API Compatibility** | N/A | See [PUBLIC_API.md](PUBLIC_API.md) for detailed coverage |
| **Core Features** | Full support | Full support (markers, shapes, camera, overlays) |
| **Google Services** | Traffic, indoor maps, POI data | Not available (requires Google data) |
| **Data Control** | Data flows through Google | Full control over all data |
| **Tile Sources** | Google tiles only | OSM tiles, custom servers, offline tiles |
| **Customization** | Limited by TOS | Complete freedom |

---

## Migration Guide

### Step 1: Update Dependencies

**Remove Google Maps:**

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
    implementation("de.afarber:openmapview:0.11.0")
}
```

Available on [Maven Central](https://central.sonatype.com/artifact/de.afarber/openmapview).

### Step 2: Update Imports

**Before (Google Maps):**

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

**Before (Google Maps):**

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

**Before (Google Maps) - Manual Forwarding:**

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

        // Single line for lifecycle - that's it!
        lifecycle.addObserver(mapView)
    }
    // No other lifecycle methods needed
}
```

See [LIFECYCLE.md](LIFECYCLE.md) for detailed comparison.

### Step 5: Replace Async Map Initialization

**Before (Google Maps) - Async Pattern:**

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
lifecycle.addObserver(mapView)

// Use right away, no callback needed
mapView.moveCamera(CameraUpdateFactory.newLatLngZoom(
    LatLng(51.4661, 7.2491), 14.0
))
mapView.addMarker(MarkerOptions()
    .position(LatLng(51.4661, 7.2491))
    .title("My Location"))
```

### Step 6: Update Marker Code

OpenMapView supports both Kotlin direct instantiation and Google Maps builder pattern:

**Google Maps Style (still works):**

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

**Before (Google Maps):**

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

#### Pattern/PathEffect Migration

Google Maps uses `PatternItem` lists, OpenMapView uses Compose `PathEffect`.

**Before (Google Maps):**

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

**Before (Google Maps):**

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

**Before (Google Maps):**

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

**Google Maps:**

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

OpenMapView implements the vast majority of Google MapView's public API. For a complete method-by-method compatibility matrix, see [PUBLIC_API.md](PUBLIC_API.md).

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

**Not Implemented (Google-Specific Features):**

- Traffic layers (requires Google traffic data)
- Indoor maps (requires Google indoor data)
- Street View (requires Google imagery)
- POI (Places of Interest) data
- My Location layer (developers implement their own location handling)

---

## Architecture Differences

OpenMapView uses a simpler architecture than Google Maps SDK. Instead of two classes (`MapView` + `GoogleMap`), everything is in one unified `OpenMapView` class.

**Benefits:**

- No async initialization complexity
- Simpler API surface
- Direct method access without controller object
- Automatic lifecycle management via `DefaultLifecycleObserver`

For architectural deep dive, see [ARCHITECTURE.md](ARCHITECTURE.md).

---

## What You Gain

### No API Keys or Registration

Start using maps immediately without:

- Creating a Google Cloud project
- Enabling Maps SDK
- Generating API keys
- Configuring billing
- Setting up usage restrictions

### No Usage Limits or Costs

- No request quotas
- No per-map-load charges
- No monthly billing
- No credit card required

Note: If using public OpenStreetMap tile servers, follow their [Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/). For high-volume applications, consider self-hosting tiles or using commercial OSM providers.

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

- [ ] Remove Google Maps SDK dependency
- [ ] Add OpenMapView dependency
- [ ] Update all imports (replace `com.google.android.gms.maps.*` with `de.afarber.openmapview.*`)
- [ ] Update XML layouts (replace Google MapView with OpenMapView)
- [ ] Replace manual lifecycle forwarding with `lifecycle.addObserver(mapView)`
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

**Cause:** Lifecycle not properly managed

**Solution:** Ensure `lifecycle.addObserver(mapView)` is called and mapView reference doesn't leak

### Issue: Memory leaks

**Cause:** Not removing lifecycle observer

**Solution:** OpenMapView automatically handles cleanup via `DefaultLifecycleObserver`. Ensure Activity/Fragment lifecycle is not interrupted abnormally.

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
