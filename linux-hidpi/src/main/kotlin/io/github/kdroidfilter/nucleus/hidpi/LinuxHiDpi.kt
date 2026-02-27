package io.github.kdroidfilter.nucleus.hidpi

/**
 * Returns the native HiDPI scale factor for the current Linux display,
 * mirroring the detection logic of JetBrains Runtime (systemScale.c).
 *
 * Sources consulted in priority order:
 *   1. `J2D_UISCALE`   — explicit JVM override (env var)
 *   2. GSettings       — GNOME `org.gnome.desktop.interface` → `scaling-factor`
 *   3. `GDK_SCALE`     — GTK / GNOME session variable
 *   4. `GDK_DPI_SCALE` — GTK fractional DPI multiplier
 *   5. `Xft.dpi`       — X Resource Manager (KDE, legacy GNOME, …)
 *
 * @return A positive scale factor (e.g. `2.0` for a 200 % HiDPI display),
 *         or `0.0` when the scale cannot be determined (let the JVM decide).
 *
 * **Call this before AWT initialises** (i.e. before `application {}`) and
 * apply the result:
 * ```kotlin
 * val scale = getLinuxNativeScaleFactor()
 * if (scale > 0.0) System.setProperty("sun.java2d.uiScale", scale.toString())
 * ```
 * This function is a no-op on non-Linux platforms and returns `0.0`.
 */
fun getLinuxNativeScaleFactor(): Double {
    if (!System.getProperty("os.name").contains("Linux", ignoreCase = true)) return 0.0
    return try {
        HiDpiLinuxBridge.nativeGetScaleFactor()
    } catch (_: Throwable) {
        // JNI unavailable — fall back to environment variables only
        System.getenv("J2D_UISCALE")?.toDoubleOrNull()?.takeIf { it > 0 }
            ?: System.getenv("GDK_SCALE")?.toDoubleOrNull()?.takeIf { it > 0 }
            ?: System.getenv("GDK_DPI_SCALE")?.toDoubleOrNull()?.takeIf { it > 0 }
            ?: 0.0
    }
}
