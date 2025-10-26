# JitPack Publishing Setup Guide

[Back to README](../README.md)

This guide documents the JitPack publishing setup for OpenMapView.

## Overview

JitPack is a package repository for Git repositories that automatically builds Maven/Gradle artifacts directly from GitHub releases and tags. It requires minimal configuration and provides instant publishing without manual uploads.

## Current Status

**Setup Complete** - OpenMapView is configured to publish to JitPack.

- **Group ID**: `com.github.afarber`
- **Artifact ID**: `OpenMapView`
- **Repository**: https://github.com/afarber/OpenMapView
- **JitPack URL**: https://jitpack.io/#afarber/OpenMapView

## How JitPack Works

1. When a user requests a dependency, JitPack automatically:
   - Checks out the code from the specified release/tag/commit
   - Runs the build process
   - Caches the built artifacts
   - Serves the artifacts to Maven/Gradle clients

2. No manual publishing required - JitPack builds on demand from GitHub

3. Supports multiple artifact types:
   - Release versions (tags)
   - Snapshot builds (branches)
   - Specific commits

## Configuration

### jitpack.yml

The repository contains a `jitpack.yml` file that specifies the build environment:

```yaml
jdk:
  - openjdk17
```

This configuration is required because Android Gradle Plugin 8.7.0 requires Java 17.

### Build Configuration

JitPack uses the existing `maven-publish` plugin configuration in `openmapview/build.gradle.kts`. No additional setup is needed beyond what is already configured for Maven Central publishing.

## Usage for Users

### Adding JitPack Repository

Users need to add the JitPack repository to their project.

In `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Or in Groovy `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

### Adding the Dependency

#### Release Version

To use a specific release (e.g., v0.1.0):

```kotlin
dependencies {
    implementation("com.github.afarber:OpenMapView:0.1.0")
}
```

```groovy
dependencies {
    implementation 'com.github.afarber:OpenMapView:0.1.0'
}
```

#### Latest Release

To always use the latest release:

```kotlin
dependencies {
    implementation("com.github.afarber:OpenMapView:+")
}
```

#### Development Snapshots

To use the latest commit from the main branch:

```kotlin
dependencies {
    implementation("com.github.afarber:OpenMapView:main-SNAPSHOT")
}
```

To use a specific commit:

```kotlin
dependencies {
    implementation("com.github.afarber:OpenMapView:abc1234")
}
```

To use a specific branch:

```kotlin
dependencies {
    implementation("com.github.afarber:OpenMapView:feature-branch-SNAPSHOT")
}
```

## Publishing Process

### For Maintainers

Publishing to JitPack is automatic and happens when users request the artifact:

1. **Create a GitHub Release** (or push a tag):
   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```

2. **JitPack automatically**:
   - Detects the new tag
   - Builds the artifact on first request
   - Caches the build for future requests

3. **No manual upload required** - JitPack pulls directly from GitHub

### First Build

The first time a user requests a new version, JitPack builds it on-demand. This may take a few minutes. Subsequent requests are served from cache.

### Build Status

Check the build status at:
```
https://jitpack.io/#afarber/OpenMapView
```

The JitPack website shows:
- Available versions
- Build status (success/failure)
- Build logs
- Download statistics

## Testing Locally

Before pushing a release, test that the library can be published to Maven Local:

```bash
./gradlew publishToMavenLocal
```

This ensures the build configuration is correct and JitPack will be able to build the artifact.

## Advantages of JitPack

1. **Zero Configuration** - Uses existing `maven-publish` setup
2. **Instant Publishing** - No waiting for Maven Central sync
3. **Development Snapshots** - Test unreleased versions from branches/commits
4. **No Credentials Required** - Builds directly from public GitHub repository
5. **Automatic Builds** - No manual upload process

## Comparison with Maven Central

| Feature | JitPack | Maven Central |
|---------|---------|---------------|
| Setup Complexity | Minimal | Moderate |
| Publishing Speed | Instant | 10-30 minutes |
| Snapshot Support | Yes (branches/commits) | No |
| User Setup | Requires repository declaration | Works out-of-the-box |
| Credential Management | None required | GPG signing + user tokens |
| Discoverability | Lower | Higher |
| Best For | Development/testing | Production releases |

## Troubleshooting

### Issue: Build fails on JitPack

**Solution:** Check the build logs at https://jitpack.io/#afarber/OpenMapView

Common issues:
- Missing `jitpack.yml` with correct JDK version
- Build configuration errors in `build.gradle.kts`
- Missing dependencies or plugins

### Issue: Build succeeds but artifact is empty

**Solution:** Verify the `maven-publish` plugin is correctly configured:

```bash
./gradlew publishToMavenLocal
ls ~/.m2/repository/de/afarber/openmapview/
```

### Issue: Users report "artifact not found"

**Solution:**
- Ensure the repository is public on GitHub
- Check that the tag/release exists
- Verify the correct artifact coordinates are used:
  - Group: `com.github.afarber`
  - Artifact: `OpenMapView` (case-sensitive)

### Issue: Want to rebuild a version

**Solution:** Delete builds at https://jitpack.io/#afarber/OpenMapView using the "Look up" button and rebuild option.

## Version Numbering

JitPack uses Git tags for versioning:

- Tags like `v0.1.0` become version `0.1.0`
- Tags like `0.1.0` also become version `0.1.0`
- Branch `main` becomes `main-SNAPSHOT`
- Specific commits use their SHA hash

## Resources

- **JitPack Website**: https://jitpack.io/
- **JitPack Documentation**: https://docs.jitpack.io/
- **Android Guide**: https://docs.jitpack.io/android/
- **Build Status**: https://jitpack.io/#afarber/OpenMapView
- **Example Android Project**: https://github.com/jitpack/android-example

## Quick Reference

```bash
# Test local publishing
./gradlew publishToMavenLocal

# Create a release (triggers JitPack availability)
git tag v0.2.0
git push origin v0.2.0

# Check build status
# Visit: https://jitpack.io/#afarber/OpenMapView

# Users can install with:
# implementation("com.github.afarber:OpenMapView:0.2.0")
```

## Related Documentation

- [Publishing Overview](PUBLISHING.md) - Complete guide to Maven Central and JitPack publishing
- [Maven Central Setup](MAVEN_CENTRAL_SETUP.md) - Maven Central specific configuration
- [GitHub Workflows](GITHUB_WORKFLOWS.md) - CI/CD pipeline
