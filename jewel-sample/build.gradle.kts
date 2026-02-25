import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinComposePlugin)
    alias(libs.plugins.jetbrainsCompose)
}

val jewelVersion = "0.34.0-253.31033.149"
val coilVersion = "3.2.0"

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(project(":core-runtime"))
    implementation(project(":darkmode-detector"))
    implementation(project(":decorated-window-jni"))
    val jewelExclusions = Action<ExternalModuleDependency> {
        exclude(group = "org.jetbrains.skiko", module = "skiko-awt-runtime-all")
    }
    implementation("org.jetbrains.jewel:jewel-int-ui-standalone:$jewelVersion", jewelExclusions)
    implementation("org.jetbrains.jewel:jewel-markdown-int-ui-standalone-styling:$jewelVersion", jewelExclusions)
    implementation("org.jetbrains.jewel:jewel-markdown-extensions-autolink:$jewelVersion", jewelExclusions)
    implementation("org.jetbrains.jewel:jewel-markdown-extensions-gfm-alerts:$jewelVersion", jewelExclusions)
    implementation("org.jetbrains.jewel:jewel-markdown-extensions-gfm-tables:$jewelVersion", jewelExclusions)
    implementation("org.jetbrains.jewel:jewel-markdown-extensions-gfm-strikethrough:$jewelVersion", jewelExclusions)
    implementation("org.jetbrains.jewel:jewel-markdown-extensions-images:$jewelVersion", jewelExclusions)
    implementation("io.coil-kt.coil3:coil-compose:$coilVersion")
    implementation("com.jetbrains.intellij.platform:icons:252.26830.102")

    // Jewel's StandalonePlatformCursorController uses JNA at runtime
    implementation(libs.jna.jpms)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

compose.desktop {
    application {
        mainClass = "jewelsample.MainKt"
        jvmArgs += listOf(
            "--add-opens", "java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt=ALL-UNNAMED",
            "--add-opens", "java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        )
    }
}
