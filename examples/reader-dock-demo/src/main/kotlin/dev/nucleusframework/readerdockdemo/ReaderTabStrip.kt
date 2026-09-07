package dev.nucleusframework.readerdockdemo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nucleusframework.window.styling.LocalTitleBarStyle
import dev.nucleusframework.window.tao.TabStrip
import dev.nucleusframework.window.tao.TabStripScope

/**
 * The seforim of one window: the stock [TabStrip], plus the button that opens
 * another sefer after the last tab.
 *
 * The stock strip is what publishes the geometry a tab dragged from another
 * window is dropped onto, so the reader's own chrome goes *around* its tabs
 * rather than in place of them.
 */
@Composable
fun TabStripScope.ReaderTabStrip(onNewBook: () -> Unit) {
    TabStrip(trailing = { NewBookButton(onNewBook) })
}

/** Opens another sefer in this workspace. */
@Composable
private fun NewBookButton(onClick: () -> Unit) {
    val colors = LocalTitleBarStyle.current.colors
    Box(
        modifier =
            Modifier
                .padding(horizontal = BUTTON_PADDING_DP.dp)
                .size(BUTTON_SIZE_DP.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Text("+", color = colors.content, fontSize = BUTTON_GLYPH_SP.sp)
    }
}

private const val BUTTON_PADDING_DP = 6
private const val BUTTON_SIZE_DP = 22
private const val BUTTON_GLYPH_SP = 15
