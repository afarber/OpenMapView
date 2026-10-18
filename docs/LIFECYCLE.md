# OpenMapView Lifecycle Management

[Back to README](../README.md)

This document explains how OpenMapView handles Android lifecycle events and compares it with the MapView of the Maps SDK.

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

## How the Maps SDK Does It

The Maps SDK provides two approaches:

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

OpenMapView manages its lifecycle itself, like `SupportMapFragment`, but without requiring Fragments. No lifecycle code is needed in the app.

### The Implementation

OpenMapView implements `DefaultLifecycleObserver` and registers itself while it is attached to a window:

```kotlin
class OpenMapView(context: Context) :
    FrameLayout(context),
    DefaultLifecycleObserver {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        // The Activity, Fragment or Compose screen the view belongs to
        findViewTreeLifecycleOwner()?.lifecycle?.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        // Stops observing the lifecycle, so it can't keep the view in memory
        observedLifecycle?.removeObserver(this)
        onDestroy()
        super.onDetachedFromWindow()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        onDestroy()
    }

    // Also public, like MapView.onDestroy() of the Maps SDK
    fun onDestroy() {
        // Release resources
    }
}
```

Activities (`ComponentActivity`), Fragments and Compose's `AndroidView` all provide the view tree lifecycle owner that the view looks up.

### Usage Pattern

Create the view, nothing else is needed:

```kotlin
@Composable
fun MapViewScreen() {
    AndroidView(
        factory = { context ->
            OpenMapView(context).apply {
                setCenter(LatLng(51.4661, 7.2491))
                setZoom(14.0f)
            }
        }
    )
}
```

Apps written for older versions call `lifecycle.addObserver(mapView)`. This still works: adding the same observer twice does nothing, and the view removes it when detached.

## What Happens During Each Lifecycle Event

### Attached to a window

The view starts observing the lifecycle of its view tree.

### onResume() and onPause()

Do nothing. Tiles are only requested while the map is drawn, and a map in the background is not drawn, so it requests no new tiles. Downloads already running finish, so their tiles are cached when the app comes back.

### Detached from the window, or onDestroy()

```kotlin
fun onDestroy() {
    stopAnimation()                          // Cancel camera animation
    scope.coroutineContext.cancelChildren()  // Cancel tile downloads
    tileDownloader?.close()                  // Close HTTP client
    tileDownloader = null
    downloadingTiles = mutableSetOf()        // Request cancelled tiles again on the next draw
    overlayDownloadingTiles.values.forEach { it.clear() }
}
```

On detach the view also stops observing the lifecycle. This matters in Compose: a map created per screen or tab is detached when the screen is left, while the Activity lives on. Without removing the observer, the Activity's lifecycle would keep every map ever created in memory, including its tile cache.

The cleanup can be undone: calling it twice is safe, and a view that is attached and drawn again (e.g. reused by a list) creates a new HTTP client and requests its tiles again.

## Comparison Table

| Feature | MapView | SupportMapFragment | OpenMapView |
|---------|---------------|---------------------------|-------------|
| **Lifecycle Setup** | Manual (6 methods) | Automatic | Automatic |
| **Memory Leaks if Forgotten** | Yes | No | No (nothing to forget) |
| **Boilerplate Code** | High | Low | None |
| **Flexibility** | High | Medium | High |
| **Works with Compose** | No | No | Yes |
| **Works with XML** | Yes | Yes | Yes |
| **Marker Support** | Yes | Yes | Yes (with colors) |
| **Touch Gestures** | Pan, Zoom, Tilt | Pan, Zoom, Tilt | Pan, Zoom |

## Example Apps

The example applications in the `examples/` directory create the map in an `AndroidView` without any lifecycle code:

```kotlin
AndroidView(
    factory = { context ->
        OpenMapView(context).apply {
            // ... configure map
        }
    }
)
```

## Summary

OpenMapView:
- Observes the lifecycle of the Activity, Fragment or Compose screen it is shown in
- Releases its resources when it leaves the window or its lifecycle owner is destroyed
- Can't be kept in memory by a lifecycle that outlives it
- Keeps working when it is attached again
