package dev.nucleusframework.readerdockdemo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import dev.nucleusframework.window.tao.DockSide
import dev.nucleusframework.window.tao.SatelliteLayoutSnapshot
import dev.nucleusframework.window.tao.SatellitePlacement
import dev.nucleusframework.window.tao.SatelliteWorkspace

/** The two looks of the reader: dividers everywhere, or every pane a rounded card. */
enum class ReaderStyle {
    Classic,
    Islands,
}

/**
 * Where a pane may be docked: anywhere but the top. The reader's top is its
 * activity bar and the text's own header; a pane dragged there is refused,
 * and the top strip never lights up.
 */
val ReaderDockSides: Set<DockSide> = setOf(DockSide.Left, DockSide.Right, DockSide.Bottom)

/** The sides [Pane.fixed] panes accept: the right of the text, where the reader puts them. */
val ReaderFixedDockSides: Set<DockSide> = setOf(DockSide.Right)

/**
 * One pane of the reader: a satellite with a home in the dock.
 *
 * [fixed] is the reader's furniture — the book tree and the table of contents
 * belong on the right of the text and nowhere else: they cannot be torn into a
 * window of their own, nor moved to another side. They can still be hidden,
 * resized, and reordered between themselves.
 */
enum class Pane(
    val id: String,
    val title: String,
    val home: SatellitePlacement.Docked,
    val openAtStart: Boolean,
    val fixed: Boolean = false,
) {
    Tree(
        "tree",
        "ספרים",
        SatellitePlacement.Docked(DockSide.Right, order = 0, extent = 200.dp),
        openAtStart = true,
        fixed = true,
    ),
    Toc(
        "toc",
        "תוכן",
        SatellitePlacement.Docked(DockSide.Right, order = 1, extent = 170.dp),
        openAtStart = true,
        fixed = true,
    ),
    Notes("notes", "הערות", SatellitePlacement.Docked(DockSide.Right, order = 2, extent = 220.dp), openAtStart = false),
    Targum("targum", "תרגום", SatellitePlacement.Docked(DockSide.Left, extent = 240.dp), openAtStart = false),
    Comments("comments", "מפרשים", SatellitePlacement.Docked(DockSide.Bottom, extent = 220.dp), openAtStart = true),
    Sources("sources", "מקורות", SatellitePlacement.Docked(DockSide.Bottom, extent = 200.dp), openAtStart = false),
}

/**
 * What the demo drives: the workspace every pane is declared against, the
 * visual style, and the saved layout.
 *
 * Everything the bars do is a workspace call — toggle a pane, save or restore
 * the layout. The layout of the reader itself (which side is layered, which
 * side owns the corners) is declared once in `Main.kt`; the workspace holds
 * only what the user changed.
 */
class ReaderState {
    val workspace = SatelliteWorkspace()

    var style: ReaderStyle by mutableStateOf(ReaderStyle.Classic)

    var savedLayout: SatelliteLayoutSnapshot? by mutableStateOf(null)
        private set

    fun isOpen(pane: Pane): Boolean = workspace.satellite(pane.id)?.isOpen == true

    /** Shows or hides a pane. Commentaries and sources share the bottom, so one closes the other. */
    fun toggle(pane: Pane) {
        val opening = !isOpen(pane)
        when (pane) {
            Pane.Comments -> if (opening) workspace.close(Pane.Sources.id)
            Pane.Sources -> if (opening) workspace.close(Pane.Comments.id)
            else -> Unit
        }
        workspace.toggle(pane.id)
    }

    fun saveLayout() {
        savedLayout = workspace.snapshot()
    }

    fun restoreLayout() {
        savedLayout?.let(workspace::restore)
    }

    /** Every pane back where it started, at its starting width. */
    fun resetLayout() {
        for (pane in Pane.entries) {
            workspace.dock(pane.id, pane.home.side, order = pane.home.order)
            pane.home.extent?.let { workspace.setDockedExtent(pane.id, it) }
            workspace.setDockedWeight(pane.id, pane.home.weight)
            if (pane.openAtStart) workspace.open(pane.id) else workspace.close(pane.id)
        }
    }
}
