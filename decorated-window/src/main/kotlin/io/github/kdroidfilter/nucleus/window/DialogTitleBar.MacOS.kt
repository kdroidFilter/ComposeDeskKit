package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle

private const val DIALOG_FALLBACK_INSET = 60

@Suppress("FunctionNaming")
@Composable
internal fun DecoratedDialogScope.MacOSDialogTitleBar(
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
        modifier =
            if (titleBar != null) {
                modifier.customTitleBarMouseEventHandler(titleBar)
            } else {
                modifier
            },
        style = style,
        applyTitleBar = { height, _ ->
            if (titleBar != null) {
                titleBar.height = height.value
                JBR.getWindowDecorations()?.setCustomTitleBar(window, titleBar)
                PaddingValues(start = titleBar.leftInset.dp, end = titleBar.rightInset.dp)
            } else {
                PaddingValues(start = DIALOG_FALLBACK_INSET.dp)
            }
        },
    ) { _ ->
        content(dialogState)
    }
}
