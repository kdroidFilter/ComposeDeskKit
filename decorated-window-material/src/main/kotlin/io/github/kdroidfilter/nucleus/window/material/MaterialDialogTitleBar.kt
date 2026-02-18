package io.github.kdroidfilter.nucleus.window.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.kdroidfilter.nucleus.window.DecoratedDialogScope
import io.github.kdroidfilter.nucleus.window.DecoratedDialogState
import io.github.kdroidfilter.nucleus.window.DialogTitleBar
import io.github.kdroidfilter.nucleus.window.TitleBarScope

@Suppress("FunctionNaming")
@Composable
fun DecoratedDialogScope.MaterialDialogTitleBar(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    content: @Composable TitleBarScope.(DecoratedDialogState) -> Unit = {},
) {
    val style = rememberMaterialTitleBarStyle(MaterialTheme.colorScheme)
    DialogTitleBar(
        modifier = modifier,
        gradientStartColor = gradientStartColor,
        style = style,
        content = content,
    )
}
