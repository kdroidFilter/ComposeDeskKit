// Inspired by the code from the jSystemThemeDetector project:
// https://github.com/Dansoftowner/jSystemThemeDetector/blob/master/src/main/java/com/jthemedetecor/WindowsThemeDetector.java

package io.github.kdroidfilter.nucleus.darkmodedetector.windows

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.sun.jna.Native
import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT
import com.sun.jna.platform.win32.WinNT.KEY_READ
import com.sun.jna.platform.win32.WinReg
import com.sun.jna.platform.win32.WinReg.HKEY
import com.sun.jna.ptr.IntByReference
import io.github.kdroidfilter.nucleus.core.runtime.Platform
import io.github.kdroidfilter.nucleus.darkmodedetector.debugln
import io.github.kdroidfilter.nucleus.darkmodedetector.errorln
import io.github.kdroidfilter.nucleus.darkmodedetector.isSystemInDarkMode
import java.awt.Window
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

private const val TAG = "WindowsThemeDetector"

/**
 * WindowsThemeDetector uses JNA to read the Windows registry value:
 * HKEY_CURRENT_USER\Software\Microsoft\Windows\CurrentVersion\Themes\Personalize\AppsUseLightTheme
 *
 * If this value = 0 => Dark mode. If this value = 1 => Light mode.
 *
 * The detector also monitors the registry for changes in real-time by
 * calling RegNotifyChangeKeyValue on a background thread.
 */
@Suppress("TooGenericExceptionCaught", "NestedBlockDepth")
internal object WindowsThemeDetector {
    private const val REGISTRY_PATH = "Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize"
    private const val REGISTRY_VALUE = "AppsUseLightTheme"

    private val listeners: MutableSet<Consumer<Boolean>> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var detectorThread: Thread? = null

    /**
     * Returns true if the system is in dark mode (i.e. registry value is 0),
     * or false if the system is in light mode (registry value is 1 or doesn't exist).
     */
    fun isDark(): Boolean =
        Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_VALUE) &&
            Advapi32Util.registryGetIntValue(WinReg.HKEY_CURRENT_USER, REGISTRY_PATH, REGISTRY_VALUE) == 0

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

                    val hKeyRef = WinReg.HKEYByReference()
                    val openErr =
                        Advapi32.INSTANCE.RegOpenKeyEx(
                            WinReg.HKEY_CURRENT_USER,
                            REGISTRY_PATH,
                            0,
                            KEY_READ,
                            hKeyRef,
                        )
                    if (openErr != WinError.ERROR_SUCCESS) {
                        errorln(TAG) { "RegOpenKeyEx failed with code $openErr" }
                        return
                    }
                    val hKey: HKEY = hKeyRef.value

                    try {
                        while (!isInterrupted) {
                            val notifyErr =
                                Advapi32.INSTANCE.RegNotifyChangeKeyValue(
                                    hKey,
                                    false,
                                    WinNT.REG_NOTIFY_CHANGE_LAST_SET,
                                    null,
                                    false,
                                )
                            if (notifyErr != WinError.ERROR_SUCCESS) {
                                errorln(TAG) { "RegNotifyChangeKeyValue failed with code $notifyErr" }
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
                        Advapi32Util.registryCloseKey(hKey)
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

/**
 * Sets the dark mode title bar appearance for a Windows application window.
 *
 * @param dark Boolean value indicating whether the title bar should use dark mode.
 *    Defaults to the result of [isSystemInDarkMode].
 */
@Suppress("TooGenericExceptionCaught", "MagicNumber")
@Composable
fun Window.setWindowsAdaptiveTitleBar(dark: Boolean = isSystemInDarkMode()) {
    try {
        if (Platform.Current == Platform.Windows) {
            val hwnd = WinDef.HWND(Native.getComponentPointer(this))
            val darkModeEnabled = IntByReference(if (dark) 1 else 0)
            DwmApi.INSTANCE.DwmSetWindowAttribute(
                hwnd,
                DwmApi.DWMWA_USE_IMMERSIVE_DARK_MODE,
                darkModeEnabled.pointer,
                4,
            )
        }
    } catch (e: Exception) {
        debugln(TAG) { "Failed to set dark mode: ${e.message}" }
    }
}
