package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import io.github.kdroidfilter.nucleus.window.styling.TitleBarStyle
import io.github.kdroidfilter.nucleus.window.utils.macos.JniMacTitleBarBridge
import io.github.kdroidfilter.nucleus.window.utils.macos.JniMacWindowUtil

// In the JNI module fullscreen controls are always enabled — replacement buttons
// are installed in the content view automatically by the native observer.
fun Modifier.newFullscreenControls(newControls: Boolean = true): Modifier = this

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

    TitleBarImpl(
        modifier = modifier,
        gradientStartColor = gradientStartColor,
        style = style,
        applyTitleBar = { height, state ->
            // Apply AWT properties so content view extends into the title bar area.
            // Idempotent — safe to call on every layout pass.
            JniMacWindowUtil.applyWindowProperties(window)

            val ptr = JniMacWindowUtil.getWindowPtr(window)

            if (state.isFullscreen) {
                // In fullscreen the native observer has installed replacement buttons
                // in the content view. Update their position and reserve 80dp for them.
                if (ptr != 0L && JniMacTitleBarBridge.isLoaded) {
                    JniMacTitleBarBridge.nativeUpdateFullScreenButtons(ptr)
                }
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
        // Window drag is handled natively by NucleusDragView installed in the titlebar.
        // No Compose pointer handler needed — the native view calls
        // performWindowDragWithEvent: which supports macOS window snapping/tiling.
        backgroundContent = {
            Spacer(modifier = Modifier.fillMaxSize())
        },
        content = content,
    )
}
