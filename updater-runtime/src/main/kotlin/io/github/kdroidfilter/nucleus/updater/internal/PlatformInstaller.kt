package io.github.kdroidfilter.nucleus.updater.internal

import io.github.kdroidfilter.nucleus.updater.Platform
import java.io.File
import kotlin.system.exitProcess

internal object PlatformInstaller {
    fun install(
        file: File,
        platform: Platform,
    ) {
        val extension = file.name.substringAfterLast('.').lowercase()

        if (platform == Platform.MACOS && extension == "zip") {
            installMacZip(file)
            exitProcess(0)
        }

        val process = buildProcessForInstaller(file, platform, extension)
        process.start()
        exitProcess(0)
    }

    private fun buildProcessForInstaller(
        file: File,
        platform: Platform,
        extension: String,
    ): ProcessBuilder =
        when (platform) {
            Platform.LINUX -> buildLinuxInstaller(file, extension)
            Platform.MACOS -> buildMacInstaller(file)
            Platform.WINDOWS -> buildWindowsInstaller(file, extension)
        }

    private fun buildLinuxInstaller(
        file: File,
        extension: String,
    ): ProcessBuilder =
        when (extension) {
            "deb" -> ProcessBuilder("sudo", "dpkg", "-i", file.absolutePath)
            "rpm" -> ProcessBuilder("sudo", "rpm", "-U", file.absolutePath)
            "appimage" -> {
                file.setExecutable(true)
                ProcessBuilder(file.absolutePath)
            }
            else -> ProcessBuilder("xdg-open", file.absolutePath)
        }

    private fun buildMacInstaller(file: File): ProcessBuilder = ProcessBuilder("open", file.absolutePath)

    private fun installMacZip(zipFile: File) {
        val appBundle = resolveCurrentAppBundle()
            ?: error("Cannot resolve current .app bundle from java.home")
        val installDir = appBundle.parentFile
        val appName = appBundle.name
        val appPath = File(installDir, appName).absolutePath

        // Write a shell script that will run after the JVM exits:
        // wait for the process to die, replace the app, remove quarantine, relaunch
        val script = File(System.getProperty("java.io.tmpdir"), "nucleus-update.sh")
        script.writeText(
            """
            |#!/usr/bin/env bash
            |set -e
            |
            |ZIP_FILE="${zipFile.absolutePath}"
            |APP_PATH="${appPath}"
            |APP_NAME="${appName}"
            |INSTALL_DIR="${installDir.absolutePath}"
            |
            |# Wait for the old app to quit
            |sleep 1
            |
            |# Remove old app bundle
            |if [ -d "${'$'}APP_PATH" ]; then
            |    rm -rf "${'$'}APP_PATH"
            |fi
            |
            |# Extract the ZIP
            |ditto -x -k "${'$'}ZIP_FILE" "${'$'}INSTALL_DIR"
            |
            |# Remove quarantine attribute
            |if command -v xattr >/dev/null 2>&1; then
            |    xattr -r -d com.apple.quarantine "${'$'}APP_PATH" 2>/dev/null || true
            |fi
            |
            |# Relaunch the app
            |open "${'$'}APP_PATH"
            |
            |# Clean up
            |rm -f "${'$'}ZIP_FILE"
            |rm -f "${'$'}{0}"
            """.trimMargin()
        )
        script.setExecutable(true)

        // Launch the script as a detached process that survives our exit
        ProcessBuilder("bash", script.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.DISCARD)
            .redirectError(ProcessBuilder.Redirect.DISCARD)
            .start()
    }

    private fun resolveCurrentAppBundle(): File? {
        val javaHome = System.getProperty("java.home") ?: return null
        var dir = File(javaHome)
        while (dir.parentFile != null) {
            if (dir.name.endsWith(".app")) return dir
            dir = dir.parentFile
        }
        return null
    }

    private fun buildWindowsInstaller(
        file: File,
        extension: String,
    ): ProcessBuilder =
        when (extension) {
            "msi" -> ProcessBuilder("msiexec", "/i", file.absolutePath, "/passive")
            else -> ProcessBuilder(file.absolutePath, "/S")
        }
}
