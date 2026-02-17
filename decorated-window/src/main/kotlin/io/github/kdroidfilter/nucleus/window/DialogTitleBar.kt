package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import io.github.kdroidfilter.nucleus.window.utils.DesktopPlatform

@Suppress("FunctionNaming")
@Composable
fun DecoratedDialogScope.DialogTitleBar(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedDialogState) -> Unit = {},
) {
    when (DesktopPlatform.Current) {
        DesktopPlatform.MacOS -> MacOSDialogTitleBar(modifier, content)
        DesktopPlatform.Windows -> WindowsDialogTitleBar(modifier, content)
        DesktopPlatform.Linux -> LinuxDialogTitleBar(modifier, content)
        DesktopPlatform.Unknown -> GenericDialogTitleBarImpl(modifier, content)
    }
}

@Suppress("FunctionNaming")
@Composable
internal fun DecoratedDialogScope.GenericDialogTitleBarImpl(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedDialogState) -> Unit = {},
) {
    val style = LocalTitleBarStyle.current
    val dialogState = LocalDecoratedDialogState.current

    // Adapt DecoratedDialogState to DecoratedWindowState for TitleBarImpl
    val windowState =
        DecoratedWindowState(
            isActive = dialogState.isActive,
            isFullscreen = false,
            isMinimized = false,
            isMaximized = false,
        )

    TitleBarImpl(
        window = window,
        state = windowState,
        modifier = modifier,
        style = style,
        applyTitleBar = { _, _ -> PaddingValues(0.dp) },
    ) { _ ->
        content(dialogState)
    }
}
