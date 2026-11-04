package org.jameslick.foresight.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/*
 * Elevation in Foresight is light, not shadow (spec §3.10.1). Instead of Material shadows we draw a soft
 * radial glow behind an element. Used sparingly: the alignment ring, the primary action, the result glyph.
 */

/** Soft radial glow centered behind the element. `alpha` is the glow's peak opacity. */
fun Modifier.glow(color: Color, radius: Dp, alpha: Float = 0.45f): Modifier = drawBehind {
    val r = radius.toPx()
    val center = Offset(size.width / 2f, size.height / 2f)
    val extent = maxOf(size.width, size.height) / 2f + r
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = extent,
        ),
        radius = extent,
        center = center,
    )
}

/** A glow that bleeds from one edge of a card (used by ResultCard to tint its outcome). */
fun Modifier.edgeGlow(color: Color, alpha: Float = 0.35f): Modifier = drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            startY = size.height,
            endY = size.height * 0.35f,
        ),
    )
}
