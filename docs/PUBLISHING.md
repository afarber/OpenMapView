[![Maven Central](https://img.shields.io/maven-central/v/de.afarber/openmapview.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/de.afarber/openmapview)
[![JitPack](https://jitpack.io/v/afarber/OpenMapView.svg)](https://jitpack.io/#afarber/OpenMapView)

# Publishing Guide

[Back to README](../README.md)

This guide documents the publishing configuration for OpenMapView to Maven Central and JitPack.

## Overview

OpenMapView is published to two package repositories:

- **Maven Central**: `de.afarber:openmapview:VERSION`
- **JitPack**: `com.github.afarber:OpenMapView:VERSION`

Both repositories are automatically updated when pushing a version tag.

## Maven Central

### Configuration

- **Namespace**: `de.afarber` (verified with Sonatype)
- **Group ID**: `de.afarber`
- **Artifact ID**: `openmapview`
- **Publishing Plugin**: `com.gradleup.nmcp` v0.1.2
- **GPG Signing Key**: `8334881A009EB69E5B5BDBF189999F05686CE169` (Ed25519, expires 2028-10-24)

### GitHub Secrets

Required secrets configured in GitHub Actions:

- **OSSRH_USERNAME** - Central Portal user token (generated at central.sonatype.com/account)
- **OSSRH_PASSWORD** - Central Portal user token
- **SIGNING_KEY** - Base64-encoded GPG private key
- **SIGNING_PASSWORD** - GPG key passphrase

### Publishing Process

1. Create and push a version tag:
   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```

2. GitHub Actions automatically:
   - Validates formatting with Spotless
   - Runs unit tests
   - Builds library AAR and example APKs
   - Signs artifacts with GPG
   - Publishes to Maven Central via Central Portal API
   - Creates GitHub Release

3. Artifacts appear on Maven Central within 10-30 minutes

### Verification

- Central Portal: https://central.sonatype.com/artifact/de.afarber/openmapview
- Maven Search: https://search.maven.org/artifact/de.afarber/openmapview
- Direct Repository: https://repo1.maven.org/maven2/de/afarber/openmapview/

### POM Metadata

```xml
<groupId>de.afarber</groupId>
<artifactId>openmapview</artifactId>
<name>OpenMapView</name>
<description>A modern, Kotlin-first MapView replacement for Android powered by OpenStreetMap.</description>
<url>https://github.com/afarber/OpenMapView</url>

<licenses>
  <license>
    <name>MIT License</name>
    <url>https://opensource.org/licenses/MIT</url>
  </license>
</licenses>

<developers>
  <developer>
    <id>afarber</id>
    <name>Alexander Farber</name>
    <email>farber72@outlook.de</email>
    <url>https://afarber.de</url>
  </developer>
</developers>

<scm>
  <connection>scm:git:https://github.com/afarber/OpenMapView.git</connection>
  <developerConnection>scm:git:ssh://github.com/afarber/OpenMapView.git</developerConnection>
  <url>https://github.com/afarber/OpenMapView</url>
</scm>
```

### Build Configuration

Publishing configuration is defined in:
- `build.gradle.kts` (root) - nmcp plugin configuration
- `openmapview/build.gradle.kts` - Maven publication setup (POM metadata, signing)
- `.github/workflows/release.yml` - Release automation

## JitPack

### Configuration

- **Repository**: `com.github.afarber:OpenMapView`
- **Build Environment**: Java 17 (specified in `jitpack.yml`)
- **Build Plugin**: Uses existing `maven-publish` configuration

### Publishing Process

JitPack builds automatically from GitHub releases on first request:

1. Create and push a version tag:
   ```bash
   git tag v0.2.0
   git push origin v0.2.0
   ```

2. JitPack automatically:
   - Detects the new tag
   - Builds artifacts when first requested
   - Caches for subsequent requests

3. Available immediately at https://jitpack.io/#afarber/OpenMapView

### Verification

- JitPack Page: https://jitpack.io/#afarber/OpenMapView
- Build Logs: Available on JitPack page for each version
- API Status: https://jitpack.io/api/builds/com.github.afarber/OpenMapView/VERSION

### Development Snapshots

JitPack supports building from branches and commits:

```kotlin
// Latest commit from main branch
implementation("com.github.afarber:OpenMapView:main-SNAPSHOT")

// Specific commit
implementation("com.github.afarber:OpenMapView:abc1234")

// Feature branch
implementation("com.github.afarber:OpenMapView:feature-branch-SNAPSHOT")
```

## Version Numbering

OpenMapView uses Semantic Versioning (SemVer):

- **vMAJOR.MINOR.PATCH** (e.g., `v0.2.0`)
- Version is automatically detected from Git tags
- Tags must match the pattern `v*.*.*` to trigger release workflow

Examples:
- `v0.1.0` - Initial release
- `v0.2.0` - New features added
- `v0.2.1` - Bug fixes
- `v1.0.0` - First stable release

## Release Checklist

When creating a new release:

- [ ] Ensure all tests pass: `./gradlew test`
- [ ] Run code formatting: `./gradlew spotlessApply`
- [ ] Verify local build: `./gradlew build`
- [ ] Test Maven publication: `./gradlew publishToMavenLocal`
- [ ] Create and push version tag (e.g., `v0.2.0`)
- [ ] Monitor GitHub Actions release workflow
- [ ] Verify Maven Central publication (10-30 minutes)
- [ ] Verify JitPack build status

## Troubleshooting

### Maven Central Issues

**Issue: Workflow fails with "401 Unauthorized"**

Solution: Verify GitHub Secrets are correctly configured:
- Generate new tokens at https://central.sonatype.com/account
- Update `OSSRH_USERNAME` and `OSSRH_PASSWORD` secrets

**Issue: "Failed to verify signature"**

Solution: Verify GPG key configuration:
- Confirm public key `8334881A009EB69E5B5BDBF189999F05686CE169` is published to keyservers
- Check `SIGNING_KEY` secret contains the full base64 private key
- Verify `SIGNING_PASSWORD` matches the GPG key passphrase

**Issue: Version conflict**

Solution: Maven Central does not allow republishing the same version:
```bash
git tag -d v0.1.0
git push origin :refs/tags/v0.1.0
git tag v0.1.1
git push origin v0.1.1
```

### JitPack Issues

**Issue: Build fails on JitPack**

Solution: Check build logs at https://jitpack.io/#afarber/OpenMapView

Common causes:
- Missing `jitpack.yml` with correct JDK version
- Build configuration errors in `build.gradle.kts`
- Missing dependencies or plugins

**Issue: Build succeeds but artifact is empty**

Solution: Verify `maven-publish` plugin is correctly configured:
```bash
./gradlew publishToMavenLocal
ls ~/.m2/repository/de/afarber/openmapview/
```

**Issue: Artifact not found**

Solution:
- Ensure repository is public on GitHub
- Verify tag/release exists: `git tag -l`
- Check artifact coordinates are correct (case-sensitive)

### General Build Issues

**Issue: Gradle build fails**

```bash
# Clean and rebuild
./gradlew clean build

# Update dependencies
./gradlew --refresh-dependencies
```

**Issue: Spotless formatting fails**

```bash
# Auto-fix formatting
./gradlew spotlessApply

# Check formatting
./gradlew spotlessCheck
```

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

## Workflow Files

The release process is orchestrated by reusable GitHub Actions workflows:

- `.github/workflows/release.yml` - Main release workflow
- `.github/workflows/_format.yml` - Code formatting check
- `.github/workflows/_test.yml` - Unit tests
- `.github/workflows/_build-library.yml` - Build AAR
- `.github/workflows/_build-examples.yml` - Build example APKs

## Resources

### Maven Central
- Central Portal: https://central.sonatype.com/
- Publishing Guide: https://central.sonatype.org/publish/publish-portal-gradle/
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

- [GitHub Workflows](GITHUB_WORKFLOWS.md) - CI/CD pipeline details
- [Contributing Guide](CONTRIBUTING.md) - Contribution guidelines
- [Testing Guide](TESTING.md) - Testing documentation

## Migration Notes

### Central Portal Migration

As of June 30, 2025, the legacy OSSRH service (oss.sonatype.org and s01.oss.sonatype.org) has reached end-of-life. All publishing now goes through the Central Portal at https://central.sonatype.com/.

The project uses the `com.gradleup.nmcp` plugin v0.1.2 for Central Portal publishing:

```kotlin
nmcp {
    publishAllProjectsProbablyBreakingProjectIsolation {
        username = System.getenv("OSSRH_USERNAME") ?: ""
        password = System.getenv("OSSRH_PASSWORD") ?: ""
        publicationType = "AUTOMATIC"
    }
}
```

GitHub Actions workflow uses: `./gradlew publishAggregationToCentralPortal`

## Quick Reference

```bash
# Create a release
git tag v0.2.0
git push origin v0.2.0

# Check workflow status
# Visit: https://github.com/afarber/OpenMapView/actions

# Verify Maven Central publication (after 10-30 minutes)
# Visit: https://central.sonatype.com/artifact/de.afarber/openmapview

# Verify JitPack build
# Visit: https://jitpack.io/#afarber/OpenMapView

# Test local build
./gradlew publishToMavenLocal
```

## Installation Examples

### Maven Central

No repository configuration required:

```kotlin
dependencies {
    implementation("de.afarber:openmapview:0.1.0")
}
```

### JitPack

Requires repository configuration in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
dependencies {
    implementation("com.github.afarber:OpenMapView:0.1.0")
}
```
