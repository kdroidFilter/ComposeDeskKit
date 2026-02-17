package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import io.github.kdroidfilter.nucleus.window.internal.isDark
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle

@Suppress("FunctionNaming")
@Composable
internal fun DecoratedWindowScope.WindowsTitleBar(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit = {},
) {
    val style = LocalTitleBarStyle.current
    val titleBar = remember { JBR.getWindowDecorations()?.createCustomTitleBar() }
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    TitleBarImpl(
        window = window,
        state = state,
        modifier = modifier,
        style = style,
        applyTitleBar = { height, _ ->
            if (titleBar != null) {
                titleBar.putProperty("controls.rtl", isRtl)
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
        content = content,
    )
}
