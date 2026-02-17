package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import io.github.kdroidfilter.nucleus.window.internal.isDark
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle

@Suppress("FunctionNaming")
@Composable
internal fun DecoratedDialogScope.WindowsDialogTitleBar(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedDialogState) -> Unit = {},
) {
    val style = LocalTitleBarStyle.current
    val dialogState = LocalDecoratedDialogState.current
    val titleBar = remember { JBR.getWindowDecorations()?.createCustomTitleBar() }

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
        applyTitleBar = { height, _ ->
            if (titleBar != null) {
                titleBar.height = height.value
                titleBar.putProperty("controls.dark", style.colors.background.isDark())
                JBR.getWindowDecorations()?.setCustomTitleBar(window, titleBar)
                PaddingValues(start = titleBar.leftInset.dp, end = titleBar.rightInset.dp)
            } else {
                PaddingValues(0.dp)
            }
        },
        backgroundContent = {
            if (titleBar != null) {
                Spacer(modifier = Modifier.fillMaxSize().customTitleBarMouseEventHandler(titleBar))
            }
        },
    ) { _ ->
        content(dialogState)
    }
}
