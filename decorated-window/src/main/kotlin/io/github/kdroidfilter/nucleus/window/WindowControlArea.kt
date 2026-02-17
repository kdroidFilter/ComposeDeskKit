package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import io.github.kdroidfilter.nucleus.window.utils.linux.linuxTitleBarIcons

private val BUTTON_SIZE = 32.dp
private val ICON_SIZE = 16.dp

@Suppress("FunctionNaming")
@Composable
internal fun WindowControlArea(
    onMinimize: () -> Unit,
    onMaximizeRestore: () -> Unit,
    onClose: () -> Unit,
    isMaximized: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val icons = linuxTitleBarIcons()
    val style = LocalTitleBarStyle.current

    Row(modifier = modifier) {
        ControlButton(
            onClick = onMinimize,
            icon = if (isActive) icons.minimize else icons.minimizeInactive,
            contentDescription = "Minimize",
            hoverColor = style.colors.hoverBackground,
            pressColor = style.colors.pressBackground,
        )
        ControlButton(
            onClick = onMaximizeRestore,
            icon =
                if (isActive) {
                    if (isMaximized) icons.restore else icons.maximize
                } else {
                    if (isMaximized) icons.restoreInactive else icons.maximizeInactive
                },
            contentDescription = if (isMaximized) "Restore" else "Maximize",
            hoverColor = style.colors.hoverBackground,
            pressColor = style.colors.pressBackground,
        )
        ControlButton(
            onClick = onClose,
            icon = if (isActive) icons.close else icons.closeInactive,
            contentDescription = "Close",
            hoverColor = style.colors.closeHoverBackground,
            pressColor = style.colors.closePressBackground,
        )
    }
}

@Suppress("FunctionNaming")
@Composable
internal fun DialogCloseButton(
    onClick: () -> Unit,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val icons = linuxTitleBarIcons()
    val style = LocalTitleBarStyle.current

    ControlButton(
        onClick = onClick,
        icon = if (isActive) icons.close else icons.closeInactive,
        contentDescription = "Close",
        hoverColor = style.colors.closeHoverBackground,
        pressColor = style.colors.closePressBackground,
        modifier = modifier,
    )
}

@Suppress("FunctionNaming")
@Composable
private fun ControlButton(
    onClick: () -> Unit,
    icon: Painter,
    contentDescription: String,
    hoverColor: Color,
    pressColor: Color,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor =
        when {
            isPressed -> pressColor
            isHovered -> hoverColor
            else -> Color.Transparent
        }

    Box(
        modifier =
            modifier
                .size(BUTTON_SIZE)
                .hoverable(interactionSource)
                .background(bgColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(ICON_SIZE),
        )
    }
}
