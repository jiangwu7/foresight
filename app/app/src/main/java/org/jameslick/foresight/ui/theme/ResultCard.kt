package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview

/** The three overall outcomes (spec §3.4). Each has a fixed shape cue so color is never the only signal. */
enum class Outcome { Green, Yellow, Red }

/**
 * The overall result card: outcome glyph (circle / triangle / square), title, body, and the mandatory
 * disclaimer line (§1.3) that the caller passes from `strings_medical.xml`. Desaturated colors, no flashing,
 * no alarm floods (§3.10.5). All text comes in as parameters: this component owns no wording.
 */
@Composable
fun ResultCard(
    outcome: Outcome,
    title: String,
    body: String,
    disclaimer: String,
    outcomeDescription: String,
    modifier: Modifier = Modifier,
) {
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    val tint = when (outcome) {
        Outcome.Green -> colors.outcomeGreen
        Outcome.Yellow -> colors.outcomeYellow
        Outcome.Red -> colors.outcomeRed
    }
    InstrumentCard(modifier = modifier, glowColor = tint) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutcomeGlyph(outcome, tint, Modifier.semantics { contentDescription = outcomeDescription })
            Spacer(Modifier.width(Dimens.Space2))
            Text(title, style = type.title)
        }
        Spacer(Modifier.height(Dimens.Space2))
        Text(body, style = type.bodyLarge)
        Spacer(Modifier.height(Dimens.Space3))
        HorizontalDivider(thickness = Dimens.Hairline, color = colors.hairline)
        Spacer(Modifier.height(Dimens.Space2))
        Text(disclaimer, style = type.body, color = colors.onBackgroundMuted)
    }
}

/** Circle = green, triangle = yellow, square = red. Outlined with a soft fill, hairline weight. */
@Composable
fun OutcomeGlyph(outcome: Outcome, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(Dimens.OutcomeGlyph)) {
        val stroke = Stroke(Dimens.RingStroke.toPx())
        val inset = stroke.width
        val fill = color.copy(alpha = 0.25f)
        when (outcome) {
            Outcome.Green -> {
                drawCircle(fill, radius = size.minDimension / 2f - inset)
                drawCircle(color, radius = size.minDimension / 2f - inset, style = stroke)
            }
            Outcome.Yellow -> {
                val p = Path().apply {
                    moveTo(size.width / 2f, inset)
                    lineTo(size.width - inset, size.height - inset)
                    lineTo(inset, size.height - inset)
                    close()
                }
                drawPath(p, fill)
                drawPath(p, color, style = stroke)
            }
            Outcome.Red -> {
                val r = CornerRadius(Dimens.RingStroke.toPx() * 2)
                val sz = Size(size.width - 2 * inset, size.height - 2 * inset)
                drawRoundRect(fill, Offset(inset, inset), sz, r)
                drawRoundRect(color, Offset(inset, inset), sz, r, style = stroke)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewResultCardYellow() = ForesightTheme {
    ResultCard(
        outcome = Outcome.Yellow,
        title = "Preview title",
        body = "Preview body text. Real wording comes from strings_medical.xml.",
        disclaimer = "Preview disclaimer line.",
        outcomeDescription = "preview",
        modifier = Modifier.padding(Dimens.Space2),
    )
}
