# Runtime APIs

Nucleus provides three runtime libraries for use in your application code. All are published on Maven Central.

## Libraries

| Library | Artifact | Description |
|---------|----------|-------------|
| Core Runtime | `io.github.kdroidfilter:nucleus-core-runtime` | Executable type detection, single instance, deep links |
| AOT Runtime | `io.github.kdroidfilter:nucleus-aot-runtime` | AOT cache detection (includes core-runtime via `api`) |
| Updater Runtime | `io.github.kdroidfilter:nucleus-updater-runtime` | Auto-update library (includes core-runtime) |

```kotlin
dependencies {
    // Pick what you need:
    implementation("io.github.kdroidfilter:nucleus-core-runtime:1.0.0")
    implementation("io.github.kdroidfilter:nucleus-aot-runtime:1.0.0")
    implementation("io.github.kdroidfilter:nucleus-updater-runtime:1.0.0")
}
```

---

## Executable Type Detection

Detect at runtime which installer format was used to package your application.

```kotlin
import io.github.kdroidfilter.nucleus.core.runtime.ExecutableRuntime
import io.github.kdroidfilter.nucleus.core.runtime.ExecutableType
```

### Usage

```kotlin
// Get the executable type
val type: ExecutableType = ExecutableRuntime.type()

// Convenience checks
if (ExecutableRuntime.isDev()) {
    // Running via ./gradlew run (not packaged)
}

if (ExecutableRuntime.isDmg()) {
    // Installed from DMG
}

if (ExecutableRuntime.isNsis()) {
    // Installed from NSIS installer
}

if (ExecutableRuntime.isDeb()) {
    // Installed from DEB package
}
```

### Available Types

| Enum Value | Convenience Method | Platform |
|------------|-------------------|----------|
| `ExecutableType.DEV` | `isDev()` | All (not packaged) |
| `ExecutableType.DMG` | `isDmg()` | macOS |
| `ExecutableType.PKG` | `isPkg()` | macOS |
| `ExecutableType.EXE` | `isExe()` | Windows |
| `ExecutableType.MSI` | `isMsi()` | Windows |
| `ExecutableType.NSIS` | `isNsis()` | Windows |
| `ExecutableType.NSIS_WEB` | `isNsisWeb()` | Windows |
| `ExecutableType.PORTABLE` | `isPortable()` | Windows |
| `ExecutableType.APPX` | `isAppX()` | Windows |
| `ExecutableType.DEB` | `isDeb()` | Linux |
| `ExecutableType.RPM` | `isRpm()` | Linux |
| `ExecutableType.SNAP` | `isSnap()` | Linux |
| `ExecutableType.FLATPAK` | `isFlatpak()` | Linux |
| `ExecutableType.APPIMAGE` | `isAppImage()` | Linux |
| `ExecutableType.ZIP` | `isZip()` | All |
| `ExecutableType.TAR` | `isTar()` | All |
| `ExecutableType.SEVEN_Z` | `isSevenZ()` | All |

### How It Works

The plugin injects a `nucleus.executable.type` system property into the launcher `.cfg` file at packaging time. `ExecutableRuntime.type()` reads this property. When running via `./gradlew run`, the value is `dev`.

You can also read a custom property name:

```kotlin
val type = ExecutableRuntime.type("my.custom.property")
```

### Use Cases

```kotlin
// Show different update UI based on installer
when (ExecutableRuntime.type()) {
    ExecutableType.SNAP -> showSnapStoreUpdate()
    ExecutableType.FLATPAK -> showFlatpakUpdate()
    ExecutableType.APPX -> showMicrosoftStoreUpdate()
    else -> showNucleusAutoUpdate()
}
```

---

## AOT Cache

Detect whether the application is running in AOT training mode or with an AOT cache.

```kotlin
import io.github.kdroidfilter.nucleus.aot.runtime.AotRuntime
import io.github.kdroidfilter.nucleus.aot.runtime.AotRuntimeMode
```

### Modes

| Method | Returns `true` when... |
|--------|------------------------|
| `AotRuntime.isTraining()` | App is running during AOT cache generation |
| `AotRuntime.isRuntime()` | App is running with an AOT cache loaded |
| `AotRuntime.mode()` | Returns `AotRuntimeMode.TRAINING`, `AotRuntimeMode.RUNTIME`, or `AotRuntimeMode.OFF` |

### Training Mode Pattern

Your application **must self-terminate** during AOT training. Use this pattern:

```kotlin
fun main() {
    // Exit after 30 seconds during AOT training
    if (AotRuntime.isTraining()) {
        Thread({
            Thread.sleep(30_000)
            System.exit(0)
        }, "aot-timer").apply {
            isDaemon = true
            start()
        }
    }

    // Skip heavy initialization during training
    if (!AotRuntime.isTraining()) {
        initializeHeavyResources()
    }

    // Launch the UI
    application {
        Window(onCloseRequest = ::exitApplication, title = "MyApp") {
            App()
        }
    }
}
```

### Requirements

- JDK 25+ for AOT cache generation
- The training run must exit with code `0`
- Safety timeout: 300 seconds (configurable in the Gradle task)
- Headless Linux builds use Xvfb automatically

### How It Works

The plugin sets the `nucleus.aot.mode` system property:
- `training` — set during the AOT cache generation step
- `runtime` — set when an AOT cache is loaded
- absent — no AOT (`AotRuntime.mode()` returns `AotRuntimeMode.OFF`)

### ExecutableRuntime Re-export

The `aot-runtime` module re-exports `ExecutableRuntime` and `ExecutableType` via type aliases, so you can import from either package:

```kotlin
// Both work:
import io.github.kdroidfilter.nucleus.core.runtime.ExecutableRuntime
import io.github.kdroidfilter.nucleus.aot.runtime.ExecutableRuntime
```

---

## Single Instance

Enforce that only one instance of your application runs at a time.

```kotlin
import io.github.kdroidfilter.nucleus.core.runtime.SingleInstanceManager
```

`SingleInstanceManager` is a Kotlin `object` (singleton). It uses file-based locking to ensure single-instance behavior across platforms.

### Usage

```kotlin
fun main() {
    val isSingle = SingleInstanceManager.isSingleInstance(
        onRestoreFileCreated = {
            // Called on a NEW instance when the restore request file is created
            // `this` is the Path to the restore request file
            // You can write deep link data here for the primary instance to read
        },
        onRestoreRequest = {
            // Called on the PRIMARY instance when another instance tries to start
            // `this` is the Path to the restore request file
            // Bring your window to the front here
            window.toFront()
        },
    )

    if (!isSingle) {
        // Another instance is already running — this process will exit
        System.exit(0)
        return
    }

    // Launch the UI — we are the primary instance
    application {
        Window(onCloseRequest = ::exitApplication) { App() }
    }
}
```

### Configuration

Configure before the first call to `isSingleInstance()`:

```kotlin
SingleInstanceManager.configuration = SingleInstanceManager.Configuration(
    lockFilesDir = Paths.get(System.getProperty("java.io.tmpdir")),  // Default
    lockIdentifier = "com.example.myapp",  // Defaults to auto-detected app ID
)
```

#### Configuration Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `lockFilesDir` | `Path` | System temp dir | Directory for lock files |
| `lockIdentifier` | `String` | Auto-detected | Unique application identifier |
| `lockFileName` | `String` | `"$lockIdentifier.lock"` | Lock file name (derived) |
| `restoreRequestFileName` | `String` | `"$lockIdentifier.restore_request"` | Restore request file name (derived) |
| `lockFilePath` | `Path` | Derived | Full path to lock file (derived) |
| `restoreRequestFilePath` | `Path` | Derived | Full path to restore request file (derived) |

### How It Works

1. Creates a lock file in the configured directory
2. Uses `java.nio.channels.FileLock` for atomic locking
3. If the lock is already held, sends a restore request via the filesystem
4. The primary instance watches for restore request files and invokes the callback
5. Cross-platform: works on macOS, Windows, and Linux

---

## Deep Links

Handle custom URL protocol links (`myapp://action?param=value`).

```kotlin
import io.github.kdroidfilter.nucleus.core.runtime.DeepLinkHandler
```

`DeepLinkHandler` is a Kotlin `object` (singleton).

### Setup

1. Register the protocol in the DSL:

```kotlin
nativeDistributions {
    protocol("MyApp", "myapp")
}
```

2. Handle incoming links in your app:

```kotlin
fun main(args: Array<String>) {
    DeepLinkHandler.register(args) { uri ->
        println("Received deep link: $uri")
        // Handle: myapp://open?file=document.txt
    }

    // The current URI is also available as a property
    val currentUri = DeepLinkHandler.uri

    // Launch the UI...
}
```

### API Reference

| Member | Type / Signature | Description |
|--------|-----------------|-------------|
| `uri` | `URI?` (read-only, volatile) | The most recent deep link URI |
| `register(args, onDeepLink)` | `fun register(args: Array<String>, onDeepLink: (URI) -> Unit)` | Register a deep link handler with CLI args |
| `writeUriTo(path)` | `fun writeUriTo(path: Path)` | Write the current URI to a file (for IPC) |
| `readUriFrom(path)` | `fun readUriFrom(path: Path)` | Read a URI from a file (for IPC) |

### Integration with Single Instance

Deep links work with `SingleInstanceManager` to forward URLs to the primary instance:

```kotlin
fun main(args: Array<String>) {
    DeepLinkHandler.register(args) { uri ->
        handleDeepLink(uri)
    }

    val isSingle = SingleInstanceManager.isSingleInstance(
        onRestoreFileCreated = {
            // New instance: write our deep link URI for the primary to read
            // `this` is the Path to the restore request file
            DeepLinkHandler.writeUriTo(this)
        },
        onRestoreRequest = {
            // Primary instance: read the URI from the new instance
            // `this` is the Path to the restore request file
            DeepLinkHandler.readUriFrom(this)
            window.toFront()
        },
    )

    if (!isSingle) {
        System.exit(0)
        return
    }

    // Handle the initial deep link if launched with one
    DeepLinkHandler.uri?.let { handleDeepLink(it) }

    // Launch the UI...
}
```

### Platform Behavior

| Platform | Mechanism |
|----------|-----------|
| macOS | Apple Events (`setOpenURIHandler`) — works even when app is already running |
| Windows | CLI argument via registry handler — new process forwards to primary instance |
| Linux | CLI argument via `.desktop` MimeType — new process forwards to primary instance |
