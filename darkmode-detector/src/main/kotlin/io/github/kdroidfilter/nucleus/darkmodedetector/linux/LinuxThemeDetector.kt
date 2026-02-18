package io.github.kdroidfilter.nucleus.darkmodedetector.linux

import androidx.compose.runtime.Composable
import io.github.kdroidfilter.nucleus.core.runtime.LinuxDesktopEnvironment

@Composable
fun isLinuxInDarkMode(): Boolean {
    return when (LinuxDesktopEnvironment.Current) {
        LinuxDesktopEnvironment.KDE -> isKdeInDarkMode()
        LinuxDesktopEnvironment.Gnome -> isGnomeInDarkMode()
        LinuxDesktopEnvironment.XFCE -> detectXfceDarkTheme() ?: false
        LinuxDesktopEnvironment.Cinnamon -> detectCinnamonDarkTheme() ?: false
        LinuxDesktopEnvironment.Mate -> detectMateDarkTheme() ?: false
        else -> false
    }
}
