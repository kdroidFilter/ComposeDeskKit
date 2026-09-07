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
 * The side the dragged panel is already docked on, in this very window, is
 * left out: dropping it back there changes nothing, so offering it as a
 * target would promise something the release does not do.
 */
@Composable
internal fun BoxScope.DockZoneHints(
    workspace: SatelliteWorkspace,
    host: TaoWindow,
    state: DockLayoutState,
) {
    val dragged = workspace.draggedSatellite ?: return
    val preview = workspace.dockPreview
    val accent = LocalTitleBarStyle.current.colors.content
    val density = LocalDensity.current
    val hinted = hintedSides(dragged, host)
    val zoneWidthPx = with(density) { SatelliteWorkspace.DockZoneWidth.toPx() }
    // What a drag is hit-tested against is what is drawn: the idle strips,
    // published to the geometry the workspace resolves drops on. Cleared when
    // the drag ends, so a stale set can never answer for a later one.
    // Recomputed on every recomposition rather than remembered: the rects come
    // from the measured bands, which move without any of the keys a remember
    // could name (a side order change, a splitter drag). Four rectangles.
    val zones =
        hinted.associateWith { side ->
            state.landingRectPx(side, zoneWidthPx, joinsStack = false, dragged = dragged)
        }
    val origin = state.layoutBoundsInWindowPx.topLeft
    DisposableEffect(zones, origin) {
        val geometry = workspace.dockHostGeometry(host)
        geometry?.zoneBoundsInWindowPx = zones.mapValues { (_, rect) -> rect.translate(origin) }
        onDispose { geometry?.zoneBoundsInWindowPx = emptyMap() }
    }
    // Keeps the closed-hand cursor over the whole layout for the length of the
    // drag: the grip itself is only under the pointer while the satellite
    // floats, and a docked panel's header is left behind at the first move.
    Box(
        Modifier
            .matchParentSize()
            .pointerHoverIcon(TaoPointerIcons.Grabbing, overrideDescendants = true),
    )
    for (side in hinted) {
        val active = preview?.host === host && preview.side == side
        // The width the drop will actually produce: on a layered side the
        // panel's own, elsewhere the side's — which on a side that has no
        // extent yet is the satellite's own size, not the default.
        val extent =
            when {
                !active -> SatelliteWorkspace.DockZoneWidth
                state.isLayered(side) -> workspace.dockSeedExtent(dragged, side)
                else -> workspace.plannedDockExtent(dragged, side)
            }
        val rect =
            if (active) {
                state.landingRectPx(side, with(density) { extent.toPx() }, joinsStack = true, dragged = dragged)
            } else {
                zones.getValue(side)
            }
        if (rect.isEmpty) continue
        Box(
            Modifier
                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .size(with(density) { rect.width.toDp() }, with(density) { rect.height.toDp() })
                .background(accent.copy(alpha = if (active) ZONE_ACTIVE_ALPHA else ZONE_HINT_ALPHA))
                .dashedOutline(accent.copy(alpha = if (active) 1f else ZONE_OUTLINE_ALPHA), dashed = !active),
        )
    }
}

/**
 * The sides worth hinting while [dragged] is in flight over [host]: every one
 * except the side [dragged] is already docked on **in this window**, since
 * dropping it back there is a no-op and offering it would promise a move that
 * does not happen. Dragged from another window, or floating, every side is a
 * real target.
 */
internal fun hintedSides(
    dragged: SatelliteEntry,
    host: TaoWindow,
): List<DockSide> {
    val own = (dragged.placement as? SatellitePlacement.Docked)?.side?.takeIf { dragged.dockHost === host }
    return if (own == null) DockSide.entries else DockSide.entries.filter { it != own }
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
private val ZoneDashOn: Dp = 5.dp
private val ZoneDashOff: Dp = 4.dp
private const val ZONE_HINT_ALPHA = 0.10f
private const val ZONE_ACTIVE_ALPHA = 0.28f
private const val ZONE_OUTLINE_ALPHA = 0.55f
