# Instrumented Testing

[Back to README](../README.md)

This document explains instrumented testing for OpenMapView, including setup and the existing test suite.

## Overview

**Instrumented tests** (also called **instrumentation tests** or **on-device tests**) run on an Android emulator or physical device. They provide access to real Android framework APIs and hardware.

**Current Status:** OpenMapView has **9 instrumented tests** (2 for TileDownloader, 7 for MapController) that test real rendering, network operations, and Canvas drawing. Unit tests with Robolectric (72 tests) cover logic and calculations.

## Unit Tests vs Instrumented Tests

| Aspect | Unit Tests (JVM) | Instrumented Tests (Android) |
|--------|------------------|------------------------------|
| **Location** | `src/test/kotlin/` | `src/androidTest/kotlin/` |
| **Runs on** | Local JVM | Emulator or device |
| **Speed** | Fast (milliseconds) | Slow (seconds to minutes) |
| **Android APIs** | Mocked (Robolectric) | Real Android framework |
| **Hardware access** | No | Yes (camera, GPS, sensors) |
| **Use for** | Logic, calculations, caching | UI, rendering, integration |

## When to Use Instrumented Tests

Add instrumented tests when:

- **Testing real rendering**: Verify actual bitmap output, canvas drawing
- **Testing touch gestures**: Complex multi-touch interactions
- **Testing hardware integration**: GPS, sensors, device-specific behavior
- **Testing UI components**: Views, animations, layouts
- **Integration testing**: Multiple components working together on real Android
- **Performance testing**: Frame rate, memory usage on actual devices

## When Unit Tests Are Sufficient

Continue using unit tests (with Robolectric) for:

- **Pure logic**: Projection math, coordinate calculations
- **Data classes**: Marker, LatLng, TileCoordinate
- **Caching logic**: TileCache operations
- **Algorithm verification**: Viewport calculations

## Adding Instrumented Tests

### 1. Create Test Directory

```bash
mkdir -p openmapview/src/androidTest/kotlin/de/afarber/openmapview
```

### 2. Dependencies (Already Configured)

The project has instrumented testing dependencies configured in `openmapview/build.gradle.kts`:

```kotlin
dependencies {
    // Instrumentation testing
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
```

### 3. Current Instrumented Tests

The project has instrumented tests in `openmapview/src/androidTest/kotlin/de/afarber/openmapview/`:

**TileDownloaderInstrumentationTest.kt** (2 tests):
```kotlin
@RunWith(AndroidJUnit4::class)
class TileDownloaderInstrumentationTest {
    @Test
    fun testDownloadRealOsmTile() = runTest {
        val downloader = TileDownloader()
        val tileUrl = TileSource.STANDARD.getTileUrl(TileCoordinate(x = 0, y = 0, zoom = 0))
        val result = downloader.downloadTile(tileUrl)

        assertNotNull("Should successfully download a real OSM tile", result)
        result?.let {
            assert(it.width > 0) { "Downloaded bitmap should have width > 0" }
            assert(it.height > 0) { "Downloaded bitmap should have height > 0" }
        }
        downloader.close()
    }
}
```

**MapControllerInstrumentationTest.kt** (7 tests):
```kotlin
@RunWith(AndroidJUnit4::class)
class MapControllerInstrumentationTest {
    private lateinit var controller: MapController

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        controller = MapController(context)
        controller.setViewSize(1080, 1920)
    }

    @Test
    fun testDraw_WithRealCanvas() {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)
        controller.draw(canvas)

        assertNotNull(bitmap)
        assertTrue(bitmap.width == 1080)
        assertTrue(bitmap.height == 1920)
    }

    @Test
    fun testDraw_WithMarkers() {
        val bitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        controller.setCenter(LatLng(51.4661, 7.2491))
        controller.setZoom(14.0)
        controller.addMarker(Marker(LatLng(51.4661, 7.2491)))
        controller.draw(canvas)

        assertNotNull(bitmap)
    }
}
```

## Running Instrumented Tests

### Prerequisites

- Android emulator running or device connected
- Enable USB debugging on physical device

### Commands

```bash
# Run all instrumented tests
./gradlew :openmapview:connectedAndroidTest

# Run on specific device
./gradlew :openmapview:connectedDebugAndroidTest

# Run with ADB
adb shell am instrument -w de.afarber.openmapview.test/androidx.test.runner.AndroidJUnitRunner
```

### Verify Device Connection

```bash
adb devices
# Should show connected emulator or device
```

## Test Categories for OpenMapView

### Current Instrumented Tests

1. **Rendering Tests** (Implemented)
   - Real Canvas drawing with actual Bitmap
   - Marker rendering with real Android graphics
   - Zoom and pan rendering validation

2. **Network Tests** (Implemented)
   - Real OSM tile downloads
   - Network error handling

3. **Lifecycle Tests** (Implemented)
   - MapController lifecycle integration

### Future Instrumented Tests

1. **Touch Gesture Tests**
   - Pan gesture recognition
   - Pinch-to-zoom gesture
   - Double-tap zoom

2. **Performance Tests**
   - Measure frame rate during panning
   - Memory usage with many markers
   - Tile cache eviction behavior

### Keep as Unit Tests

1. **Projection Math**
   - `latLngToPixel()`, `pixelToLatLng()`
   - `latLngToTile()`, `tileToPixel()`
   - Coordinate transformations

2. **Data Structures**
   - Marker equality and hashing
   - TileCoordinate validation
   - LatLng bounds checking

3. **Cache Logic**
   - TileCache put/get operations
   - LRU eviction algorithm

## CI Integration

Instrumented tests require an emulator or device, making CI setup more complex.

### GitHub Actions Example

```yaml
name: Instrumented Tests

on: [push, pull_request]

jobs:
  instrumented-test:
    runs-on: macos-latest  # macOS for hardware acceleration
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - name: Run instrumented tests
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 29
          target: default
          arch: x86_64
          script: ./gradlew :openmapview:connectedCheck
```

**Note:** Instrumented tests on CI are significantly slower and may require paid runners.

## Test Structure

Current instrumented test structure:

```
openmapview/src/androidTest/kotlin/de/afarber/openmapview/
├── TileDownloaderInstrumentationTest.kt    # Real OSM tile downloads (2 tests)
└── MapControllerInstrumentationTest.kt     # Canvas rendering and markers (7 tests)
```

Total: 9 instrumented tests covering real Android framework behavior.

## Espresso UI Testing

For testing UI interactions, use Espresso:

```kotlin
@RunWith(AndroidJUnit4::class)
class MapInteractionTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testMapPanning() {
        onView(withId(R.id.openMapView))
            .perform(swipeLeft())
            .check(matches(isDisplayed()))
    }
}
```

## Best Practices

1. **Keep instrumented tests minimal** - They are slower and more resource-intensive
2. **Test what Robolectric cannot** - Real rendering, hardware, complex UI
3. **Use test flavors** - Separate test APKs from production code
4. **Mock network calls** - Use MockWebServer for tile downloading tests
5. **Clean up resources** - Close HTTP clients, clear caches after tests

## Trade-offs

| Approach | Pros | Cons |
|----------|------|------|
| **Unit + Robolectric** | Fast, no device needed, works in CI | Some Android APIs not fully supported |
| **Instrumented** | Real Android, hardware access, accurate | Slow, requires device, CI complexity |
| **Hybrid** | Best of both worlds | More test code to maintain |

**Current OpenMapView approach:** Hybrid approach with 72 unit tests (Robolectric) for logic and 9 instrumented tests for real Android framework validation.

## Expanding Instrumented Tests

Consider adding more instrumented tests when:

- Users report device-specific rendering issues
- Adding complex gesture handling (currently tested via unit tests)
- Implementing hardware-dependent features (GPS, sensors)
- Validating performance on low-end devices
- Testing pixel-perfect rendering accuracy

## References

- [Android Testing Documentation](https://developer.android.com/training/testing/instrumented-tests)
- [Espresso Testing Framework](https://developer.android.com/training/testing/espresso)
- [AndroidX Test Library](https://developer.android.com/training/testing/set-up-project)
- [Android Emulator Runner (CI)](https://github.com/ReactiveCircus/android-emulator-runner)
