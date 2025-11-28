# Contributing to OpenMapView

[Back to README](../README.md)

This guide outlines the development workflow and requirements for contributing to OpenMapView.

## Code Quality Requirements

All contributions must meet the following requirements before being merged:

### 1. Code Formatting

All Kotlin code must follow the project's formatting rules enforced by Spotless with ktlint 1.3.1.

**Before committing**, run:

```bash
./gradlew spotlessApply
```

This automatically formats all code according to the project's style rules:
- 4 spaces indentation
- No trailing whitespace
- Files end with newline
- Consistent ktlint formatting

**To check formatting** without applying changes:

```bash
./gradlew spotlessCheck
```

The CI pipeline will fail if code is not properly formatted.

### 2. Copyright Headers

All `.kt` files must include the MIT license header at the top:

```kotlin
/*
 * Copyright (c) 2025 Alexander Farber
 * SPDX-License-Identifier: MIT
 *
 * This file is part of the OpenMapView project (https://github.com/afarber/OpenMapView)
 */
```

Running `./gradlew spotlessApply` automatically adds this header to any files missing it.

**Why copyright headers matter:** Including copyright headers on all source files helps avoid potential legal issues and keeps this library clean to use for both personal and commercial applications. The MIT license identifier makes licensing terms explicit and unambiguous.

The CI pipeline includes a copyright check that will fail if any `.kt` files are missing the required header.

### 3. Dependency Management

Before adding new dependencies to OpenMapView, verify the following:

1. **License Compatibility**: Only add dependencies with permissive licenses
   - Preferred: Apache 2.0, MIT, BSD
   - Prohibited: GPL, AGPL (would affect library users)
   - Consult before adding: LGPL, EPL, MPL

2. **Verify License**: Run the license report to check all dependencies
   ```bash
   ./gradlew generateLicenseReport
   ```
   View the report at: `openmapview/build/reports/licenses/licenses.html`

3. **Minimal Footprint**: Keep dependencies minimal
   - OpenMapView currently has only 5 runtime dependencies
   - New dependencies must provide essential functionality
   - Avoid bloat - see [ARCHITECTURE.md](ARCHITECTURE.md) for philosophy

4. **Auto-Publishing**: License reports automatically publish to the documentation site
   - URL: https://afarber.github.io/OpenMapView/licenses/
   - Updated on every docs deployment
   - No manual maintenance needed

**Current Dependencies** (all Apache 2.0 or MIT):
- androidx.core:core-ktx
- androidx.lifecycle:lifecycle-runtime-ktx
- androidx.compose.ui:ui-graphics
- io.ktor:ktor-client-android
- com.jakewharton:disklrucache

## Automated Git Hooks (Recommended)

Git hooks can automatically check formatting and copyright headers before commits, preventing CI failures.

### Setup Pre-commit Hook

From the repository root, run:

```bash
./scripts/setup-git-hooks.sh
```

This installs a pre-commit hook that automatically:
1. Checks code formatting with `./scripts/check-format.sh`
2. Checks copyright headers with `./scripts/check-copyright.sh`
3. Blocks the commit if any issues are found
4. Provides instructions to run `./gradlew spotlessApply` to fix issues

### What Happens on Commit

When committing changes, the hook runs:

```
Running pre-commit checks...

1. Checking code formatting...
   Code formatting: OK
2. Checking copyright headers...
   Copyright headers: OK

All pre-commit checks passed!
```

If issues are found, the commit is blocked and instructions are displayed.

**Note:** Git hooks are local and not tracked in the repository. Each developer must run the setup script after cloning.

## Testing Requirements

### Unit Tests

All new features and bug fixes should include unit tests. See [Unit Testing Guide](TESTING_UNIT.md) for detailed information on writing tests with Robolectric.

Run unit tests:

```bash
./gradlew :openmapview:test
```

### Test Coverage

The project enforces a **minimum test coverage of 20%** for all code. Coverage is measured using JaCoCo.

Generate coverage report:

```bash
./gradlew :openmapview:jacocoTestReport
```

View the HTML coverage report at:
```
openmapview/build/reports/jacoco/jacocoTestReport/html/index.html
```

Check coverage meets minimum threshold:

```bash
./scripts/check-coverage.sh
```

The CI pipeline will fail if coverage drops below the minimum threshold. Focus on testing:
- Core business logic
- Public API methods
- Edge cases and error handling

Coverage excludes:
- Generated code (R.class, BuildConfig)
- Test files
- Android framework classes

### Instrumentation Tests

For features requiring real Android framework APIs or rendering, add instrumentation tests.

Run instrumentation tests (requires emulator or device):

```bash
./gradlew :openmapview:connectedAndroidTest
```

## Pull Request Process

1. Fork the repository
2. Create a feature branch from `main`
3. Make changes following the code quality requirements
4. Run `./gradlew spotlessApply` before committing
5. Ensure all tests pass locally
6. Push changes and create a pull request
7. CI must pass (formatting, tests, builds)
8. Address any review feedback

## CI Pipeline

The CI pipeline runs automatically on all pull requests and includes:

1. **Format Check** - Verifies Spotless formatting
2. **Copyright Check** - Verifies MIT license headers
3. **Unit Tests** - Runs all JVM unit tests
4. **Test Coverage** - Ensures minimum 20% code coverage
5. **Build Library** - Builds the OpenMapView AAR
6. **Build Examples** - Builds all example applications

All checks must pass before merging.

## Build Commands

```bash
# Format code
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck

# Run unit tests
./gradlew :openmapview:test

# Generate coverage report
./gradlew :openmapview:jacocoTestReport

# Check coverage threshold
./scripts/check-coverage.sh

# Build library
./gradlew :openmapview:assembleRelease

# Build all examples
./gradlew assembleDebug

# Run instrumentation tests
./gradlew :openmapview:connectedAndroidTest
```

## Questions or Issues?

For questions about contributing, open an issue on GitHub: https://github.com/afarber/OpenMapView/issues

