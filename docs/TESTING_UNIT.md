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

### Current Test Coverage (43 tests)

| Test Class | Tests | Description |
|------------|-------|-------------|
| **BitmapDescriptorFactoryTest** | 7 | Marker icon generation with colors |
| **MarkerTest** | 8 | Marker creation, equality, and properties |
| **ProjectionTest** | 12 | Web Mercator projection calculations |
| **TileCacheTest** | 6 | LRU bitmap caching |
| **ViewportCalculatorTest** | 10 | Visible tile calculation |

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

- Bitmap operations (`BitmapDescriptorFactoryTest`, `TileCacheTest`)
- Canvas drawing (`MarkerIconFactory`)
- View classes (if testing custom views)
- Android API calls (Context, Resources, etc.)

Do not use Robolectric for:

- Pure Kotlin logic (`ProjectionTest` - math calculations)
- Data classes (`Marker`, `LatLng`, `TileCoordinate`)
- Business logic without Android dependencies

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

## MockK: Mocking Framework

MockK is used for mocking Kotlin classes and interfaces. While Robolectric handles Android framework classes, MockK is used for application-level mocking.

### Current Usage

The project uses MockK minimally since Robolectric provides real Android implementations. MockK is available for mocking dependencies like:

```kotlin
// Example: Mocking a repository
val mockRepository = mockk<TileRepository>()
every { mockRepository.getTile(any()) } returns mockBitmap
```

### MockK vs Robolectric

| Use Case | Tool | Example |
|----------|------|---------|
| Mock `Bitmap` operations | Robolectric (preferred) | Use real `Bitmap.createBitmap()` |
| Mock `UserRepository` | MockK | Use `mockk<UserRepository>()` |
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

## Test Coverage Goals

Current coverage focuses on:
- Core projection math (Web Mercator)
- Tile coordinate calculations
- Marker API and bitmap generation
- Tile caching logic
- Viewport calculation

Future coverage should include:
- MapController rendering logic
- Touch gesture handling
- Zoom level validation
- Network tile downloading (with mocking)
- Error handling and edge cases

## References

- [JUnit 4 Documentation](https://junit.org/junit4/)
- [Robolectric Documentation](http://robolectric.org/)
- [MockK Documentation](https://mockk.io/)
- [Android Testing Guide](https://developer.android.com/training/testing)
