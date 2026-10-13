# Unit Testing

[Back to README](../README.md)

This document explains the unit testing setup for OpenMapView, including the use of Robolectric for testing Android framework classes.

## Overview

OpenMapView uses **JVM unit tests** with Robolectric to test Android-specific code without requiring an emulator or physical device. All unit tests are located in:

```
openmapview/src/test/kotlin/de/afarber/openmapview/
```

## Test Framework Stack

| Component | Purpose |
|-----------|---------|
| **JUnit 4** | Test runner and assertions |
| **Kotlin Test** | Kotlin-specific test utilities |
| **MockK** | Mocking framework for Kotlin |
| **Robolectric** | Android framework shadow implementations |
| **Ktor Client Mock** | HTTP client mocking for network tests |
| **Coroutines Test** | Testing coroutines and async code |

The versions are in `gradle/libs.versions.toml` and kept up to date by Dependabot.

## Running Tests

### Run all unit tests
```bash
./gradlew :openmapview:test
```

### Run tests for specific build variant
```bash
./gradlew :openmapview:testDebugUnitTest
./gradlew :openmapview:testReleaseUnitTest
```

### Run a specific test class
```bash
./gradlew :openmapview:testDebugUnitTest --tests "*ProjectionTest*"
```

### Run with detailed output
```bash
./gradlew :openmapview:test --continue
```

The `--continue` flag ensures all tests run even if some fail, useful for getting a complete test report.

## Test Structure

### Current Test Coverage (461 tests across 35 test classes)

| Test Class | Tests | Description |
|------------|-------|-------------|
| **AttributionOverlayTest** | 4 | Attribution rendering and touch detection |
| **BitmapDescriptorFactoryTest** | 15 | Marker icon generation with colors and custom images |
| **CameraPositionTest** | 7 | Camera position validation and equality |
| **CameraUpdateFactoryTest** | 11 | Camera update creation (zoom, pan, position) |
| **CircleTest** | 4 | Circle defaults, radius validation, identity equality |
| **DiskTileCacheTest** | 8 | Persistent disk cache with DiskLruCache, per-source namespaces |
| **GeoJsonParserTest** | 14 | GeoJSON parsing (Point, LineString, Polygon, Multi*, Features) |
| **GeodesicUtilsTest** | 17 | Great-circle interpolation for geodesic shapes |
| **GroundOverlayTest** | 10 | Ground overlay position/bounds validation and equality |
| **IntentUtilsTest** | 7 | External app integration (geo: URI, OSM fallback) |
| **LatLngBoundsTest** | 13 | Lat/lng bounds calculations and contains checks |
| **LatLngTest** | 11 | Latitude clamping and longitude wrapping |
| **MapControllerOverlaysTest** | 13 | Circles, ground and tile overlays, their touch detection, info windows, drawing all shape types |
| **MapControllerTest** | 103 | Zoom, pan, marker/polyline/polygon management, touch detection, padding, camera bounds, edge effects |
| **MapFeaturesTest** | 5 | Feature query methods (traffic, indoor, buildings, myLocation) |
| **MapProjectionTest** | 8 | Projection screen/LatLng conversion, pan offset, padding, visible region |
| **MapTypeTest** | 18 | Map type switching and validation |
| **MarkerIconFactoryTest** | 3 | Default marker icon generation and caching |
| **MarkerTest** | 23 | Marker creation, equality, properties, info windows, dragging |
| **OptionsBuildersTest** | 13 | Google-style *Options builders for markers, shapes and overlays |
| **PolygonTest** | 20 | Polygon creation, validation, holes, and styling |
| **PolylineTest** | 31 | Polyline creation, validation, and styling |
| **PredefinedTileProvidersTest** | 2 | URL templates of the predefined overlay tile providers, Stadia key lookup |
| **ProjectionTest** | 12 | Web Mercator projection calculations |
| **SnapshotTest** | 8 | Map snapshot API and callbacks |
| **TileCacheTest** | 9 | Memory LRU cache behavior, clearing, namespaces, disk writes |
| **TileDownloaderTest** | 5 | HTTP tile downloading with mocked Ktor client, HTTP errors, cancellation |
| **TileOverlayTest** | 5 | Tile overlay defaults, transparency validation, copy helpers |
| **TileRetryTrackerTest** | 4 | Exponential backoff for failed tiles |
| **TileSourceTest** | 9 | Tile source URL generation, attribution and API keys |
| **TileTest** | 6 | Tile content equality, validation and toString |
| **UiSettingsTest** | 25 | UI settings (gestures, zoom controls, not-implemented features) |
| **UrlTileProviderTest** | 4 | Overlay tile downloading, HTTP 404 handling, shared HTTP client |
| **ViewportCalculatorTest** | 10 | Visible tile calculation |
| **VisibleRegionTest** | 4 | Visible region bounds and corners |

### Example Test

```kotlin
@RunWith(RobolectricTestRunner::class)
class BitmapDescriptorFactoryTest {
    @Test
    fun testDefaultMarker_Red() {
        val bitmap = BitmapDescriptorFactory.defaultMarker()
        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(72, bitmap.height)
    }
}
```

## Robolectric: Testing Android Framework Classes

### Why Robolectric?

Android framework classes like `Bitmap`, `Canvas`, and `Paint` return `null` or throw exceptions in standard JVM unit tests. Robolectric provides "shadow" implementations that simulate real Android behavior.

**Without Robolectric:**
```kotlin
val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
// Returns: null (Android framework not available in JVM)
```

**With Robolectric:**
```kotlin
@RunWith(RobolectricTestRunner::class)
class MyTest {
    @Test
    fun testBitmap() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertNotNull(bitmap)  // Works!
        assertEquals(100, bitmap.width)
    }
}
```

### When to Use Robolectric

Use the `@RunWith(RobolectricTestRunner::class)` annotation when testing:

- Bitmap operations (`BitmapDescriptorFactoryTest`, `TileCacheTest`, `TileDownloaderTest`)
- Canvas drawing (`MarkerIconFactory`, `MapControllerTest`)
- View classes (if testing custom views)
- Android API calls (Context, Resources, etc.)

Do not use Robolectric for:

- Pure Kotlin logic (`ProjectionTest` - math calculations)
- Data classes (`Marker`, `LatLng`, `TileCoordinate`)
- Business logic without Android dependencies

### Testing Async Code with Coroutines

Use `kotlinx-coroutines-test` for testing suspend functions:

```kotlin
@Test
fun testAsyncOperation() = runTest {
    val result = myRepository.fetchData()
    assertNotNull(result)
}
```

The `runTest` function creates a test coroutine scope that automatically advances time.

### Configuration

Robolectric is configured in `openmapview/build.gradle.kts`:

```kotlin
android {
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation(libs.robolectric)
}
```

**Key settings:**
- `isReturnDefaultValues = true` - Android methods return default values instead of throwing exceptions
- `isIncludeAndroidResources = true` - Makes Android resources available to tests

## MockK and Ktor MockEngine: Mocking Frameworks

### MockK for Application Classes

MockK is used for mocking Kotlin classes and interfaces. While Robolectric handles Android framework classes, MockK is used for application-level mocking.

**Current Usage:**

```kotlin
@Test
fun testDraw_ValidViewport() {
    val canvas = mockk<Canvas>(relaxed = true)
    controller.draw(canvas)
    verify(atLeast = 1) { canvas.drawRect(any(), any(), any(), any(), any()) }
}
```

### Ktor MockEngine for HTTP Tests

Use `io.ktor:ktor-client-mock` to test network code without making real HTTP calls:

```kotlin
@Test
fun testDownloadTile_Success() = runTest {
    val mockBitmapBytes = createMockPngBytes()
    val mockEngine = MockEngine { request ->
        respond(
            content = ByteReadChannel(mockBitmapBytes),
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "image/png")
        )
    }

    val client = HttpClient(mockEngine)
    // Test with mocked client
}
```

### Comparison Table

| Use Case | Tool | Example |
|----------|------|---------|
| Mock `Bitmap` operations | Robolectric (preferred) | Use real `Bitmap.createBitmap()` |
| Mock `Canvas` drawing | MockK | `mockk<Canvas>(relaxed = true)` |
| Mock HTTP requests | Ktor MockEngine | `MockEngine { respond(...) }` |
| Mock `UserRepository` | MockK | `mockk<UserRepository>()` |
| Test projection math | Neither | Pure unit tests |

## Test Reports

Test reports are generated in:
```
openmapview/build/reports/tests/testDebugUnitTest/index.html
openmapview/build/reports/tests/testReleaseUnitTest/index.html
```

Open these HTML files in a browser to see:
- Test success/failure counts
- Execution time per test
- Detailed failure messages with stack traces

## CI Integration

Unit tests run automatically on every push via GitHub Actions (`.github/workflows/_test.yml`):

```yaml
- name: Run unit tests
  run: ./gradlew :openmapview:test --continue

- name: Upload test results
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: test-results
    path: '**/build/test-results/test*UnitTest/*.xml'
```

Test results and reports are uploaded as artifacts and retained for 30 days.

## Writing New Tests

### 1. Standard Unit Test (No Android Dependencies)

```kotlin
class MyUtilTest {
    @Test
    fun testCalculation() {
        val result = MyUtil.calculate(5, 10)
        assertEquals(15, result)
    }
}
```

### 2. Test with Android Framework (Robolectric)

```kotlin
@RunWith(RobolectricTestRunner::class)
class MyBitmapTest {
    @Test
    fun testBitmapCreation() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertNotNull(bitmap)
        assertEquals(100, bitmap.width)
    }
}
```

### 3. Test with Mocking (MockK)

```kotlin
class MyViewModelTest {
    @Test
    fun testLoadData() {
        val mockRepo = mockk<DataRepository>()
        every { mockRepo.getData() } returns listOf("a", "b", "c")

        val viewModel = MyViewModel(mockRepo)
        viewModel.loadData()

        verify(exactly = 1) { mockRepo.getData() }
    }
}
```

## Common Test Patterns

### Testing Projection Math

```kotlin
@Test
fun testLatLngToPixel_Equator() {
    val (x, y) = Projection.latLngToPixel(LatLng(0.0, 0.0), 0)
    assertEquals(128.0, x, 0.0001)
    assertEquals(128.0, y, 0.0001)
}
```

### Testing Bitmap Operations

```kotlin
@RunWith(RobolectricTestRunner::class)
class TileCacheTest {
    @Test
    fun testPutAndGet() {
        val cache = TileCache()
        val tile = TileCoordinate(1, 2, 3)
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)

        cache.put(tile, bitmap)
        val result = cache.get(tile)

        assertNotNull(result)
        assertEquals(bitmap, result)
    }
}
```

### Testing Data Classes

```kotlin
@Test
fun testMarkerCreation() {
    val position = LatLng(51.4661, 7.2491)
    val marker = Marker(position = position, title = "Test")

    assertEquals(position, marker.position)
    assertEquals("Test", marker.title)
    assertNotNull(marker.id)
}
```

## Troubleshooting

### Issue: `NullPointerException` when creating Bitmap

**Problem:** Test does not use Robolectric
```kotlin
class MyTest {  // Missing @RunWith annotation
    @Test
    fun test() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        // NPE: bitmap is null
    }
}
```

**Solution:** Add Robolectric runner
```kotlin
@RunWith(RobolectricTestRunner::class)
class MyTest {
    @Test
    fun test() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        assertNotNull(bitmap)  // Works!
    }
}
```

### Issue: Tests slow to run

Robolectric tests are slower than pure JVM tests because they initialize Android framework shadows. Tips:

- Use Robolectric only when necessary
- Keep pure logic tests separate (faster execution)
- Use `@Config` to customize Robolectric behavior if needed

### Issue: Tests pass locally but fail in CI

Check Robolectric version compatibility with CI's JDK version. The project uses:
- Robolectric 4.14
- JDK 17
- Both are compatible

## Test Coverage

OpenMapView measures test coverage with the Android Gradle Plugin's built-in JaCoCo support. For coverage requirements and thresholds, see the [Contributing Guide](CONTRIBUTING.md#test-coverage).

### Generating Coverage Reports

Run tests and generate coverage:

```bash
./gradlew :openmapview:createDebugUnitTestCoverageReport
```

View HTML coverage report:

```
openmapview/build/reports/coverage/test/debug/index.html
```

Check coverage meets minimum threshold:

```bash
./scripts/check-coverage.sh
```

### Coverage Configuration

Coverage is enabled with `enableUnitTestCoverage = true` for the debug build type in `openmapview/build.gradle.kts`. The only extra setting is `isIncludeNoLocationClasses = true` on the test tasks, so classes loaded by Robolectric's instrumenting class loader are counted.

`./scripts/check-coverage.sh` deletes the previous report, runs the tests and fails if they fail, so it never judges a stale report.

### Current Coverage Areas

The test suite currently covers:
- Core projection math (Web Mercator)
- Tile coordinate calculations
- Marker API and bitmap generation
- Polyline and polygon data classes with validation
- Vector shape management (add, remove, clear operations)
- Memory cache (LRU) behavior
- Disk cache (persistent storage)
- Viewport calculation
- MapController rendering logic for all shape types, ground and tile overlays
- Options builders and the public Projection class
- Touch gesture handling (marker and attribution hit detection)
- Zoom level validation and bounds
- Network tile downloading (with mocking)
- Pan offset calculations
- Attribution overlay rendering and interaction

### Coverage in CI

The `.github/workflows/_coverage.yml` workflow:
1. Runs all unit tests and generates the coverage report (`./scripts/check-coverage.sh`)
2. Checks coverage meets minimum threshold
3. Writes a coverage table (instructions, lines, branches) to the job summary
4. Uploads HTML and XML reports as artifacts

Coverage results are available:
- In the job summary of the "Check Test Coverage" job
- As CI artifacts (HTML report)
- In PR status checks (pass/fail)

See [Contributing Guide](CONTRIBUTING.md#test-coverage) for coverage requirements when submitting PRs.

## References

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Robolectric Documentation](http://robolectric.org/)
- [MockK Documentation](https://mockk.io/)
- [Android Testing Guide](https://developer.android.com/training/testing)
