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

## ProGuard

When ProGuard is enabled in a release build, the Nucleus Gradle plugin **automatically includes** the required rules for all Nucleus runtime libraries (`default-compose-desktop-rules.pro`). No manual configuration is needed.

Libraries that use JNI (`decorated-window`, `darkmode-detector`) require `-keep` rules for their native bridge classes — these are handled by the plugin. See each library's documentation for the exact rules if you need to add them manually.

You can add extra project-specific rules in your `proguard-rules.pro` file:

```kotlin
nucleus.application {
    buildTypes {
        release {
            proguard {
                isEnabled = true
                // Your custom rules file:
                configurationFiles.from(project.file("proguard-rules.pro"))
            }
        }
    }
}
```
