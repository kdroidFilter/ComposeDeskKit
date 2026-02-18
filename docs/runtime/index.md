# Runtime APIs

Nucleus provides runtime libraries for use in your application code. All are published on Maven Central.

## Libraries

| Library | Artifact | Description |
|---------|----------|-------------|
| Core Runtime | `io.github.kdroidfilter:nucleus.core-runtime` | Executable type detection, single instance, deep links |
| AOT Runtime | `io.github.kdroidfilter:nucleus.aot-runtime` | AOT cache detection (includes core-runtime via `api`) |
| Updater Runtime | `io.github.kdroidfilter:nucleus.updater-runtime` | Auto-update library (includes core-runtime) |
| Decorated Window | `io.github.kdroidfilter:nucleus.decorated-window` | Custom window decorations with native title bar |
| Decorated Window — Material | `io.github.kdroidfilter:nucleus.decorated-window-material` | Material 3 color mapping for decorated windows |
| Dark Mode Detector | `io.github.kdroidfilter:nucleus.darkmode-detector` | Reactive OS dark mode detection via JNI |

```kotlin
dependencies {
    // Pick what you need:
    implementation("io.github.kdroidfilter:nucleus.core-runtime:<version>")
    implementation("io.github.kdroidfilter:nucleus.aot-runtime:<version>")
    implementation("io.github.kdroidfilter:nucleus.updater-runtime:<version>")
    implementation("io.github.kdroidfilter:nucleus.decorated-window:<version>")
    implementation("io.github.kdroidfilter:nucleus.decorated-window-material:<version>")
    implementation("io.github.kdroidfilter:nucleus.darkmode-detector:<version>")
}
```
