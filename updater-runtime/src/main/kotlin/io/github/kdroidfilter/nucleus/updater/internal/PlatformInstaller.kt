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

        // Extract to a temp directory first, so the old app survives if extraction fails
        val tempDir = File(System.getProperty("java.io.tmpdir"), "nucleus-update-${System.currentTimeMillis()}")
        tempDir.mkdirs()

        val extractExitCode = ProcessBuilder("ditto", "-xk", zipFile.absolutePath, tempDir.absolutePath)
            .inheritIO()
            .start()
            .waitFor()

        val extractedApp = tempDir.listFiles()?.firstOrNull { it.name.endsWith(".app") }

        if (extractExitCode != 0 || extractedApp == null || !extractedApp.exists()) {
            // Extraction failed — clean up temp and fall back to opening the ZIP
            tempDir.deleteRecursively()
            ProcessBuilder("open", zipFile.absolutePath).start()
            return
        }

        // Extraction succeeded — now safe to replace the old app
        appBundle.deleteRecursively()

        val exitCode = ProcessBuilder("mv", extractedApp.absolutePath, File(installDir, appName).absolutePath)
            .start()
            .waitFor()

        tempDir.deleteRecursively()

        if (exitCode != 0) {
            // Move failed (permissions?) — fall back to opening the ZIP
            ProcessBuilder("open", zipFile.absolutePath).start()
            return
        }

        val newAppPath = File(installDir, appName).absolutePath

        // Remove quarantine attribute (ignore errors — file may not be quarantined)
        runCatching {
            ProcessBuilder("xattr", "-rd", "com.apple.quarantine", newAppPath)
                .start()
                .waitFor()
        }

        // Relaunch the new app
        ProcessBuilder("open", newAppPath).start()

        // Clean up the downloaded ZIP
        zipFile.delete()
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
