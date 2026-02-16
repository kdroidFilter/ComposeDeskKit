# CI/CD

Nucleus provides a `setup-nucleus` composite action and ready-to-use GitHub Actions workflows for building, packaging, and publishing desktop applications across all platforms.

## Overview

A typical release pipeline has four stages:

```
Tag push (v1.0.0)
       │
       ▼
┌──────────────────────────────────┐
│  Build (6 parallel runners)      │
│  Ubuntu amd64 / arm64            │
│  Windows amd64 / arm64           │
│  macOS arm64 / x64               │
└──────────┬───────────────────────┘
           │
     ┌─────┴──────┐
     ▼            ▼
┌─────────┐ ┌──────────┐
│ macOS   │ │ Windows  │
│Universal│ │ MSIX     │
│ Binary  │ │ Bundle   │
└────┬────┘ └────┬─────┘
     │           │
     ▼           ▼
┌──────────────────────────────────┐
│  Publish — GitHub Release        │
│  + Update YML metadata           │
└──────────────────────────────────┘
```

## `setup-nucleus` Action

The `setup-nucleus` composite action (`.github/actions/setup-nucleus`) sets up the complete build environment: JetBrains Runtime 25, packaging tools, Gradle, and Node.js — all cross-platform.

### Usage

```yaml
- uses: ./.github/actions/setup-nucleus
  with:
    jbr-version: '25b176.4'
    packaging-tools: 'true'
    flatpak: 'true'
    snap: 'true'
    setup-gradle: 'true'
    setup-node: 'true'
```

### Inputs

| Input | Default | Description |
|-------|---------|-------------|
| `jbr-version` | `25.0.2b315.62` | JBR version (e.g. `25b176.4`, `25.0.2b315.62`) |
| `jbr-variant` | `jbrsdk` | JBR variant (`jbrsdk`, `jbrsdk_jcef`, etc.) |
| `jbr-download-url` | — | Override complete JBR download URL (bypasses version/variant) |
| `packaging-tools` | `true` | Install xvfb, rpm, fakeroot (Linux only) |
| `flatpak` | `false` | Install Flatpak + Freedesktop SDK 24.08 (Linux only) |
| `snap` | `false` | Install Snapd + Snapcraft (Linux only) |
| `setup-gradle` | `true` | Setup Gradle via `gradle/actions/setup-gradle@v4` |
| `setup-node` | `false` | Setup Node.js (needed for electron-builder) |
| `node-version` | `20` | Node.js version when `setup-node` is `true` |

### Outputs

| Output | Description |
|--------|-------------|
| `java-home` | Path to the JBR installation |

### What It Does

The action automatically:
- Downloads and installs **JBR 25** for the current platform and architecture (Linux x64/aarch64, macOS x64/arm64, Windows x64/arm64)
- Sets `JAVA_HOME` and adds JBR to `PATH`
- Installs Linux packaging tools (`xvfb`, `rpm`, `fakeroot`) and starts Xvfb with `DISPLAY=:99`
- Installs Flatpak + Freedesktop SDK 24.08 (if enabled)
- Installs Snapd + Snapcraft (if enabled)
- Sets up Gradle caching via `gradle/actions/setup-gradle@v4`
- Sets up Node.js (if enabled)

## Pre-Merge Checks

Run tests on every push to `main` and every pull request.

```yaml
# .github/workflows/pre-merge.yaml
name: Pre Merge Checks

on:
  push:
    branches: [main]
  pull_request:
    branches: ['*']

jobs:
  check:
    runs-on: ubuntu-latest
    if: ${{ !contains(github.event.head_commit.message, 'ci skip') }}
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - uses: gradle/actions/setup-gradle@v4

      - run: ./gradlew preMerge --continue
```

> **Note:** Pre-merge checks only run Gradle tests and don't produce packages, so Temurin JDK 21 is sufficient. For builds that produce native packages, use `setup-nucleus` with JBR 25.

## Release Build

Build native packages for all platforms on tag push.

### Build Matrix

```yaml
# .github/workflows/release.yaml
name: Release Desktop App (All Platforms)

on:
  push:
    tags: ['v*']
  workflow_dispatch:

permissions:
  contents: write

concurrency:
  group: release-${{ github.ref }}
  cancel-in-progress: false

jobs:
  build:
    name: Build (${{ matrix.os }} / ${{ matrix.arch }})
    runs-on: ${{ matrix.os }}
    timeout-minutes: 120
    strategy:
      fail-fast: false
      matrix:
        include:
          # Linux
          - os: ubuntu-latest
            arch: amd64
          - os: ubuntu-24.04-arm
            arch: arm64

          # Windows
          - os: windows-latest
            arch: amd64
          - os: windows-11-arm
            arch: arm64

          # macOS
          - os: macos-latest
            arch: arm64
          - os: macos-15-intel
            arch: amd64

    env:
      GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      RELEASE_VERSION: ${{ github.ref_name }}

    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0

      - name: Normalize version for manual runs
        if: github.event_name == 'workflow_dispatch'
        shell: bash
        run: |
          set -euo pipefail
          tag="$(git describe --tags --abbrev=0)"
          echo "RELEASE_VERSION=$tag" >> "$GITHUB_ENV"

      - name: Setup Nucleus
        uses: ./.github/actions/setup-nucleus
        with:
          jbr-version: '25b176.4'
          packaging-tools: 'true'
          flatpak: 'true'
          snap: 'true'
          setup-gradle: 'true'
          setup-node: 'true'

      - name: Build packages
        shell: bash
        run: ./gradlew packageReleaseDistributionForCurrentOS --stacktrace --no-daemon

      - uses: actions/upload-artifact@v4
        with:
          name: release-assets-${{ runner.os }}-${{ matrix.arch }}
          path: |
            build/compose/binaries/**/*.dmg
            build/compose/binaries/**/*.pkg
            build/compose/binaries/**/*.exe
            build/compose/binaries/**/*.msi
            build/compose/binaries/**/*.appx
            build/compose/binaries/**/*.deb
            build/compose/binaries/**/*.rpm
            build/compose/binaries/**/*.AppImage
            build/compose/binaries/**/*.snap
            build/compose/binaries/**/*.flatpak
            build/compose/binaries/**/*.zip
            build/compose/binaries/**/*.tar
            build/compose/binaries/**/*.7z
            build/compose/binaries/**/*.blockmap
            build/compose/binaries/**/signing-metadata.json
            build/compose/binaries/**/packaging-metadata.json
            !build/compose/binaries/**/app/**
            !build/compose/binaries/**/runtime/**
          if-no-files-found: error
```

### Custom JBR URL (per-matrix entry)

You can override the JBR download URL for specific matrix entries. This is useful for custom JBR builds (e.g. with RTL patches):

```yaml
matrix:
  include:
    - os: macos-latest
      arch: arm64
      jbr-download-url: 'https://example.com/jbr-25-macos-aarch64-custom.tar.gz'
    - os: macos-15-intel
      arch: amd64
      jbr-download-url: 'https://example.com/jbr-25-macos-x64-custom.tar.gz'

steps:
  - uses: ./.github/actions/setup-nucleus
    with:
      jbr-version: '25b176.4'
      jbr-download-url: ${{ matrix.jbr-download-url || '' }}
```

### Version from Tag

The `RELEASE_VERSION` environment variable is set from the Git tag. In your `build.gradle.kts`:

```kotlin
val releaseVersion = System.getenv("RELEASE_VERSION")
    ?.removePrefix("v")
    ?.takeIf { it.isNotBlank() }
    ?: "1.0.0"

nucleus.application {
    nativeDistributions {
        packageVersion = releaseVersion
    }
}
```

## Universal macOS Binaries

Merge arm64 and x64 builds into a universal (fat) binary using `lipo`. Nucleus includes a reusable composite action (`build-macos-universal`):

```yaml
  universal-macos:
    name: Universal macOS Binary
    needs: [build]
    if: needs.build.result == 'success'
    runs-on: macos-latest
    timeout-minutes: 30

    steps:
      - uses: actions/checkout@v4
        with:
          sparse-checkout: .github/actions
          fetch-depth: 1

      - uses: actions/setup-node@v4
        with:
          node-version: '20'

      - uses: actions/download-artifact@v4
        with:
          name: release-assets-macOS-arm64
          path: artifacts/release-assets-macOS-arm64

      - uses: actions/download-artifact@v4
        with:
          name: release-assets-macOS-amd64
          path: artifacts/release-assets-macOS-amd64

      - name: Build universal binary
        uses: ./.github/actions/build-macos-universal
        with:
          arm64-path: artifacts/release-assets-macOS-arm64
          x64-path: artifacts/release-assets-macOS-amd64
          output-path: artifacts/release-assets-macOS-universal

      - uses: actions/upload-artifact@v4
        with:
          name: release-assets-macOS-universal
          path: artifacts/release-assets-macOS-universal
          if-no-files-found: error
```

## Windows MSIX Bundle

Combine amd64 and arm64 `.appx` files into a single `.msixbundle`. Nucleus includes a reusable composite action (`build-windows-appxbundle`):

```yaml
  bundle-windows:
    name: Windows APPX Bundle
    needs: [build]
    if: needs.build.result == 'success'
    runs-on: windows-latest
    timeout-minutes: 15

    steps:
      - uses: actions/checkout@v4
        with:
          sparse-checkout: |
            .github/actions
            example/packaging
          fetch-depth: 1

      - uses: actions/download-artifact@v4
        with:
          name: release-assets-Windows-amd64
          path: artifacts/release-assets-Windows-amd64

      - uses: actions/download-artifact@v4
        with:
          name: release-assets-Windows-arm64
          path: artifacts/release-assets-Windows-arm64

      - name: Build APPX Bundle
        uses: ./.github/actions/build-windows-appxbundle
        with:
          amd64-path: artifacts/release-assets-Windows-amd64
          arm64-path: artifacts/release-assets-Windows-arm64
          output-path: artifacts/release-assets-Windows-bundle
          certificate-password: ${{ secrets.WIN_CSC_KEY_PASSWORD }}

      - uses: actions/upload-artifact@v4
        with:
          name: release-assets-Windows-bundle
          path: artifacts/release-assets-Windows-bundle
          if-no-files-found: error
```

## Publish to GitHub Releases

After all builds complete, create a GitHub Release with all artifacts and update YML files. Nucleus includes composite actions for both (`generate-update-yml` and `publish-release`):

```yaml
  publish:
    name: Publish Release
    needs: [build, universal-macos, bundle-windows]
    if: ${{ !cancelled() && needs.build.result == 'success' }}
    runs-on: ubuntu-latest
    timeout-minutes: 30

    env:
      GH_TOKEN: ${{ secrets.GITHUB_TOKEN }}
      GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

    steps:
      - uses: actions/checkout@v4
        with:
          sparse-checkout: .github/actions
          fetch-depth: 1

      - uses: actions/download-artifact@v4
        with:
          path: artifacts
          pattern: release-assets-*

      - name: Determine version and channel
        shell: bash
        run: |
          set -euo pipefail
          TAG="${GITHUB_REF_NAME}"
          VERSION="${TAG#v}"
          echo "TAG=$TAG" >> "$GITHUB_ENV"
          echo "VERSION=$VERSION" >> "$GITHUB_ENV"

          if [[ "$VERSION" == *"-alpha"* ]]; then
            echo "CHANNEL=alpha" >> "$GITHUB_ENV"
            echo "RELEASE_TYPE=prerelease" >> "$GITHUB_ENV"
          elif [[ "$VERSION" == *"-beta"* ]]; then
            echo "CHANNEL=beta" >> "$GITHUB_ENV"
            echo "RELEASE_TYPE=prerelease" >> "$GITHUB_ENV"
          else
            echo "CHANNEL=latest" >> "$GITHUB_ENV"
            echo "RELEASE_TYPE=release" >> "$GITHUB_ENV"
          fi

      - name: Generate update YML files
        uses: ./.github/actions/generate-update-yml
        with:
          artifacts-path: artifacts
          version: ${{ env.VERSION }}
          channel: ${{ env.CHANNEL }}

      - name: Publish release
        uses: ./.github/actions/publish-release
        with:
          artifacts-path: artifacts
          tag: ${{ env.TAG }}
          release-type: ${{ env.RELEASE_TYPE }}
```

## Publish Plugin to Gradle Portal

Automatically publish the plugin to the Gradle Plugin Portal on tag push:

```yaml
# .github/workflows/publish-plugin.yaml
name: Publish Plugin

on:
  push:
    tags: ['*']

jobs:
  publish:
    runs-on: ubuntu-latest
    env:
      GRADLE_PUBLISH_KEY: ${{ secrets.GRADLE_PUBLISH_KEY }}
      GRADLE_PUBLISH_SECRET: ${{ secrets.GRADLE_PUBLISH_SECRET }}

    steps:
      - uses: actions/checkout@v4
      - uses: gradle/actions/setup-gradle@v4

      - name: Run checks
        run: ./gradlew preMerge --continue

      - name: Publish to Gradle Plugin Portal
        if: success()
        run: ./gradlew -p plugin-build setupPluginUploadFromEnvironment publishPlugins
```

### Required Secrets

| Secret | Description |
|--------|-------------|
| `GRADLE_PUBLISH_KEY` | Gradle Plugin Portal API key |
| `GRADLE_PUBLISH_SECRET` | Gradle Plugin Portal API secret |

Get these from [plugins.gradle.org/user/settings](https://plugins.gradle.org/user/settings).

## Required Secrets Summary

| Secret | Used By | Description |
|--------|---------|-------------|
| `GITHUB_TOKEN` | Release workflow | Auto-provided by GitHub Actions |
| `GRADLE_PUBLISH_KEY` | Plugin publish | Gradle Plugin Portal key |
| `GRADLE_PUBLISH_SECRET` | Plugin publish | Gradle Plugin Portal secret |
| `WIN_CSC_LINK` | Build (Windows) | Base64-encoded `.pfx` certificate |
| `WIN_CSC_KEY_PASSWORD` | Build (Windows) | Certificate password |
| `MACOS_CERTIFICATE` | Build (macOS) | Base64-encoded `.p12` certificate |
| `MACOS_CERTIFICATE_PWD` | Build (macOS) | Certificate password |
| `KEYCHAIN_PWD` | Build (macOS) | Temp keychain password |

## Composite Actions Reference

Nucleus includes four reusable composite actions in `.github/actions/`:

| Action | Description |
|--------|-------------|
| `setup-nucleus` | Setup JBR 25, packaging tools, Gradle, Node.js |
| `build-macos-universal` | Merge arm64 + x64 into universal binary via `lipo` |
| `build-windows-appxbundle` | Combine amd64 + arm64 `.appx` into `.msixbundle` |
| `generate-update-yml` | Generate `latest-*.yml` / `beta-*.yml` / `alpha-*.yml` metadata |
| `publish-release` | Create GitHub Release with all artifacts |

## Tips

- **JBR 25 required**: Use `setup-nucleus` for all packaging builds — it installs JBR 25 automatically
- **Concurrency**: Use `concurrency` to prevent parallel releases
- **fail-fast: false**: Continue building other platforms if one fails
- **Timeout**: Set generous timeouts (120min) for Flatpak/Snap builds
- **Caching**: `setup-nucleus` enables Gradle caching automatically via `gradle/actions/setup-gradle@v4`
- **Sparse checkout**: Post-build jobs only need the `.github/actions` directory
- **workflow_dispatch**: Add it as a trigger to allow re-running a release manually
