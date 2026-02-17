# Decorated Window

The `decorated-window` module provides custom window decorations (title bar, window controls) with native rendering per platform. It is design-system agnostic (no Material3 dependency) and works best with JetBrains Runtime (JBR).

## Installation

```kotlin
dependencies {
    implementation("io.github.kdroidfilter:nucleus.decorated-window:1.0.0")
}
```

## Quick Start

```kotlin
fun main() = application {
    NucleusDecoratedWindowTheme(isDark = true) {
        DecoratedWindow(
            onCloseRequest = ::exitApplication,
            title = "My App",
        ) {
            TitleBar { state ->
                Text(
                    title,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = Color.White,
                )
            }
            // Your app content
            MyContent()
        }
    }
}
```

## Platform Behavior

|  | macOS | Windows | Linux |
|---|-------|---------|-------|
| Decoration | JBR CustomTitleBar | JBR CustomTitleBar | Undecorated |
| Window controls | Native (traffic lights) | Native (min/max/close) | Compose (`WindowControlArea`) |
| Drag | JBR hit-test | JBR forceHitTest | `WindowMove.startMovingTogetherWithMouse()` |
| Double-click maximize | Native | Native | Manual detection |
| Border | None | None | `insideBorder` (hidden when maximized) |

On **macOS** and **Windows**, the module uses JBR's `CustomTitleBar` API to integrate with the native window chrome. The OS-native window buttons (traffic lights on macOS, min/max/close on Windows) are preserved.

On **Linux**, the window is fully undecorated. The module renders its own close/minimize/maximize buttons using SVG icons adapted to the desktop environment (GNOME or KDE/Breeze).

## Components

### `NucleusDecoratedWindowTheme`

Provides styling for all decorated window components. Must wrap `DecoratedWindow` / `DecoratedDialog`.

```kotlin
NucleusDecoratedWindowTheme(
    isDark = true,  // or false for light theme
    windowStyle = DecoratedWindowDefaults.darkWindowStyle(),   // optional override
    titleBarStyle = DecoratedWindowDefaults.darkTitleBarStyle(), // optional override
) {
    // ...
}
```

### `DecoratedWindow`

Drop-in replacement for Compose `Window()`. Manages window state (active, fullscreen, minimized, maximized) and platform-specific border rendering.

```kotlin
DecoratedWindow(
    onCloseRequest = ::exitApplication,
    state = rememberWindowState(),
    title = "My App",
    icon = null,
    resizable = true,
) {
    TitleBar { state -> /* title bar content */ }
    // window content
}
```

### `DecoratedDialog`

Same concept for dialog windows. Uses `DialogWindow` internally.

```kotlin
DecoratedDialog(
    onCloseRequest = { showDialog = false },
    title = "Settings",
) {
    DialogTitleBar { state ->
        Text(title, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
    // dialog content
}
```

### `TitleBar` / `DialogTitleBar`

Platform-dispatched title bar composable. Provides a `TitleBarScope` with `align()` for positioning content:

```kotlin
TitleBar { state ->
    // Aligned to the start (left on LTR)
    Icon(
        painter = myIcon,
        contentDescription = null,
        modifier = Modifier.align(Alignment.Start),
    )

    // Centered title
    Text(
        title,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )

    // Right-aligned action
    IconButton(
        onClick = { /* ... */ },
        modifier = Modifier.align(Alignment.End),
    ) {
        Icon(Icons.Default.Settings, contentDescription = "Settings")
    }
}
```

## Styling

### `DecoratedWindowStyle`

Controls window border appearance:

```kotlin
data class DecoratedWindowStyle(
    val colors: DecoratedWindowColors,  // border, borderInactive
    val metrics: DecoratedWindowMetrics, // borderWidth
)
```

### `TitleBarStyle`

Controls title bar appearance:

```kotlin
data class TitleBarStyle(
    val colors: TitleBarColors,   // background, content, border, hover/press states
    val metrics: TitleBarMetrics, // height
    val icons: TitleBarIcons,     // optional custom Painters for window buttons
)
```

### Custom Colors

```kotlin
NucleusDecoratedWindowTheme(
    isDark = true,
    titleBarStyle = DecoratedWindowDefaults.darkTitleBarStyle().copy(
        colors = DecoratedWindowDefaults.darkTitleBarStyle().colors.copy(
            background = Color(0xFF1A1A2E),
        ),
    ),
) {
    // ...
}
```

## JBR Requirement

For the best experience on **macOS** and **Windows**, run on [JetBrains Runtime (JBR)](https://github.com/JetBrains/JetBrainsRuntime). JBR provides the `CustomTitleBar` API for native window decoration integration.

Without JBR:

- macOS/Windows title bars will use fallback padding values
- Linux is unaffected (always uses its own Compose-based decorations)

!!! tip
    When packaging with Nucleus, the bundled JDK is typically a standard JDK. If you need JBR features, configure your build to bundle JBR instead.

## Linux Desktop Environment Detection

On Linux, the module detects the current desktop environment and loads appropriate icons:

- **GNOME** — Adwaita-style icons
- **KDE** — Breeze-style icons
- **Other** — Falls back to GNOME style

Detection uses `XDG_CURRENT_DESKTOP` and `DESKTOP_SESSION` environment variables.
