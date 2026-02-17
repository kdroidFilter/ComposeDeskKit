<p align="center">
  <img src="art/header.png" alt="Nucleus" />
</p>

# Nucleus

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.kdroidfilter.nucleus?label=Gradle%20Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.kdroidfilter.nucleus)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.kdroidfilter/nucleus.core-runtime?label=Maven%20Central)](https://central.sonatype.com/search?q=io.github.kdroidfilter.nucleus)
[![Pre Merge Checks](https://github.com/kdroidFilter/Nucleus/actions/workflows/pre-merge.yaml/badge.svg)](https://github.com/kdroidFilter/Nucleus/actions/workflows/pre-merge.yaml)
[![License: MIT](https://img.shields.io/github/license/kdroidFilter/Nucleus)](https://github.com/kdroidFilter/Nucleus/blob/main/LICENSE)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0%2B-7F52FF?logo=kotlin&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-macOS%20%7C%20Windows%20%7C%20Linux-blue)

Nucleus is a Gradle plugin for building, packaging, and distributing **JVM desktop applications** as native installers on macOS, Windows, and Linux. It is compatible with any JVM application but optimized for **Compose Desktop**.

## Key Features

- **JDK 25+ AOT Cache** — Dramatically faster cold startup with ahead-of-time class loading cache, no GraalVM required
- **16 modern packaging formats** — DMG, PKG, NSIS, MSI, AppX, Portable, DEB, RPM, AppImage, Snap, Flatpak, and archives
- **Direct store distribution** — Publish to GitHub Releases, S3, or generate store-ready packages (AppX for Microsoft Store, Snap for Snapcraft, Flatpak for Flathub)
- **Code signing & notarization** — Windows (PFX, Azure Trusted Signing) and macOS (Apple Developer ID) with full notarization support
- **Auto-update built-in** — Integrated update metadata generation and runtime update library
- **App sandboxing** — macOS App Sandbox, Windows UWP, Linux Flatpak
- **CI/CD ready** — Reusable composite actions for GitHub Actions with multi-platform matrix builds, universal macOS binaries, and MSIX bundles
- **Deep links & file associations** — Cross-platform protocol handlers and file type registration

## Documentation

Full documentation is available at **[nucleus.kdroidfilter.com](https://nucleus.kdroidfilter.com/)**.

## License

MIT — See [LICENSE](LICENSE).
