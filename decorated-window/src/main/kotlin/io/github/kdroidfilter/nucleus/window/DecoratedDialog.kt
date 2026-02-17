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
import androidx.compose.ui.awt.ComposeDialog
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.DialogWindowScope
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberDialogState
import io.github.kdroidfilter.nucleus.window.internal.insideBorder
import io.github.kdroidfilter.nucleus.window.styling.LocalDecoratedWindowStyle
import io.github.kdroidfilter.nucleus.window.utils.DesktopPlatform
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

@Stable
class DecoratedDialogState(
    val isActive: Boolean,
)

val LocalDecoratedDialogState =
    staticCompositionLocalOf {
        DecoratedDialogState(isActive = true)
    }

val LocalDialogWindow = staticCompositionLocalOf<ComposeDialog?> { null }

interface DecoratedDialogScope : DialogWindowScope {
    val state: DecoratedDialogState
    val title: String
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
fun DecoratedDialog(
    onCloseRequest: () -> Unit,
    visible: Boolean = true,
    title: String = "",
    icon: Painter? = null,
    undecorated: Boolean = DesktopPlatform.Current == DesktopPlatform.Linux,
    resizable: Boolean = true,
    enabled: Boolean = true,
    focusable: Boolean = true,
    onPreviewKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { false },
    onKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { false },
    content: @Composable DecoratedDialogScope.() -> Unit,
) {
    val dialogState = rememberDialogState(position = WindowPosition.PlatformDefault)

    DialogWindow(
        onCloseRequest = onCloseRequest,
        state = dialogState,
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
        var isActive by remember { mutableStateOf(true) }

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

        val decoratedDialogState = DecoratedDialogState(isActive = isActive)

        val scope =
            remember(this, decoratedDialogState, title) {
                object : DecoratedDialogScope, DialogWindowScope by this {
                    override val state: DecoratedDialogState get() = decoratedDialogState
                    override val title: String get() = title
                }
            }

        val style = LocalDecoratedWindowStyle.current

        CompositionLocalProvider(
            LocalDecoratedDialogState provides decoratedDialogState,
            LocalDialogWindow provides window,
            LocalTitleBarInfo provides title,
        ) {
            val borderColor = if (isActive) style.colors.border else style.colors.borderInactive
            val showBorder = DesktopPlatform.Current == DesktopPlatform.Linux

            Layout(
                content = { scope.content() },
                modifier =
                    if (showBorder) {
                        Modifier.fillMaxSize().insideBorder(style.metrics.borderWidth, borderColor)
                    } else {
                        Modifier.fillMaxSize()
                    },
            ) { measurables, constraints ->
                if (measurables.isEmpty()) {
                    return@Layout layout(constraints.maxWidth, constraints.maxHeight) {}
                }

                val titleBarPlaceable =
                    measurables.firstOrNull()?.measure(
                        Constraints(maxWidth = constraints.maxWidth),
                    )
                val titleBarHeight = titleBarPlaceable?.height ?: 0

                val contentConstraints =
                    Constraints(
                        maxWidth = constraints.maxWidth,
                        maxHeight = (constraints.maxHeight - titleBarHeight).coerceAtLeast(0),
                    )

                val contentPlaceables =
                    if (measurables.size > 1) {
                        measurables.drop(1).map { it.measure(contentConstraints) }
                    } else {
                        emptyList()
                    }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    titleBarPlaceable?.place(0, 0)
                    contentPlaceables.forEach { it.place(0, titleBarHeight) }
                }
            }
        }
    }
}
