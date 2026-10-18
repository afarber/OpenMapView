# Performance Optimization

[Back to README](../README.md)

This document describes the performance optimization strategies implemented in OpenMapView to provide smooth map interaction while minimizing memory usage and network requests.

## Overview

OpenMapView employs a multi-layered approach to performance optimization:

1. **Memory-efficient bitmap decoding** - Reduces memory footprint per tile
2. **Two-level caching system** - Fast memory cache with persistent disk fallback
3. **Intelligent tile prefetching** - Anticipates user panning for smoother experience
4. **Optimized rendering** - Minimizes unnecessary redraws and allocations

## Memory Optimization

### RGB_565 Bitmap Format

Map tiles are decoded using the `RGB_565` bitmap configuration instead of the default `ARGB_8888`:

```kotlin
val options = BitmapFactory.Options().apply {
    inPreferredConfig = Bitmap.Config.RGB_565
    inScaled = false
}
```

**Benefits:**
- 50% memory reduction: 2 bytes per pixel vs 4 bytes for ARGB_8888
- Sufficient quality for map tiles (OSM tiles don't use transparency)
- Allows more tiles to be cached in memory
- Reduces garbage collection pressure

**Example:** A single 256x256 tile uses 128KB instead of 256KB.

### Dynamic Memory Allocation

The memory cache size is calculated dynamically based on available heap:

```kotlin
private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
private val cacheSize = maxMemory / 8  // Use 1/8 of available heap
```

This ensures the cache adapts to different device capabilities while avoiding OutOfMemoryErrors.

## Two-Level Caching System

### Architecture

OpenMapView implements a two-tier cache hierarchy:

```
┌─────────────────────────────────────────┐
│         Tile Request                    │
└─────────────────┬───────────────────────┘
                  │
                  v
┌─────────────────────────────────────────┐
│  Level 1: Memory Cache (LRU)            │
│  - Fast access (nanoseconds)            │
│  - ~1/8 of heap memory                  │
│  - Automatically sized                  │
└─────────────────┬───────────────────────┘
                  │ Cache miss
                  v
┌─────────────────────────────────────────┐
│  Level 2: Disk Cache (LRU)              │
│  - Persistent across app restarts       │
│  - 50MB maximum size                    │
│  - Tiles as sent by the server          │
└─────────────────┬───────────────────────┘
                  │ Cache miss
                  v
┌─────────────────────────────────────────┐
│  Level 3: Network Download              │
│  - OpenStreetMap tile servers           │
│  - HTTP with Ktor client                │
└─────────────────────────────────────────┘
```

### Memory Cache (Level 1)

Implemented using Android's `LruCache`:

- **Access time:** Nanoseconds (in-memory lookup)
- **Size:** Dynamically calculated as 1/8 of max heap
- **Eviction:** Least Recently Used (LRU) algorithm
- **Lifecycle:** Shared by all maps in the process, with the tiles of each tile source kept apart. A map that is created again, e.g. when its tab or list item is shown again, draws the tiles from the first frame on instead of showing gray placeholders until they are read from disk. Tile overlays have memory caches of their own.

New tiles are written to both levels as soon as they are downloaded, so they survive the process even if they are never evicted from memory:

```kotlin
fun put(tile: TileCoordinate, bitmap: Bitmap, bytes: ByteArray) {
    memory.bitmaps.put(key(tile), bitmap)
    diskCache?.put(tile, bytes) // the server's bytes, called from the IO download coroutine
}
```

### Disk Cache (Level 2)

`DiskTileCache` keeps one file per tile, without any extra library:

- **Access time:** Milliseconds (disk I/O)
- **Size:** 50MB maximum
- **Format:** The PNG or JPEG bytes as sent by the tile server, so tiles are never re-encoded. They are decoded with RGB_565, the same as fresh downloads
- **Location:** App cache directory (`context.cacheDir/tiles2/<source>/<z>_<x>_<y>.tile`)
- **Persistence:** Survives app restarts
- **Eviction:** When the 50MB are exceeded, the least recently used tiles are deleted until 45MB are left
- **Expiry:** Tiles older than 7 days (the minimum of the OSM tile usage policy) are downloaded again. Reading a tile does not make it younger, the download time is stored in the file
- **Offline fallback:** If downloading an expired tile fails, the old tile is shown and retried with the usual backoff (see [Retry Backoff](#retry-backoff))
- **Writes:** Each tile is written to a temporary file and renamed, so an interrupted write never leaves a broken tile

**Benefits:**
- Reduces network requests on app restart
- Tiles for frequently visited areas persist
- System can clear cache when storage is low

### Cache Promotion

When a tile is found in disk cache, it's promoted back to memory cache:

```kotlin
val diskBitmap = diskCache?.get(tile) // null if missing or expired
if (diskBitmap != null) {
    // Promote to memory for faster subsequent access
    memoryCache.put(tile, diskBitmap)
    return diskBitmap
}
```

This ensures frequently accessed tiles migrate to faster storage.

## Intelligent Tile Prefetching

### Strategy

OpenMapView prefetches tiles adjacent to the visible viewport to enable smooth panning:

```
┌───────────────────────────────────┐
│  Prefetch Buffer (512px / 2 tiles)│
│  ┌─────────────────────────────┐  │
│  │                             │  │
│  │   Visible Viewport          │  │
│  │                             │  │
│  │                             │  │
│  └─────────────────────────────┘  │
│                                   │
└───────────────────────────────────┘
```

### Implementation

Prefetching is triggered only when the viewport changes:

```kotlin
if (lastDrawnTiles != visibleTiles.toSet()) {
    prefetchAdjacentTiles(visibleTiles)
    lastDrawnTiles = visibleTiles.toMutableSet()
}
```

**Why this matters:**
- Prevents continuous downloads during smooth panning
- Reduces redundant network requests
- Avoids unnecessary cache lookups

### Low-Priority Downloads

Prefetched tiles are marked as low-priority and don't trigger redraws:

```kotlin
downloadTile(tile, lowPriority = true)
```

In the download handler:

```kotlin
if (!lowPriority) {
    launch(Dispatchers.Main) {
        onTileLoadedCallback?.invoke()  // Trigger redraw
    }
}
```

This ensures:
- Visible tiles load first and trigger immediate redraws
- Background prefetch doesn't cause UI jank
- Main thread is not overwhelmed with invalidate() calls

### Prefetch Buffer Size

The current implementation uses a 2-tile (512px) buffer around the visible area:

```kotlin
val prefetchTiles = ViewportCalculator.getVisibleTiles(
    center,
    zoom,
    viewWidth + 512,   // Add 2 tiles horizontally
    viewHeight + 512,  // Add 2 tiles vertically
    panOffsetX,
    panOffsetY,
)
```

**Rationale:**
- Balances network usage vs user experience
- Most panning gestures stay within 2-tile range
- Prevents excessive bandwidth consumption
- Keeps memory footprint reasonable

## Rendering Optimizations

### Fractional Zoom

The camera zoom is a float, e.g. 12.4 during a pinch or after `newLatLngBounds()`. The map
draws the tiles of the nearest whole zoom level (12 for 12.4, 13 for 12.6), scaled by
2^(zoom - tile zoom), so between 0.71 and 1.41. Shapes, markers and hit-testing use the
fractional zoom directly. Tile edges are rounded to whole pixels, so neighboring tiles meet
without gaps.

### Minimal Invalidations

The view only invalidates when necessary:

1. **User interactions:** Pan, zoom, marker changes
2. **Tile downloads complete:** Only for visible (high-priority) tiles
3. **Explicit API calls:** setCenter(), setZoom(), etc.

Prefetch downloads don't trigger redraws, reducing rendering overhead.

### Efficient Canvas Operations

- Tiles are drawn directly with `canvas.drawBitmap()` without intermediate transformations
- Placeholder rectangles use simple fill and stroke operations
- Fallback tiles: while a tile downloads, the nearest cached ancestor (up to 4 zoom levels up) is drawn in its place, its matching part scaled up, and any cached children one level down are drawn scaled down on top. This replaces the gray grid with a preview during camera animations and zooming. Only the memory cache is used, so no disk I/O happens while drawing, and `OnMapLoadedCallback` still waits for the real tiles
- Marker rendering uses pre-cached bitmaps from `MarkerIconFactory`

### Download Deduplication

A set tracks tiles currently being downloaded to prevent duplicate requests:

```kotlin
if (!downloadingTiles.contains(tile)) {
    downloadingTiles.add(tile)
    downloadTile(tile)
}
```

This prevents the same tile from being downloaded multiple times during rapid view changes.

### Retry Backoff

When a tile fails to load (offline, HTTP error, or a tile overlay without data for that area), `TileRetryTracker` blocks new requests for it for 2 seconds, doubling with every further failure up to 60 seconds. A delayed redraw retries visible tiles once the delay has passed, even if the map isn't moving. A successful load resets the backoff. This keeps the map from requesting every missing tile on every frame, which matters for the OSM tile usage policy.

## Network Optimizations

### HTTP Client Configuration

Ktor client is configured with reasonable timeouts:

```kotlin
HttpClient(Android) {
    engine {
        connectTimeout = 10_000  // 10 seconds
        socketTimeout = 10_000   // 10 seconds
    }
}
```

### User-Agent Header

All requests include a user-agent header that identifies both OpenMapView and the app, as required by the OSM tile usage policy:

```kotlin
header("User-Agent", UserAgent.value) // "OpenMapView/<version> (<app package name>; https://github.com/afarber/OpenMapView)"
```

The package name comes from the `Context` of the map, so if one app overuses the tile servers, a block only affects that app. The version is `LIBRARY_VERSION`, generated at build time by the `generateLibraryVersion` Gradle task from the same Git tag as the published artifact.

Tile overlays send the same header by default. A `UrlTileProvider` subclass can override `getUserAgent()` to send a different one.

### Coroutine-Based Downloads

Tile downloads use Kotlin coroutines for efficient async I/O:

```kotlin
scope.launch(Dispatchers.IO) {
    // Disk hits don't wait for a download permit
    val bitmap = cache.get(tile) ?: tileDownloadSemaphore.withPermit {
        tileDownloader.downloadTileBytes(url)?.let { bytes ->
            decodeTile(bytes)?.also { cache.put(tile, it, bytes) }
        }
    } ?: cache.getStale(tile) // expired tile as offline fallback
    // ...
}
```

At most 4 base map tiles download at once. Each tile overlay has its own limit of 2, so a slow or failing overlay server only delays the tiles of that overlay, not the base map or other overlays.

Every frame of a camera animation requests the tiles it shows, so a long flight queues hundreds of tiles that are off screen again a moment later. When a queued base map download gets its permit, it is skipped unless its tile is still needed: visible, prefetched, or at the animation target. A skipped tile is requested again as soon as it is visible.

When a camera animation starts, the tiles at its target are requested right away, ahead of the tiles the camera flies over: first a few tiles 3 zoom levels up, which the fallback drawing scales up while the camera zooms in, then the target tiles themselves. So the map is usually complete shortly after the animation ends, instead of waiting for the downloads queued during the flight.

**Benefits:**
- Non-blocking tile downloads
- Bounded bitmap decode pressure during rapid camera changes
- Easy cancellation on view destruction

## Memory Leak Prevention

### Lifecycle Management

MapController releases its resources in `onDestroy()`:

```kotlin
fun onDestroy() {
    stopAnimation()                          // Cancel camera animation
    scope.coroutineContext.cancelChildren()  // Cancel tile downloads, keep the scope usable
    tileDownloader?.close()                  // Close HTTP client, created again on the next download
    tileDownloader = null
    downloadingTiles = mutableSetOf()        // Cancelled tiles are requested again on the next draw
    overlayDownloadingTiles.values.forEach { it.clear() }
    MarkerIconFactory.clearCache()           // Clear marker icon cache
}
```

`OpenMapView` calls it when the view is detached from its window and when its lifecycle owner is destroyed. On detach it also stops observing the lifecycle, so an Activity that outlives a Compose screen doesn't keep the map in memory. A view that is attached again keeps working.

### Bitmap Management

- Bitmaps are stored in caches, not leaked through closures
- Old bitmaps are evicted by LRU policy
- No bitmap references retained after view destruction

## Performance Metrics

### Memory Usage

Typical memory footprint for a visible viewport at zoom level 10:

- **Visible tiles:** ~12 tiles = 1.5MB (with RGB_565)
- **Prefetch buffer:** ~20 additional tiles = 2.5MB
- **Total active memory:** ~4MB for tiles
- **Memory cache capacity:** ~32-64MB depending on device

### Network Usage

With prefetching enabled:

- **Initial view:** ~12 tile downloads (~400KB)
- **Pan gesture:** 0-4 tile downloads (~0-150KB) from cache
- **Zoom change:** 0-20 tile downloads (~0-700KB) depending on zoom delta

## Benchmarking

The tile caches are internal, so measure from the outside:

- **Memory:** Android Studio Memory Profiler, or `adb shell dumpsys meminfo <your.package>`
- **Network:** Android Studio Network Inspector shows every tile request and whether panning hits the cache
- **Rendering:** Profile GPU Rendering in the developer options, or a `Choreographer.FrameCallback`

## Best Practices

For optimal performance:

1. **Lifecycle integration:** Automatic, the view cleans up when it leaves the window (see [Lifecycle Management](LIFECYCLE.md))
2. **Several maps at once:** Each OpenMapView has its own memory cache of up to 1/8 of the heap, so watch memory usage when showing more than one

## References

- [Android Bitmap Best Practices](https://developer.android.com/topic/performance/graphics)
- [OSM Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
