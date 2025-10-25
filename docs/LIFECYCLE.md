# OpenMapView Lifecycle Management

[Back to README](../README.md)

This document explains how OpenMapView handles Android lifecycle events and compares it with Google's MapView approach.

## Why Lifecycle Management Matters

Map views need to manage resources properly:
- **Network connections** for downloading tiles
- **Memory caches** for storing bitmaps
- **Coroutines** for async operations
- **HTTP clients** for tile requests

Without proper lifecycle management, your app will:
- Waste battery downloading tiles when in background
- Leak memory by not cleaning up caches
- Keep network connections open unnecessarily

## How Google MapView Does It

Google provides two approaches:

### Approach 1: MapView (XML-based) - Manual Lifecycle

```kotlin
class MainActivity : AppCompatActivity() {
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)  // REQUIRED
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()  // REQUIRED
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()  // REQUIRED
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()  // REQUIRED
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)  // REQUIRED
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()  // REQUIRED
    }
}
```

**Pros:** Fine-grained control
**Cons:** Easy to forget, verbose, error-prone

### Approach 2: SupportMapFragment - Automatic Lifecycle

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .add(R.id.container, mapFragment)
            .commit()
    }
    // No lifecycle methods needed!
}
```

**Pros:** Automatic, no boilerplate
**Cons:** Must use Fragment architecture

## How OpenMapView Does It

OpenMapView uses **Android Architecture Components** - specifically `DefaultLifecycleObserver` - which provides the best of both worlds.

### The Implementation

OpenMapView implements `DefaultLifecycleObserver`:

```kotlin
class OpenMapView(context: Context) :
    FrameLayout(context),
    DefaultLifecycleObserver {  // Implements lifecycle callbacks

    override fun onResume(owner: LifecycleOwner) {
        // Called when app comes to foreground
    }

    override fun onPause(owner: LifecycleOwner) {
        // Called when app goes to background
    }

    override fun onDestroy(owner: LifecycleOwner) {
        // Clean up resources
    }
}
```

### Usage Pattern

You just need to register the observer once:

```kotlin
@Composable
fun MapViewScreen() {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    AndroidView(
        factory = { context ->
            OpenMapView(context).apply {
                // Register lifecycle observer - ONE LINE!
                lifecycleOwner.lifecycle.addObserver(this)

                setCenter(LatLng(51.4661, 7.2491))
                setZoom(14.0)
            }
        }
    )
}
```

**That's it!** The lifecycle is automatically managed from this point forward.

## What Happens During Each Lifecycle Event

### onResume()
```kotlin
fun onResume() {
    // Called when app comes to foreground
    // Could be used to resume tile downloads if paused
}
```

**Current implementation:** Does nothing (tiles continue downloading)
**Future optimization:** Could resume paused downloads

### onPause()
```kotlin
fun onPause() {
    // Called when app goes to background
    // Could be used to pause tile downloads to save battery
}
```

**Current implementation:** Does nothing (tiles continue downloading)
**Future optimization:** Could pause ongoing downloads to save battery

### onDestroy()
```kotlin
fun onDestroy() {
    // Clean up resources to prevent memory leaks
    scope.cancel()                     // Cancel all coroutines (tile downloads)
    tileDownloader.close()             // Close HTTP client
    tileCache.clear()                  // Clear cached bitmaps
    MarkerIconFactory.clearCache()     // Clear marker icon cache
}
```

**Current implementation:** Full cleanup!
- Cancels all running tile downloads
- Closes Ktor HTTP client
- Clears bitmap memory cache (LRU cache for tiles)
- Clears marker icon cache (up to 10 colored marker icons)

## Best Practices

### DO: Register the lifecycle observer

```kotlin
// GOOD - Proper cleanup will happen
OpenMapView(context).apply {
    lifecycleOwner.lifecycle.addObserver(this)
    // ... configure map
}
```

### DON'T: Forget to register

```kotlin
// BAD - Memory leaks on Activity destruction
OpenMapView(context).apply {
    // Missing: lifecycleOwner.lifecycle.addObserver(this)
    setCenter(...)  // Map works but won't clean up!
}
```

## Comparison Table

| Feature | Google MapView | Google SupportMapFragment | OpenMapView |
|---------|---------------|---------------------------|-------------|
| **Lifecycle Setup** | Manual (6 methods) | Automatic | Semi-automatic (1 line) |
| **Memory Leaks if Forgotten** | Yes | No | Yes (but less likely) |
| **Boilerplate Code** | High | Low | Very Low |
| **Flexibility** | High | Medium | High |
| **Works with Compose** | No | No | Yes |
| **Works with XML** | Yes | Yes | Yes |
| **Marker Support** | Yes | Yes | Yes (with colors) |
| **Touch Gestures** | Pan, Zoom, Tilt | Pan, Zoom, Tilt | Pan, Zoom |

## Testing Lifecycle

To verify lifecycle is working:

1. Run the app and observe Logcat
2. Press home button - `onPause()` should be called
3. Return to app - `onResume()` should be called
4. Kill the app - `onDestroy()` should be called

Look for these log messages (if you add logging):
```
OpenMapView: onResume called
OpenMapView: onPause called
OpenMapView: onDestroy called - cleaning up resources
```

## What If I Forget?

If you forget to register the lifecycle observer:

**What works:**
- Map displays correctly
- Panning and zooming work
- Markers work
- Everything appears normal

**What breaks:**
- Memory leaks when Activity is destroyed
- Tile downloads continue in background after app closes
- HTTP client stays open
- Cached bitmaps not released

**How to detect:**
- Run app, use map, then close app
- Check Android Profiler in Android Studio
- Look for memory not being released
- Check for ongoing network activity after app closes

## Example Apps

All three example apps demonstrate proper lifecycle management:

### Example01Pan
Basic panning with lifecycle observer registration.

### Example02Zoom
Zoom controls (pinch, buttons) with lifecycle observer registration.

### Example03Markers
Markers with colors and click handling with lifecycle observer registration.

Each example shows the same pattern:
```kotlin
AndroidView(
    factory = { context ->
        OpenMapView(context).apply {
            lifecycleOwner.lifecycle.addObserver(this)  // Always register!
            // ... configure map
        }
    }
)
```

## Summary

OpenMapView uses modern Android Architecture Components to provide:
- Simple one-line lifecycle registration
- Automatic cleanup when Activity/Fragment is destroyed
- Prevention of memory leaks and battery drain
- Best practices demonstrated in all example apps

Always register the lifecycle observer in production apps!
