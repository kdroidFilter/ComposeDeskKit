package io.github.kdroidfilter.nucleus.darkmodedetector.linux

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import io.github.kdroidfilter.nucleus.core.runtime.LinuxDesktopEnvironment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

data class KdeThemeState(
    val windowTheme: Boolean?,
    val panelTheme: Boolean?,
) {
    val isMixed: Boolean get() = windowTheme != null && panelTheme != null && windowTheme != panelTheme
    val isFullDark: Boolean get() = windowTheme == true && panelTheme == true
    val isFullLight: Boolean get() = windowTheme == false && panelTheme == false
}

/** Singleton detector: one monitor, shared state for the whole app. */
object KdeThemeDetector {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val state = MutableStateFlow<KdeThemeState?>(null)
    private val startStopMutex = Mutex()

    @Volatile private var dbusProcess: Process? = null
    @Volatile private var dbusReaderJob: Job? = null
    @Volatile private var pollerJob: Job? = null

    fun themeState(): StateFlow<KdeThemeState?> = state.asStateFlow()

    suspend fun startIfNeeded() {
        startStopMutex.withLock {
            if (started.get()) return
            started.set(true)

            state.value = computeSnapshot()

            if (isCommandAvailable("dbus-monitor")) {
                startDbusMonitor()
            } else {
                startPollingFallback()
            }
        }
    }

    suspend fun stop() {
        startStopMutex.withLock {
            started.set(false)
            dbusReaderJob?.cancel()
            dbusReaderJob = null
            dbusProcess?.let { p ->
                try { p.destroy() } catch (_: Throwable) {}
                dbusProcess = null
            }
            pollerJob?.cancel()
            pollerJob = null
        }
    }

    private fun startDbusMonitor() {
        pollerJob?.cancel()
        pollerJob = null

        dbusReaderJob = scope.launch {
            while (started.get()) {
                dbusProcess = try {
                    ProcessBuilder(
                        "dbus-monitor",
                        "--session",
                        "type='signal',interface='org.kde.KGlobalSettings',member='notifyChange'",
                        "type='signal',interface='org.kde.PlasmaShell',member='themeChanged'",
                        "type='signal',interface='org.kde.kdeglobals',member='configChanged'",
                    )
                        .redirectErrorStream(true)
                        .start()
                } catch (_: Exception) {
                    startPollingFallback()
                    return@launch
                }

                try {
                    BufferedReader(InputStreamReader(dbusProcess!!.inputStream)).use { reader ->
                        var debounceJob: Job? = null

                        state.emitSafely(computeSnapshot())

                        while (started.get()) {
                            val line = reader.readLine() ?: break
                            if (line.contains("notifyChange") ||
                                line.contains("themeChanged") ||
                                line.contains("configChanged")
                            ) {
                                debounceJob?.cancel()
                                debounceJob = launch {
                                    delay(200)
                                    state.emitIfChanged(computeSnapshot())
                                }
                            }
                        }

                        debounceJob?.join()
                    }
                } catch (_: CancellationException) {
                    // Normal shutdown
                } catch (_: Exception) {
                    // Reader crashed; will restart below
                } finally {
                    try { dbusProcess?.destroy() } catch (_: Throwable) {}
                    dbusProcess = null
                }

                if (started.get()) delay(500)
            }
        }
    }

    private fun startPollingFallback() {
        dbusReaderJob?.cancel()
        dbusReaderJob = null
        dbusProcess?.let { try { it.destroy() } catch (_: Throwable) {} }
        dbusProcess = null

        pollerJob = scope.launch {
            var last: KdeThemeState? = state.value
            while (started.get()) {
                val snap = computeSnapshot()
                if (snap != last) {
                    last = snap
                    state.emitSafely(snap)
                }
                delay(750)
            }
        }
    }

    private fun computeSnapshot(): KdeThemeState =
        KdeThemeState(
            windowTheme = detectKdeDarkTheme(),
            panelTheme = detectKdePanelDark(),
        )

    private fun isCommandAvailable(cmd: String): Boolean {
        return try {
            val p = ProcessBuilder("which", cmd)
                .redirectErrorStream(true)
                .start()
            p.waitFor(300, TimeUnit.MILLISECONDS) && p.exitValue() == 0
        } catch (_: Exception) {
            false
        }
    }

    private suspend fun MutableStateFlow<KdeThemeState?>.emitSafely(v: KdeThemeState?) {
        try {
            emit(v)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Swallow any other exception
        }
    }

    private suspend fun MutableStateFlow<KdeThemeState?>.emitIfChanged(v: KdeThemeState?) {
        if (value != v) emitSafely(v)
    }
}

/**
 * Starts the singleton monitor (idempotent) and returns the latest KDE theme state
 * as a Compose state value.
 */
@Composable
fun rememberKdeDarkModeState(): KdeThemeState? {
    var current by remember { mutableStateOf<KdeThemeState?>(null) }

    DisposableEffect(Unit) {
        val job = Job()
        val scope = CoroutineScope(Dispatchers.IO + job)
        scope.launch {
            KdeThemeDetector.startIfNeeded()
        }
        onDispose {
            job.cancel()
        }
    }

    val flow = remember { KdeThemeDetector.themeState() }
    val collected by flow.collectAsState(initial = null)
    LaunchedEffect(collected) { current = collected }

    return current
}

/** Convenience boolean: prefer window theme, then panel, else false. */
@Composable
fun isKdeInDarkMode(): Boolean {
    val st = rememberKdeDarkModeState()
    return st?.windowTheme ?: st?.panelTheme ?: false
}

/* ============================================================================
 * KDE-specific detection (pure functions)
 * ========================================================================== */

private fun isTwilightLaf(lookAndFeel: String?): Boolean {
    if (lookAndFeel.isNullOrBlank()) return false
    val s = lookAndFeel.lowercase()
    return s.contains("breezetwilight") || (s.contains("breeze") && s.contains("twilight"))
}

private fun runKReadConfig(file: String, group: String, key: String): String? {
    val cmds = listOf(
        arrayOf("kreadconfig6", "--file", file, "--group", group, "--key", key),
        arrayOf("kreadconfig5", "--file", file, "--group", group, "--key", key),
    )
    for (cmd in cmds) {
        try {
            val p = ProcessBuilder(*cmd).redirectErrorStream(true).start()
            val out = BufferedReader(InputStreamReader(p.inputStream)).use { it.readLine()?.trim() }
            p.waitFor(300, TimeUnit.MILLISECONDS)
            if (!out.isNullOrBlank()) return out
        } catch (_: Exception) {}
    }
    return null
}

private fun normalizePlasmaThemeName(rawTheme: String?, lookAndFeel: String?): String? {
    if (isTwilightLaf(lookAndFeel)) return "breeze-dark"
    if (rawTheme.isNullOrBlank() || rawTheme.equals("default", true)) return "breeze"
    return when (rawTheme.lowercase()) {
        "org.kde.breezedark.desktop", "breeze-dark", "breezedark" -> "breeze-dark"
        "org.kde.breeze.desktop", "breeze" -> "breeze"
        else -> rawTheme
    }
}

private fun firstReadable(vararg paths: String): File? =
    paths.asSequence().map(::File).firstOrNull { it.isFile && it.canRead() }

private fun plasmaThemeDirCandidates(themeName: String): List<File> {
    val home = System.getenv("HOME") ?: System.getProperty("user.home")
    val variants = listOf(
        themeName,
        themeName.replace(' ', '_'),
        themeName.replace(' ', '-'),
        themeName.lowercase(),
        themeName.lowercase().replace(' ', '_'),
        themeName.lowercase().replace(' ', '-'),
    ).distinct()

    val roots = listOf(
        "$home/.local/share/plasma/desktoptheme",
        "/usr/share/plasma/desktoptheme",
    )

    val dirs = mutableListOf<File>()
    for (v in variants) for (r in roots) dirs += File("$r/$v")
    return dirs
}

private fun parseIni(file: File): Map<String, Map<String, String>> {
    val result = mutableMapOf<String, MutableMap<String, String>>()
    var section = ""
    file.forEachLine { raw ->
        val line = raw.trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) return@forEachLine
        if (line.startsWith("[") && line.endsWith("]")) {
            section = line.substring(1, line.length - 1)
            result.putIfAbsent(section, mutableMapOf())
        } else {
            val idx = line.indexOf('=')
            if (idx > 0) {
                val k = line.substring(0, idx).trim()
                val v = line.substring(idx + 1).trim()
                result.getOrPut(section) { mutableMapOf() }[k] = v
            }
        }
    }
    return result
}

private fun parseKdeRgb(value: String): Triple<Int, Int, Int>? {
    val nums = value.split(',', ' ')
        .mapNotNull { it.trim().toIntOrNull() }
        .take(3)
    return if (nums.size == 3) {
        Triple(nums[0].coerceIn(0, 255), nums[1].coerceIn(0, 255), nums[2].coerceIn(0, 255))
    } else {
        null
    }
}

private fun isRgbDark(r: Int, g: Int, b: Int): Boolean {
    val lum = 0.299 * r + 0.587 * g + 0.114 * b
    return lum < 140.0
}

private fun deriveDarkFromColorsMap(colors: Map<String, Map<String, String>>): Boolean? {
    val sectionsInPriority = listOf("Colors:Panel", "Colors:Window", "Colors:View", "Colors:Button")
    for (sec in sectionsInPriority) {
        val bg = colors[sec]?.get("BackgroundNormal") ?: continue
        val rgb = parseKdeRgb(bg) ?: continue
        return isRgbDark(rgb.first, rgb.second, rgb.third)
    }
    colors.values.forEach { section ->
        section["BackgroundNormal"]?.let { v ->
            parseKdeRgb(v)?.let { (r, g, b) -> return isRgbDark(r, g, b) }
        }
    }
    return null
}

private fun resolvePlasmaColorsFile(themeName: String, visited: MutableSet<String> = mutableSetOf()): File? {
    val key = themeName.lowercase()
    if (!visited.add(key)) return null

    val dir = plasmaThemeDirCandidates(themeName).firstOrNull { it.isDirectory && it.canRead() }
    val directColors = dir?.let { firstReadable("${it.path}/colors") }
    if (directColors != null) return directColors

    val meta = dir?.let { firstReadable("${it.path}/metadata.desktop") }
    if (meta != null) {
        val ini = parseIni(meta)
        val inherits = ini.values.firstNotNullOfOrNull { map ->
            map.entries.firstOrNull { (k, _) -> k.equals("Inherits", true) }?.value
        }
        if (!inherits.isNullOrBlank()) {
            inherits.split(',', ';')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { parent ->
                    resolvePlasmaColorsFile(parent, visited)?.let { return it }
                }
        }
    }
    return null
}

private fun detectKdeDarkTheme(): Boolean? {
    if (LinuxDesktopEnvironment.Current != LinuxDesktopEnvironment.KDE) return null
    return try {
        val scheme = runKReadConfig(file = "kdeglobals", group = "General", key = "ColorScheme") ?: return null
        val home = System.getenv("HOME") ?: System.getProperty("user.home")
        val variants = listOf(
            scheme, scheme.replace(' ', '_'), scheme.replace(' ', '-'),
            scheme.lowercase(), scheme.lowercase().replace(' ', '_'), scheme.lowercase().replace(' ', '-'),
        ).distinct()
        val schemeFile = variants
            .flatMap { v -> listOf("$home/.local/share/color-schemes/$v.colors", "/usr/share/color-schemes/$v.colors") }
            .map(::File).firstOrNull { it.isFile && it.canRead() } ?: return null
        val colors = parseIni(schemeFile)
        deriveDarkFromColorsMap(colors)
    } catch (_: Exception) {
        try {
            val laf = runKReadConfig(file = "kdeglobals", group = "KDE", key = "LookAndFeelPackage")
            laf?.contains("dark", ignoreCase = true)
        } catch (_: Exception) {
            null
        }
    }
}

private fun detectKdePanelDark(): Boolean? {
    if (LinuxDesktopEnvironment.Current != LinuxDesktopEnvironment.KDE) return null
    return try {
        val rawTheme = runKReadConfig("plasmarc", "Theme", "name")
            ?: runKReadConfig("plasmashellrc", "Theme", "name")
        val lookAndFeel = runKReadConfig("kdeglobals", "KDE", "LookAndFeelPackage")
        val theme = normalizePlasmaThemeName(rawTheme, lookAndFeel) ?: return null

        val colorsFile = resolvePlasmaColorsFile(theme)
        if (colorsFile != null) {
            val colors = parseIni(colorsFile)
            val derived = deriveDarkFromColorsMap(colors)
            if (derived != null) return derived
        }

        val scheme = runKReadConfig("kdeglobals", "General", "ColorScheme")
        when {
            scheme?.contains("dark", true) == true -> true
            scheme?.contains("light", true) == true -> false
            lookAndFeel?.contains("dark", true) == true -> true
            lookAndFeel?.contains("light", true) == true -> false
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
