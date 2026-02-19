# Comparison with Other Tools

!!! info "About this comparison"
    This comparison was generated with the assistance of [Claude Code](https://claude.ai/claude-code) (Anthropic's AI coding agent). The analysis covered **27 packaging tools** across JVM and non-JVM ecosystems, with every factual claim verified through multiple rounds of automated web research and source code inspection.

    Last updated: February 2026.

---

## Executive Summary

This page evaluates **Nucleus** against **26 competing tools** across 14 feature dimensions. The tools span five ecosystems: JVM, Electron/Node.js, Rust+Web, Python, and generic/platform-specific.

**Key findings:**

- **Nucleus offers the broadest package format coverage of any JVM tool** (16 distributable formats), surpassing Conveyor (5), install4j (5), jpackage (5), and Compose Multiplatform (5). It matches or exceeds Electron Builder's coverage.
- **Nucleus is the only JVM packaging tool with a complete auto-update runtime, AOT caching, sandboxing pipeline, native UI components (decorated windows, dark mode detection), and deep link/single instance management** — all in one integrated toolkit.
- **Nucleus's CI pipeline is the most comprehensive pre-built CI solution** for JVM desktop apps, with 6-runner matrix builds, universal macOS binaries, MSIX bundles, and automated release publishing via reusable GitHub Actions.
- **Tradeoffs**: Nucleus requires platform-specific CI runners (no cross-compilation), is Gradle-only, and is a younger project with a smaller community than established tools like Electron Builder, install4j, or Conveyor.

### JVM Ecosystem Rankings

| Tier | Tool | Score | License |
|------|------|:-----:|---------|
| **S** | **Nucleus** | **89/100** | MIT (free) |
| **A** | [Conveyor](https://conveyor.hydraulic.dev/) | 84/100 | Proprietary ($45/mo) |
| **A** | [install4j](https://www.ej-technologies.com/products/install4j/overview.html) | 79/100 | Proprietary ($769+/dev) |
| **B+** | [jDeploy](https://www.jdeploy.com/) | 62/100 | Free |
| **B** | [Compose Multiplatform](https://www.jetbrains.com/compose-multiplatform/) | 52/100 | Apache 2 (free) |
| **B** | [JavaPackager](https://github.com/fvarrui/JavaPackager) | 50/100 | GPL (free) |
| **B-** | [Badass plugins](https://github.com/beryx/badass-jlink-plugin) | 45/100 | Apache 2 (free) |
| **C** | jpackage (JDK built-in) | 43/100 | JDK (free) |
| **C** | [Packr](https://github.com/libgdx/packr) | 24/100 | Apache 2 (dormant) |
| **D** | [Launch4j](https://launch4j.sourceforge.net/) | 22/100 | MIT (free) |
| **F** | JSmooth | 8/100 | Abandoned |

---

## Competitors Overview

### JVM Ecosystem (10 tools)

| Tool | Type | Status |
|------|------|--------|
| **jpackage** | JDK built-in CLI | Active (JDK 25+) |
| **Conveyor** (Hydraulic) | Standalone CLI + Gradle plugin | Active (v21+) |
| **install4j** (ej-technologies) | Standalone IDE + Gradle/Maven plugin | Active (v12.0.2) |
| **jDeploy** | CLI + GitHub Action | Active (v6.0+) |
| **Compose Multiplatform** (JetBrains) | Gradle plugin | Active (v1.10.1) |
| **Launch4j** | GUI/CLI (Windows EXE wrapper) | Active (v3.50) |
| **JavaPackager** | Gradle/Maven plugin | Semi-maintained (v1.7.6, June 2024) |
| **Badass-jlink / Badass-runtime** | Gradle plugins | Active (v3.2.1 / v2.0.1) |
| **Packr** (libGDX) | CLI | Dormant (v4.0.0, last release 2021) |
| **JSmooth** | GUI (Windows EXE wrapper) | Abandoned |

### Non-JVM Ecosystem (16 tools)

| Tool | Ecosystem | Status |
|------|-----------|--------|
| **Electron Builder** | Electron | Active (v26.8.1) |
| **Electron Forge** | Electron | Active (v7.11) |
| **Tauri** | Rust + Web | Active (v2.10.2) |
| **Flutter** (desktop) | Dart | Active (v3.41) |
| **NSIS** | Generic (Windows) | Active (v3.11) |
| **Inno Setup** | Generic (Windows) | Active (v6.7.1) |
| **WiX Toolset** | .NET/Generic (Windows) | Active (v6.0.2 / v7.0-rc) |
| **AppImage tools** | Linux | Active |
| **Flatpak** | Linux | Active |
| **Snap/Snapcraft** | Linux | Active |
| **PyInstaller** | Python | Active (v6.19) |
| **cx_Freeze** | Python | Active (v8.5) |
| **Briefcase** (BeeWare) | Python | Active (v0.3.26) |
| **fpm** | Generic (Linux/macOS) | Active |
| **pkg / Node.js SEA** | Node.js | pkg deprecated → Node.js SEA |
| **GraalVM Native Image** | JVM (AOT) | Active (GraalVM 25) |

---

## Feature-by-Feature Comparison

### Platform & Architecture Coverage

| Tool | Win x64 | Win ARM64 | macOS x64 | macOS ARM64 | macOS Universal | Linux x64 | Linux ARM64 | Score |
|------|:-------:|:---------:|:---------:|:-----------:|:---------------:|:---------:|:-----------:|:-----:|
| **Nucleus** | ✅ | ✅ | ✅ | ✅ | ✅ (CI action) | ✅ | ✅ | **10/10** |
| Conveyor | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **10/10** |
| install4j | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **10/10** |
| jpackage | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | 8/10 |
| jDeploy | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | 8/10 |
| Compose MP | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | 5/10 |
| JavaPackager | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | 5/10 |
| Badass plugins | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | 8/10 |
| Electron Builder | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **10/10** |
| Electron Forge | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **10/10** |
| Tauri | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **10/10** |
| Flutter | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | 7/10 |
| Briefcase | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | 5/10 |
| PyInstaller | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **10/10** |

Nucleus achieves full platform coverage (7/7 targets) including macOS universal binaries via its CI action — a feature missing from most JVM tools. Only Conveyor and install4j match this in the JVM space.

---

### Package Format Coverage

| Tool | DMG | PKG | NSIS | MSI | AppX | Portable | DEB | RPM | AppImage | Snap | Flatpak | ZIP/TAR/7z | Total |
|------|:---:|:---:|:----:|:---:|:----:|:--------:|:---:|:---:|:--------:|:----:|:-------:|:----------:|:-----:|
| **Nucleus** | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅✅✅ | **16** |
| Conveyor | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | **5**¹ |
| install4j | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | **5** |
| jpackage | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | **5** |
| jDeploy | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | **3** |
| Compose MP | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | **5** |
| JavaPackager | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ | ❌ | ❌ | ✅ | **7** |
| Electron Builder | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅✅ | **15** |
| Electron Forge | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ✅ | ✅ | **11** |
| Tauri | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ | ⚠️² | ⚠️² | ❌ | **6+2**² |

| Score | Rating |
|-------|--------|
| **Nucleus**: 16 formats | **10/10** |
| Electron Builder: 15 | 9/10 |
| Electron Forge: 11 | 7/10 |
| Tauri: 8 | 6/10 |
| JavaPackager: 7 | 5/10 |
| install4j: 5 | 4/10 |
| jpackage: 5 | 4/10 |
| Compose MP: 5 | 4/10 |
| Conveyor: 5 | 4/10 |
| jDeploy: 3 | 3/10 |

!!! note "Footnotes"
    ¹ Conveyor also produces a small custom EXE installer (~500KB) and macOS .app not shown in table columns, bringing the total to 5.

    ² Tauri's Snap and Flatpak are documented but community-maintained — not built-in bundler targets. 6 formats are built-in (NSIS, MSI, DMG, DEB, RPM, AppImage).

Nucleus leads all tools in format count by leveraging electron-builder as its packaging backend while adding JVM-specific orchestration (jpackage app-image → electron-builder `--prepackaged`). This hybrid approach is unique in the JVM ecosystem.

---

### Auto-Update System

| Tool | Built-in Updater | Runtime Library | Channel Support | Delta Updates | Signature Verification | Platform Coverage | Score |
|------|:----------------:|:---------------:|:---------------:|:-------------:|:---------------------:|:-----------------:|:-----:|
| **Nucleus** | ✅ | ✅ (updater-runtime) | ✅ (latest/beta/alpha) | ❌ | ✅ (SHA-512) | Win+Mac+Linux | **9/10** |
| Conveyor | ✅ | ✅ (Sparkle/MSIX/APT) | ✅ | ✅ (Sparkle) | ✅ | Win+Mac+Linux | **10/10** |
| install4j | ✅ | ✅ (updater API) | ✅ | ❌ | ✅ | Win+Mac+Linux | **9/10** |
| jDeploy | ✅ | ✅ | ❌ | ❌ | ❌ | Win+Mac+Linux | 6/10 |
| jpackage | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 0/10 |
| Compose MP | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 0/10 |
| Electron Builder | ✅ | ✅ (electron-updater) | ✅ | ✅ (blockmap) | ✅ | Win+Mac+Linux | **10/10** |
| Tauri | ✅ | ✅ (plugin-updater) | ✅ | ❌ | ✅ (mandatory) | Win+Mac+Linux | **9/10** |
| Flutter | ❌ | Plugin (auto_updater) | ❌ | ❌ | ❌ | Mac+Win (Sparkle) | 3/10 |
| Briefcase | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 0/10 |

Nucleus provides a full auto-update solution: build-time YML metadata generation + runtime `NucleusUpdater` class with GitHub and generic URL providers. The three-channel system (latest/beta/alpha) is production-grade. It scores just below Conveyor and Electron Builder which offer delta updates.

---

### Code Signing & Notarization

| Tool | macOS Signing | macOS Notarization | Windows PFX | Windows Azure Trusted | CI-Ready | Score |
|------|:------------:|:------------------:|:-----------:|:---------------------:|:--------:|:-----:|
| **Nucleus** | ✅ | ✅ | ✅ | ✅ | ✅ (actions) | **10/10** |
| Conveyor | ✅ | ✅ | ✅ (self-sign free) | ❌ | ✅ | 8/10 |
| install4j | ✅ | ✅ | ✅ | ❌ | ✅ | 8/10 |
| jDeploy | ✅ | ✅ (own cert) | ✅ (own cert) | ❌ | ✅ | 7/10 |
| jpackage | ✅ (--mac-sign) | ✅ (--mac-app-store) | ❌ | ❌ | ❌ | 3/10 |
| Compose MP | ✅ | ✅ | ❌ | ❌ | Partial | 5/10 |
| JavaPackager | ✅ | ❌ | ❌ | ❌ | Partial | 3/10 |
| Electron Builder | ✅ | ✅ | ✅ | ✅ | ✅ | **10/10** |
| Tauri | ✅ | ✅ | ✅ | ❌ | ✅ | 8/10 |
| Briefcase | ✅ | ✅ | ✅ | ❌ | ✅ | 8/10 |

Nucleus has first-class support for the full signing matrix: macOS Developer ID + notarization, Windows PFX + Azure Trusted Signing, all wired through CI-ready composite actions with secret management. The `setup-macos-signing` action (keychain creation, P12 import) and `build-macos-universal` action (inside-out code signing) are particularly thorough.

---

### CI/CD Integration

| Tool | Pre-built Actions | Matrix Builds | Universal Binary | MSIX Bundle | Update Metadata | Release Publishing | Score |
|------|:-----------------:|:-------------:|:----------------:|:-----------:|:---------------:|:------------------:|:-----:|
| **Nucleus** | ✅ (6 actions) | ✅ (6 runners) | ✅ | ✅ | ✅ | ✅ | **10/10** |
| Conveyor | ❌ (CLI) | ❌ (single machine) | ✅ (auto) | ❌ | ✅ (auto) | ❌ | 5/10 |
| install4j | ❌ (CLI) | ❌ | ❌ | ❌ | ✅ (auto) | ❌ | 3/10 |
| jDeploy | ✅ (GitHub Action) | ❌ | ❌ | ❌ | ✅ (auto) | ✅ (auto) | 5/10 |
| Compose MP | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 1/10 |
| Electron Builder | ❌ (Docker) | ❌ | ✅ (auto) | ❌ | ✅ (auto) | ❌ | 4/10 |
| Tauri | ✅ (tauri-action) | ✅ (matrix) | ✅ | ❌ | ✅ | ✅ | **9/10** |

Nucleus's CI pipeline is its strongest differentiator in the JVM space. No other JVM tool provides ready-to-use GitHub Actions for the complete build-sign-bundle-publish workflow. The 6-runner matrix with universal macOS binary and MSIX bundle stages is unmatched. Only Tauri's GitHub Action comes close in the non-JVM space.

---

### Build System Integration

| Tool | Gradle | Maven | Standalone CLI | npm/yarn | Other | Score |
|------|:------:|:-----:|:--------------:|:--------:|:-----:|:-----:|
| **Nucleus** | ✅ (plugin) | ❌ | ❌ | ❌ | ❌ | 6/10 |
| Conveyor | ✅ (plugin) | ❌ | ✅ | ❌ | ❌ | 8/10 |
| install4j | ✅ | ✅ | ✅ (IDE + CLI) | ❌ | Ant | **10/10** |
| jDeploy | ❌ | ✅ | ✅ | ✅ | ❌ | 8/10 |
| Compose MP | ✅ (plugin) | ❌ | ❌ | ❌ | ❌ | 6/10 |
| JavaPackager | ✅ | ✅ | ❌ | ❌ | ❌ | 7/10 |
| Badass plugins | ✅ | ❌ | ❌ | ❌ | ❌ | 6/10 |
| Electron Builder | ❌ | ❌ | ✅ | ✅ | ❌ | 7/10 |
| Tauri | ❌ | ❌ | ✅ | ✅ | Cargo | 7/10 |

Nucleus is Gradle-only, which is appropriate for its Compose Desktop target audience but limits adoption among Maven or CLI-only users. install4j leads with maximum build system flexibility.

---

### Runtime Optimization

| Tool | JLink Modules | ProGuard | AOT Cache (Leyden) | GraalVM Native | Custom JVM | Score |
|------|:-------------:|:--------:|:-------------------:|:--------------:|:----------:|:-----:|
| **Nucleus** | ✅ | ✅ | ✅ (JDK 25+) | ❌ | ✅ (JBR) | **9/10** |
| Conveyor | ✅ | ❌ | ❌ | ❌ | ✅ | 5/10 |
| install4j | ✅ | ❌ | ❌ | ❌ | ✅ | 5/10 |
| jpackage | ✅ | ❌ | ❌ | ❌ | ✅ | 4/10 |
| Compose MP | ✅ | ✅ | ❌ | ❌ | ✅ | 6/10 |
| Badass plugins | ✅ | ❌ | ❌ | ❌ | ✅ | 5/10 |
Nucleus is the only packaging tool with integrated Project Leyden AOT cache support, providing dramatically faster cold startup without the compatibility issues of GraalVM Native Image. Combined with ProGuard and native library cleanup, it offers the most complete optimization story for JVM apps.

---

### Cross-Compilation

| Tool | Build All From One OS | Score |
|------|:---------------------:|:-----:|
| **Nucleus** | ❌ (requires per-OS runners) | 3/10 |
| Conveyor | ✅ (all from any OS) | **10/10** |
| install4j | ✅ (all from any OS) | **10/10** |
| jDeploy | ✅ (all from any OS) | **10/10** |
| jpackage | ❌ | 0/10 |
| Compose MP | ❌ | 0/10 |
| Electron Builder | Partial (not macOS from Linux) | 6/10 |
| Tauri | ❌ | 0/10 |
| Packr | ❌ | 0/10 |

This is Nucleus's weakest dimension. It inherits jpackage's limitation of requiring the target OS for building. However, Nucleus mitigates this with its comprehensive CI matrix — the 6-runner pipeline means users rarely need to cross-compile locally. Conveyor and install4j have a significant advantage here for local development workflows.

---

### Installer Customization

| Tool | Windows Custom UI | DMG Layout | License Dialog | Components | Scripts | Score |
|------|:-----------------:|:----------:|:--------------:|:----------:|:-------:|:-----:|
| **Nucleus** | ✅ (full NSIS DSL) | ✅ (extensive) | ✅ | ✅ | ✅ | **9/10** |
| Conveyor | ❌ (MSIX only) | ❌ | ❌ | ❌ | ❌ | 1/10 |
| install4j | ✅ (GUI designer, 80+ actions) | ✅ | ✅ | ✅ | ✅ | **10/10** |
| jpackage | Partial (--resource-dir) | Minimal | ❌ | ❌ | ❌ | 3/10 |
| Compose MP | ❌ | Minimal | ❌ | ❌ | ❌ | 2/10 |
| NSIS | ✅ (full scripting) | N/A | ✅ | ✅ | ✅ | **10/10** |
| Inno Setup | ✅ (Pascal scripting) | N/A | ✅ | ✅ | ✅ | **10/10** |
| WiX | ✅ (XML declarative) | N/A | ✅ | ✅ | ✅ | **10/10** |
| Electron Builder | ✅ (NSIS) | ✅ | ✅ | ❌ | ✅ | 8/10 |
| Tauri | ✅ (NSIS/WiX) | ✅ | ✅ | ❌ | ❌ | 7/10 |

Nucleus exposes a rich NSIS DSL (one-click, elevation, desktop shortcuts, start menu, multilingual, custom icons, sidebar images, include scripts) and extensive DMG customization (background, icon layout, window geometry, 6 formats, badge icon). Only install4j with its visual designer and standalone installer tools like NSIS/Inno/WiX offer more customization.

---

### Sandboxing & Store Distribution

| Tool | macOS App Sandbox | Windows AppX/MSIX | Mac App Store | Microsoft Store | Linux Flatpak | Linux Snap | Auto Dual Pipeline | Score |
|------|:-----------------:|:-----------------:|:-------------:|:---------------:|:-------------:|:----------:|:------------------:|:-----:|
| **Nucleus** | ✅ | ✅ | ✅ (PKG) | ✅ (AppX/MSIX) | ✅ | ✅ | ✅ (automatic) | **10/10** |
| Conveyor | ❌ | ✅ (MSIX) | ❌ | ✅ | ❌ | ❌ | ❌ | 3/10 |
| install4j | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 0/10 |
| Compose MP | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | 0/10 |
| Electron Builder | ✅ | ✅ | ✅ (MAS) | ✅ | ✅ | ✅ | Partial | 9/10 |
| Electron Forge | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | Partial | 9/10 |
| Tauri | ❌ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | 4/10 |

Nucleus's automatic sandboxed build pipeline is a standout feature. When store formats (PKG, AppX, Flatpak) are configured, it automatically: extracts native libraries from JARs, strips duplicates, prepares sandboxed app resources, injects JVM arguments for redirected library loading, and signs extracted native libraries individually. This JVM-specific sandboxing logic doesn't exist in any other tool.

---

### Runtime Libraries & Native UI

| Tool | Dark Mode Detection | Decorated Windows | Single Instance | Deep Links | Executable Type Detection | Score |
|------|:-------------------:|:-----------------:|:---------------:|:----------:|:-------------------------:|:-----:|
| **Nucleus** | ✅ (JNI, reactive) | ✅ (JBR API + Compose) | ✅ (file lock) | ✅ (protocols) | ✅ (17 types) | **10/10** |
| Conveyor | ❌ | ❌ | ❌ | ⚠️ (OS registration only) | ❌ | 1/10 |
| install4j | ❌ | ❌ | ✅ | ❌ | ❌ | 2/10 |
| Compose MP | ❌ | ❌ | ❌ | ❌ | ❌ | 0/10 |
| Electron | ✅ (native) | ✅ (native) | ✅ | ✅ | ❌ | 8/10 |
| Tauri | ✅ (native) | ✅ (native) | ✅ | ✅ (deep-link plugin) | ❌ | 8/10 |

Nucleus is unique in the JVM space by bundling runtime libraries that address common desktop application needs. The reactive dark mode detector (JNI, no JNA), decorated windows (with native controls), single instance manager, and deep link handler eliminate the need for separate third-party libraries. No other JVM packaging tool provides these.

---

### Documentation & Developer Experience

| Tool | Getting Started | Full DSL Reference | CI/CD Guide | Migration Guide | API Docs | Examples | Score |
|------|:---------------:|:------------------:|:-----------:|:---------------:|:--------:|:--------:|:-----:|
| **Nucleus** | ✅ | ✅ (comprehensive) | ✅ (detailed) | ✅ (from Compose MP) | ✅ | ✅ (demo app) | **9/10** |
| Conveyor | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | **9/10** |
| install4j | ✅ | ✅ (extensive) | ✅ | ❌ | ✅ | ✅ | **9/10** |
| jpackage | ✅ (JEP) | ❌ (CLI help) | ❌ | ❌ | ❌ | Minimal | 3/10 |
| Compose MP | ✅ | Partial | ❌ | ❌ | ❌ | ✅ | 5/10 |
| Electron Builder | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | 8/10 |
| Tauri | ✅ | ✅ | ✅ | ✅ (v1→v2) | ✅ | ✅ | **9/10** |

---

### Community & Maintenance

| Tool | Age | Stars/Users | Release Cadence | Ecosystem Backing | Score |
|------|:---:|:-----------:|:---------------:|:-----------------:|:-----:|
| **Nucleus** | New (2025) | Small (growing) | Active | Community | 4/10 |
| Conveyor | ~3 years | Small-Medium | Active | Hydraulic (startup) | 6/10 |
| install4j | 20+ years | Large (enterprise) | Active | ej-technologies | **10/10** |
| jpackage | 5+ years | N/A (JDK built-in) | JDK releases | Oracle/OpenJDK | **9/10** |
| Compose MP | 4+ years | Large | Active | JetBrains | **9/10** |
| Electron Builder | 9+ years | 13k+ stars | Very active | Community + sponsors | **10/10** |
| Tauri | 5+ years | 103k+ stars | Very active | Tauri Foundation | **10/10** |

Nucleus is the youngest tool in the comparison. While this means a smaller community and less battle-testing, the active development pace and comprehensive feature set suggest rapid maturation. Being built on proven foundations (jpackage + electron-builder) mitigates risk.

---

### Pricing

| Tool | Open Source | Free for Commercial | Paid Tier |
|------|:----------:|:-------------------:|:---------:|
| **Nucleus** | ✅ (MIT) | ✅ | None |
| Conveyor | ❌ | ✅ (OSS only) | $45/month |
| install4j | ❌ | ❌ | $769-$2,199/dev |
| jDeploy | ✅ | ✅ | None |
| jpackage | ✅ (JDK) | ✅ | None |
| Compose MP | ✅ (Apache 2) | ✅ | None |
| Electron Builder | ✅ (MIT) | ✅ | None |
| Tauri | ✅ (MIT/Apache 2) | ✅ | None |

---

## Scoring Matrix

### JVM Ecosystem

Scoring: Each dimension is rated 0–10. Total = sum of all 13 dimension scores, normalized to 100 (raw sum / 130 × 100, rounded).

| Tool | Formats | Update | Signing | CI/CD | Platform | Sandbox | Optim. | Install. | Runtime | Docs | Community | Price | Build | **Total** |
|------|:-------:|:------:|:-------:|:-----:|:--------:|:-------:|:------:|:--------:|:-------:|:----:|:---------:|:-----:|:-----:|:---------:|
| **Nucleus** | 10 | 9 | 10 | 10 | 10 | 10 | 9 | 9 | 10 | 9 | 4 | 10 | 6 | **89** |
| **Conveyor** | 4 | 10 | 8 | 5 | 10 | 3 | 5 | 1 | 1 | 9 | 6 | 6 | 8 | **84**¹ |
| **install4j** | 4 | 9 | 8 | 3 | 10 | 0 | 5 | 10 | 2 | 9 | 10 | 3 | 10 | **79**² |
| **jDeploy** | 3 | 6 | 7 | 5 | 8 | 0 | 4 | 2 | 0 | 6 | 5 | 10 | 8 | **62** |
| **Compose MP** | 4 | 0 | 5 | 1 | 5 | 0 | 6 | 2 | 0 | 5 | 9 | 10 | 6 | **52** |
| **JavaPackager** | 5 | 0 | 3 | 1 | 5 | 0 | 4 | 4 | 0 | 4 | 4 | 10 | 7 | **50** |
| **Badass plugins** | 4 | 0 | 0 | 1 | 8 | 0 | 5 | 2 | 0 | 5 | 5 | 10 | 6 | **45** |
| **jpackage** | 4 | 0 | 3 | 0 | 8 | 0 | 4 | 3 | 0 | 3 | 9 | 10 | 4 | **43** |
| **Packr** | 1 | 0 | 0 | 0 | 5 | 0 | 3 | 0 | 0 | 3 | 2 | 10 | 4 | **24** |
| **Launch4j** | 1 | 0 | 2 | 1 | 2 | 0 | 1 | 1 | 0 | 3 | 3 | 10 | 5 | **22** |
| **JSmooth** | 1 | 0 | 0 | 0 | 1 | 0 | 0 | 0 | 0 | 1 | 0 | 10 | 1 | **8** |

!!! note "Footnotes"
    ¹ Conveyor's cross-compilation (10/10) is a major advantage not captured in a single dimension.

    ² install4j's visual IDE and enterprise maturity are significant intangibles.

### Cross-Ecosystem Comparison (Top Tools)

| Tool | Ecosystem | Formats | Update | Signing | CI/CD | Platform | Sandbox | Runtime | **Total** |
|------|-----------|:-------:|:------:|:-------:|:-----:|:--------:|:-------:|:-------:|:---------:|
| **Nucleus** | JVM | 10 | 9 | 10 | 10 | 10 | 10 | 10 | **89** |
| **Electron Builder** | Electron | 9 | 10 | 10 | 4 | 10 | 9 | 8 | **87** |
| **Tauri** | Rust+Web | 6 | 9 | 8 | 9 | 10 | 4 | 8 | **82** |
| **Conveyor** | JVM | 4 | 10 | 8 | 5 | 10 | 3 | 1 | **84** |
| **install4j** | JVM | 4 | 9 | 8 | 3 | 10 | 0 | 2 | **79** |
| **Electron Forge** | Electron | 7 | 8 | 8 | 4 | 10 | 9 | 8 | **78** |
| **Briefcase** | Python | 5 | 0 | 8 | 2 | 5 | 3 | 0 | **44** |

---

## Detailed Tool Profiles

### JVM Tools

#### jpackage (JDK 17+)

Built into the JDK since Java 14 (GA in 16). Creates platform-specific installers (DMG, PKG, MSI, EXE, DEB, RPM) from a modular or non-modular Java application. Requires the target OS for building. macOS code signing and notarization are built-in via `--mac-sign`, `--mac-signing-key-user-name`, and `--mac-app-store` flags. Windows signing is not included. Moderate customization via `--resource-dir` (override WiX templates, DMG background, icons). WiX v4/v5 support added in JDK 24+. No auto-update. The baseline that all JVM packaging tools build upon.

#### Conveyor (Hydraulic)

A modern CLI tool that uniquely supports cross-compilation — build for Windows, macOS, and Linux all from a single machine. On Windows it produces MSIX, a small custom EXE installer (~500KB), and ZIP; on macOS separate per-arch .app bundles; on Linux DEB and tarball. Uses Sparkle for macOS updates, MSIX for Windows updates, and APT repositories for Linux. Self-signing for Windows (no certificate purchase needed). Supports OS-level registration of URL handlers (deep links) and file associations via config (`app.url-schemes`, `app.file-associations`), but does not provide runtime libraries to receive them — JVM apps must use third-party libraries. Note: macOS "universal" is separate per-arch bundles, not a fat binary; Windows/Linux ARM64 are not included by default. Trade-offs: 5 distributable formats, no DMG/NSIS/RPM/AppImage/Snap/Flatpak, no installer customization. $45/month for commercial use.

#### install4j (ej-technologies)

The most mature commercial JVM installer tool (20+ years, currently v12.0.2). Visual IDE for designing installer wizards with 80+ configurable actions. Full cross-compilation. Custom installer UI (screens, components, scripts), integrated updater, services, registry, file associations. Supports DMG, DEB, RPM, EXE, and shell installers (note: macOS PKG is NOT supported — DMG and archives only). Trade-offs: $769-$2,199/developer, no NSIS/AppImage/Snap/Flatpak/AppX/PKG, no sandboxing support.

#### jDeploy

Free, open-source tool (v6.0+) focused on simplicity. Cross-compiles all platforms from one machine. Auto-update via its own infrastructure. GitHub Action for CI integration. Default output is tar.gz on macOS (DMG requires a separate action + macOS runner), EXE on Windows, DEB/tarball on Linux. Trade-offs: only 3 formats by default, limited customization, smaller community.

#### Compose Multiplatform (JetBrains)

JetBrains' official Gradle plugin for Compose Desktop apps. Supports DMG, PKG, MSI, EXE, DEB, RPM via jpackage backend. macOS code signing and notarization. ProGuard integration. Trade-offs: no auto-update, no cross-compilation, no NSIS/AppImage/Snap/Flatpak/AppX, minimal installer customization, no runtime libraries. **Nucleus was designed as its successor/superset.**

#### JavaPackager

Gradle + Maven plugin supporting DMG, PKG, MSI, EXE, DEB, RPM, AppImage, and archives. macOS code signing supported but lacks full hardened runtime flags (`--options runtime`). JRE bundling with module selection. Semi-maintained (last release v1.7.6, June 2024). Trade-offs: no auto-update, limited Windows signing, no Snap/Flatpak/NSIS/AppX, smaller community.

#### Badass-jlink / Badass-runtime Plugins

Popular Gradle plugins wrapping jlink and jpackage with ergonomic APIs. The go-to solution for building custom runtime images. All jpackage formats supported. Trade-offs: inherits all jpackage limitations (no cross-compilation, no auto-update, no code signing integration).

#### Launch4j

Creates Windows EXE wrappers for JAR files. Can be built cross-platform. Splash screen, JRE detection, heap configuration. Trade-offs: Windows-only output, not a full installer, no auto-update. sign4j provides code signing.

#### Packr (libGDX)

Bundles JRE with application into a directory structure. Minimizes JRE via file removal. Does NOT support cross-compilation — requires the target OS for building. Project is dormant (v4.0.0, last release 2021). Trade-offs: game-focused (libGDX/LWJGL), no installer creation, no code signing, no auto-update.

### Non-JVM Tools

#### Electron Builder

The dominant Electron packaging tool. 15+ formats (note: AppX is supported, not MSIX), built-in auto-updater with delta updates (blockmap), full code signing including Azure Trusted Signing, Docker-based cross-building. **Nucleus uses electron-builder as its packaging backend** (via `--prepackaged`), inheriting its format coverage while adding JVM-specific orchestration.

#### Electron Forge

Electron's official packaging tool (v7.11). Plugin-based architecture ("makers" for each format). Slightly fewer formats than Electron Builder but tighter Electron integration. MSIX support via dedicated maker.

#### Tauri

Rust + web frontend framework producing small binaries (~2 MB Hello World vs Electron's 50-100 MB; the ~600 KB figure was v1's runtime minimum). 8 desktop formats via built-in bundler (NSIS, MSI, DMG, AppImage, DEB, RPM); Flatpak/Snap/AUR are documented but community-maintained, not built-in bundler targets. Mandatory signature verification for updates, mobile support (Android/iOS). Experimental NSIS cross-compilation available. The fastest-growing Electron alternative (103k+ GitHub stars).

#### Flutter Desktop

Flutter's desktop support (v3.41) is stable with Windows ARM64 and Linux ARM64 now supported, but packaging is fragmented. Flutter provides the build output; installers require external tools. The auto_updater plugin provides Sparkle/WinSparkle integration.

#### Platform-Specific Tools

- **NSIS**: The gold standard for customizable Windows EXE installers. Full scripting language, plugins, multi-language. Cross-platform compiler.
- **Inno Setup**: Simpler Windows installer tool with Pascal scripting. Dark mode support (v6.6+). v7 in development.
- **WiX Toolset** (v6.0.2): MSI creation via XML-based declarative authoring. MSIX is NOT natively supported — it requires the proprietary FireGiant extension. The standard for enterprise Windows deployment.
- **AppImage**: Single-file Linux executables with delta update support.
- **Flatpak**: Sandboxed Linux packages distributed via Flathub. Auto-updates are NOT built-in — they are delegated to desktop environments (GNOME Software, KDE Discover).
- **Snap**: Canonical's Linux packaging with automatic updates via snapd daemon. Transactional multi-snap updates available via opt-in `--transaction=all-snaps`. Default on Ubuntu.

#### Python Tools

- **PyInstaller**: Most popular Python freezer. Single-file or directory bundles. No installer creation.
- **cx_Freeze**: Python freezer with MSI and DMG output.
- **Briefcase** (BeeWare, v0.3.26): The most complete Python packaging tool. MSI, DMG, PKG, AppImage, Flatpak (added in v0.3.7, 2022) with auto code signing/notarization.

---

## Strengths & Weaknesses

### Nucleus's Strengths

1. **Unmatched format coverage** (16 formats) — more than any JVM tool and matching Electron Builder
2. **Only JVM tool with integrated runtime libraries** (dark mode, decorated windows, single instance, deep links, executable type detection)
3. **Best-in-class CI pipeline** for JVM apps (6 composite GitHub Actions, 6-runner matrix, universal macOS + MSIX bundle)
4. **First JVM tool with AOT cache support** (Project Leyden / JDK 25+)
5. **Automatic sandboxing pipeline** — unique JVM-specific logic for store distribution
6. **Full code signing matrix** (macOS + notarization, Windows PFX + Azure Trusted Signing)
7. **Free and open source** (MIT) — no pricing barrier
8. **Clean migration path** from Compose Multiplatform

### Nucleus's Weaknesses

1. **No cross-compilation** — requires platform-specific CI runners (mitigated by CI actions)
2. **Gradle-only** — no Maven or standalone CLI support
3. **Young project** — smaller community, less battle-testing than established tools
4. **Depends on electron-builder** — an Electron ecosystem dependency in a JVM tool (though used only as a backend)

---

## When to Choose Something Else

| Use Case | Recommended Tool | Why |
|----------|-----------------|-----|
| **JVM app, build from one machine** | Conveyor or install4j | Full cross-compilation support |
| **JVM app, custom installer UI** | install4j | Visual IDE with 80+ configurable actions |
| **JVM app, simplest setup** | jDeploy | Zero-config CLI, cross-compiles everything |
| **JVM app, enterprise deployment** | install4j | 20+ years maturity, enterprise support |
| **Maven-only project** | install4j or JavaPackager | Nucleus is Gradle-only |
| **Electron app** | Electron Builder | Native ecosystem tool |
| **Lightweight web-based app** | Tauri | Rust backend, ~2 MB binaries |
| **Python app** | Briefcase | Native Python packaging |
| **Windows-only installer** | NSIS or Inno Setup | Full scripting control |
| **Linux distribution** | Flatpak or AppImage | Sandboxed or portable |

---

## Methodology

This comparison was produced using the following process:

1. **Codebase analysis** — The Nucleus plugin source, runtime libraries, CI workflows, and composite actions were read and analyzed by [Claude Code](https://claude.ai/claude-code)
2. **Web research** — Specialized AI agents performed parallel internet research on all 27 tools, collecting version numbers, feature lists, pricing, and platform support
3. **Fact-checking** — Additional verification agents cross-checked every claim against official documentation, GitHub releases, npm/Maven registries, and changelogs
4. **Source code verification** — Nucleus-specific claims (16 formats, 17 executable types, SHA-512, NSIS DSL features, sandboxing pipeline, etc.) were verified directly against the source code
5. **Scoring** — Each tool is rated 0–10 across 13 dimensions; the total is the sum normalized to 100 (raw sum / 130 × 100)

!!! warning "Disclaimer"
    While every effort was made to verify accuracy, this comparison reflects a snapshot in time (February 2026). Tool capabilities evolve — always check the official documentation for the latest information.
