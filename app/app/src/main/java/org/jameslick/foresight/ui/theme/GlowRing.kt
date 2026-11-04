package org.jameslick.foresight.ui.theme

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import kotlin.math.cos
import kotlin.math.sin

/** What the alignment ring is telling the screener. */
enum class RingState {
    /** Waiting for an eye: ring breathes slowly. */
    Idle,
    /** Something is in view but not yet good enough: ring steady, half-bright. */
    Aligning,
    /** Quality threshold met, capture fired: ring snaps tight and glows. */
    Locked,
}

/**
 * The alignment ring/reticle that echoes through the whole design (spec §3.10.3–4). A thin luminous
 * ring with four tick marks; breathes while idle, snaps and glows when locked. Respects reduce-motion.
 *
 * @param contentDescription spoken by TalkBack; pass a localized string resource.
 */
@Composable
fun GlowRing(
    state: RingState,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = Dimens.RingSize,
    color: Color = ForesightTheme.colors.accent,
) {
    val reduceMotion = rememberReduceMotion()

    val breathe: Float = if (state == RingState.Idle && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "ring-breathe")
        val value by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(Motion.Breathe, easing = Motion.EaseInOut), RepeatMode.Reverse),
            label = "breathe",
        )
        value
    } else {
        1f
    }

    val targetScale = when (state) {
        RingState.Idle -> 1f
        RingState.Aligning -> 0.97f
        RingState.Locked -> 0.92f
    }
    val scale by animateFloatAsState(targetScale, animationSpec = Motion.Snap, label = "ring-scale")

    val targetGlow = when (state) {
        RingState.Idle -> 0.35f
        RingState.Aligning -> 0.55f
        RingState.Locked -> 1f
    }
    val glowAlpha by animateFloatAsState(targetGlow, animationSpec = tween(Motion.Medium, easing = Motion.Ease), label = "ring-glow")

    Canvas(
        modifier = modifier
            .size(size)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val radius = this.size.minDimension / 2f * 0.78f * scale
        val stroke = Dimens.RingStroke.toPx()
        val ringAlpha = 0.55f + 0.45f * breathe

        // Glow halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = 0.40f * glowAlpha * breathe), color.copy(alpha = 0f)),
                center = center,
                radius = radius * 1.45f,
            ),
            radius = radius * 1.45f,
            center = center,
        )
        // The ring itself (hairline, not bold)
        drawCircle(color = color.copy(alpha = ringAlpha), radius = radius, center = center, style = Stroke(stroke))
        // Four tick marks at the compass points
        val tick = Dimens.RingTick.toPx()
        for (i in 0 until 4) {
            val a = Math.toRadians(90.0 * i)
            val dir = Offset(cos(a).toFloat(), sin(a).toFloat())
            drawLine(
                color = color.copy(alpha = ringAlpha),
                start = center + dir * (radius + tick * 0.6f),
                end = center + dir * (radius + tick * 1.6f),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        // Locked: a small solid dot at the center (the "got it" cue, also legible without color)
        if (state == RingState.Locked) {
            drawCircle(color = color, radius = stroke * 2.5f, center = center)
        }
    }
}

@Preview(name = "GlowRing idle", showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewIdle() = ForesightTheme { GlowRing(RingState.Idle, contentDescription = "preview") }

@Preview(name = "GlowRing locked", showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewLocked() = ForesightTheme { GlowRing(RingState.Locked, contentDescription = "preview") }
