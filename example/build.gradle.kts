import io.github.kdroidfilter.composedeskkit.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.compose") version "2.3.10"
    id("io.github.kdroidfilter.composedeskkit")
}

val currentTarget: String by lazy {
    val os = System.getProperty("os.name")
    val arch = System.getProperty("os.arch")
    val osId =
        when {
            os.equals("Mac OS X", ignoreCase = true) -> "macos"
            os.startsWith("Win", ignoreCase = true) -> "windows"
            os.startsWith("Linux", ignoreCase = true) -> "linux"
            else -> error("Unsupported OS: $os")
        }
    val archId =
        when (arch) {
            "x86_64", "amd64" -> "x64"
            "aarch64" -> "arm64"
            else -> error("Unsupported arch: $arch")
        }
    "$osId-$archId"
}

val defaultWindowsIcon =
    rootProject.layout.projectDirectory.file(
        "plugin-build/plugin/src/main/resources/default-compose-desktop-icon-windows.ico",
    )
val defaultMacIcon =
    rootProject.layout.projectDirectory.file(
        "plugin-build/plugin/src/main/resources/default-compose-desktop-icon-mac.icns",
    )
val defaultLinuxIcon =
    rootProject.layout.projectDirectory.file(
        "plugin-build/plugin/src/main/resources/default-compose-desktop-icon-linux.png",
    )

dependencies {
    implementation("org.jetbrains.compose.desktop:desktop-jvm-$currentTarget:1.10.0")
    implementation("org.jetbrains.compose.material3:material3:1.9.0")
}

composeDeskKit.desktop.application {
    mainClass = "com.example.demo.MainKt"

    nativeDistributions {
        targetFormats(*TargetFormat.values())

        packageName = "ComposeDeskKitDemo"
        packageVersion = "1.0.0"
        description = "Demo application for ComposeDeskKit"
        vendor = "KDroidFilter"

        modules(
            "jdk.accessibility",
        )

        publish {
            github {
                enabled = true
                owner = "kdroidfilter"
                repo = "ComposeDeskKit"
            }
        }

        linux {
            iconFile.set(defaultLinuxIcon)
            appCategory = "Utility"
        }

        windows {
            iconFile.set(defaultWindowsIcon)
            menu = true
            shortcut = true
            nsis {
                oneClick = true
                allowElevation = false
                perMachine = true
            }
        }

        macOS {
            iconFile.set(defaultMacIcon)
            bundleID = "io.github.kdroidfilter.composedeskkit.demo"
        }
    }
}
