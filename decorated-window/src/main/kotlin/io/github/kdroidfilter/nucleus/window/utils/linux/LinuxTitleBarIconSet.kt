package io.github.kdroidfilter.nucleus.window.utils.linux

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource

internal data class LinuxTitleBarIconSet(
    val close: Painter,
    val closeInactive: Painter,
    val minimize: Painter,
    val minimizeInactive: Painter,
    val maximize: Painter,
    val maximizeInactive: Painter,
    val restore: Painter,
    val restoreInactive: Painter,
)

@Suppress("DEPRECATION")
@Composable
internal fun linuxTitleBarIcons(de: LinuxDesktopEnvironment = LinuxDesktopEnvironment.Current): LinuxTitleBarIconSet {
    val prefix =
        when (de) {
            LinuxDesktopEnvironment.KDE -> "nucleus/window/icons/linux/kde"
            else -> "nucleus/window/icons/linux/gnome"
        }
    return LinuxTitleBarIconSet(
        close = painterResource("$prefix/close.svg"),
        closeInactive = painterResource("$prefix/closeInactive.svg"),
        minimize = painterResource("$prefix/minimize.svg"),
        minimizeInactive = painterResource("$prefix/minimizeInactive.svg"),
        maximize = painterResource("$prefix/maximize.svg"),
        maximizeInactive = painterResource("$prefix/maximizeInactive.svg"),
        restore = painterResource("$prefix/restore.svg"),
        restoreInactive = painterResource("$prefix/restoreInactive.svg"),
    )
}
