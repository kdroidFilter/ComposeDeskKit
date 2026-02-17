package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.rememberWindowState
import io.github.kdroidfilter.nucleus.window.internal.insideBorder
import io.github.kdroidfilter.nucleus.window.styling.LocalDecoratedWindowStyle
import io.github.kdroidfilter.nucleus.window.utils.DesktopPlatform
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

@Stable
class DecoratedWindowState(
    val isActive: Boolean,
    val isFullscreen: Boolean,
    val isMinimized: Boolean,
    val isMaximized: Boolean,
)

val LocalDecoratedWindowState =
    staticCompositionLocalOf<DecoratedWindowState> {
        DecoratedWindowState(
            isActive = true,
            isFullscreen = false,
            isMinimized = false,
            isMaximized = false,
        )
    }

val LocalWindow = staticCompositionLocalOf<ComposeWindow?> { null }

val LocalTitleBarInfo = staticCompositionLocalOf { "" }

interface DecoratedWindowScope : FrameWindowScope {
    val state: DecoratedWindowState
    val title: String
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
fun DecoratedWindow(
    onCloseRequest: () -> Unit,
    state: WindowState = rememberWindowState(),
    visible: Boolean = true,
    title: String = "",
    icon: Painter? = null,
    undecorated: Boolean = DesktopPlatform.Current == DesktopPlatform.Linux,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { false },
    onKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { false },
    content: @Composable DecoratedWindowScope.() -> Unit,
) {
    Window(
        onCloseRequest = onCloseRequest,
        state = state,
        visible = visible,
        title = title,
        icon = icon,
        undecorated = undecorated,
        resizable = resizable,
        enabled = enabled,
        focusable = focusable,
        onPreviewKeyEvent = onPreviewKeyEvent,
        onKeyEvent = onKeyEvent,
    ) {
        var isActive by remember { mutableStateOf(window.isActive) }

        DisposableEffect(window) {
            val listener =
                object : WindowAdapter() {
                    override fun windowActivated(e: WindowEvent?) {
                        isActive = true
                    }

                    override fun windowDeactivated(e: WindowEvent?) {
                        isActive = false
                    }
                }
            window.addWindowListener(listener)
            onDispose { window.removeWindowListener(listener) }
        }

        val decoratedState =
            DecoratedWindowState(
                isActive = isActive,
                isFullscreen = state.placement == WindowPlacement.Fullscreen,
                isMinimized =
                    state.placement == WindowPlacement.Floating &&
                        window.extendedState == java.awt.Frame.ICONIFIED,
                isMaximized = state.placement == WindowPlacement.Maximized,
            )

        val scope =
            remember(this, decoratedState, title) {
                object : DecoratedWindowScope, FrameWindowScope by this {
                    override val state: DecoratedWindowState get() = decoratedState
                    override val title: String get() = title
                }
            }

        val style = LocalDecoratedWindowStyle.current

        CompositionLocalProvider(
            LocalDecoratedWindowState provides decoratedState,
            LocalWindow provides window,
            LocalTitleBarInfo provides title,
        ) {
            val borderColor = if (isActive) style.colors.border else style.colors.borderInactive
            val showBorder = DesktopPlatform.Current == DesktopPlatform.Linux && !decoratedState.isMaximized

            DecoratedWindowLayout(
                modifier =
                    if (showBorder) {
                        Modifier.insideBorder(style.metrics.borderWidth, borderColor)
                    } else {
                        Modifier
                    },
            ) {
                scope.content()
            }
        }
    }
}

@Suppress("FunctionNaming")
@Composable
private fun DecoratedWindowLayout(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(
        content = content,
        modifier = modifier.fillMaxSize(),
    ) { measurables, constraints ->
        if (measurables.isEmpty()) {
            return@Layout layout(constraints.maxWidth, constraints.maxHeight) {}
        }

        // First measurable is the title bar (if present), rest is content
        val titleBarMeasurable = measurables.firstOrNull()
        val contentMeasurables = if (measurables.size > 1) measurables.drop(1) else emptyList()

        val titleBarPlaceable =
            titleBarMeasurable?.measure(
                Constraints(maxWidth = constraints.maxWidth),
            )
        val titleBarHeight = titleBarPlaceable?.height ?: 0

        val contentConstraints =
            Constraints(
                maxWidth = constraints.maxWidth,
                maxHeight = (constraints.maxHeight - titleBarHeight).coerceAtLeast(0),
            )

        val contentPlaceables = contentMeasurables.map { it.measure(contentConstraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            titleBarPlaceable?.place(0, 0)
            contentPlaceables.forEach { it.place(0, titleBarHeight) }
        }
    }
}
