package io.github.kdroidfilter.nucleus.window.utils.macos

import java.awt.Component
import java.awt.Window
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.RootPaneContainer

@Suppress("TooGenericExceptionCaught")
internal object JniMacWindowUtil {
    private val logger = Logger.getLogger(JniMacWindowUtil::class.java.simpleName)

    // Extracts the native NSWindow pointer from an AWT window via reflection.
    // Returns 0 if the pointer cannot be obtained (e.g. peer not yet created).
    fun getWindowPtr(w: Window?): Long {
        if (w == null) return 0L
        try {
            val cPlatformWindow = getPlatformWindow(w) ?: return 0L
            val ptr = cPlatformWindow.javaClass.superclass.getDeclaredField("ptr")
            ptr.isAccessible = true
            return ptr.getLong(cPlatformWindow)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to get NSWindow pointer from AWT window.", e)
        }
        return 0L
    }

    private fun getPlatformWindow(w: Window): Any? {
        try {
            val awtAccessor = Class.forName("sun.awt.AWTAccessor")
            val componentAccessor = awtAccessor.getMethod("getComponentAccessor").invoke(null)
            val accessorInterface = Class.forName("sun.awt.AWTAccessor\$ComponentAccessor")
            val getPeer = accessorInterface.getMethod("getPeer", Component::class.java)
            val peer = getPeer.invoke(componentAccessor, w) ?: return null
            val getPlatformWindowMethod = peer.javaClass.getDeclaredMethod("getPlatformWindow")
            return getPlatformWindowMethod.invoke(peer)
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to get cPlatformWindow from AWT window.", e)
        }
        return null
    }

    // Sets the AWT client properties that make the content view extend into the title bar
    // area and make the title bar transparent. Guards against re-firing PropertyChangeEvents
    // on every layout pass, which would cause repeated native style mask updates and jitter.
    fun applyWindowProperties(w: Window) {
        (w as? RootPaneContainer)?.rootPane?.let { rootPane ->
            if (rootPane.getClientProperty("apple.awt.fullWindowContent") != true) {
                rootPane.putClientProperty("apple.awt.fullWindowContent", true)
            }
            if (rootPane.getClientProperty("apple.awt.transparentTitleBar") != true) {
                rootPane.putClientProperty("apple.awt.transparentTitleBar", true)
            }
            if (rootPane.getClientProperty("apple.awt.windowTitleVisible") != false) {
                rootPane.putClientProperty("apple.awt.windowTitleVisible", false)
            }
        }
    }
}
