package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import java.awt.Frame
import java.awt.event.MouseEvent

@Suppress("FunctionNaming")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DecoratedWindowScope.LinuxTitleBar(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit = {},
) {
    val style = LocalTitleBarStyle.current
    var lastPress = 0L
    val viewConfig = LocalViewConfiguration.current

    TitleBarImpl(
        window = window,
        state = state,
        modifier =
            modifier.onPointerEvent(PointerEventType.Press, PointerEventPass.Main) {
                if (
                    this.currentEvent.button == PointerButton.Primary &&
                    this.currentEvent.changes.any { changed -> !changed.isConsumed }
                ) {
                    JBR.getWindowMove()?.startMovingTogetherWithMouse(window, MouseEvent.BUTTON1)
                    if (
                        System.currentTimeMillis() - lastPress in
                        viewConfig.doubleTapMinTimeMillis..viewConfig.doubleTapTimeoutMillis
                    ) {
                        if (state.isMaximized) {
                            window.extendedState = Frame.NORMAL
                        } else {
                            window.extendedState = Frame.MAXIMIZED_BOTH
                        }
                    }
                    lastPress = System.currentTimeMillis()
                }
            },
        style = style,
        applyTitleBar = { _, _ -> PaddingValues(0.dp) },
    ) { currentState ->
        WindowControlArea(
            onMinimize = { window.extendedState = Frame.ICONIFIED },
            onMaximizeRestore = {
                window.extendedState =
                    if (window.extendedState == Frame.MAXIMIZED_BOTH) {
                        Frame.NORMAL
                    } else {
                        Frame.MAXIMIZED_BOTH
                    }
            },
            onClose = { window.dispose() },
            isMaximized = currentState.isMaximized,
            isActive = currentState.isActive,
            modifier = Modifier.align(androidx.compose.ui.Alignment.End),
        )
        content(currentState)
    }
}
