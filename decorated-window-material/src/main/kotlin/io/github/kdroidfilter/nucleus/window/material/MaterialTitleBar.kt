package io.github.kdroidfilter.nucleus.window.material

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.github.kdroidfilter.nucleus.window.DecoratedWindowScope
import io.github.kdroidfilter.nucleus.window.DecoratedWindowState
import io.github.kdroidfilter.nucleus.window.TitleBar
import io.github.kdroidfilter.nucleus.window.TitleBarScope

@Suppress("FunctionNaming")
@Composable
fun DecoratedWindowScope.MaterialTitleBar(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit = {},
) {
    val style = rememberMaterialTitleBarStyle(MaterialTheme.colorScheme)
    TitleBar(
        modifier = modifier,
        gradientStartColor = gradientStartColor,
        style = style,
        content = content,
    )
}
