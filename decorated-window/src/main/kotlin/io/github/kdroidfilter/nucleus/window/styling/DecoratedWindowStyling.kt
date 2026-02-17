package io.github.kdroidfilter.nucleus.window.styling

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class DecoratedWindowStyle(
    val colors: DecoratedWindowColors,
    val metrics: DecoratedWindowMetrics,
)

data class DecoratedWindowColors(
    val border: Color,
    val borderInactive: Color,
)

data class DecoratedWindowMetrics(
    val borderWidth: Dp = 1.dp,
)

val LocalDecoratedWindowStyle =
    staticCompositionLocalOf<DecoratedWindowStyle> {
        error("No DecoratedWindowStyle provided. Wrap your content with NucleusDecoratedWindowTheme.")
    }
