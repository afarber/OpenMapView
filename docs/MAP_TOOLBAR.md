# Map Toolbar - External App Integration

[Back to README](../README.md)

This document explains how OpenMapView integrates with external map applications using Android's geo: URI scheme with automatic fallback to browser-based OpenStreetMap.

---

## Overview

OpenMapView provides a method to open the current map location in external map applications installed on the device. If no map application is available, it automatically falls back to opening OpenStreetMap.org in the device's browser.

This feature is similar to Google Maps SDK's map toolbar, but instead of requiring Google services, it:
- Uses the standard Android geo: URI scheme
- Works with any map app (Google Maps, OsmAnd, Maps.me, etc.)
- Falls back to OpenStreetMap.org (not Google Maps web)
- Respects user's default map app preference

---

## How It Works

### Geo URI Scheme

Android supports the `geo:` URI scheme defined in [RFC 5870](https://datatracker.ietf.org/doc/html/rfc5870). When you launch a geo: URI, Android finds all apps that can handle map locations and either:
- Opens the user's default map app directly
- Shows an app picker if multiple map apps are installed
- Falls back to browser if no map apps are available

### Format

```
geo:latitude,longitude?z=zoom
```

Example:
```
geo:51.4661,7.2491?z=14
```

---

## User Scenarios

### Scenario 1: User Has Google Maps Installed

```
User action: Taps "Open in External App" button
→ OpenMapView creates geo: intent
→ Android launches Google Maps
→ Location opens in Google Maps at specified zoom level
```

### Scenario 2: User Has OsmAnd Installed

```
User action: Taps "Open in External App" button
→ OpenMapView creates geo: intent
→ Android launches OsmAnd (user's preferred map app)
→ Location opens in OsmAnd at specified zoom level
```

### Scenario 3: User Has No Map Apps (Fallback)

```
User action: Taps "Open in External App" button
→ OpenMapView creates geo: intent
→ Android finds no handlers for geo: URIs
→ OpenMapView detects this and creates browser intent
→ Android launches default browser
→ Browser opens OpenStreetMap.org at specified location
```

### Scenario 4: User Has Multiple Map Apps

```
User action: Taps "Open in External App" button
→ OpenMapView creates geo: intent
→ Android shows app picker dialog
→ User selects preferred app (Google Maps, OsmAnd, Maps.me, etc.)
→ Selected app opens with location
```

---

## Supported Map Applications

The following map applications are known to support geo: URIs:

- **Google Maps** (com.google.android.apps.maps)
- **OsmAnd** (net.osmand.plus / net.osmand)
- **Maps.me** (com.mapswithme.maps.pro)
- **Organic Maps** (app.organicmaps)
- **HERE WeGo** (com.here.app.maps)
- **Waze** (com.waze)
- **Yandex Maps** (ru.yandex.yandexmaps)
- **Sygic** (com.sygic.aura)

If none of these apps are installed, OpenMapView automatically falls back to the browser.

---

## API Usage

### Basic Usage

```kotlin
// Open current map center in external app
mapView.openInExternalApp()
```

### With Label

```kotlin
// Open with a label (shown as pin title in some apps)
mapView.openInExternalApp("Coffee Shop")
```

### Long-Press Example

```kotlin
mapView.setOnMapLongClickListener { latLng ->
    // Move camera to tapped location
    mapView.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    // Open in external app
    mapView.openInExternalApp("Selected Location")
}
```

### Button Click Example

```kotlin
openButton.setOnClickListener {
    val success = mapView.openInExternalApp()
    if (!success) {
        Toast.makeText(context, "Failed to open map app", Toast.LENGTH_SHORT).show()
    }
}
```

---

## Implementation Details

### Intent Resolution

OpenMapView uses `PackageManager.queryIntentActivities()` to check if any app can handle the geo: URI before launching. This prevents crashes and enables automatic fallback.

```kotlin
private fun canHandleIntent(intent: Intent, context: Context): Boolean {
    val packageManager = context.packageManager
    val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.queryIntentActivities(
            intent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
        )
    } else {
        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
    }
    return activities.isNotEmpty()
}
```

### Fallback URL Format

OpenStreetMap.org uses this URL format:

```
https://www.openstreetmap.org/#map=zoom/latitude/longitude
```

Example:
```
https://www.openstreetmap.org/#map=14/51.4661/7.2491
```

### Android Version Compatibility

- Geo URI scheme: Supported since Android 1.0
- PackageManager query: Works on all Android versions
- API 33+ handling: Uses ResolveInfoFlags for compatibility
- Minimum SDK: 23 (Android 6.0) - same as OpenMapView

---

## Privacy Considerations

When using `openInExternalApp()`:
- Location coordinates are sent to the external app
- External app may track user location and behavior
- Browser fallback sends coordinates to OpenStreetMap.org (via URL)
- Users should be aware their location is being shared

---

## References

- [Android Intents - Maps](https://developer.android.com/guide/components/intents-common#Maps)
- [RFC 5870 - Geo URI Scheme](https://datatracker.ietf.org/doc/html/rfc5870)
- [OpenStreetMap.org](https://www.openstreetmap.org/)

---

[Back to README](../README.md)
