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
    inDither = false
    inPreferQualityOverSpeed = false
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
│  - PNG compressed tiles                 │
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
- **Lifecycle:** Cleared when map view is destroyed

When a tile is evicted from memory, it's automatically written to disk:

```kotlin
override fun entryRemoved(
    evicted: Boolean,
    key: TileCoordinate,
    oldValue: Bitmap,
    newValue: Bitmap?,
) {
    if (evicted && diskCache != null) {
        CoroutineScope(Dispatchers.IO).launch {
            diskCache?.put(key, oldValue)
        }
    }
}
```

### Disk Cache (Level 2)

Implemented using Jake Wharton's DiskLruCache library:

- **Access time:** Milliseconds (disk I/O)
- **Size:** 50MB maximum
- **Format:** PNG compressed images
- **Location:** App cache directory (`context.cacheDir/tiles`)
- **Persistence:** Survives app restarts
- **Eviction:** LRU with size limit

**Benefits:**
- Reduces network requests on app restart
- Tiles for frequently visited areas persist
- System can clear cache when storage is low

### Cache Promotion

When a tile is found in disk cache, it's promoted back to memory cache:

```kotlin
val diskBitmap = diskCache?.get(tile)
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
    zoom.toInt(),
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

### Minimal Invalidations

The view only invalidates when necessary:

1. **User interactions:** Pan, zoom, marker changes
2. **Tile downloads complete:** Only for visible (high-priority) tiles
3. **Explicit API calls:** setCenter(), setZoom(), etc.

Prefetch downloads don't trigger redraws, reducing rendering overhead.

### Efficient Canvas Operations

- Tiles are drawn directly with `canvas.drawBitmap()` without intermediate transformations
- Placeholder rectangles use simple fill and stroke operations
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

All requests include a user-agent header as required by OSM tile usage policy:

```kotlin
header("User-Agent", "OpenMapView/0.11.0 (https://github.com/afarber/OpenMapView)")
```

### Coroutine-Based Downloads

Tile downloads use Kotlin coroutines for efficient async I/O:

```kotlin
scope.launch(Dispatchers.IO) {
    val bitmap = tileDownloader.downloadTile(url)
    // ...
}
```

**Benefits:**
- Non-blocking tile downloads
- Efficient thread pool management
- Easy cancellation on view destruction

## Memory Leak Prevention

### Lifecycle Management

MapController properly cleans up resources on destruction:

```kotlin
fun onDestroy() {
    scope.cancel()              // Cancel all coroutines
    tileDownloader.close()       // Close HTTP client
    tileCache.close()            // Close disk cache
    MarkerIconFactory.clearCache() // Clear marker icon cache
}
```

This is called from `OpenMapView.onDestroy(owner: LifecycleOwner)` lifecycle callback.

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

### Cache Hit Rates

Expected cache hit rates with typical usage:

- **Memory cache:** 80-90% for panning within same area
- **Disk cache:** 60-70% on app restart
- **Network downloads:** 10-20% for new areas

### Network Usage

With prefetching enabled:

- **Initial view:** ~12 tile downloads (~400KB)
- **Pan gesture:** 0-4 tile downloads (~0-150KB) from cache
- **Zoom change:** 0-20 tile downloads (~0-700KB) depending on zoom delta


## Benchmarking

To measure performance in your app:

```kotlin
// Memory usage
val memoryInfo = ActivityManager.MemoryInfo()
activityManager.getMemoryInfo(memoryInfo)

// Cache statistics
val cacheSize = tileCache.size()
val cacheHits = tileCache.hitCount()
val cacheMisses = tileCache.missCount()

// Rendering performance
Choreographer.getInstance().postFrameCallback { frameTimeNanos ->
    // Measure frame time
}
```

## Best Practices

For optimal performance:

1. **Lifecycle integration:** Register OpenMapView with lifecycle observer (see [Lifecycle Management](LIFECYCLE.md))
2. **Memory limits:** Monitor memory usage if displaying multiple maps simultaneously
3. **Cache clearing:** Provide UI to clear cache if storage becomes an issue
4. **Network awareness:** Consider pausing downloads on metered connections
5. **Error handling:** Handle network failures gracefully

## References

- [Android Bitmap Best Practices](https://developer.android.com/topic/performance/graphics)
- [OSM Tile Usage Policy](https://operations.osmfoundation.org/policies/tiles/)
- [DiskLruCache](https://github.com/JakeWharton/DiskLruCache)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
