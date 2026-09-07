package dev.nucleusframework.window.tao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.nucleusframework.window.styling.LocalTitleBarStyle
import dev.nucleusframework.window.tao.workspace.DockDropZone
import kotlin.math.roundToInt

/**
 * The four drop zones of this layout, shown while a satellite is being
 * dragged anywhere in the workspace.
 *
 * Every side is outlined as soon as the drag starts — that is what tells the
 * user the gesture exists — and the one the satellite has entered fills in
 * solid. Both are drawn where the panel would actually land
 * ([DockLayoutState.landingRectPx]): along the side's own band rather than the
 * whole edge, inside the layers already docked there, at the width the drop
 * will produce once it is the active one.
 *
 * A side with panels on it is also cut into ranks ([DockLayoutState.dropSlotsPx]),
 * one region per place the panel can take among them, and the active rank is
 * drawn as a bar on the edge it would slide into — except a new innermost
 * layer, drawn as the column it becomes. The rank the dragged panel already
 * holds is not a target: a side it is alone on is left out altogether, and
 * with neighbours the strip past the stack is not lit while the panel is the
 * last of them, since a drop there changes nothing.
 */
@Composable
internal fun BoxScope.DockZoneHints(
    workspace: SatelliteWorkspace,
    host: TaoWindow,
    state: DockLayoutState,
) {
    val dragged = workspace.draggedSatellite ?: return
    val accent = LocalTitleBarStyle.current.colors.content
    val density = LocalDensity.current
    val hinted = hintedSides(dragged, host, workspace.satellites)
    val zoneWidthPx = with(density) { SatelliteWorkspace.DockZoneWidth.toPx() }
    // What a drag is hit-tested against is what is drawn: the idle strips and
    // the ranks of each stack, published to the geometry the workspace
    // resolves drops on. Cleared when the drag ends, so a stale set can never
    // answer for a later one. Recomputed on every recomposition rather than
    // remembered: the rects come from the measured bands, which move without
    // any of the keys a remember could name (a side order change, a splitter
    // drag). Four strips and a handful of slots.
    val zones =
        hinted.associateWith { side ->
            val strip = state.landingRectPx(side, zoneWidthPx, joinsStack = false, dragged = dragged)
            DockDropZone(strip, state.dropSlotsPx(side, strip, dragged))
        }
    val origin = state.layoutBoundsInWindowPx.topLeft
    DisposableEffect(zones, origin) {
        val geometry = workspace.dockHostGeometry(host)
        geometry?.zoneBoundsInWindowPx = zones.mapValues { (_, zone) -> zone.translate(origin) }
        onDispose { geometry?.zoneBoundsInWindowPx = emptyMap() }
    }
    val own = workspace.ownTarget(dragged, host)
    // Keeps the closed-hand cursor over the whole layout for the length of the
    // drag: the grip itself is only under the pointer while the satellite
    // floats, and a docked panel's header is left behind at the first move.
    Box(
        Modifier
            .matchParentSize()
            .pointerHoverIcon(TaoPointerIcons.Grabbing, overrideDescendants = true),
    )
    for (side in hinted) {
        SideHint(workspace, state, host, side, zones.getValue(side), dragged, own, accent)
    }
}

/**
 * One side's feedback: the active rank as a bar between the two panels it
 * lands between — or, for a new innermost layer and for an empty side, the
 * rect the panel will occupy — else the idle strip.
 */
@Suppress("LongParameterList") // the drag's whole state, read once per side
@Composable
private fun SideHint(
    workspace: SatelliteWorkspace,
    state: DockLayoutState,
    host: TaoWindow,
    side: DockSide,
    zone: DockDropZone,
    dragged: SatelliteEntry,
    own: DockTarget?,
    accent: Color,
) {
    val density = LocalDensity.current
    val preview = workspace.dockPreview
    val active = preview?.host === host && preview.side == side
    // Its own side, with itself last: the strip past the stack is the rank it
    // holds, so lighting it up would promise a move that does not happen.
    if (!active && own?.side == side && own.order == zone.slots.lastIndex) return
    val order = preview?.order?.takeIf { active && zone.slots.isNotEmpty() }
    when {
        // Between two panels of the stack — a new innermost layer is drawn as
        // the column it becomes, like a drop on an empty side.
        order != null && !(state.isLayered(side) && order == zone.slots.lastIndex) -> {
            val bar = state.insertionBarPx(side, dragged, order, with(density) { InsertionBarThickness.toPx() })
            if (bar != null) ZoneRect(bar, accent.copy(alpha = INSERTION_BAR_ALPHA), outline = null)
        }
        active -> {
            // The width the drop will actually produce: on a layered side the
            // panel's own, elsewhere the side's — which on a side that has no
            // extent yet is the satellite's own size, not the default.
            val extent =
                if (state.isLayered(side)) {
                    workspace.dockSeedExtent(dragged, side)
                } else {
                    workspace.plannedDockExtent(dragged, side)
                }
            val rect = state.landingRectPx(side, with(density) { extent.toPx() }, joinsStack = true, dragged = dragged)
            ZoneRect(rect, accent.copy(alpha = ZONE_ACTIVE_ALPHA), outline = accent, dashed = false)
        }
        else -> {
            ZoneRect(
                zone.strip,
                accent.copy(alpha = ZONE_HINT_ALPHA),
                outline = accent.copy(alpha = ZONE_OUTLINE_ALPHA),
            )
        }
    }
}

@Composable
private fun ZoneRect(
    rect: Rect,
    fill: Color,
    outline: Color?,
    dashed: Boolean = true,
) {
    if (rect.isEmpty) return
    val density = LocalDensity.current
    Box(
        Modifier
            .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
            .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
            .background(fill)
            .then(if (outline != null) Modifier.dashedOutline(outline, dashed) else Modifier),
    )
}

/**
 * The sides worth hinting while [dragged] is in flight over [host]: every one
 * except the side [dragged] is already docked on **in this window** while it
 * is alone there, since dropping it back is a no-op and offering it would
 * promise a move that does not happen. With other panels on that side it is
 * a target again — the panel can be dropped at another rank among them.
 * Dragged from another window, or floating, every side is a real target.
 * [satellites] are the workspace's, to tell a lone panel from a stack.
 */
internal fun hintedSides(
    dragged: SatelliteEntry,
    host: TaoWindow,
    satellites: Collection<SatelliteEntry>,
): List<DockSide> {
    val own = (dragged.placement as? SatellitePlacement.Docked)?.side?.takeIf { dragged.dockHost === host }
    val alone =
        own != null &&
            satellites.none {
                it !== dragged &&
                    it.isShown &&
                    it.dockHost === host &&
                    (it.placement as? SatellitePlacement.Docked)?.side == own
            }
    return if (alone) DockSide.entries.filter { it != own } else DockSide.entries
}

/** The smallest rect containing both. */
internal fun unionOf(
    a: Rect,
    b: Rect,
): Rect = Rect(minOf(a.left, b.left), minOf(a.top, b.top), maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))

/** A dashed (or solid) 1 dp outline, drawn rather than composed so it costs no layout. */
private fun Modifier.dashedOutline(
    color: Color,
    dashed: Boolean,
): Modifier =
    drawBehind {
        val stroke = ZoneOutlineWidth.toPx()
        drawRect(
            color = color,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style =
                Stroke(
                    width = stroke,
                    pathEffect =
                        if (dashed) {
                            PathEffect.dashPathEffect(floatArrayOf(ZoneDashOn.toPx(), ZoneDashOff.toPx()))
                        } else {
                            null
                        },
                ),
        )
    }

private val ZoneOutlineWidth: Dp = 1.5.dp
private val InsertionBarThickness: Dp = 4.dp
private const val INSERTION_BAR_ALPHA = 0.9f
private val ZoneDashOn: Dp = 5.dp
private val ZoneDashOff: Dp = 4.dp
private const val ZONE_HINT_ALPHA = 0.10f
private const val ZONE_ACTIVE_ALPHA = 0.28f
private const val ZONE_OUTLINE_ALPHA = 0.55f
