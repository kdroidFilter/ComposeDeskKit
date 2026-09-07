package dev.nucleusframework.window.tao

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.nucleusframework.window.tao.workspace.HostGeometry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Where a drop preview is drawn ([DockLayoutState.landingRectPx]): along the
 * side's band, not the whole layout; inside the layers already docked on a
 * layered side; on the stack itself when the panel joins a split stack.
 *
 * The geometry is the reader layout — right side first and layered, bottom
 * inside it — laid out at 1000 × 600 px, offset in the window by (20, 40).
 */
class DockLandingRectTest {
    private val host = TaoWindow(handle = 1L)
    private val workspace = SatelliteWorkspace().apply { join(host) }
    private val state =
        DockLayoutState(workspace).apply {
            layeredSides = setOf(DockSide.Right)
            layoutBoundsInWindowPx = Rect(20f, 40f, 1020f, 640f)
            // The right band is the whole layout; the bottom band stops at the right stack.
            bandBoundsInWindowPx[DockSide.Right] = Rect(20f, 40f, 1020f, 640f)
            bandBoundsInWindowPx[DockSide.Bottom] = Rect(20f, 40f, 720f, 640f)
            bandBoundsInWindowPx[DockSide.Left] = Rect(20f, 40f, 720f, 440f)
            bandBoundsInWindowPx[DockSide.Top] = Rect(220f, 40f, 720f, 440f)
        }

    private fun docked(
        id: String,
        side: DockSide,
        order: Int,
        boundsInWindowPx: Rect,
    ): SatelliteEntry {
        val entry =
            workspace.register(
                id,
                id,
                SatellitePlacement.Docked(side, order, extent = 100.dp),
                initiallyOpen = true,
            )
        entry.dockedBoundsInWindowPx = boundsInWindowPx
        return entry
    }

    @Test
    fun `a bottom preview spans the bottom band, not the layout`() {
        state.docked = emptyList()
        assertEquals(Rect(0f, 540f, 700f, 600f), state.landingRectPx(DockSide.Bottom, 60f, joinsStack = true))
    }

    @Test
    fun `a layered side previews a new innermost layer`() {
        state.docked =
            listOf(
                docked("tree", DockSide.Right, 0, Rect(920f, 40f, 1020f, 640f)),
                docked("toc", DockSide.Right, 1, Rect(820f, 40f, 920f, 640f)),
            )
        assertEquals(Rect(740f, 0f, 800f, 600f), state.landingRectPx(DockSide.Right, 60f, joinsStack = true))
    }

    @Test
    fun `a split side with a stack previews the stack the panel joins`() {
        state.docked = listOf(docked("targum", DockSide.Left, 0, Rect(20f, 40f, 220f, 440f)))
        assertEquals(Rect(0f, 0f, 200f, 400f), state.landingRectPx(DockSide.Left, 60f, joinsStack = true))
        // The idle outline stays a strip at the edge of the band.
        assertEquals(Rect(0f, 0f, 60f, 400f), state.landingRectPx(DockSide.Left, 60f, joinsStack = false))
    }

    @Test
    fun `an empty side previews a strip at the edge of its band`() {
        state.docked = emptyList()
        assertEquals(Rect(200f, 0f, 700f, 60f), state.landingRectPx(DockSide.Top, 60f, joinsStack = true))
        assertEquals(Rect(0f, 0f, 60f, 400f), state.landingRectPx(DockSide.Left, 60f, joinsStack = true))
    }

    @Test
    fun `the side the dragged panel frees is counted as already gone`() {
        // The reader shape: the bottom band stops at the layered right stack,
        // and the left band stops above the bottom panel.
        val comments = docked("comments", DockSide.Bottom, 0, Rect(20f, 440f, 720f, 640f))
        state.docked = listOf(comments)

        // Previewing the left side while dragging the *only* bottom panel: the
        // bottom frees up, so the drop will run the full height of the band.
        assertEquals(
            Rect(0f, 0f, 60f, 600f),
            state.landingRectPx(DockSide.Left, 60f, joinsStack = true, dragged = comments),
        )
        // Without the drag it is the band as measured, above the panel.
        assertEquals(Rect(0f, 0f, 60f, 400f), state.landingRectPx(DockSide.Left, 60f, joinsStack = true))
    }

    @Test
    fun `a side the dragged panel shares with another is not freed`() {
        val comments = docked("comments", DockSide.Bottom, 0, Rect(20f, 440f, 380f, 640f))
        val sources = docked("sources", DockSide.Bottom, 1, Rect(380f, 440f, 720f, 640f))
        state.docked = listOf(comments, sources)

        assertEquals(
            Rect(0f, 0f, 60f, 400f),
            state.landingRectPx(DockSide.Left, 60f, joinsStack = true, dragged = comments),
            "the bottom side keeps its extent, so the left band is unchanged",
        )
    }

    @Test
    fun `without a measured band the layout itself is the band`() {
        val bare = DockLayoutState(workspace).apply { layoutBoundsInWindowPx = Rect(0f, 0f, 400f, 300f) }
        assertEquals(Rect(340f, 0f, 400f, 300f), bare.landingRectPx(DockSide.Right, 60f, joinsStack = true))
    }
}

/**
 * Which sides a drag is offered ([hintedSides]): all four, minus the one the
 * dragged panel already occupies in the very window being hinted.
 */
class DockZoneHintSidesTest {
    private val host = TaoWindow(handle = 1L)
    private val other = TaoWindow(handle = 2L)
    private val workspace = SatelliteWorkspace().apply { join(host) }

    private val floating =
        SatellitePlacement.Floating(
            positioner = WindowPositioner(parentAnchor = WindowAnchor.Right, childAnchor = WindowAnchor.Left),
            size = DpSize(200.dp, 300.dp),
        )

    @Test
    fun `a floating satellite is offered every side`() {
        val entry = workspace.register("tools", "Tools", floating, initiallyOpen = true)
        assertEquals(DockSide.entries, hintedSides(entry, host))
    }

    @Test
    fun `a docked panel is not offered the side it is on`() {
        val entry = workspace.register("tools", "Tools", floating, initiallyOpen = true)
        workspace.dock("tools", DockSide.Bottom, host = host)
        assertEquals(listOf(DockSide.Left, DockSide.Right, DockSide.Top), hintedSides(entry, host))
    }

    @Test
    fun `another window offers the side too, since dropping there is a move`() {
        val entry = workspace.register("tools", "Tools", floating, initiallyOpen = true)
        workspace.join(other)
        workspace.dock("tools", DockSide.Bottom, host = host)
        assertEquals(DockSide.entries, hintedSides(entry, other))
    }
}

/**
 * Which zone a drag resolves to ([SatelliteWorkspace.dockTargetAt] with the
 * dragged rect): the zone the satellite on screen has been brought against,
 * with the pointer as a second trigger and as the tie-break.
 *
 * Host a's layout is (100, 140)-(900, 700) on screen, zone width 64 px.
 */
class DockTargetFromDraggedRectTest {
    private val a = TaoWindow(handle = 1L)

    private fun workspace(): SatelliteWorkspace =
        SatelliteWorkspace().apply {
            join(a)
            dockHosts.register(
                HostGeometry(a, outerBoundsPx = { longArrayOf(100L, 100L, 800L, 600L) }, scaleFactor = { 1f }).apply {
                    layoutBoundsInWindowPx = Rect(0f, 40f, 800f, 600f)
                    containerSizePx = IntSize(800, 600)
                },
            )
        }

    @Test
    fun `the dragged rect decides the zone, not the pointer`() {
        val workspace = workspace()

        // A 200 x 300 palette pushed against the left edge: its own edge is in
        // the zone while the pointer sits in the middle of the palette, far
        // from any edge of the layout.
        val atLeft = Rect(120f, 300f, 320f, 600f)
        assertEquals(
            DockTarget(a, DockSide.Left),
            workspace.dockTargetAt(atLeft, atLeft.center),
            "the palette's own edge has entered the left zone",
        )
        assertNull(workspace.dockTargetAt(atLeft.center), "the pointer alone is over the content")

        // Aligned from the outside too: pushed 20 px past the edge is still
        // brought against it.
        val justOver = Rect(80f, 300f, 280f, 600f)
        assertEquals(DockTarget(a, DockSide.Left), workspace.dockTargetAt(justOver, justOver.center))

        // Deep past the edge is no longer an alignment — but the pointer, now
        // over the left strip itself, still is.
        val overhanging = Rect(20f, 300f, 220f, 600f)
        assertEquals(DockTarget(a, DockSide.Left), workspace.dockTargetAt(overhanging, Offset(120f, 450f)))
        assertNull(workspace.dockTargetAt(overhanging, Offset(200f, 450f)), "neither edge nor pointer is at a zone")

        // Over the middle: no zone, whatever the pointer does.
        val middle = Rect(400f, 350f, 600f, 500f)
        assertNull(workspace.dockTargetAt(middle, middle.center), "nothing has entered a zone")

        // Beside the layout, not on it: no drop, even with the pointer inside.
        val beside = Rect(950f, 300f, 1150f, 600f)
        assertNull(workspace.dockTargetAt(beside, beside.center))

        // Aligned with two sides at once: the pointer decides.
        val topLeftCorner = Rect(120f, 150f, 320f, 250f)
        assertEquals(DockTarget(a, DockSide.Top), workspace.dockTargetAt(topLeftCorner, Offset(300f, 240f)))
    }

    @Test
    fun `an inset zone is the target, not the window's own edge`() {
        val workspace = workspace()
        // What a layered right side draws while two columns are already
        // docked: the strip is inset 200 px behind them, not at x 900.
        val geometry = requireNotNull(workspace.dockHostGeometry(a))
        geometry.zoneBoundsInWindowPx = mapOf(DockSide.Right to Rect(540f, 40f, 604f, 600f))

        // The palette brought against the drawn strip docks…
        val onStrip = Rect(440f, 300f, 700f, 600f)
        assertEquals(DockTarget(a, DockSide.Right), workspace.dockTargetAt(onStrip, onStrip.center))
        // …while the window's own right edge, behind the columns, is nothing.
        val atWindowEdge = Rect(700f, 300f, 900f, 600f)
        assertNull(workspace.dockTargetAt(atWindowEdge, atWindowEdge.center))
        // The pointer in the drawn strip is a target too.
        assertNull(workspace.dockTargetAt(atWindowEdge, Offset(880f, 400f)), "the window edge is not a zone")
        assertEquals(
            DockTarget(a, DockSide.Right),
            workspace.dockTargetAt(atWindowEdge, Offset(670f, 400f)),
            "the pointer inside the drawn strip",
        )
        // A side the layout does not draw is not a target at all.
        assertNull(workspace.dockTargetAt(Rect(120f, 300f, 320f, 600f), Offset(120f, 400f)), "no left zone is drawn")
    }

    @Test
    fun `a dragged rect covering every zone is resolved by the pointer`() {
        val workspace = workspace()
        // Larger than the layout: every side is within reach at once.
        val covering = Rect(50f, 100f, 950f, 750f)

        assertEquals(DockTarget(a, DockSide.Left), workspace.dockTargetAt(covering, Offset(120f, 400f)))
        assertEquals(DockTarget(a, DockSide.Bottom), workspace.dockTargetAt(covering, Offset(500f, 690f)))
        // Pointer off the layout: the closest alignment decides instead — the
        // covering rect overhangs the top by the least.
        assertEquals(DockTarget(a, DockSide.Top), workspace.dockTargetAt(covering, Offset(0f, 0f)))
    }
}
