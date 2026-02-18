# Decorated Window — Material 3

The `decorated-window-material` module provides Material 3 wrappers around the [Decorated Window](decorated-window.md) components. It reads colors from `MaterialTheme.colorScheme` and automatically builds the matching `DecoratedWindowStyle` and `TitleBarStyle` — no manual color mapping needed.

## Installation

```kotlin
dependencies {
    implementation("io.github.kdroidfilter:nucleus.decorated-window-material:<version>")
    // Transitive: nucleus.decorated-window is pulled in via `api`
}
```

## Quick Start

```kotlin
fun main() = application {
    val isDark = isSystemInDarkMode()  // from nucleus.darkmode-detector
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        MaterialDecoratedWindow(
            onCloseRequest = ::exitApplication,
            title = "My App",
        ) {
            MaterialTitleBar { state ->
                Text(
                    title,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Surface(modifier = Modifier.fillMaxSize()) {
                // Your app content
            }
        }
    }
}
```

That is all you need. The title bar, borders, and window control hover states automatically match your Material color scheme.

## Components

### `MaterialDecoratedWindow`

Drop-in replacement for `DecoratedWindow`. Same parameters, but wraps the window in a `NucleusDecoratedWindowTheme` derived from the current `MaterialTheme.colorScheme`.

```kotlin
MaterialDecoratedWindow(
    onCloseRequest = ::exitApplication,
    state = rememberWindowState(),
    title = "My App",
) {
    MaterialTitleBar { state -> /* ... */ }
    // content
}
```

The `isDark` flag for `NucleusDecoratedWindowTheme` is inferred automatically from the luminance of `colorScheme.background`.

### `MaterialDecoratedDialog`

Same concept for dialogs:

```kotlin
MaterialDecoratedDialog(
    onCloseRequest = { showDialog = false },
    title = "Settings",
) {
    MaterialDialogTitleBar { state ->
        Text(title, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        // dialog content
    }
}
```

### `MaterialTitleBar` / `MaterialDialogTitleBar`

Material-styled title bars. They read `MaterialTheme.colorScheme` and build a `TitleBarStyle` from it. They accept the same `gradientStartColor` parameter as the base `TitleBar`.

```kotlin
MaterialTitleBar(
    modifier = Modifier.newFullscreenControls(), // optional, macOS fullscreen
    gradientStartColor = Color.Unspecified,       // optional gradient
) { state ->
    // TitleBarScope content
}
```

## Color Mapping

The module maps Material 3 tokens to decorated window styling as follows:

| Decorated Window token | Material 3 source |
|------------------------|--------------------|
| Title bar background | `surface` |
| Title bar inactive background | `surface` |
| Title bar content color | `onSurface` |
| Title bar border | `outlineVariant` |
| Window border | `outlineVariant` |
| Window border (inactive) | `outlineVariant` at 50% alpha |
| Button hover background | `onSurface` at 8% alpha |
| Button press background | `onSurface` at 12% alpha |
| Close button hover | `error` |
| Close button press | `error` at 70% alpha |
| Fullscreen controls background | `surface` |

This means switching from a `lightColorScheme()` to a `darkColorScheme()` (or a dynamic/custom scheme) will update the title bar, borders, and window controls automatically.

## Using with Dark Mode Detector

Combine with [`nucleus.darkmode-detector`](darkmode-detector.md) for automatic theme switching:

```kotlin
fun main() = application {
    val isDark = isSystemInDarkMode()
    val colorScheme = if (isDark) darkColorScheme() else lightColorScheme()

    MaterialTheme(colorScheme = colorScheme) {
        MaterialDecoratedWindow(
            onCloseRequest = ::exitApplication,
            title = "My App",
        ) {
            MaterialTitleBar { /* ... */ }
            Surface(modifier = Modifier.fillMaxSize()) {
                Text("Theme follows OS setting")
            }
        }
    }
}
```

When the user toggles dark mode in their OS settings, `isSystemInDarkMode()` recomposes, the `colorScheme` changes, and the decorated window updates to match — including the title bar, borders, and window control hover states.
