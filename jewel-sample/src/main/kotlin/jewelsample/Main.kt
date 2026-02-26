package jewelsample

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.key.Key
import java.io.File
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Density
import androidx.compose.ui.window.application
import io.github.kdroidfilter.nucleus.darkmodedetector.isSystemInDarkMode
import io.github.kdroidfilter.nucleus.window.DecoratedWindow
import io.github.kdroidfilter.nucleus.window.DecoratedWindowDefaults
import io.github.kdroidfilter.nucleus.window.NucleusDecoratedWindowTheme
import io.github.kdroidfilter.nucleus.window.styling.DecoratedWindowColors
import io.github.kdroidfilter.nucleus.window.styling.DecoratedWindowStyle
import io.github.kdroidfilter.nucleus.window.styling.TitleBarStyle
import jewelsample.view.TitleBarView
import jewelsample.viewmodel.MainViewModel
import jewelsample.viewmodel.MainViewModel.currentView
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToSvgPainter
import org.jetbrains.jewel.foundation.theme.JewelTheme
import org.jetbrains.jewel.foundation.util.JewelLogger
import org.jetbrains.jewel.intui.standalone.theme.IntUiTheme
import org.jetbrains.jewel.intui.standalone.theme.createDefaultTextStyle
import org.jetbrains.jewel.intui.standalone.theme.createEditorTextStyle
import org.jetbrains.jewel.intui.standalone.theme.darkThemeDefinition
import org.jetbrains.jewel.intui.standalone.theme.default
import org.jetbrains.jewel.intui.markdown.standalone.ProvideMarkdownStyling
import org.jetbrains.jewel.intui.standalone.theme.lightThemeDefinition
import org.jetbrains.jewel.ui.ComponentStyling

private val isNativeImage = System.getProperty("org.graalvm.nativeimage.imagecode") != null

@ExperimentalLayoutApi
fun main() {
    if (isNativeImage) {
        // Metal L&F avoids loading platform-specific modules unsupported in native image
        System.setProperty("swing.defaultlaf", "javax.swing.plaf.metal.MetalLookAndFeel")
        // Set java.home to the executable's dir so Skiko can find jawt (lib/ on macOS/Linux, bin/ on Windows)
        val execDir = File(ProcessHandle.current().info().command().orElse("")).parentFile?.absolutePath ?: "."
        System.setProperty("java.home", execDir)
        // Ensure the native libraries next to the executable (fontmanager, freetype, awt, etc.) are
        // discoverable. After overriding java.home, the default java.library.path may only include
        // <java.home>/bin, missing the DLLs in the executable's root directory.
        val sep = File.pathSeparator
        System.setProperty("java.library.path", "$execDir$sep$execDir${File.separator}bin")
    }

    JewelLogger.getInstance("StandaloneSample").info("Starting Jewel Standalone sample")

    if (isNativeImage) {
        // GraalVM native images may not have platform encoding initialized when
        // Font.createFont() is first called, causing "InternalError: platform encoding
        // not initialized". Force early initialization of the charset subsystem and
        // fontmanager native library.
        java.nio.charset.Charset.defaultCharset()
        try {
            System.loadLibrary("fontmanager")
        } catch (_: Throwable) {
            // Ignore — fontmanager may already be loaded or unavailable
        }
    }

    val icon = svgResource("icons/jewel-logo.svg")

    application {
        val textStyle = JewelTheme.createDefaultTextStyle()
        val editorStyle = JewelTheme.createEditorTextStyle()

        val systemIsDark = isSystemInDarkMode()
        val isDark = if (MainViewModel.theme == IntUiThemes.System) systemIsDark else MainViewModel.theme.isDark()

        val themeDefinition =
            if (isDark) {
                JewelTheme.darkThemeDefinition(defaultTextStyle = textStyle, editorTextStyle = editorStyle)
            } else {
                JewelTheme.lightThemeDefinition(defaultTextStyle = textStyle, editorTextStyle = editorStyle)
            }

        IntUiTheme(
            theme = themeDefinition,
            styling = ComponentStyling.default(),
            swingCompatMode = MainViewModel.swingCompat,
        ) {
            val jewelWindowStyle = jewelDecoratedWindowStyle(isDark)
            val jewelTitleBarStyle = jewelTitleBarStyle(isDark)

            NucleusDecoratedWindowTheme(
                isDark = isDark,
                windowStyle = jewelWindowStyle,
                titleBarStyle = jewelTitleBarStyle,
            ) {
                DecoratedWindow(
                    onCloseRequest = { exitApplication() },
                    title = "Jewel standalone sample",
                    icon = icon,
                    onKeyEvent = { keyEvent ->
                        processKeyShortcuts(keyEvent = keyEvent, onNavigateTo = MainViewModel::onNavigateTo)
                    },
                    content = {
                        TitleBarView()
                        ProvideMarkdownStyling { currentView.content() }
                    },
                )
            }
        }
    }
}

/*
   Alt + W -> Welcome
   Alt + M -> Markdown
   Alt + C -> Components
*/
private fun processKeyShortcuts(keyEvent: KeyEvent, onNavigateTo: (String) -> Unit): Boolean {
    if (!keyEvent.isAltPressed || keyEvent.type != KeyEventType.KeyDown) return false
    return when (keyEvent.key) {
        Key.W -> {
            onNavigateTo("Welcome")
            true
        }

        Key.M -> {
            onNavigateTo("Markdown")
            true
        }

        Key.C -> {
            onNavigateTo("Components")
            true
        }

        else -> false
    }
}

@Suppress("MagicNumber")
@Composable
private fun jewelDecoratedWindowStyle(isDark: Boolean): DecoratedWindowStyle {
    val borderColor = JewelTheme.globalColors.borders.normal
    return DecoratedWindowDefaults.run { if (isDark) darkWindowStyle() else lightWindowStyle() }.copy(
        colors = DecoratedWindowColors(
            border = borderColor,
            borderInactive = borderColor.copy(alpha = 0.5f),
        ),
    )
}

@Suppress("MagicNumber")
@Composable
private fun jewelTitleBarStyle(isDark: Boolean): TitleBarStyle {
    val background = JewelTheme.globalColors.panelBackground
    val contentColor = JewelTheme.contentColor
    val borderColor = JewelTheme.globalColors.borders.normal
    val defaults = DecoratedWindowDefaults.run { if (isDark) darkTitleBarStyle() else lightTitleBarStyle() }

    val hoverOverlay = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.1f)
    val pressOverlay = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.2f)
    val inactiveBackground = if (isDark) {
        background.darken(0.15f)
    } else {
        background.lighten(0.3f)
    }

    return defaults.copy(
        colors = defaults.colors.copy(
            background = background,
            inactiveBackground = inactiveBackground,
            content = contentColor,
            border = borderColor,
            fullscreenControlButtonsBackground = background,
            titlePaneButtonHoveredBackground = hoverOverlay,
            titlePaneButtonPressedBackground = pressOverlay,
            iconButtonHoveredBackground = hoverOverlay,
            iconButtonPressedBackground = pressOverlay,
        ),
    )
}

@Suppress("MagicNumber")
private fun Color.darken(fraction: Float): Color {
    return Color(
        red = red * (1f - fraction),
        green = green * (1f - fraction),
        blue = blue * (1f - fraction),
        alpha = alpha,
    )
}

@Suppress("MagicNumber")
private fun Color.lighten(fraction: Float): Color {
    return Color(
        red = red + (1f - red) * fraction,
        green = green + (1f - green) * fraction,
        blue = blue + (1f - blue) * fraction,
        alpha = alpha,
    )
}

@Suppress("SameParameterValue")
@OptIn(ExperimentalResourceApi::class)
private fun svgResource(resourcePath: String): Painter =
    checkNotNull(ResourceLoader.javaClass.classLoader.getResourceAsStream(resourcePath)) {
            "Could not load resource $resourcePath: it does not exist or can't be read."
        }
        .readAllBytes()
        .decodeToSvgPainter(Density(1f))

private object ResourceLoader
