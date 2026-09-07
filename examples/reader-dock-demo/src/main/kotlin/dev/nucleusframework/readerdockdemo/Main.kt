package dev.nucleusframework.readerdockdemo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.rememberWindowState
import dev.nucleusframework.application.DecoratedWindow
import dev.nucleusframework.application.Satellite
import dev.nucleusframework.application.nucleusApplication
import dev.nucleusframework.darkmodedetector.isSystemInDarkMode
import dev.nucleusframework.window.WindowAppearance
import dev.nucleusframework.window.WindowAppearanceMode
import dev.nucleusframework.window.WindowBackground
import dev.nucleusframework.window.WindowScaffold
import dev.nucleusframework.window.material.MaterialTitleBar
import dev.nucleusframework.window.material.rememberMaterialTitleBarStyle
import dev.nucleusframework.window.material.rememberMaterialWindowStyle
import dev.nucleusframework.window.styling.LocalDecoratedWindowStyle
import dev.nucleusframework.window.styling.LocalTitleBarStyle
import dev.nucleusframework.window.tao.DockLayout
import dev.nucleusframework.window.tao.DockSide
import dev.nucleusframework.window.tao.JoinSatelliteWorkspace

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF8AA4FF),
        surface = Color(0xFF1B1D22),
        surfaceContainer = Color(0xFF23262D),
        surfaceContainerHigh = Color(0xFF2B2F38),
        background = Color(0xFF14161A),
        outlineVariant = Color(0xFF3A3F4A),
    )

private val LightColors =
    lightColorScheme(
        primary = Color(0xFF3F5DDB),
        surface = Color(0xFFFFFFFF),
        surfaceContainer = Color(0xFFF2F3F7),
        surfaceContainerHigh = Color(0xFFE6E8EF),
        background = Color(0xFFEDEFF4),
        outlineVariant = Color(0xFFD5D8E0),
    )

/**
 * A right-to-left book reader built entirely from satellites.
 *
 * The pane tree of a classic split-pane reader — books | contents | notes on
 * the right, the text in the middle with the translation beside it, the
 * commentaries under both — is one `DockLayout`: the right side is *layered*,
 * so its three panes are three columns each with its own width and splitter,
 * and the side order puts the right side first so the commentaries stop at it
 * and run under the translation. The dividers are the reader's own 1 dp lines
 * with a 5 dp grip; the headers are the reader's own 32 dp hover strips; the
 * *Islands* style turns every pane into a rounded card. And because every pane
 * is a satellite, each can be torn out into a window of its own and dropped
 * back — that is the only thing the split panes could not do.
 */
fun main() =
    nucleusApplication {
        val reader = remember { ReaderState() }
        val dark = isSystemInDarkMode()
        val colors = if (dark) DarkColors else LightColors

        DecoratedWindow(
            onCloseRequest = ::exitApplication,
            title = "Reader",
            state = rememberWindowState(width = WINDOW_W_DP.dp, height = WINDOW_H_DP.dp),
            minimumSize = DpSize(MIN_W_DP.dp, MIN_H_DP.dp),
        ) {
            JoinSatelliteWorkspace(reader.workspace)
            ReaderTheme(colors) {
                WindowBackground(colors.background)
                WindowAppearance(if (dark) WindowAppearanceMode.Dark else WindowAppearanceMode.Light)
                WindowScaffold(titleBar = { MaterialTitleBar { Text("Reader") } }) { padding ->
                    Surface(Modifier.fillMaxSize().padding(padding), color = colors.background) {
                        ReaderBody(reader)
                    }
                }
            }
        }

        // Every pane, declared once at application scope; the workspace decides
        // whether it is a panel of the dock or a window of its own.
        ReaderTheme(colors) {
            for (pane in Pane.entries) {
                Satellite(
                    workspace = reader.workspace,
                    id = pane.id,
                    title = pane.title,
                    initialPlacement = pane.home,
                    initiallyOpen = pane.openAtStart,
                    dockSides = if (pane.fixed) ReaderFixedDockSides else ReaderDockSides,
                    floatable = !pane.fixed,
                    reorderable = !pane.fixed,
                    header = { PaneHeader(reader.style) },
                    // Only reserved where the compositor owns the window move;
                    // elsewhere the whole bar drags the pane and this is not composed.
                    floatingCaption = { PaneMoveAffordance() },
                ) {
                    Surface(Modifier.fillMaxSize(), color = colors.surface) { PaneContent(pane) }
                }
            }
        }
    }

/** The reader: its two activity bars around the dock layout, all right-to-left. */
@Composable
private fun ReaderBody(reader: ReaderState) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(Modifier.fillMaxSize()) {
            // Start bar: at the right edge in RTL, toggling the navigation panes.
            ActivityBar {
                for (pane in listOf(Pane.Tree, Pane.Toc, Pane.Notes)) {
                    BarButton(pane.title.take(1), selected = reader.isOpen(pane)) { reader.toggle(pane) }
                }
            }
            VerticalDivider()
            DockLayout(
                workspace = reader.workspace,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                // The navigation runs the full height on the right; the
                // commentaries run under the text and the translation, not
                // under the navigation.
                sideOrder = listOf(DockSide.Right, DockSide.Bottom, DockSide.Left, DockSide.Top),
                // Books | contents | notes are three columns, not a stack.
                layeredSides = setOf(DockSide.Right),
                splitter = { ReaderSplitter(reader.style) },
                panel = { body -> PaneCard(reader.style) { body() } },
            ) {
                PaneCard(reader.style) { TextColumn() }
            }
            VerticalDivider()
            // End bar: the content panes and the style switch.
            ActivityBar {
                for (pane in listOf(Pane.Targum, Pane.Comments, Pane.Sources)) {
                    BarButton(pane.title.take(1), selected = reader.isOpen(pane)) { reader.toggle(pane) }
                }
                Spacer(Modifier.height(BAR_GAP_DP.dp))
                BarButton("◫", selected = reader.style == ReaderStyle.Islands) {
                    reader.style = if (reader.style == ReaderStyle.Islands) ReaderStyle.Classic else ReaderStyle.Islands
                }
                Spacer(Modifier.weight(1f))
                BarButton("S", selected = false) { reader.saveLayout() }
                BarButton("R", selected = reader.savedLayout != null) { reader.restoreLayout() }
                BarButton("⟲", selected = false) { reader.resetLayout() }
            }
        }
    }
}

/** The main text: the document, with a breadcrumb strip under it. */
@Composable
private fun TextColumn() {
    Column(Modifier.fillMaxSize()) {
        val scroll = rememberScrollState()
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(TEXT_PADDING_DP.dp),
            verticalArrangement = Arrangement.spacedBy(TEXT_GAP_DP.dp),
        ) {
            Text("בראשית", fontSize = TITLE_SP.sp, fontWeight = FontWeight.Bold)
            repeat(VERSES) { index ->
                Text(
                    "פסוק ${index + 1} — ${SAMPLE_TEXT.repeat(1 + index % 3)}",
                    fontSize = TEXT_SP.sp,
                    textAlign = TextAlign.Start,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().height(BREADCRUMB_H_DP.dp).padding(horizontal = TEXT_PADDING_DP.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "תנ״ך  ›  תורה  ›  בראשית  ›  פרק א",
                fontSize = BREADCRUMB_SP.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A pane's body: a list the user can scroll, whose position survives dock and undock. */
@Composable
private fun PaneContent(pane: Pane) {
    val scroll = rememberScrollState()
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    Column(
        Modifier.fillMaxSize().verticalScroll(scroll).padding(PANE_PADDING_DP.dp),
        verticalArrangement = Arrangement.spacedBy(ITEM_GAP_DP.dp),
    ) {
        repeat(ITEMS) { index ->
            val chosen = selected == index
            Text(
                text = "${pane.title} ${index + 1}",
                fontSize = TEXT_SP.sp,
                color = if (chosen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(ITEM_CORNER_DP.dp))
                        .background(if (chosen) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                        .clickable { selected = index }
                        .padding(ITEM_PADDING_DP.dp),
            )
        }
    }
}

@Composable
private fun ActivityBar(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxHeight()
            .width(
                BAR_W_DP.dp,
            ).background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = BAR_GAP_DP.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BAR_GAP_DP.dp),
    ) { content() }
}

@Composable
private fun BarButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(BAR_BUTTON_DP.dp),
        colors =
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = if (selected) colors.primary.copy(alpha = SELECTED_ALPHA) else Color.Transparent,
                contentColor = if (selected) colors.primary else colors.onSurfaceVariant,
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) { Text(label, fontSize = BAR_LABEL_SP.sp, fontWeight = FontWeight.SemiBold) }
    }
}

/**
 * Material colours plus the window-chrome styles derived from them, per
 * window scene — and once more around the satellites, whose floating windows
 * get it through the bridged locals.
 */
@Composable
private fun ReaderTheme(
    colors: ColorScheme,
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(
            LocalTitleBarStyle provides rememberMaterialTitleBarStyle(colors),
            LocalDecoratedWindowStyle provides rememberMaterialWindowStyle(colors),
            content = content,
        )
    }
}

private const val WINDOW_W_DP = 1280
private const val WINDOW_H_DP = 820
private const val MIN_W_DP = 640
private const val MIN_H_DP = 420
private const val BAR_W_DP = 48
private const val BAR_GAP_DP = 8
private const val BAR_BUTTON_DP = 36
private const val BAR_LABEL_SP = 14
private const val SELECTED_ALPHA = 0.18f
private const val TEXT_PADDING_DP = 24
private const val TEXT_GAP_DP = 12
private const val TITLE_SP = 26
private const val TEXT_SP = 17
private const val VERSES = 40
private const val BREADCRUMB_H_DP = 28
private const val BREADCRUMB_SP = 12
private const val PANE_PADDING_DP = 8
private const val ITEM_GAP_DP = 2
private const val ITEM_PADDING_DP = 6
private const val ITEM_CORNER_DP = 6
private const val ITEMS = 60
private const val SAMPLE_TEXT = "בְּרֵאשִׁית בָּרָא אֱלֹהִים אֵת הַשָּׁמַיִם וְאֵת הָאָרֶץ. "
