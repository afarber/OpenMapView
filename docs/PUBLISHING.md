# Publishing Guide

[Back to README](../README.md)

This guide provides an overview of publishing OpenMapView to public package repositories.

## Overview

OpenMapView is published to two package repositories:

1. **Maven Central** - Primary distribution channel (recommended for production)
2. **JitPack** - Alternative channel with instant publishing and snapshot support

## Quick Comparison

| Feature | Maven Central | JitPack |
|---------|---------------|---------|
| **User Setup** | None (default repository) | Add JitPack repository |
| **Coordinates** | `de.afarber:openmapview:0.1.0` | `com.github.afarber:OpenMapView:0.1.0` |
| **Publishing Speed** | 10-30 minutes | Instant (on-demand build) |
| **Snapshot Support** | No | Yes (branches/commits) |
| **Discoverability** | High | Lower |
| **Best For** | Production releases | Development/testing |
| **Setup Complexity** | Moderate (GPG, tokens) | Minimal (automatic) |

## Maven Central

### Installation (Users)

Users can add the dependency without any additional repository configuration:

```kotlin
dependencies {
    implementation("de.afarber:openmapview:0.1.0")
}
```

### Publishing Process (Maintainers)

1. Create and push a version tag:
   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```

2. GitHub Actions automatically:
   - Runs tests and builds
   - Signs artifacts with GPG
   - Publishes to Maven Central via Central Portal API
   - Creates GitHub Release

3. Artifacts appear on Maven Central within 10-30 minutes

### Configuration

- **Namespace**: `de.afarber` (verified with Sonatype)
- **Publishing Plugin**: `com.gradleup.nmcp` v0.1.2
- **Credentials**: Stored in GitHub Secrets (`OSSRH_USERNAME`, `OSSRH_PASSWORD`)
- **Signing**: GPG key `8334881A009EB69E5B5BDBF189999F05686CE169`

See [MAVEN_CENTRAL_SETUP.md](MAVEN_CENTRAL_SETUP.md) for complete Maven Central configuration details.

## JitPack

### Installation (Users)

Users need to add the JitPack repository:

**settings.gradle.kts:**
```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**Dependency:**
```kotlin
dependencies {
    implementation("com.github.afarber:OpenMapView:0.1.0")
}
```

### Publishing Process (Maintainers)

JitPack automatically builds from GitHub releases - no manual publishing required:

1. Create and push a version tag:
   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```

2. JitPack automatically:
   - Detects the new tag
   - Builds artifacts on first user request
   - Caches for subsequent requests

3. Available immediately via `https://jitpack.io/#afarber/OpenMapView`

### Configuration

- **Repository**: `com.github.afarber:OpenMapView`
- **Build Config**: `jitpack.yml` (specifies Java 17)
- **No Credentials Required**: Builds directly from public GitHub repo

See [JITPACK_SETUP.md](JITPACK_SETUP.md) for complete JitPack configuration details.

## Development Snapshots

### Maven Central
Maven Central does not support snapshot versions. Use JitPack for testing unreleased code.

### JitPack
JitPack supports snapshots from branches and specific commits:

```kotlin
// Latest commit from main branch
implementation("com.github.afarber:OpenMapView:main-SNAPSHOT")

// Specific commit
implementation("com.github.afarber:OpenMapView:abc1234")

// Feature branch
implementation("com.github.afarber:OpenMapView:feature-branch-SNAPSHOT")
```

## Release Checklist

When creating a new release:

- [ ] Ensure all tests pass locally
- [ ] Update version number expectations if needed
- [ ] Run code formatting: `./gradlew spotlessApply`
- [ ] Verify local build: `./gradlew build`
- [ ] Create and push version tag (e.g., `v0.2.0`)
- [ ] Monitor GitHub Actions release workflow
- [ ] Verify Maven Central publication (10-30 minutes)
- [ ] Verify JitPack build status (https://jitpack.io/#afarber/OpenMapView)
- [ ] Update README.md version numbers if needed
- [ ] Announce release (GitHub Releases, social media, etc.)

## Version Numbering

OpenMapView uses Semantic Versioning (SemVer):

- **vMAJOR.MINOR.PATCH** (e.g., `v0.2.0`)
- MAJOR: Breaking API changes
- MINOR: New features, backward compatible
- PATCH: Bug fixes, backward compatible

Examples:
- `v0.1.0` - Initial release
- `v0.2.0` - New features added
- `v0.2.1` - Bug fixes
- `v1.0.0` - First stable release

## Verifying Publications

### Maven Central

- Central Portal: https://central.sonatype.com/artifact/de.afarber/openmapview
- Maven Search: https://search.maven.org/artifact/de.afarber/openmapview
- Direct Repository: https://repo1.maven.org/maven2/de/afarber/openmapview/

### JitPack

- JitPack Page: https://jitpack.io/#afarber/OpenMapView
- Build Logs: Available on JitPack page for each version
- Badge: [![JitPack](https://jitpack.io/v/afarber/OpenMapView.svg)](https://jitpack.io/#afarber/OpenMapView)

## Testing Local Publication

Before pushing a release, test that artifacts can be built locally:

```bash
# Test Maven publication
./gradlew publishToMavenLocal

# Verify artifacts
ls ~/.m2/repository/de/afarber/openmapview/

# Check AAR was created
ls openmapview/build/outputs/aar/
```

## Badges

Add badges to README.md to show publication status:

```markdown
[![Maven Central](https://img.shields.io/maven-central/v/de.afarber/openmapview.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/de.afarber/openmapview)
[![JitPack](https://jitpack.io/v/afarber/OpenMapView.svg)](https://jitpack.io/#afarber/OpenMapView)
```

## Recommendations

### For Production Apps
Use **Maven Central** for stable, production releases:
- Better discoverability
- Standard repository (no extra configuration)
- Preferred by most Android developers
- More professional appearance

### For Development and Testing
Use **JitPack** for:
- Testing unreleased features from branches
- Quick iterations during development
- Projects that need bleeding-edge updates
- Contributing to the library (test your changes)

### For Library Contributors
If contributing a feature:
1. Fork the repository
2. Create a feature branch
3. Test your changes using JitPack snapshots
4. Submit a pull request
5. Changes will be available via Maven Central after the next release

## Troubleshooting

### Maven Central Issues
See [MAVEN_CENTRAL_SETUP.md](MAVEN_CENTRAL_SETUP.md#troubleshooting)

### JitPack Issues
See [JITPACK_SETUP.md](JITPACK_SETUP.md#troubleshooting)

### General Build Issues

**Issue: Gradle build fails**
```bash
# Clean and rebuild
./gradlew clean build

# Check Gradle wrapper version
./gradlew --version

# Update dependencies
./gradlew --refresh-dependencies
```

**Issue: Spotless formatting fails**
```bash
# Auto-fix formatting issues
./gradlew spotlessApply

# Check what would be changed
./gradlew spotlessCheck
```

## Resources

### Maven Central
- Central Portal: https://central.sonatype.com/
- Documentation: https://central.sonatype.org/publish/publish-portal-gradle/
- Maven Repository: https://repo1.maven.org/maven2/

### JitPack
- Website: https://jitpack.io/
- Documentation: https://docs.jitpack.io/
- Android Guide: https://docs.jitpack.io/android/

### Project Resources
- GitHub Repository: https://github.com/afarber/OpenMapView
- Issue Tracker: https://github.com/afarber/OpenMapView/issues
- GitHub Actions: https://github.com/afarber/OpenMapView/actions

## Related Documentation

- [Maven Central Setup](MAVEN_CENTRAL_SETUP.md) - Detailed Maven Central configuration
- [JitPack Setup](JITPACK_SETUP.md) - Detailed JitPack configuration
- [GitHub Workflows](GITHUB_WORKFLOWS.md) - CI/CD pipeline details
- [Contributing Guide](CONTRIBUTING.md) - How to contribute to the project
