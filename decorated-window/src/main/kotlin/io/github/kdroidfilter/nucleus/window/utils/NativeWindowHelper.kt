package io.github.kdroidfilter.nucleus.window.utils

import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import io.github.kdroidfilter.nucleus.core.runtime.Platform
import io.github.kdroidfilter.nucleus.window.utils.windows.NativeWinBridge
import java.awt.Frame
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.SwingUtilities

@Suppress("TooGenericExceptionCaught")
internal object NativeWindowHelper {
    private val logger = Logger.getLogger(NativeWindowHelper::class.java.simpleName)

    /**
     * Shows the window in maximized state using platform-native APIs.
     * On Windows, uses JAWT + Win32 ShowWindow(SW_SHOWMAXIMIZED) which
     * atomically shows and maximizes the window with no visual glitch.
     *
     * Ensures the native peer is created (via addNotify) before attempting
     * the native call, and syncs both AWT and Compose state afterwards.
     */
    fun showMaximized(
        window: java.awt.Window,
        state: WindowState,
    ) {
        try {
            // Note: Don't force addNotify() here - let AWT/Compose create the peer naturally.
            // Forcing it causes issues because it triggers layout before Compose measures components.

            val success =
                when (Platform.Current) {
                    Platform.Windows -> showMaximizedWindows(window)
                    else -> false
                }

            if (success) {
                // Sync Java and Compose state
                (window as? Frame)?.let {
                    it.extendedState = it.extendedState or Frame.MAXIMIZED_BOTH
                }
                state.placement = WindowPlacement.Maximized
            } else {
                // Fallback: set AWT extended state directly
                (window as? Frame)?.let {
                    it.extendedState = it.extendedState or Frame.MAXIMIZED_BOTH
                }
                state.placement = WindowPlacement.Maximized
            }

            // Bring window to front after maximize - defer to ensure native peer is ready
            SwingUtilities.invokeLater {
                window.toFront()
                window.requestFocus()
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to apply native maximized placement", e)
        }
    }

    private fun showMaximizedWindows(window: java.awt.Window): Boolean {
        if (!NativeWinBridge.isLoaded) {
            logger.warning("nucleus_windows native library not loaded, falling back to AWT")
            return false
        }
        return NativeWinBridge.nativeShowMaximized(window)
    }

    /**
     * Brings the window to foreground using native Win32 API.
     * More reliable than toFront() on Windows.
     */
    fun bringToForeground(window: java.awt.Window) {
        try {
            if (Platform.Current == Platform.Windows) {
                if (!NativeWinBridge.isLoaded) {
                    logger.warning("nucleus_windows native library not loaded")
                    return
                }
                NativeWinBridge.nativeBringToForeground(window)
            }
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to bring window to foreground", e)
        }
    }
}
