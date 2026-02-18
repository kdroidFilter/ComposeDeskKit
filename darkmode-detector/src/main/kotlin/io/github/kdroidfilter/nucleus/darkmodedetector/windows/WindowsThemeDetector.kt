// Inspired by the code from the jSystemThemeDetector project:
// https://github.com/Dansoftowner/jSystemThemeDetector/blob/master/src/main/java/com/jthemedetecor/WindowsThemeDetector.java

package io.github.kdroidfilter.nucleus.darkmodedetector.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import io.github.kdroidfilter.nucleus.darkmodedetector.debugln
import io.github.kdroidfilter.nucleus.darkmodedetector.errorln
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

private const val TAG = "WindowsThemeDetector"

/**
 * WindowsThemeDetector uses a JNI native library to read the Windows registry value:
 * HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize\AppsUseLightTheme
 *
 * If this value = 0 => Dark mode. If this value = 1 => Light mode.
 *
 * The detector also monitors the registry for changes in real-time by
 * calling RegNotifyChangeKeyValue on a background thread.
 */
@Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
internal object WindowsThemeDetector {
    private val listeners: MutableSet<Consumer<Boolean>> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var detectorThread: Thread? = null

    /**
     * Returns true if the system is in dark mode (i.e. registry value is 0),
     * or false if the system is in light mode (registry value is 1 or doesn't exist).
     */
    fun isDark(): Boolean = NativeWindowsBridge.nativeIsDark()

    fun registerListener(listener: Consumer<Boolean>) {
        synchronized(this) {
            listeners.add(listener)
            if (listeners.size == 1 || detectorThread?.isInterrupted == true) {
                startMonitoringThread()
            }
        }
    }

    fun removeListener(listener: Consumer<Boolean>) {
        synchronized(this) {
            listeners.remove(listener)
            if (listeners.isEmpty()) {
                detectorThread?.interrupt()
                detectorThread = null
            }
        }
    }

    private fun startMonitoringThread() {
        val thread =
            object : Thread("Windows Theme Detector Thread") {
                private var lastValue = isDark()

                override fun run() {
                    debugln(TAG) { "Windows theme monitor thread started" }

                    val hKey = NativeWindowsBridge.nativeOpenMonitorKey()
                    if (hKey == 0L) {
                        errorln(TAG) { "nativeOpenMonitorKey failed" }
                        return
                    }

                    try {
                        while (!isInterrupted) {
                            val ok = NativeWindowsBridge.nativeWaitForChange(hKey)
                            if (!ok) {
                                errorln(TAG) { "nativeWaitForChange failed" }
                                return
                            }

                            val currentValue = isDark()
                            if (currentValue != lastValue) {
                                lastValue = currentValue
                                debugln(TAG) { "Windows theme changed => dark: $currentValue" }
                                val snapshot = listeners.toList()
                                for (l in snapshot) {
                                    try {
                                        l.accept(currentValue)
                                    } catch (e: RuntimeException) {
                                        errorln(TAG, e) { "Error while notifying listener" }
                                    }
                                }
                            }
                        }
                    } finally {
                        debugln(TAG) { "Detector thread closing registry key" }
                        NativeWindowsBridge.nativeCloseKey(hKey)
                    }
                }
            }
        thread.isDaemon = true
        detectorThread = thread
        thread.start()
    }
}

/**
 * Composable function that returns whether Windows is currently in dark mode.
 */
@Composable
internal fun isWindowsInDarkMode(): Boolean {
    val darkModeState = remember { mutableStateOf(WindowsThemeDetector.isDark()) }

    DisposableEffect(Unit) {
        debugln(TAG) { "Registering Windows dark mode listener in Compose" }
        val listener =
            Consumer<Boolean> { newValue ->
                debugln(TAG) { "Windows dark mode updated: $newValue" }
                darkModeState.value = newValue
            }

        WindowsThemeDetector.registerListener(listener)

        onDispose {
            debugln(TAG) { "Removing Windows dark mode listener in Compose" }
            WindowsThemeDetector.removeListener(listener)
        }
    }

    return darkModeState.value
}
