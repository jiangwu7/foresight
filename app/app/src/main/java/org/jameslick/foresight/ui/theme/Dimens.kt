package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/*
 * Spacing, size and shape tokens (spec §3.10.2–3). Everything sits on an 8dp grid. This is the only file
 * allowed to contain .dp literals; DesignTokenLintTest enforces it.
 */
object Dimens {
    // 8dp grid
    val Space1 = 8.dp
    val Space2 = 16.dp
    val Space3 = 24.dp
    val Space4 = 32.dp
    val Space6 = 48.dp
    val Space8 = 64.dp

    /** Screen side margin: wide, negative space is the main design material. */
    val ScreenMargin = 24.dp

    // Touch targets (§3.9: ≥ 48dp; §3.10: 48–64dp)
    val TouchTargetMin = 48.dp
    val TouchTarget = 56.dp
    val BigActionHeight = 64.dp
    val LanguageTileHeight = 112.dp

    // Precision details
    val Hairline = 1.dp
    val RingStroke = 2.dp
    val RingTick = 8.dp
    val RingSize = 240.dp
    val RingSizeSmall = 96.dp
    val PillDot = 10.dp
    val OutcomeGlyph = 28.dp

    // Glow radii
    val GlowSoft = 24.dp
    val GlowStrong = 48.dp
}

object Shapes {
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val Pill = RoundedCornerShape(50)
}
