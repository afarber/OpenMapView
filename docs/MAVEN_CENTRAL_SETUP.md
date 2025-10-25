# Maven Central Publishing Setup Guide

This guide documents the Maven Central publishing setup for OpenMapView.

## Current Status

✅ **Setup Complete** - OpenMapView is configured to publish to Maven Central.

- **Namespace**: `de.afarber` (verified with Sonatype)
- **Group ID**: `de.afarber`
- **Artifact ID**: `openmapview`
- **Maintainer**: Alexander Farber (farber72@outlook.de)

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

1. **OSSRH_USERNAME** - Sonatype JIRA username
2. **OSSRH_PASSWORD** - Sonatype JIRA password
3. **SIGNING_KEY** - Base64-encoded GPG private key
4. **SIGNING_PASSWORD** - GPG key passphrase

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

### First Release (Manual Steps)

For the first release only, you need to manually release the staging repository:

1. Push a version tag (e.g., `v0.1.0`)
2. Wait for GitHub Actions workflow to complete
3. Log in to https://s01.oss.sonatype.org/
4. Navigate to **Staging Repositories**
5. Find your repository (e.g., `deafarber-1001`)
6. Click **Close** to validate artifacts
7. Wait for validation to complete (1-5 minutes)
8. Click **Release** to publish to Maven Central
9. Wait 2-4 hours for sync to central.maven.org

### Subsequent Releases (Automatic)

After the first successful release, Sonatype enables automatic release for the `de.afarber` namespace. Future releases publish automatically without manual intervention.

## Verifying Publication

After publishing, verify the library is available:

### Maven Central Search
https://central.sonatype.com/artifact/de.afarber/openmapview

### Maven Repository Browser
https://search.maven.org/artifact/de.afarber/openmapview

### Direct Repository URL
https://repo1.maven.org/maven2/de/afarber/openmapview/

**Note:** Allow 2-4 hours for new releases to sync to Maven Central.

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
- Check `OSSRH_USERNAME` and `OSSRH_PASSWORD`
- Ensure credentials match your Sonatype JIRA account

### Issue: "Failed to verify signature"
**Solution:** Verify GPG key configuration:
- Confirm public key `8334881A009EB69E5B5BDBF189999F05686CE169` is published to keyservers
- Check `SIGNING_KEY` secret contains the full base64 private key
- Verify `SIGNING_PASSWORD` matches your GPG key passphrase

### Issue: Staging repository not found
**Solution:**
- Ensure you're logged into https://s01.oss.sonatype.org/ (not the old oss.sonatype.org)
- Wait a few minutes after workflow completion
- Check GitHub Actions logs for publish errors

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

## Resources

- **Sonatype OSSRH Guide**: https://central.sonatype.org/publish/
- **Sonatype Portal**: https://s01.oss.sonatype.org/
- **Maven Central Repository**: https://repo1.maven.org/maven2/
- **Maven Central Search**: https://central.sonatype.com/

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
