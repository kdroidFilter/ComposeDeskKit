package io.github.kdroidfilter.nucleus.core.runtime

import io.github.kdroidfilter.nucleus.core.runtime.tools.debugln
import io.github.kdroidfilter.nucleus.core.runtime.tools.errorln
import java.awt.Desktop
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

/**
 * Utility object for handling deep links across platforms.
 *
 * On macOS, deep links are delivered via Apple Events (`Desktop.setOpenURIHandler`).
 * On Windows/Linux, deep links are passed as command-line arguments.
 *
 * Integrates with [SingleInstanceManager] to forward deep links from secondary instances
 * to the primary instance via the restore request file mechanism.
 */
object DeepLinkHandler {

    private const val TAG = "DeepLinkHandler"

    /** The last received deep link URI. */
    @Volatile
    var uri: URI? = null
        private set

    private var onDeepLink: ((URI) -> Unit)? = null

    /**
     * Registers deep link handling for the application.
     *
     * Sets up the macOS Apple Events handler and parses command-line arguments
     * for a URI. When a deep link is received (now or later via Apple Events),
     * [onDeepLink] is invoked.
     *
     * @param args command-line arguments passed to `main()`
     * @param onDeepLink callback invoked each time a deep link URI is received
     */
    fun register(args: Array<String>, onDeepLink: (URI) -> Unit) {
        this.onDeepLink = onDeepLink

        // Handle protocol URLs on macOS (delivered via Apple Events)
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().setOpenURIHandler { event ->
                    debugLog { "Received URI via Apple Events: ${event.uri}" }
                    handleUri(event.uri)
                }
            } catch (e: UnsupportedOperationException) {
                debugLog { "setOpenURIHandler not supported on this platform" }
            }
        }

        // Handle protocol URLs on Windows/Linux (passed as command-line args)
        args.firstOrNull { it.contains("://") }?.let { raw ->
            try {
                val parsed = URI(raw)
                debugLog { "Received URI via CLI args: $parsed" }
                handleUri(parsed)
            } catch (e: Exception) {
                errorLog { "Failed to parse URI from args: $raw — $e" }
            }
        }
    }

    /**
     * Writes the current [uri] to the given file path.
     * Intended to be called from [SingleInstanceManager]'s `onRestoreFileCreated` callback.
     */
    fun writeUriTo(path: Path) {
        val currentUri = uri ?: return
        try {
            Files.writeString(path, currentUri.toString())
            debugLog { "Wrote URI to $path: $currentUri" }
        } catch (e: Exception) {
            errorLog { "Failed to write URI to $path: $e" }
        }
    }

    /**
     * Reads a URI from the given file path and triggers the [onDeepLink] callback.
     * Intended to be called from [SingleInstanceManager]'s `onRestoreRequest` callback.
     */
    fun readUriFrom(path: Path) {
        try {
            val content = Files.readString(path).trim()
            if (content.isNotEmpty()) {
                val parsed = URI(content)
                debugLog { "Read URI from $path: $parsed" }
                handleUri(parsed)
            }
        } catch (e: Exception) {
            errorLog { "Failed to read URI from $path: $e" }
        }
    }

    private fun handleUri(newUri: URI) {
        uri = newUri
        onDeepLink?.invoke(newUri)
    }

    private fun debugLog(msg: () -> String) {
        debugln { "[$TAG] ${msg()}" }
    }

    private fun errorLog(msg: () -> String) {
        errorln { "[$TAG] ${msg()}" }
    }
}
