package io.github.kdroidfilter.nucleus.window.utils

import java.util.Locale

enum class DesktopPlatform {
    Linux,
    Windows,
    MacOS,
    Unknown,
    ;

    companion object {
        val Current: DesktopPlatform by lazy {
            val os = System.getProperty("os.name", "unknown").lowercase(Locale.ENGLISH)
            when {
                os.contains("mac") || os.contains("darwin") -> MacOS
                os.contains("win") -> Windows
                os.contains("nux") || os.contains("nix") || os.contains("aix") -> Linux
                else -> Unknown
            }
        }
    }
}
