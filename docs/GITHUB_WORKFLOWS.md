# GitHub Actions Workflows Guide

[Back to README](../README.md)

This document explains the GitHub Actions workflows used in the OpenMapView project.

> **Naming Convention:** Reusable workflows are prefixed with an underscore (e.g., `_docs.yml`, `_test.yml`)
> to distinguish them from caller workflows (e.g., `ci.yml`, `release.yml`). This convention
> makes it easy to identify modular components that can be composed into different pipelines.

## Architecture Overview

The workflows are designed using a **modular, reusable component architecture** to eliminate code duplication and enable parallel execution.

### Design Principles

- **DRY (Don't Repeat Yourself)**: Common setup code is in reusable workflows
- **Composable**: Mix and match workflow components
- **Parallel Execution**: Independent jobs run simultaneously for speed
- **Maintainable**: Change configuration once, affects all workflows
- **Fail Fast**: Format checks run first, expensive builds run last

## Workflow Files

### Reusable Components (Building Blocks)

Located in `.github/workflows/`:

#### `_copyright.yml`
- **Purpose**: Verify MIT license headers in all `.kt` files
- **Runs**: `./scripts/check-copyright.sh`
- **Usage**: Called by CI and Release workflows
- **Fast**: ~30 seconds

#### `_format.yml`
- **Purpose**: Verify code formatting with Spotless
- **Runs**: `./gradlew spotlessCheck`
- **Usage**: Called by CI and Release workflows
- **Fast**: ~30 seconds

#### `_coverage.yml`
- **Purpose**: Check test coverage meets minimum threshold
- **Runs**: `./scripts/check-coverage.sh` (minimum threshold defined in [Contributing Guide](CONTRIBUTING.md#test-coverage))
- **Artifacts**: Uploads coverage reports to Codecov and as artifacts
- **Usage**: Called by CI workflow
- **Duration**: ~1-2 minutes

#### `_test.yml`
- **Purpose**: Run unit tests
- **Runs**: `./gradlew :openmapview:test --continue`
- **Artifacts**: Uploads test results and HTML reports
- **Usage**: Called by CI and Release workflows
- **Duration**: ~1-2 minutes

#### `_build-library.yml`
- **Purpose**: Build the OpenMapView AAR library
- **Runs**: `./gradlew :openmapview:assembleRelease`
- **Artifacts**: Uploads the compiled AAR file
- **Usage**: Called by CI and Release workflows
- **Duration**: ~2-3 minutes

#### `_build-examples.yml`
- **Purpose**: Build all three example apps
- **Runs**: Builds Example01Pan, Example02Zoom, Example03Markers
- **Artifacts**: Uploads all debug APKs
- **Usage**: Called by CI and Release workflows
- **Duration**: ~3-4 minutes

#### `_instrumentation.yml`
- **Purpose**: Run instrumentation tests on Android emulators
- **Runs**: Tests on both phone (Nexus 6) and automotive (automotive_1024p_landscape) profiles
- **Artifacts**: Uploads test results and reports for both device types
- **Usage**: Called by daily.yml and ci.yml (PR only)
- **Duration**: ~10-15 minutes (may vary due to emulator startup and potential flakiness)

#### `_docs.yml`
- **Purpose**: Build API documentation with Dokka
- **Runs**: `./gradlew dokkaGenerate`
- **Artifacts**: Uploads generated HTML documentation
- **Usage**: Called by CI for validation and docs-deploy for publishing
- **Duration**: ~1-2 minutes

### Main Workflows

#### `ci.yml` - Continuous Integration
**Trigger**: Push or Pull Request to `main` or `master` branch

**Purpose**: Validate all changes before they're merged

**Execution Flow**:
```
format (Check formatting)
   |
   v
copyright (Check headers)
   |
   v
   +----------------+----------------+----------------+
   |                |                                 |
   v                v                                 v
test            docs                       coverage
(Unit tests)    (Build API docs)           (Test coverage)
   |                |                                 |
   +----------------+----------------+----------------+
                              |
   +--------------------------+-------------------------+
   |                                                    |
   v                                                    v
build-library                                  build-examples
(Build AAR)                                    (Build 3 APKs)

For Pull Requests only:
   v
instrumented-test (Phone + Automotive)
(Runs in parallel, not blocking)
```

**Jobs**:
1. **format** - Runs Spotless formatting check
2. **copyright** - Verifies MIT license headers
3. **test** - Runs unit tests (depends on format + copyright)
4. **docs** - Builds API documentation with Dokka (depends on format + copyright)
5. **coverage** - Checks test coverage meets 20% minimum (depends on format + copyright)
6. **build-library** - Builds library AAR (depends on format + copyright + test + docs + coverage, runs in parallel with build-examples)
7. **build-examples** - Builds example APKs (depends on format + copyright + test + docs + coverage, runs in parallel with build-library)
8. **instrumented-test** - Runs instrumentation tests on phone and automotive emulators (PR only, runs independently)

**Total Duration**:
- **Maintainer pushes to main**: ~3-4 minutes (no instrumentation tests)
- **External PRs**: ~10-15 minutes (includes instrumentation tests on phone + automotive)

**Benefits**:
- Catches formatting issues early (before expensive builds)
- Parallel builds save time
- PR checks ensure code quality before merge
- Instrumentation tests only run on PRs, reducing CI time for maintainer's direct commits
- External contributors' PRs are thoroughly tested on both phone and automotive platforms

#### `release.yml` - Maven Central Release
**Trigger**: Push of version tag matching `v*.*.*` pattern (e.g., `v0.2.0`)

**Purpose**: Run full CI pipeline, publish to Maven Central, and create GitHub Release

**Execution Flow**:
```
format (Check formatting)
   |
   v
test (Run unit tests)
   |
   v
   +----------------+----------------+
   |                                 |
   v                                 v
build-library              build-examples
   |                                 |
   +-----------------+---------------+
                     |
                     v
                 publish
      (Maven Central + GitHub Release)
```

**Jobs**:
1. **format** - Runs Spotless formatting check
2. **test** - Runs unit tests (depends on format)
3. **build-library** - Builds library AAR (depends on format + test)
4. **build-examples** - Builds example APKs (depends on format + test)
5. **publish** - Publishes to Maven Central and creates GitHub Release (depends on all above)

**Publish Job Details**:
- Validates tag format (must be `vMAJOR.MINOR.PATCH`)
- Extracts version number (e.g., `v0.2.0` -> `0.2.0`)
- Signs artifacts with GPG key
- Uploads to Maven Central OSSRH
- Generates changelog from git history
- Creates GitHub Release with:
  - Version number and changelog
  - Maven/Gradle installation instructions
  - Links to artifacts

**Total Duration**: ~5-7 minutes (including publish)

**Secrets Required**: See "Required GitHub Secrets" section below

#### `daily.yml` - Daily Instrumentation Tests
**Trigger**: Scheduled daily at 09:45 UTC, or manual trigger via workflow_dispatch

**Purpose**: Run comprehensive instrumentation tests on both phone and automotive platforms daily to catch potential issues early

**Execution Flow**:
```
Scheduled trigger (09:45 UTC daily)
   |
   v
instrumented-test (calls _instrumentation.yml)
   |
   v
   +------------------+------------------+
   |                                     |
   v                                     v
Phone Tests                   Automotive Tests
(Nexus 6 profile)            (automotive_1024p_landscape)
```

**Jobs**:
1. **instrumented-test** - Calls the reusable `_instrumentation.yml` workflow

**Total Duration**: ~10-15 minutes (may vary due to emulator startup and flakiness)

**Benefits**:
- Catches device-specific regressions early
- Tests on both phone and automotive form factors
- Runs independently from main CI pipeline
- Manual trigger available for on-demand testing
- Acceptable flakiness since it doesn't block development workflow

#### `docs-deploy.yml` - API Documentation Deployment
**Trigger**: Push to `main` or `master` branch (paths: `openmapview/src/**/*.kt`, `README.md`, `.github/workflows/_docs.yml`, `.github/workflows/docs-deploy.yml`)

**Purpose**: Generate API documentation from KDoc comments and deploy to GitHub Pages

**Execution Flow**:
```
Push to main (filtered by paths)
   |
   v
build-docs (calls _docs.yml)
   |
   v
Build documentation with Dokka
   |
   v
Upload documentation artifact
   |
   v
deploy (Download artifact)
   |
   v
Deploy to GitHub Pages
```

**Jobs**:
1. **build-docs** - Calls the reusable `_docs.yml` workflow to build documentation
2. **deploy** - Downloads artifact and deploys to GitHub Pages

**Configuration**:
- Only runs when Kotlin source files or docs workflows change
- Uses concurrency control (only one deployment at a time)
- Requires GitHub Pages to be enabled with "GitHub Actions" as source

**Total Duration**: ~2-3 minutes

**Published URL**: https://afarber.github.io/OpenMapView/

**Benefits**:
- Documentation always in sync with code
- Automatic updates on every push to main
- No manual publishing needed
- Professional, searchable API reference

## Required GitHub Secrets

For the release workflow to function, the project owner must configure these secrets in the GitHub repository:

**Location**: Settings -> Secrets and variables -> Actions -> Repository secrets

### 1. OSSRH_USERNAME
- **Description**: Sonatype JIRA username
- **Used for**: Authenticating to Maven Central OSSRH
- **How to get**: Created when registering at https://issues.sonatype.org

### 2. OSSRH_PASSWORD
- **Description**: Sonatype JIRA password
- **Used for**: Authenticating to Maven Central OSSRH
- **How to get**: Set when registering at https://issues.sonatype.org

### 3. SIGNING_KEY
- **Description**: Base64-encoded GPG private key
- **Used for**: Signing artifacts (required by Maven Central)
- **How to get**:
  ```bash
  gpg --export-secret-keys YOUR_KEY_ID | base64
  ```
- **Format**: Very long base64 string (this is normal)
- **Security**: Never commit this or share it publicly

### 4. SIGNING_PASSWORD
- **Description**: Passphrase for the GPG key
- **Used for**: Unlocking the GPG private key for signing
- **How to get**: The passphrase set when creating the GPG key
- **Note**: If no passphrase was set, use an empty string

### Automatic Secrets

These secrets are automatically provided by GitHub Actions:

- **GITHUB_TOKEN**: Used for creating GitHub Releases
- **GITHUB_ACTOR**: Used for GitHub Packages (if enabled)

## Common Operations

### Running CI Checks Locally

Before pushing, developers can run the same checks locally:

```bash
# Format check
./gradlew spotlessCheck

# Auto-fix formatting
./gradlew spotlessApply

# Run unit tests
./gradlew :openmapview:test

# Build library
./gradlew :openmapview:assembleRelease

# Build examples
./gradlew :examples:Example01Pan:assembleDebug
./gradlew :examples:Example02Zoom:assembleDebug
./gradlew :examples:Example03Markers:assembleDebug

# Or build everything
./gradlew build
```

### Creating a Release

```bash
# 1. Ensure all changes are committed and pushed
git status
git push

# 2. Create and push a version tag
git tag v0.2.0
git push origin v0.2.0

# 3. Watch the workflow
# Go to: https://github.com/afarber/OpenMapView/actions

# 4. For first release only: manually release staging repository
# Log in to: https://s01.oss.sonatype.org/
# Find your staging repo, click "Close", then "Release"

# 5. Verify publication after 2-4 hours
# https://central.sonatype.com/artifact/de.afarber/openmapview
```

### Viewing Workflow Results

**GitHub Actions Tab**:
https://github.com/afarber/OpenMapView/actions

**Workflow Runs**:
- Green checkmark = Success
- Red X = Failed
- Yellow circle = In progress

**Artifacts**:
Click on a workflow run to see uploaded artifacts:
- Test results (XML)
- Test reports (HTML)
- Library AAR
- Example APKs

Artifacts are retained for 30 days.

### Debugging Failed Workflows

1. Go to Actions tab
2. Click on the failed workflow run
3. Click on the failed job
4. Expand the failed step to see logs

**Common Failures**:
- **Format check fails**: Run `./gradlew spotlessApply` locally and commit
- **Test fails**: Run `./gradlew :openmapview:test` locally to debug
- **Build fails**: Check for compilation errors in logs
- **Publish fails**: Verify GitHub Secrets are configured correctly

## Workflow Execution Examples

### Example: Pull Request CI Check

```
Developer creates PR with code changes
   |
   v
GitHub triggers ci.yml workflow
   |
   v
format job: Check Spotless formatting (30 sec) - PASS
   |
   v
copyright job: Check license headers (30 sec) - PASS
   |
   v
   +-------------+--------------+
   |                            |
   v                            v
test job: Unit tests       coverage job: Test coverage
   (1 min) - PASS               (1 min) - PASS (>20%)
   |                            |
   +-------------+--------------+
                 |
   +-------------+--------------+
   |                            |
   v                            v
build-library: Build AAR  build-examples: Build 3 APKs
   (2 min) - PASS               (3 min) - PASS
   |                            |
   +-------------+--------------+
                 |
                 v
All core checks pass (3-4 min)

Meanwhile (in parallel, PR only):
instrumented-test: Phone + Automotive tests
   (10-15 min) - PASS (may be flaky)
   |
   v
All checks complete -> PR is ready to merge
```

### Example: Release to Maven Central

```
Developer tags v0.2.0 and pushes
   |
   v
GitHub triggers release.yml workflow
   |
   v
format job: Check Spotless formatting - PASS
   |
   v
test job: Run unit tests - PASS
   |
   v
   +-------------+--------------+
   |                            |
   v                            v
build-library: Build AAR  build-examples: Build 3 APKs
   - PASS                       - PASS
   |                            |
   +-------------+--------------+
                 |
                 v
publish job:
   - Validate tag format - PASS
   - Sign artifacts with GPG - PASS
   - Upload to Maven Central - PASS
   - Generate changelog - PASS
   - Create GitHub Release - PASS
   |
   v
Wait 2-4 hours -> Library available on Maven Central
```

## Benefits of This Architecture

### 1. No Code Duplication
**Before** (old workflow): 124 lines with repeated setup code
**After** (new workflow): 27 lines in ci.yml, reusable components

### 2. Parallel Execution
Building library and examples happens simultaneously, saving ~2 minutes per workflow run.

### 3. Easy Maintenance
To upgrade Java version or change Gradle cache settings:
- Update in one reusable workflow file
- Automatically applies to CI and Release workflows

### 4. Composability
The reusable `_instrumentation.yml` workflow demonstrates this principle:
```yaml
# Used by both daily.yml and ci.yml
jobs:
  instrumentation-tests:
    uses: ./.github/workflows/_instrumentation.yml
```
This single workflow definition powers both daily scheduled tests and PR validation tests.

### 5. Flexible Testing Strategy
- **Maintainer commits to main**: Fast feedback (~3-4 min) without instrumentation delays
- **External PRs**: Comprehensive testing (~10-15 min) including phone and automotive instrumentation tests
- **Daily scheduled runs**: Proactive regression detection on both platforms
- **Test flakiness handled gracefully**: Instrumentation tests run independently and don't block merges

### 6. Clear Separation of Concerns
- Format checks catch simple issues fast
- Tests verify correctness
- Builds ensure compilation works
- Publish handles distribution


## Monitoring

### Status Badges

Add to README.md to show workflow status:

```markdown
[![CI](https://github.com/afarber/OpenMapView/workflows/CI/badge.svg)](https://github.com/afarber/OpenMapView/actions)
[![Release](https://github.com/afarber/OpenMapView/workflows/Release%20to%20Maven%20Central/badge.svg)](https://github.com/afarber/OpenMapView/actions)
```

### Email Notifications

GitHub sends email notifications on workflow failures to repository watchers.

Configure in: Settings -> Notifications -> Actions

## Resources

- **GitHub Actions Documentation**: https://docs.github.com/en/actions
- **Reusable Workflows Guide**: https://docs.github.com/en/actions/using-workflows/reusing-workflows
- **Gradle Build Cache**: https://docs.gradle.org/current/userguide/build_cache.html
- **Spotless Plugin**: https://github.com/diffplug/spotless

## Summary

The OpenMapView GitHub Actions setup provides:

- **Fast feedback** - Format checks in 30 seconds
- **Parallel builds** - Save time with concurrent execution
- **Automated releases** - Tag and publish in one step
- **Quality gates** - No broken code reaches main branch
- **Easy maintenance** - Change configuration once
- **Full automation** - From code change to Maven Central

For Maven Central setup details, see [Publishing Guide](PUBLISHING.md).
