# Instrumented Testing

[Back to README](../README.md)

This document explains instrumented testing for OpenMapView. Currently, the project does not have instrumented tests, but this guide describes when and how to add them.

## Overview

**Instrumented tests** (also called **instrumentation tests** or **on-device tests**) run on an Android emulator or physical device. They provide access to real Android framework APIs and hardware.

**Current Status:** OpenMapView has **no instrumented tests** yet. All testing is done via [unit tests with Robolectric](TESTING_UNIT.md).

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

### 2. Add Dependencies

Update `openmapview/build.gradle.kts`:

```kotlin
dependencies {
    // Existing dependencies...

    // Instrumented testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
}
```

### 3. Example Instrumented Test

Create `openmapview/src/androidTest/kotlin/de/afarber/openmapview/OpenMapViewInstrumentedTest.kt`:

```kotlin
package de.afarber.openmapview

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenMapViewInstrumentedTest {
    @Test
    fun testOpenMapViewCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mapView = OpenMapView(context)

        assertNotNull(mapView)
        assertEquals(0, mapView.childCount) // FrameLayout with no children initially
    }

    @Test
    fun testBitmapRendering() {
        // Test actual bitmap rendering with real Android framework
        val bitmap = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)

        assertNotNull(bitmap)
        assertEquals(48, bitmap.width)
        assertEquals(72, bitmap.height)

        // Verify actual pixel colors (not possible with Robolectric)
        val centerPixel = bitmap.getPixel(24, 36)
        // Assert red-ish color (exact value depends on marker design)
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

### Recommended Instrumented Tests

1. **Rendering Tests**
   - Verify tile rendering produces non-null bitmaps
   - Check marker icon pixel colors
   - Test canvas drawing operations

2. **Touch Gesture Tests**
   - Pan gesture recognition
   - Pinch-to-zoom gesture
   - Double-tap zoom

3. **Integration Tests**
   - Full map initialization
   - Tile downloading and caching
   - Marker addition and rendering

4. **Performance Tests**
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

Typical instrumented test structure:

```
openmapview/src/androidTest/kotlin/de/afarber/openmapview/
├── OpenMapViewTest.kt           # View creation and basic functionality
├── MapControllerRenderTest.kt   # Rendering verification
├── GestureHandlingTest.kt       # Touch gestures
└── MarkerIntegrationTest.kt     # Marker display and interaction
```

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

**Current OpenMapView approach:** Pure unit tests with Robolectric provide sufficient coverage for the current feature set.

## Future Considerations

Add instrumented tests when:

- Users report device-specific rendering issues
- Adding complex gesture handling
- Implementing hardware-dependent features
- Validating performance on low-end devices

## References

- [Android Testing Documentation](https://developer.android.com/training/testing/instrumented-tests)
- [Espresso Testing Framework](https://developer.android.com/training/testing/espresso)
- [AndroidX Test Library](https://developer.android.com/training/testing/set-up-project)
- [Android Emulator Runner (CI)](https://github.com/ReactiveCircus/android-emulator-runner)
