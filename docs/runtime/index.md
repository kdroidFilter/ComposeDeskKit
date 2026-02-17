# Runtime APIs

Nucleus provides three runtime libraries for use in your application code. All are published on Maven Central.

## Libraries

| Library | Artifact | Description |
|---------|----------|-------------|
| Core Runtime | `io.github.kdroidfilter:nucleus.core-runtime` | Executable type detection, single instance, deep links |
| AOT Runtime | `io.github.kdroidfilter:nucleus.aot-runtime` | AOT cache detection (includes core-runtime via `api`) |
| Updater Runtime | `io.github.kdroidfilter:nucleus.updater-runtime` | Auto-update library (includes core-runtime) |
| Decorated Window | `io.github.kdroidfilter:nucleus.decorated-window` | Custom window decorations with native title bar |

```kotlin
dependencies {
    // Pick what you need:
    implementation("io.github.kdroidfilter:nucleus.core-runtime:1.0.0")
    implementation("io.github.kdroidfilter:nucleus.aot-runtime:1.0.0")
    implementation("io.github.kdroidfilter:nucleus.updater-runtime:1.0.0")
    implementation("io.github.kdroidfilter:nucleus.decorated-window:1.0.0")
}
```
