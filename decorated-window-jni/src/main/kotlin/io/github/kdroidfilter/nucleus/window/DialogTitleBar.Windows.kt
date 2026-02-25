package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import io.github.kdroidfilter.nucleus.window.styling.TitleBarStyle

@Suppress("FunctionNaming")
@Composable
internal fun DecoratedDialogScope.WindowsDialogTitleBar(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    style: TitleBarStyle = LocalTitleBarStyle.current,
    content: @Composable TitleBarScope.(DecoratedDialogState) -> Unit = {},
) {
    DialogTitleBarImpl(
        modifier = modifier,
        gradientStartColor = gradientStartColor,
        style = style,
        applyTitleBar = { _, _ -> PaddingValues(0.dp) },
        backgroundContent = {
            Spacer(modifier = Modifier.fillMaxSize().windowDragHandler(window))
        },
        content = content,
    )
}
