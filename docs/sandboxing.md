# Sandboxing

Nucleus automatically manages a **store build pipeline** for store formats. When your target formats include PKG, AppX, or Flatpak, the plugin splits the build into two parallel pipelines: one for direct-distribution formats (DMG, NSIS, DEB...) and one for store formats that require special handling (sandboxing on macOS/Linux, native library extraction for all).

## Store Formats

| Format | OS | Sandbox Type |
|--------|----|--------------|
| PKG | macOS | [App Sandbox](https://developer.apple.com/documentation/security/app-sandbox) |
| AppX | Windows | [MSIX packaging](https://learn.microsoft.com/en-us/windows/msix/overview) (full trust — not sandboxed) |
| Flatpak | Linux | [Flatpak sandbox](https://docs.flatpak.org/en/latest/sandbox-permissions.html) |

The plugin determines which formats require sandboxing via `TargetFormat.isStoreFormat`:

```kotlin
targetFormats(
    // Direct distribution — non-sandboxed pipeline
    TargetFormat.Dmg,
    TargetFormat.Nsis,
    TargetFormat.Deb,

    // Store distribution — sandboxed pipeline
    TargetFormat.Pkg,
    TargetFormat.AppX,
    TargetFormat.Flatpak,
)
```

Both pipelines run in the same `./gradlew packageDistributionForCurrentOS` invocation. No extra configuration is needed.

## What the Sandboxed Pipeline Does

When at least one store format is configured, Nucleus registers additional Gradle tasks that handle the constraints imposed by OS-level sandboxing:

### 1. Extract Native Libraries from JARs

Sandboxed apps (especially macOS App Sandbox) cannot load unsigned native code extracted to temp directories at runtime. The plugin scans all dependency JARs for `.dylib`, `.jnilib`, `.so`, and `.dll` files and extracts them to the app's `resources/` directory.

**Task:** `extractNativeLibsForSandboxing`

### 2. Strip Native Libraries from JARs

After extraction, the plugin rewrites JARs without the native library entries to avoid duplication in the final package.

**Task:** `stripNativeLibsFromJars`

### 3. Prepare Sandboxed App Resources

A separate `Sync` task merges the user's `appResources` with the extracted native libraries into a single resources directory for the sandboxed app-image.

**Task:** `prepareSandboxedAppResources`

### 4. Inject Sandboxing JVM Arguments

The sandboxed app-image is configured with JVM arguments that redirect native library loading to the pre-extracted resources directory:

```
-Djava.library.path=$APPDIR/resources
-Djna.nounpack=true
-Djna.nosys=true
-Djna.boot.library.path=$APPDIR/resources
-Djna.library.path=$APPDIR/resources
```

This ensures JNA/JNI libraries are loaded from signed, pre-extracted locations instead of being dynamically extracted to temp at runtime.

### 5. Sign Native Libraries in Resources (macOS)

On macOS, all `.dylib` files in the sandboxed app's `resources/` directory are individually code-signed so they pass Gatekeeper checks.

### 6. Handle Skiko and icudtl.dat

The Skiko library path is adjusted to point to `resources/` instead of the app root. The companion `icudtl.dat` file is copied alongside the Skiko native library.

## macOS App Sandbox

### Entitlements

The plugin ships default entitlements for both sandboxed and non-sandboxed builds:

**Non-sandboxed** (DMG, ZIP — direct distribution):

```xml
<dict>
    <key>com.apple.security.cs.allow-jit</key>                        <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>  <true/>
    <key>com.apple.security.cs.disable-library-validation</key>        <true/>
</dict>
```

**Sandboxed app** (PKG — App Store):

```xml
<dict>
    <key>com.apple.security.app-sandbox</key>                         <true/>
    <key>com.apple.security.cs.allow-jit</key>                        <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>  <true/>
    <key>com.apple.security.cs.disable-library-validation</key>        <true/>
    <key>com.apple.security.network.client</key>                      <true/>
    <key>com.apple.security.files.user-selected.read-write</key>      <true/>
</dict>
```

**Sandboxed runtime** (JVM runtime binaries):

```xml
<dict>
    <key>com.apple.security.app-sandbox</key>                         <true/>
    <key>com.apple.security.cs.allow-jit</key>                        <true/>
    <key>com.apple.security.cs.allow-unsigned-executable-memory</key>  <true/>
    <key>com.apple.security.cs.disable-library-validation</key>        <true/>
</dict>
```

The runtime entitlements are more restrictive (no network, no file access) since only the app code should declare capabilities.

### Custom Entitlements

Override the defaults for additional capabilities (camera, microphone, etc.):

```kotlin
macOS {
    entitlementsFile.set(project.file("packaging/entitlements.plist"))
    runtimeEntitlementsFile.set(project.file("packaging/runtime-entitlements.plist"))
}
```

### Provisioning Profiles (Mac App Store)

Mac App Store builds require provisioning profiles:

```kotlin
macOS {
    appStore = true
    provisioningProfile.set(project.file("packaging/MyApp.provisionprofile"))
    runtimeProvisioningProfile.set(project.file("packaging/MyApp_Runtime.provisionprofile"))
}
```

### AOT Cache and Sandboxing

When generating an AOT cache for a sandboxed build, the plugin temporarily strips the code signature from `jspawnhelper` during the training phase. macOS kills sandboxed forked processes, so the signature must be removed for AOT training and re-applied afterward with the runtime entitlements.

This is handled automatically — no configuration needed.

## Windows AppX

AppX packages use MSIX packaging for the Microsoft Store. Desktop Bridge apps run with full trust (`runFullTrust`) and are **not sandboxed** — they have the same system access as regular desktop apps. Capabilities are declared via the AppX manifest settings:

```kotlin
windows {
    appx {
        publisher = "CN=D541E802-6D30-446A-864E-2E8ABD2DAA5E"
        identityName = "MyCompany.MyApp"
        publisherDisplayName = "My Company"
        applicationId = "MyApp"
    }
}
```

See [Windows Targets](targets/windows.md#appx-windows-store-msix) for all AppX settings.

## Flatpak Sandbox

Flatpak apps are sandboxed by default. Use `finishArgs` to grant permissions:

```kotlin
linux {
    flatpak {
        finishArgs = listOf(
            "--share=ipc",
            "--socket=x11",
            "--socket=wayland",
            "--socket=pulseaudio",
            "--device=dri",
            "--filesystem=home",         // access home directory
            "--share=network",           // network access
        )
    }
}
```

See [Linux Targets](targets/linux.md#flatpak) for all Flatpak settings.

## CI Integration

The sandboxed pipeline runs transparently in CI. A single `./gradlew packageReleaseDistributionForCurrentOS` builds both sandboxed and non-sandboxed formats:

```yaml
- name: Setup Nucleus
  uses: ./.github/actions/setup-nucleus
  with:
    jbr-version: '25b176.4'
    packaging-tools: 'true'
    flatpak: 'true'     # Flatpak sandbox support
    snap: 'true'
    setup-gradle: 'true'
    setup-node: 'true'

- name: Build packages
  run: ./gradlew packageReleaseDistributionForCurrentOS --stacktrace --no-daemon
```

The `setup-nucleus` action installs all dependencies needed for sandboxed builds:

- **Linux:** Flatpak SDK/runtime, Snapcraft
- **macOS:** JBR 25 (for entitlements signing via `codesign`)
- **Windows:** No extra setup needed (AppX uses the built-in Windows SDK)

Sandboxed outputs go to `<format>-sandboxed/` subdirectories and are uploaded alongside non-sandboxed artifacts. The post-build jobs (universal macOS binary, MSIX bundle, publish) handle both transparently.

## Gradle Tasks

| Task | Description |
|------|-------------|
| `extractNativeLibsForSandboxing` | Extract `.dylib`/`.so`/`.dll` from dependency JARs |
| `stripNativeLibsFromJars` | Rewrite JARs without native libraries |
| `prepareSandboxedAppResources` | Merge app resources + extracted native libs |
| `createSandboxedDistributable` | Build app-image with sandbox JVM args |
| `generateSandboxedAotCache` | AOT cache for sandboxed distributable |
| `package<Pkg\|AppX\|Flatpak>` | Final packaging using sandboxed distributable |

These tasks are only registered when at least one store format is configured.
