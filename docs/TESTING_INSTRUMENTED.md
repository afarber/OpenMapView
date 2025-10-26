# Instrumented Testing

[Back to README](../README.md)

This document explains instrumented testing for OpenMapView, including setup and the existing test suite.

## Overview

**Instrumented tests** run on an Android emulator or physical device. They provide access to real Android framework APIs and hardware.

**Current Status:** OpenMapView has **9 instrumented tests** (2 for TileDownloader, 7 for MapController) that test real rendering, network operations, and Canvas drawing. Unit tests with Robolectric (72 tests) cover logic and calculations.

**CI Integration:** Instrumented tests run automatically daily at 09:45 UTC on GitHub Actions using the free Ubuntu runner with Android emulator.

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

Use instrumented tests for:

- **Real rendering**: Verify actual bitmap output, canvas drawing
- **Touch gestures**: Complex multi-touch interactions
- **Hardware integration**: GPS, sensors, device-specific behavior
- **UI components**: Views, animations, layouts
- **Integration testing**: Multiple components working together on real Android

## When to Use Unit Tests

Use unit tests (with Robolectric) for:

- **Pure logic**: Projection math, coordinate calculations
- **Data classes**: Marker, LatLng, TileCoordinate
- **Caching logic**: TileCache operations
- **Algorithm verification**: Viewport calculations

## Current Instrumented Tests

The project has instrumented tests in `openmapview/src/androidTest/kotlin/de/afarber/openmapview/`:

- **TileDownloaderInstrumentationTest.kt** (2 tests) - Real OSM tile downloads with network
- **MapControllerInstrumentationTest.kt** (7 tests) - Real Canvas rendering, marker drawing, lifecycle

Total: 9 instrumented tests covering real Android framework behavior.

## Running Instrumented Tests

### Local Testing

Start an Android emulator or connect a physical device, then run:

```bash
# Run all instrumented tests
./gradlew :openmapview:connectedAndroidTest

# Verify device connection
adb devices
```

### CI Testing

Instrumented tests run automatically on GitHub Actions:

- **Schedule**: Daily at 09:45 UTC
- **Trigger**: `.github/workflows/daily.yml`
- **Runner**: Ubuntu latest (free tier)
- **Devices**: Phone (Nexus 6) and Automotive (1024p landscape)
- **Emulator**: Android API 29, x86_64
- **Acceleration**: KVM hardware acceleration enabled
- **Manual trigger**: Available via GitHub Actions UI (workflow_dispatch)

Test results and reports are uploaded as separate artifacts for each device with 30-day retention.

## Test Structure

```
openmapview/src/androidTest/kotlin/de/afarber/openmapview/
├── TileDownloaderInstrumentationTest.kt    # Real OSM tile downloads (2 tests)
└── MapControllerInstrumentationTest.kt     # Canvas rendering and markers (7 tests)
```

## Dependencies

Instrumented testing dependencies in `openmapview/build.gradle.kts`:

```kotlin
dependencies {
    androidTestImplementation("androidx.test:core-ktx:1.6.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}
```

## References

- [Android Testing Documentation](https://developer.android.com/training/testing/instrumented-tests)
- [AndroidX Test Library](https://developer.android.com/training/testing/set-up-project)
- [Android Emulator Runner](https://github.com/ReactiveCircus/android-emulator-runner)
