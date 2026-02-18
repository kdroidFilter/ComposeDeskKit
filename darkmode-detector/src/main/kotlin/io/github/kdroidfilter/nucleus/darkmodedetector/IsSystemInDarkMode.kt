package io.github.kdroidfilter.nucleus.darkmodedetector

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalInspectionMode
import io.github.kdroidfilter.nucleus.core.runtime.Platform
import io.github.kdroidfilter.nucleus.darkmodedetector.linux.isLinuxInDarkMode
import io.github.kdroidfilter.nucleus.darkmodedetector.mac.isMacOsInDarkMode
import io.github.kdroidfilter.nucleus.darkmodedetector.windows.isWindowsInDarkMode

/**
 * Composable function that returns whether the system is in dark mode.
 * It handles macOS, Windows, and Linux.
 */
@Composable
fun isSystemInDarkMode(): Boolean {
    val isInPreview = LocalInspectionMode.current
    if (isInPreview) {
        return isSystemInDarkTheme()
    }

    return when (Platform.Current) {
        Platform.MacOS -> isMacOsInDarkMode()
        Platform.Windows -> isWindowsInDarkMode()
        Platform.Linux -> isLinuxInDarkMode()
        else -> false
    }
}
