package io.github.kdroidfilter.nucleus.darkmodedetector.linux

import java.io.BufferedReader
import java.io.InputStreamReader

internal fun detectMateDarkTheme(): Boolean? {
    return try {
        val p = Runtime.getRuntime().exec(arrayOf("gsettings", "get", "org.mate.interface", "gtk-theme"))
        val theme = BufferedReader(InputStreamReader(p.inputStream)).use { it.readLine()?.trim('\'', '"') }
        theme?.contains("dark", ignoreCase = true)
    } catch (_: Exception) {
        null
    }
}
