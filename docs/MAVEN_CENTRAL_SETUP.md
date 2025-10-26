# Maven Central Publishing Setup Guide

[Back to README](../README.md)

This guide documents the Maven Central publishing setup for OpenMapView.

## Current Status

**Setup Complete** - OpenMapView is configured to publish to Maven Central via the Central Portal.

- **Namespace**: `de.afarber` (verified with Sonatype)
- **Group ID**: `de.afarber`
- **Artifact ID**: `openmapview`
- **Maintainer**: Alexander Farber (farber72@outlook.de)

**Note:** As of June 30, 2025, OSSRH has reached end-of-life and has been shut down. All publishing now goes through the new Central Portal (central.sonatype.com).

## Publishing Configuration

### Maven Coordinates

```kotlin
dependencies {
    implementation("de.afarber:openmapview:VERSION")
}
```

### GPG Signing Key

**Key ID**: `8334881A009EB69E5B5BDBF189999F05686CE169`
**Algorithm**: Ed25519
**Expires**: 2028-10-24
**Email**: farber72@outlook.de

The public key has been published to keyservers and is used to sign all published artifacts.

### GitHub Secrets Configuration

The following secrets are configured in GitHub Actions:

1. **OSSRH_USERNAME** - Central Portal username (user token generated at central.sonatype.com/account)
2. **OSSRH_PASSWORD** - Central Portal password (user token generated at central.sonatype.com/account)
3. **SIGNING_KEY** - Base64-encoded GPG private key
4. **SIGNING_PASSWORD** - GPG key passphrase

**Note:** The credential names remain `OSSRH_USERNAME` and `OSSRH_PASSWORD` for backward compatibility, but they now contain Central Portal user tokens, not the old OSSRH/JIRA credentials.

## Release Process

### Creating a New Release

To publish a new version to Maven Central:

```bash
# 1. Ensure all changes are committed
git status

# 2. Create and push a version tag (format: vMAJOR.MINOR.PATCH)
git tag v0.2.0
git push origin v0.2.0
```

### Automated Workflow

When you push a version tag, GitHub Actions automatically:

1. **Validates** - Checks formatting with Spotless
2. **Tests** - Runs unit tests
3. **Builds** - Builds library AAR and example APKs
4. **Publishes** - Uploads artifacts to Maven Central
5. **Releases** - Creates GitHub Release with changelog

### Publishing with Central Portal

The project uses the OSSRH Staging API compatibility layer provided by Central Portal. This allows using existing Gradle publishing configuration with minimal changes.

**Publishing Flow:**

1. Push a version tag (e.g., `v0.1.0`)
2. GitHub Actions workflow automatically builds and publishes to Central Portal
3. Artifacts are validated and published to Maven Central
4. Wait 10-30 minutes for sync to Maven Central

**Important Notes:**

- The old OSSRH web UI (s01.oss.sonatype.org) is no longer available
- All management is now done through the Central Portal at https://central.sonatype.com/
- This project uses the Central Portal OSSRH Staging API endpoint: `https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/`
- The old endpoint (`https://s01.oss.sonatype.org/...`) may not work reliably with Central Portal tokens

## Verifying Publication

After publishing, verify the library is available:

### Maven Central Search
https://central.sonatype.com/artifact/de.afarber/openmapview

### Maven Repository Browser
https://search.maven.org/artifact/de.afarber/openmapview

### Direct Repository URL
https://repo1.maven.org/maven2/de/afarber/openmapview/

**Note:** With the new Central Portal, artifacts typically appear in Maven Central within 10-30 minutes after publishing (faster than the old 2-4 hour sync time).

## Version Numbering

OpenMapView uses semantic versioning (SemVer):

- **vMAJOR.MINOR.PATCH** (e.g., `v0.2.0`)
- Version is automatically detected from Git tags
- Tags must match the pattern `v*.*.*` to trigger release workflow

Examples:
- `v0.1.0` - Initial release
- `v0.2.0` - New features added
- `v0.2.1` - Bug fixes
- `v1.0.0` - First stable release

## Build Configuration

The publishing configuration is defined in:
- `openmapview/build.gradle.kts` - Maven publication setup
- `.github/workflows/release.yml` - Release automation

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

## Troubleshooting

### Issue: Workflow fails with "401 Unauthorized"
**Solution:** Verify GitHub Secrets are correctly configured:
- Check `OSSRH_USERNAME` and `OSSRH_PASSWORD` contain Central Portal user tokens
- Generate new tokens at https://central.sonatype.com/account (click "Generate User Token")
- The tokens should be different from your old OSSRH/JIRA credentials

### Issue: "Failed to verify signature"
**Solution:** Verify GPG key configuration:
- Confirm public key `8334881A009EB69E5B5BDBF189999F05686CE169` is published to keyservers
- Check `SIGNING_KEY` secret contains the full base64 private key
- Verify `SIGNING_PASSWORD` matches your GPG key passphrase

### Issue: Need to manage published artifacts
**Solution:**
- Log in to the Central Portal at https://central.sonatype.com/
- Navigate to the "Publishing" section to view deployments
- The old OSSRH web UI (s01.oss.sonatype.org) is no longer available

### Issue: Version conflict
**Solution:**
- You cannot republish the same version
- Delete the tag and create a new one with an incremented version:
  ```bash
  git tag -d v0.1.0
  git push origin :refs/tags/v0.1.0
  git tag v0.1.1
  git push origin v0.1.1
  ```

## Workflow Files

The release process is orchestrated by reusable GitHub Actions workflows:

- `.github/workflows/release.yml` - Main release workflow
- `.github/workflows/_format.yml` - Code formatting check
- `.github/workflows/_test.yml` - Unit tests
- `.github/workflows/_build-library.yml` - Build AAR
- `.github/workflows/_build-examples.yml` - Build example APKs

## Optional: GitHub Packages

To also publish to GitHub Packages, uncomment the GitHub Packages repository section in `openmapview/build.gradle.kts` (lines 119-128):

```kotlin
maven {
    name = "GitHubPackages"
    url = uri("https://maven.pkg.github.com/afarber/OpenMapView")
    credentials {
        username = System.getenv("GITHUB_ACTOR")
        password = System.getenv("GITHUB_TOKEN")
    }
}
```

**Note:** Users need authentication to download from GitHub Packages, even for public repositories. Maven Central is recommended for public distribution.

## Migration from OSSRH to Central Portal

### Background

As of June 30, 2025, the legacy OSSRH service (oss.sonatype.org and s01.oss.sonatype.org) has reached end-of-life. All publishing now goes through the new Central Portal at https://central.sonatype.com/.

### OSSRH Staging API Compatibility Layer

The project currently uses the OSSRH Staging API compatibility layer, which provides a smooth transition path:

- **Current endpoint**: `https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/`
- **Status**: This endpoint is maintained as a compatibility layer that forwards requests to Central Portal
- **Authentication**: Requires Central Portal user tokens (not old OSSRH/JIRA credentials)

### Generating Central Portal User Tokens

1. Log in to https://central.sonatype.com/
2. Navigate to your account settings at https://central.sonatype.com/account
3. Click "Generate User Token"
4. Copy the username and password tokens
5. Update GitHub Secrets:
   - `OSSRH_USERNAME` = Central Portal username token
   - `OSSRH_PASSWORD` = Central Portal password token

### Current Configuration

The project is configured to use the Central Portal OSSRH Staging API endpoint:

```kotlin
repositories {
    maven {
        name = "MavenCentral"
        url = uri("https://ossrh-staging-api.central.sonatype.com/service/local/staging/deploy/maven2/")
        credentials {
            username = System.getenv("OSSRH_USERNAME")
            password = System.getenv("OSSRH_PASSWORD")
        }
    }
}
```

This is the official endpoint for the Central Portal and works with the user tokens generated from your Central Portal account.

## Resources

- **Central Portal**: https://central.sonatype.com/
- **Central Portal Publishing Guide**: https://central.sonatype.org/publish/publish-portal-gradle/
- **OSSRH EOL Information**: https://central.sonatype.org/pages/ossrh-eol/
- **Maven Central Repository**: https://repo1.maven.org/maven2/
- **Maven Central Search**: https://search.maven.org/

## Quick Reference

```bash
# Create a release
git tag v0.2.0
git push origin v0.2.0

# Check workflow status
# Go to: https://github.com/afarber/OpenMapView/actions

# Verify publication (after 2-4 hours)
# Go to: https://central.sonatype.com/artifact/de.afarber/openmapview

# Users can then install with:
# implementation("de.afarber:openmapview:0.2.0")
```
