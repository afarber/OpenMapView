[![Maven Central](https://img.shields.io/maven-central/v/de.afarber/openmapview.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/de.afarber/openmapview)
[![JitPack](https://jitpack.io/v/afarber/OpenMapView.svg)](https://jitpack.io/#afarber/OpenMapView)

# Publishing Guide

[Back to README](../README.md)

This guide documents the publishing configuration for OpenMapView to Maven Central and JitPack.

## Overview

OpenMapView is published to two package repositories:

- **Maven Central**: `de.afarber:openmapview:VERSION` and the optional `de.afarber:openmapview-compose:VERSION`
- **JitPack**: `com.github.afarber:OpenMapView:VERSION`

Both repositories are automatically updated when pushing a version tag.

## Maven Central

### Configuration

- **Namespace**: `de.afarber` (verified with Sonatype)
- **Group ID**: `de.afarber`
- **Artifact IDs**: `openmapview` and `openmapview-compose`, published together with the same version
- **Publishing Plugin**: `com.gradleup.nmcp.aggregation` (version in `gradle/libs.versions.toml`)
- **GPG Signing Key**: `8334881A009EB69E5B5BDBF189999F05686CE169` (Ed25519, expires 2028-10-24)

### Release Environment and Secrets

The four secrets of the release live only in the GitHub environment `release` (Settings -> Environments), not as repository secrets. The environment accepts only tags matching `v*.*.*`, and only the publish job of `release.yml` uses it, so no other workflow or branch can read them.

- **OSSRH_USERNAME** and **OSSRH_PASSWORD** - the two parts of a Central Portal user token (central.sonatype.com, account menu, View User Tokens, Generate User Token). They are not the login of the website, and the names are kept from the former OSSRH service
- **SIGNING_KEY** - the ASCII-armored PGP private key above, the text starting with `-----BEGIN PGP PRIVATE KEY BLOCK-----`
- **SIGNING_PASSWORD** - the passphrase of that key

The Portal shows a token only once, and GitHub never shows a secret again, so keep a backup of the private key and its passphrase offline.

Set or replace a secret (each command prompts for the value, or reads it from a file):

```bash
gh secret set OSSRH_USERNAME --env release
gh secret set OSSRH_PASSWORD --env release
gh secret set SIGNING_KEY --env release < private_key.asc
gh secret set SIGNING_PASSWORD --env release
```

**Check a Portal token** without publishing anything. The call answers 200 with valid credentials and 401 otherwise:

```bash
# zsh syntax; in bash use: read -rp "Token username: " u; read -rsp "Token password: " p; echo
read -r "u?Token username: "; read -rs "p?Token password: "; echo
curl -s -o /dev/null -w "%{http_code}\n" \
  -H "Authorization: Bearer $(printf '%s:%s' "$u" "$p" | base64)" \
  "https://central.sonatype.com/api/v1/publisher/published?namespace=de.afarber&name=openmapview&version=0.28.1"
unset u p
```

**Rotate the token:** generate a new one, set both secrets, then revoke the old token on the Portal.

**Check the signing key and passphrase** in a throwaway keyring, which leaves the normal GnuPG setup untouched:

```bash
export GNUPGHOME=$(mktemp -d)
gpg --import private_key.asc
echo test | gpg --pinentry-mode loopback --clearsign > /dev/null && echo "Passphrase OK"
rm -rf "$GNUPGHOME"; unset GNUPGHOME
```

**Replace the signing key** (e.g. before it expires): create a new key, publish its public key with `gpg --keyserver keyserver.ubuntu.com --send-keys KEY_ID` and to keys.openpgp.org (which confirms by email), set `SIGNING_KEY` and `SIGNING_PASSWORD`, and update the fingerprint above.

### Repository Protection

- **Release tags**: a ruleset on `refs/tags/v*` lets only admins create release tags and blocks moving and deleting them. Pushing a tag is what publishes a release.
- **Pinned actions**: every action in `.github/workflows` is pinned to a commit SHA, with its version in a comment. Dependabot updates the SHA and the comment together, so a moved tag upstream cannot change what runs next to the release secrets.
- **CODEOWNERS**: `.github/CODEOWNERS` names the maintainer as owner of every file.

### Publishing Limits

The Central Portal limits publishing per organization and calendar month, currently 7 releases, 1000 files and 80 MB. One release of both artifacts is about 40 files and 1.6 MB, so the release count is the limit that matters. Release merged changes in batches, e.g. weekly, rather than after every pull request. The Usage Center on the Portal shows the current numbers.

### Publishing Process

1. Run the release script on an up-to-date `main`:

   ```bash
   ./scripts/publish-release.sh          # next patch version
   ./scripts/publish-release.sh v0.29.0  # or a given version
   ```

   It bumps the version in `README.md` and `docs/MIGRATING.md`, commits it, creates a signed tag and pushes `main` and the tag together.

2. The tag starts `release.yml`, which:

   - Checks formatting and runs Lint and the unit tests
   - Builds the library AAR and the example APKs
   - Signs the artifacts and uploads them to the Central Portal, which publishes them
   - Creates the GitHub Release

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
- `openmapview/build.gradle.kts` and `openmapview-compose/build.gradle.kts` - Maven publication setup (POM metadata, signing)
- `.github/workflows/release.yml` - Release automation

## JitPack

### Configuration

- **Repository**: `com.github.afarber:OpenMapView`
- **Build Environment**: Java 17 (specified in `jitpack.yml`)
- **Build Plugin**: Uses existing `maven-publish` configuration

### Publishing Process

JitPack builds automatically from GitHub releases on first request:

1. The release tag is pushed by `./scripts/publish-release.sh`, see Maven Central above

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

- **vMAJOR.MINOR.PATCH** (e.g., `v0.3.0`)
- Version is automatically detected from Git tags
- Tags must match the pattern `v*.*.*` to trigger release workflow

Examples:

- `v0.1.0` - Initial release
- `v0.2.0` - New features added
- `v0.2.1` - Bug fixes
- `v1.0.0` - First stable release

## Release Checklist

When creating a new release:

- [ ] Make sure CI on main is green and your local main is up to date
- [ ] Check the Usage Center on the Central Portal: at most 7 releases per month
- [ ] Run `./scripts/publish-release.sh` (next patch version) or `./scripts/publish-release.sh vX.Y.Z`. It bumps the version in the docs, commits, creates a signed tag and pushes main and the tag
- [ ] Monitor GitHub Actions release workflow
- [ ] Verify Maven Central publication (10-30 minutes)
- [ ] Verify JitPack build status

## Troubleshooting

### Maven Central Issues

**Issue: Workflow fails with "401 Unauthorized"**

Solution: Check the token with the `curl` call under "Release Environment and Secrets". If it answers 401, generate a new token and update `OSSRH_USERNAME` and `OSSRH_PASSWORD` in the `release` environment.

**Issue: "Failed to verify signature"**

Solution: Verify GPG key configuration:

- Confirm public key `8334881A009EB69E5B5BDBF189999F05686CE169` is published to keyservers
- Check that `SIGNING_KEY` in the `release` environment contains the whole ASCII-armored private key, including the BEGIN and END lines
- Check the passphrase with a throwaway keyring, see "Release Environment and Secrets"

**Issue: Version conflict**

Solution: Maven Central does not allow republishing the same version, and the tag ruleset keeps release tags from being moved or deleted. Fix the problem on `main` and release the next patch version with `./scripts/publish-release.sh`.

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

- `.github/workflows/release.yml` - Main release workflow, its publish job runs in the `release` environment
- `.github/workflows/_lint.yml` - Android Lint
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
- [Unit Testing Guide](TESTING_UNIT.md) - Unit testing with Robolectric
- [Instrumented Testing Guide](TESTING_INSTRUMENTED.md) - On-device testing

## Migration Notes

### Central Portal Migration

As of June 30, 2025, the legacy OSSRH service (oss.sonatype.org and s01.oss.sonatype.org) has reached end-of-life. All publishing now goes through the Central Portal at https://central.sonatype.com/.

The project uses the `com.gradleup.nmcp.aggregation` plugin for Central Portal publishing (root `build.gradle.kts`):

```kotlin
nmcpAggregation {
    centralPortal {
        username = System.getenv("OSSRH_USERNAME") ?: ""
        password = System.getenv("OSSRH_PASSWORD") ?: ""
        publishingType = "AUTOMATIC"
    }
}

dependencies {
    nmcpAggregation(project(":openmapview"))
    nmcpAggregation(project(":openmapview-compose"))
}
```

The `:openmapview` module applies `com.gradleup.nmcp` (without a version, the root project already puts it on the classpath), so its publications can be aggregated. This replaces `publishAllProjectsProbablyBreakingProjectIsolation()`, which uses a dependency notation that Gradle 10 will reject.

nmcp rejects builds where two projects have the same name, compared case-insensitively. That's why the root project is named `OpenMapView-root` in `settings.gradle.kts` rather than `OpenMapView`, which would clash with the `:openmapview` module.

GitHub Actions workflow uses: `./gradlew publishAggregationToCentralPortal`

## Quick Reference

```bash
# Create a release (bumps the docs, commits, pushes a signed tag)
./scripts/publish-release.sh

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

Replace `<version>` with the latest release shown in the [README](../README.md#installation).

### Maven Central

No repository configuration required:

```kotlin
dependencies {
    implementation("de.afarber:openmapview:<version>")
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
    implementation("com.github.afarber:OpenMapView:<version>")
}
```
