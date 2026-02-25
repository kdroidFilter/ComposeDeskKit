package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import io.github.kdroidfilter.nucleus.window.styling.TitleBarStyle
import io.github.kdroidfilter.nucleus.window.utils.macos.JniMacTitleBarBridge
import io.github.kdroidfilter.nucleus.window.utils.macos.JniMacWindowUtil

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("FunctionNaming")
@Composable
internal fun DecoratedWindowScope.MacOSTitleBar(
    modifier: Modifier = Modifier,
    gradientStartColor: Color = Color.Unspecified,
    style: TitleBarStyle = LocalTitleBarStyle.current,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit = {},
) {
    DisposableEffect(window) {
        onDispose {
            val ptr = JniMacWindowUtil.getWindowPtr(window)
            if (ptr != 0L) JniMacTitleBarBridge.nativeResetTitleBar(ptr)
        }
    }

    val viewConfig = LocalViewConfiguration.current
    var lastPress = 0L

    TitleBarImpl(
        // Detect double-click to zoom/minimize respecting macOS system preference.
        // Uses Final pass so interactive Compose children (buttons) consume the
        // event first — only unconsumed double-clicks trigger the action.
        modifier =
            modifier.onPointerEvent(PointerEventType.Press, PointerEventPass.Final) {
                if (
                    this.currentEvent.button == PointerButton.Primary &&
                    this.currentEvent.changes.any { !it.isConsumed }
                ) {
                    val now = System.currentTimeMillis()
                    if (now - lastPress in viewConfig.doubleTapMinTimeMillis..viewConfig.doubleTapTimeoutMillis) {
                        val ptr = JniMacWindowUtil.getWindowPtr(window)
                        if (ptr != 0L && JniMacTitleBarBridge.isLoaded) {
                            JniMacTitleBarBridge.nativePerformTitleBarDoubleClickAction(ptr)
                        }
                    }
                    lastPress = now
                }
            },
        gradientStartColor = gradientStartColor,
        style = style,
        applyTitleBar = { height, titleBarState ->
            JniMacWindowUtil.applyWindowProperties(window)

            val ptr = JniMacWindowUtil.getWindowPtr(window)

            if (titleBarState.isFullscreen) {
                PaddingValues(start = 80.dp)
            } else {
                val leftInset =
                    if (ptr != 0L && JniMacTitleBarBridge.isLoaded) {
                        JniMacTitleBarBridge.nativeApplyTitleBar(ptr, height.value)
                    } else {
                        val shrink = minOf(height.value / 28f, 1f)
                        height.value + 2f * shrink * 20f
                    }
                PaddingValues(start = leftInset.dp)
            }
        },
        onPlace = {
            if (state.isFullscreen) {
                val ptr = JniMacWindowUtil.getWindowPtr(window)
                if (ptr != 0L && JniMacTitleBarBridge.isLoaded) {
                    JniMacTitleBarBridge.nativeUpdateFullScreenButtons(ptr)
                }
            }
        },
        // Window drag is handled natively by NucleusDragView installed in the titlebar.
        // No Compose pointer handler needed — the native view calls
        // performWindowDragWithEvent: which supports macOS window snapping/tiling.
        backgroundContent = {
            Spacer(modifier = Modifier.fillMaxSize())
        },
        content = content,
    )
}
