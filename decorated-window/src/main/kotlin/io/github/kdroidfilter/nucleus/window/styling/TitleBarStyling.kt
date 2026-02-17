package io.github.kdroidfilter.nucleus.window.styling

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class TitleBarStyle(
    val colors: TitleBarColors,
    val metrics: TitleBarMetrics,
    val icons: TitleBarIcons,
)

data class TitleBarColors(
    val background: Color,
    val backgroundInactive: Color,
    val content: Color,
    val contentInactive: Color,
    val border: Color,
    val borderInactive: Color,
    val hoverBackground: Color,
    val pressBackground: Color,
    val closeHoverBackground: Color,
    val closePressBackground: Color,
)

data class TitleBarMetrics(
    val height: Dp = 40.dp,
    val gradientStartX: Float = 0f,
    val gradientEndX: Float = 0f,
)

data class TitleBarIcons(
    val close: Painter? = null,
    val minimize: Painter? = null,
    val maximize: Painter? = null,
    val restore: Painter? = null,
)

val LocalTitleBarStyle =
    staticCompositionLocalOf<TitleBarStyle> {
        error("No TitleBarStyle provided. Wrap your content with NucleusDecoratedWindowTheme.")
    }
