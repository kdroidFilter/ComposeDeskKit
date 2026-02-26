package io.github.kdroidfilter.nucleus.window.material

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.nucleus.core.runtime.LinuxDesktopEnvironment
import io.github.kdroidfilter.nucleus.core.runtime.Platform
import io.github.kdroidfilter.nucleus.window.styling.DecoratedWindowColors
import io.github.kdroidfilter.nucleus.window.styling.DecoratedWindowMetrics
import io.github.kdroidfilter.nucleus.window.styling.DecoratedWindowStyle
import io.github.kdroidfilter.nucleus.window.styling.TitleBarColors
import io.github.kdroidfilter.nucleus.window.styling.TitleBarIcons
import io.github.kdroidfilter.nucleus.window.styling.TitleBarMetrics
import io.github.kdroidfilter.nucleus.window.styling.TitleBarStyle

private const val INACTIVE_BORDER_ALPHA = 0.5f
private const val DARK_LUMINANCE_THRESHOLD = 0.5f

// Platform-native button colors — never influenced by Material theme
private val WindowsCloseButtonHoveredBackground = Color(0xFFE81123)
private val WindowsCloseButtonPressedBackground = Color(0xFFF1707A)
private val WindowsButtonHoveredBackgroundLight = Color(0x1A000000)
private val WindowsButtonHoveredBackgroundDark = Color(0x1AFFFFFF)
private val WindowsButtonPressedBackgroundLight = Color(0x33000000)
private val WindowsButtonPressedBackgroundDark = Color(0x33FFFFFF)

private val KdeCloseButtonHoveredBackground = Color(0xFFED1515)
private val KdeCloseButtonPressedBackground = Color(0xFFF44F4F)
private val GnomeCloseButtonHoveredBackground = Color(0xFFE81123)
private val GnomeCloseButtonPressedBackground = Color(0xFF2596BE)

private val isKde =
    Platform.Current == Platform.Linux && LinuxDesktopEnvironment.Current == LinuxDesktopEnvironment.KDE

@Composable
internal fun rememberMaterialWindowStyle(colorScheme: ColorScheme): DecoratedWindowStyle =
    remember(colorScheme.outlineVariant) {
        DecoratedWindowStyle(
            colors =
                DecoratedWindowColors(
                    border = colorScheme.outlineVariant,
                    borderInactive = colorScheme.outlineVariant.copy(alpha = INACTIVE_BORDER_ALPHA),
                ),
            metrics = DecoratedWindowMetrics(borderWidth = 1.dp),
        )
    }

@Composable
internal fun rememberMaterialTitleBarStyle(colorScheme: ColorScheme): TitleBarStyle =
    remember(
        colorScheme.surface,
        colorScheme.onSurface,
        colorScheme.outlineVariant,
    ) {
        TitleBarStyle(
            colors =
                TitleBarColors(
                    background = colorScheme.surface,
                    inactiveBackground = colorScheme.surface,
                    content = colorScheme.onSurface,
                    border = colorScheme.outlineVariant,
                    fullscreenControlButtonsBackground = colorScheme.surface,
                    titlePaneButtonHoveredBackground = if (colorScheme.isDark()) WindowsButtonHoveredBackgroundDark else WindowsButtonHoveredBackgroundLight,
                    titlePaneButtonPressedBackground = if (colorScheme.isDark()) WindowsButtonPressedBackgroundDark else WindowsButtonPressedBackgroundLight,
                    titlePaneCloseButtonHoveredBackground = when {
                        Platform.Current == Platform.Windows -> WindowsCloseButtonHoveredBackground
                        isKde -> KdeCloseButtonHoveredBackground
                        else -> GnomeCloseButtonHoveredBackground
                    },
                    titlePaneCloseButtonPressedBackground = when {
                        Platform.Current == Platform.Windows -> WindowsCloseButtonPressedBackground
                        isKde -> KdeCloseButtonPressedBackground
                        else -> GnomeCloseButtonPressedBackground
                    },
                ),
            metrics =
                TitleBarMetrics(
                    height = 40.dp,
                    titlePaneButtonSize = if (isKde) DpSize(28.dp, 28.dp) else DpSize(40.dp, 40.dp),
                ),
            icons = TitleBarIcons(),
        )
    }

internal fun ColorScheme.isDark(): Boolean = background.luminance() < DARK_LUMINANCE_THRESHOLD
