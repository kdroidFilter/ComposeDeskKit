package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle

private const val FALLBACK_INSET = 80

@Suppress("FunctionNaming")
@Composable
internal fun DecoratedWindowScope.MacOSTitleBar(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit = {},
) {
    val style = LocalTitleBarStyle.current
    val titleBar = remember { JBR.getWindowDecorations()?.createCustomTitleBar() }

    TitleBarImpl(
        window = window,
        state = state,
        modifier =
            if (titleBar != null) {
                modifier.customTitleBarMouseEventHandler(titleBar)
            } else {
                modifier
            },
        style = style,
        applyTitleBar = { height, currentState ->
            if (titleBar != null) {
                titleBar.height = height.value
                JBR.getWindowDecorations()?.setCustomTitleBar(window, titleBar)

                if (currentState.isFullscreen) {
                    PaddingValues(start = FALLBACK_INSET.dp)
                } else {
                    PaddingValues(start = titleBar.leftInset.dp, end = titleBar.rightInset.dp)
                }
            } else {
                PaddingValues(start = FALLBACK_INSET.dp)
            }
        },
        content = content,
    )
}
