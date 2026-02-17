package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import com.jetbrains.JBR
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import java.awt.event.MouseEvent

@Suppress("FunctionNaming")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun DecoratedDialogScope.LinuxDialogTitleBar(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedDialogState) -> Unit = {},
) {
    val style = LocalTitleBarStyle.current
    val dialogState = LocalDecoratedDialogState.current

    val windowState =
        DecoratedWindowState(
            isActive = dialogState.isActive,
            isFullscreen = false,
            isMinimized = false,
            isMaximized = false,
        )

    TitleBarImpl(
        window = window,
        state = windowState,
        modifier =
            modifier.onPointerEvent(PointerEventType.Press, PointerEventPass.Main) {
                if (
                    this.currentEvent.button == PointerButton.Primary &&
                    this.currentEvent.changes.any { changed -> !changed.isConsumed }
                ) {
                    JBR.getWindowMove()?.startMovingTogetherWithMouse(window, MouseEvent.BUTTON1)
                }
            },
        style = style,
        applyTitleBar = { _, _ -> PaddingValues(0.dp) },
    ) { _ ->
        DialogCloseButton(
            onClick = { window.dispose() },
            isActive = dialogState.isActive,
            modifier = Modifier.align(Alignment.End),
        )
        content(dialogState)
    }
}
