package io.github.kdroidfilter.nucleus.darkmodedetector.linux

import java.io.BufferedReader
import java.io.InputStreamReader

internal fun detectXfceDarkTheme(): Boolean? =
    try {
        val p = Runtime.getRuntime().exec(arrayOf("xfconf-query", "-c", "xsettings", "-p", "/Net/ThemeName"))
        val theme = BufferedReader(InputStreamReader(p.inputStream)).use { it.readLine()?.trim() }
        theme?.contains("dark", ignoreCase = true)
    } catch (_: Exception) {
        null
    }
