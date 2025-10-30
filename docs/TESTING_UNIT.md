# Unit Testing

[Back to README](../README.md)

This document explains the unit testing setup for OpenMapView, including the use of Robolectric for testing Android framework classes.

## Overview

OpenMapView uses **JVM unit tests** with Robolectric to test Android-specific code without requiring an emulator or physical device. All unit tests are located in:

```
openmapview/src/test/kotlin/de/afarber/openmapview/
```

## Test Framework Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| **JUnit** | 4.13.2 | Test runner and assertions |
| **Kotlin Test** | (inherited) | Kotlin-specific test utilities |
| **MockK** | 1.13.8 | Mocking framework for Kotlin |
| **Robolectric** | 4.14 | Android framework shadow implementations |
| **Ktor Client Mock** | 2.3.7 | HTTP client mocking for network tests |
| **Coroutines Test** | 1.9.0 | Testing coroutines and async code |

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

### Current Test Coverage (260+ tests across 21 test classes)

| Test Class | Tests | Description |
|------------|-------|-------------|
| **AttributionOverlayTest** | 4 | Attribution rendering and touch detection |
| **BitmapDescriptorFactoryTest** | 18 | Marker icon generation with colors and custom images |
| **CameraPositionTest** | 6 | Camera position validation and equality |
| **CameraUpdateFactoryTest** | 8 | Camera update creation (zoom, pan, position) |
| **DiskTileCacheTest** | 4 | Persistent disk cache with DiskLruCache |
| **GeoJsonParserTest** | 14 | GeoJSON parsing (Point, LineString, Polygon, Multi*, Features) |
| **IntentUtilsTest** | 4 | External app integration (geo: URI, OSM fallback) |
| **LatLngBoundsTest** | 13 | Lat/lng bounds calculations and contains checks |
| **MapControllerTest** | 63 | Zoom, pan, marker/polyline/polygon/circle management, touch detection, padding |
| **MapFeaturesTest** | 4 | Feature query methods (traffic, indoor, buildings, myLocation) |
| **MapTypeTest** | 9 | Map type switching and validation |
| **MarkerTest** | 24 | Marker creation, equality, properties, info windows, dragging |
| **PolygonTest** | 16 | Polygon creation, validation, holes, and styling |
| **PolylineTest** | 16 | Polyline creation, validation, and styling |
| **ProjectionTest** | 12 | Web Mercator projection calculations |
| **TileCacheTest** | 6 | Memory LRU cache behavior |
| **TileDownloaderTest** | 2 | HTTP tile downloading with mocked Ktor client |
| **TileSourceTest** | 7 | Tile source URL generation and attribution |
| **UiSettingsTest** | 19 | UI settings (gestures, zoom controls, not-implemented features) |
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
    testImplementation("org.robolectric:robolectric:4.14")
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

OpenMapView uses JaCoCo for test coverage measurement. For coverage requirements and thresholds, see the [Contributing Guide](CONTRIBUTING.md#test-coverage).

### Generating Coverage Reports

Run tests and generate coverage:

```bash
./gradlew :openmapview:testDebugUnitTest jacocoTestReport
```

View HTML coverage report:

```
openmapview/build/reports/jacoco/jacocoTestReport/html/index.html
```

Check coverage meets minimum threshold:

```bash
./scripts/check-coverage.sh
```

### Coverage Configuration

JaCoCo is configured in `openmapview/build.gradle.kts` with exclusions for:

- Generated code (R.class, BuildConfig)
- Test files
- Android framework classes

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
- MapController rendering logic
- Touch gesture handling (marker and attribution hit detection)
- Zoom level validation and bounds
- Network tile downloading (with mocking)
- Pan offset calculations
- Attribution overlay rendering and interaction

### Coverage in CI

The `.github/workflows/_coverage.yml` workflow:
1. Runs all unit tests
2. Generates JaCoCo coverage report
3. Uploads coverage to Codecov
4. Checks coverage meets minimum threshold
5. Uploads HTML and XML reports as artifacts

Coverage results are available:
- On Codecov: https://codecov.io/gh/afarber/OpenMapView
- As CI artifacts (HTML report)
- In PR status checks (pass/fail)

See [Contributing Guide](CONTRIBUTING.md#test-coverage) for coverage requirements when submitting PRs.

## References

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Robolectric Documentation](http://robolectric.org/)
- [MockK Documentation](https://mockk.io/)
- [Android Testing Guide](https://developer.android.com/training/testing)
