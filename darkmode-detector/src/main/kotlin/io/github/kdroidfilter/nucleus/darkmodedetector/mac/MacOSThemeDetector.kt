// Inspired by the code from the jSystemThemeDetector project:
// https://github.com/Dansoftowner/jSystemThemeDetector/blob/master/src/main/java/com/jthemedetecor/MacOSThemeDetector.java

package io.github.kdroidfilter.nucleus.darkmodedetector.mac

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import de.jangassen.jfa.foundation.Foundation
import de.jangassen.jfa.foundation.ID
import io.github.kdroidfilter.nucleus.darkmodedetector.debugln
import io.github.kdroidfilter.nucleus.darkmodedetector.errorln
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.regex.Pattern

private const val TAG = "MacOSThemeDetector"

/**
 * MacOSThemeDetector registers an observer with NSDistributedNotificationCenter
 * to detect theme changes in macOS. It reads the system preference "AppleInterfaceStyle"
 * (which is "Dark" when in dark mode) from NSUserDefaults.
 */
@Suppress("TooGenericExceptionCaught")
internal object MacOSThemeDetector {
    private val listeners: MutableSet<Consumer<Boolean>> = ConcurrentHashMap.newKeySet()
    private val darkPattern: Pattern = Pattern.compile(".*dark.*", Pattern.CASE_INSENSITIVE)

    private val callbackExecutor =
        Executors.newSingleThreadExecutor { r ->
            Thread(r, "MacOS Theme Detector Thread").apply { isDaemon = true }
        }

    @JvmStatic
    private val themeChangedCallback =
        object : com.sun.jna.Callback {
            fun callback() {
                callbackExecutor.execute {
                    val isDark = isDark()
                    debugln(TAG) { "Theme change detected. Dark mode: $isDark" }
                    notifyListeners(isDark)
                }
            }
        }

    init {
        initObserver()
    }

    private fun initObserver() {
        debugln(TAG) { "Initializing macOS theme observer" }
        val pool = Foundation.NSAutoreleasePool()
        try {
            val delegateClass: ID =
                Foundation.allocateObjcClassPair(
                    Foundation.getObjcClass("NSObject"),
                    "NSColorChangesObserver",
                )
            if (!ID.NIL.equals(delegateClass)) {
                val selector = Foundation.createSelector("handleAppleThemeChanged:")
                val added = Foundation.addMethod(delegateClass, selector, themeChangedCallback, "v@")
                if (!added) {
                    errorln(TAG) { "Failed to add observer method to NSColorChangesObserver" }
                }
                Foundation.registerObjcClassPair(delegateClass)
            }
            val delegateObj = Foundation.invoke("NSColorChangesObserver", "new")
            Foundation.invoke(
                Foundation.invoke("NSDistributedNotificationCenter", "defaultCenter"),
                "addObserver:selector:name:object:",
                delegateObj,
                Foundation.createSelector("handleAppleThemeChanged:"),
                Foundation.nsString("AppleInterfaceThemeChangedNotification"),
                ID.NIL,
            )
            debugln(TAG) { "Observer successfully registered" }
        } finally {
            pool.drain()
        }
    }

    fun isDark(): Boolean {
        val pool = Foundation.NSAutoreleasePool()
        return try {
            val userDefaults = Foundation.invoke("NSUserDefaults", "standardUserDefaults")
            val styleKey = Foundation.nsString("AppleInterfaceStyle")
            val result = Foundation.invoke(userDefaults, "objectForKey:", styleKey)
            val styleString = Foundation.toStringViaUTF8(result)
            darkPattern.matcher(styleString ?: "").matches()
        } catch (e: Exception) {
            errorln(TAG, e) { "Error reading system theme" }
            false
        } finally {
            pool.drain()
        }
    }

    fun registerListener(listener: Consumer<Boolean>) {
        listeners.add(listener)
    }

    fun removeListener(listener: Consumer<Boolean>) {
        listeners.remove(listener)
    }

    private fun notifyListeners(isDark: Boolean) {
        listeners.forEach { it.accept(isDark) }
    }
}

/**
 * A helper composable function that returns the current macOS dark mode state,
 * updating automatically when the system theme changes.
 */
@Composable
internal fun isMacOsInDarkMode(): Boolean {
    val darkModeState = remember { mutableStateOf(MacOSThemeDetector.isDark()) }
    DisposableEffect(Unit) {
        debugln(TAG) { "Registering macOS dark mode listener in Compose" }
        val listener =
            Consumer<Boolean> { newValue ->
                debugln(TAG) { "Compose macOS dark mode updated: $newValue" }
                darkModeState.value = newValue
            }
        MacOSThemeDetector.registerListener(listener)
        onDispose {
            debugln(TAG) { "Removing macOS dark mode listener in Compose" }
            MacOSThemeDetector.removeListener(listener)
        }
    }
    return darkModeState.value
}
