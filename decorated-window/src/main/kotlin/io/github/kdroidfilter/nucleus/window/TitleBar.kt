package io.github.kdroidfilter.nucleus.window

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ParentDataModifierNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.jetbrains.WindowDecorations.CustomTitleBar
import io.github.kdroidfilter.nucleus.window.styling.LocalTitleBarStyle
import io.github.kdroidfilter.nucleus.window.styling.TitleBarStyle
import io.github.kdroidfilter.nucleus.window.utils.DesktopPlatform
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.awt.Window
import kotlin.math.max

@Suppress("FunctionNaming")
@Composable
fun DecoratedWindowScope.TitleBar(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit = {},
) {
    when (DesktopPlatform.Current) {
        DesktopPlatform.MacOS -> MacOSTitleBar(modifier, content)
        DesktopPlatform.Windows -> WindowsTitleBar(modifier, content)
        DesktopPlatform.Linux -> LinuxTitleBar(modifier, content)
        DesktopPlatform.Unknown -> GenericTitleBarImpl(modifier, content)
    }
}

@Suppress("FunctionNaming")
@Composable
internal fun DecoratedWindowScope.GenericTitleBarImpl(
    modifier: Modifier = Modifier,
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit = {},
) {
    val style = LocalTitleBarStyle.current
    TitleBarImpl(
        window = window,
        state = state,
        modifier = modifier,
        style = style,
        applyTitleBar = { _, _ -> PaddingValues(0.dp) },
        content = content,
    )
}

@Suppress("FunctionNaming", "LongParameterList")
@Composable
internal fun TitleBarImpl(
    window: Window,
    state: DecoratedWindowState,
    modifier: Modifier = Modifier,
    style: TitleBarStyle = LocalTitleBarStyle.current,
    applyTitleBar: (Dp, DecoratedWindowState) -> PaddingValues,
    backgroundContent: @Composable () -> Unit = {},
    content: @Composable TitleBarScope.(DecoratedWindowState) -> Unit,
) {
    val titleBarInfo = LocalTitleBarInfo.current
    val background = if (state.isActive) style.colors.background else style.colors.backgroundInactive
    val density = LocalDensity.current

    Box(
        modifier =
            modifier
                .background(SolidColor(background))
                .focusProperties { canFocus = false }
                .height(style.metrics.height)
                .onSizeChanged { with(density) { applyTitleBar(it.height.toDp(), state) } }
                .fillMaxWidth(),
    ) {
        backgroundContent()
        Layout(
            content = {
                val scope = TitleBarScopeImpl(titleBarInfo)
                scope.content(state)
            },
            modifier = Modifier.fillMaxSize(),
            measurePolicy = rememberTitleBarMeasurePolicy(window, state, applyTitleBar),
        )
    }

    Spacer(
        Modifier
            .height(1.dp)
            .fillMaxWidth()
            .background(if (state.isActive) style.colors.border else style.colors.borderInactive),
    )
}

// -- TitleBarScope with proper alignment via ParentData --

@Stable
interface TitleBarScope {
    val title: String

    fun Modifier.align(alignment: Alignment.Horizontal): Modifier
}

private class TitleBarScopeImpl(
    override val title: String,
) : TitleBarScope {
    @Suppress("MaxLineLength")
    override fun Modifier.align(alignment: Alignment.Horizontal): Modifier = this then TitleBarChildDataElement(alignment)
}

private class TitleBarChildDataElement(
    val horizontalAlignment: Alignment.Horizontal,
) : ModifierNodeElement<TitleBarChildDataNode>() {
    override fun create(): TitleBarChildDataNode = TitleBarChildDataNode(horizontalAlignment)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherModifier = other as? TitleBarChildDataElement ?: return false
        return horizontalAlignment == otherModifier.horizontalAlignment
    }

    override fun hashCode(): Int = horizontalAlignment.hashCode()

    override fun update(node: TitleBarChildDataNode) {
        node.horizontalAlignment = horizontalAlignment
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "align"
        value = horizontalAlignment
    }
}

private class TitleBarChildDataNode(
    var horizontalAlignment: Alignment.Horizontal,
) : Modifier.Node(),
    ParentDataModifierNode {
    override fun Density.modifyParentData(parentData: Any?) = this@TitleBarChildDataNode
}

// -- MeasurePolicy with alignment support --

internal class TitleBarMeasurePolicy(
    private val window: Window,
    private val state: DecoratedWindowState,
    private val applyTitleBar: (Dp, DecoratedWindowState) -> PaddingValues,
) : MeasurePolicy {
    @Suppress("CyclomaticComplexMethod", "LongMethod")
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints,
    ): MeasureResult {
        if (measurables.isEmpty()) {
            return layout(width = constraints.minWidth, height = constraints.minHeight) {}
        }

        var occupiedSpaceHorizontally = 0
        var maxSpaceVertically = constraints.minHeight
        val contentConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val measuredPlaceable = mutableListOf<Pair<Measurable, Placeable>>()

        for (it in measurables) {
            val placeable = it.measure(contentConstraints.offset(horizontal = -occupiedSpaceHorizontally))
            if (constraints.maxWidth < occupiedSpaceHorizontally + placeable.width) {
                break
            }
            occupiedSpaceHorizontally += placeable.width
            maxSpaceVertically = max(maxSpaceVertically, placeable.height)
            measuredPlaceable += it to placeable
        }

        val boxHeight = maxSpaceVertically
        val contentPadding = applyTitleBar(boxHeight.toDp(), state)
        val leftInset = contentPadding.calculateLeftPadding(layoutDirection).roundToPx()
        val rightInset = contentPadding.calculateRightPadding(layoutDirection).roundToPx()

        occupiedSpaceHorizontally += leftInset + rightInset
        val boxWidth = maxOf(constraints.minWidth, occupiedSpaceHorizontally)

        return layout(boxWidth, boxHeight) {
            val placeableGroups =
                measuredPlaceable.groupBy { (measurable, _) ->
                    (measurable.parentData as? TitleBarChildDataNode)?.horizontalAlignment
                        ?: Alignment.CenterHorizontally
                }

            var headUsedSpace = leftInset
            var trailerUsedSpace = rightInset

            placeableGroups[Alignment.Start]?.forEach { (_, placeable) ->
                val x = headUsedSpace
                val y = Alignment.CenterVertically.align(placeable.height, boxHeight)
                placeable.placeRelative(x, y)
                headUsedSpace += placeable.width
            }
            placeableGroups[Alignment.End]?.forEach { (_, placeable) ->
                val x = boxWidth - placeable.width - trailerUsedSpace
                val y = Alignment.CenterVertically.align(placeable.height, boxHeight)
                placeable.placeRelative(x, y)
                trailerUsedSpace += placeable.width
            }

            val centerPlaceable = placeableGroups[Alignment.CenterHorizontally].orEmpty()
            val requiredCenterSpace = centerPlaceable.sumOf { it.second.width }
            val minX = headUsedSpace
            val maxX = boxWidth - trailerUsedSpace - requiredCenterSpace
            var centerX = (boxWidth - requiredCenterSpace) / 2

            if (minX <= maxX) {
                if (centerX > maxX) centerX = maxX
                if (centerX < minX) centerX = minX

                centerPlaceable.forEach { (_, placeable) ->
                    val x = centerX
                    val y = Alignment.CenterVertically.align(placeable.height, boxHeight)
                    placeable.placeRelative(x, y)
                    centerX += placeable.width
                }
            }
        }
    }
}

@Composable
internal fun rememberTitleBarMeasurePolicy(
    window: Window,
    state: DecoratedWindowState,
    applyTitleBar: (Dp, DecoratedWindowState) -> PaddingValues,
): MeasurePolicy = remember(window, state, applyTitleBar) { TitleBarMeasurePolicy(window, state, applyTitleBar) }

// -- Mouse event handler for JBR CustomTitleBar hit-testing (macOS/Windows) --

internal fun Modifier.customTitleBarMouseEventHandler(titleBar: CustomTitleBar): Modifier =
    pointerInput(Unit) {
        val currentContext = currentCoroutineContext()
        awaitPointerEventScope {
            var inUserControl = false
            while (currentContext.isActive) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                event.changes.forEach {
                    if (!it.isConsumed && !inUserControl) {
                        titleBar.forceHitTest(false)
                    } else {
                        if (event.type == PointerEventType.Press) {
                            inUserControl = true
                        }
                        if (event.type == PointerEventType.Release) {
                            inUserControl = false
                        }
                        titleBar.forceHitTest(true)
                    }
                }
            }
        }
    }
