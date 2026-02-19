# AOT Cache

Detect whether the application is running in AOT training mode or with an AOT cache.

## Installation

```kotlin
dependencies {
    implementation("io.github.kdroidfilter:nucleus.aot-runtime:<version>")
    // Transitive: nucleus.core-runtime is pulled in via `api`
}
```

```kotlin
import io.github.kdroidfilter.nucleus.aot.runtime.AotRuntime
import io.github.kdroidfilter.nucleus.aot.runtime.AotRuntimeMode
```

## Modes

| Method | Returns `true` when... |
|--------|------------------------|
| `AotRuntime.isTraining()` | App is running during AOT cache generation |
| `AotRuntime.isRuntime()` | App is running with an AOT cache loaded |
| `AotRuntime.mode()` | Returns `AotRuntimeMode.TRAINING`, `AotRuntimeMode.RUNTIME`, or `AotRuntimeMode.OFF` |

## Training Mode Pattern

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

## Requirements

- JDK 25+ for AOT cache generation
- The training run must exit with code `0`
- Safety timeout: 300 seconds (configurable in the Gradle task)
- Headless Linux builds use Xvfb automatically

## How It Works

The plugin sets the `nucleus.aot.mode` system property:
- `training` — set during the AOT cache generation step
- `runtime` — set when an AOT cache is loaded
- absent — no AOT (`AotRuntime.mode()` returns `AotRuntimeMode.OFF`)

## ExecutableRuntime Re-export

The `aot-runtime` module re-exports `ExecutableRuntime` and `ExecutableType` via type aliases, so you can import from either package:

```kotlin
// Both work:
import io.github.kdroidfilter.nucleus.core.runtime.ExecutableRuntime
import io.github.kdroidfilter.nucleus.aot.runtime.ExecutableRuntime
```
