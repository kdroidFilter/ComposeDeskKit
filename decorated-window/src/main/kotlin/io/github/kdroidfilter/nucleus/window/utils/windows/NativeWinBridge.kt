package io.github.kdroidfilter.nucleus.window.utils.windows

import java.awt.Component
import java.nio.file.Files
import java.util.logging.Level
import java.util.logging.Logger

internal object NativeWinBridge {
    private val logger = Logger.getLogger(NativeWinBridge::class.java.simpleName)

    @Volatile
    private var loaded = false

    init {
        loadNativeLibrary()
    }

    private fun loadNativeLibrary() {
        if (loaded) return

        // Try system library path first (packaged app)
        try {
            System.loadLibrary("nucleus_windows")
            loaded = true
            return
        } catch (_: UnsatisfiedLinkError) {
            // Fall through to JAR extraction
        }

        // Fallback: extract from JAR resources
        @Suppress("TooGenericExceptionCaught")
        try {
            val resourcePath = "/nucleus/native/win32-x64/nucleus_windows.dll"
            val stream =
                NativeWinBridge::class.java
                    .getResourceAsStream(resourcePath)
                    ?: throw UnsatisfiedLinkError("Native library not found in JAR at $resourcePath")
            val tempDir = Files.createTempDirectory("nucleus-native")
            val tempLib = tempDir.resolve("nucleus_windows.dll")
            stream.use { Files.copy(it, tempLib) }
            tempLib.toFile().deleteOnExit()
            tempDir.toFile().deleteOnExit()
            System.load(tempLib.toAbsolutePath().toString())
            loaded = true
        } catch (e: Exception) {
            logger.log(Level.WARNING, "Failed to load nucleus_windows native library", e)
        }
    }

    val isLoaded: Boolean get() = loaded

    /**
     * Uses JAWT to obtain the native HWND from the given AWT Component,
     * then uses SetWindowPos with SWP_SHOWWINDOW | SWP_MAXIMIZE 
     * to show the window already maximized - no animation, instant maximize.
     *
     * @return true if the window was successfully shown maximized
     */
    @JvmStatic
    external fun nativeShowMaximized(component: Component): Boolean

    /**
     * Sets the window to maximized state using Win32 SetWindowLongPtr.
     * Should be called before window is shown.
     *
     * @return true if successful
     */
    @JvmStatic
    external fun nativeCreateMaximized(component: Component): Boolean

    /**
     * Brings the window to foreground using Win32 SetForegroundWindow.
     * This is more reliable than toFront() on Windows.
     * Uses multiple attempts and thread attachment for reliability.
     *
     * @return true if successful
     */
    @JvmStatic
    external fun nativeBringToForeground(component: Component): Boolean
}
