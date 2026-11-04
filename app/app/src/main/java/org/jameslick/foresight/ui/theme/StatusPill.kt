package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview

/** Tone of a status pill. Never the only cue: the label text always says what the state is. */
enum class PillTone { Neutral, Good, Caution, Alert, Accent }

/**
 * Small rounded pill with a dot and a small-caps micro-label: "GRADABLE", "EXPERIMENTAL", "OFFLINE".
 * Spec §3.10.3 (micro-labels) and §1.3 (the Experimental badge).
 */
@Composable
fun StatusPill(text: String, tone: PillTone = PillTone.Neutral, modifier: Modifier = Modifier) {
    val colors = ForesightTheme.colors
    val dot = when (tone) {
        PillTone.Neutral -> colors.onBackgroundMuted
        PillTone.Good -> colors.outcomeGreen
        PillTone.Caution -> colors.outcomeYellow
        PillTone.Alert -> colors.outcomeRed
        PillTone.Accent -> colors.accent
    }
    Surface(
        modifier = modifier.height(Dimens.Space4),
        shape = Shapes.Pill,
        color = colors.surfaceRaised,
        contentColor = colors.onBackground,
        border = BorderStroke(Dimens.Hairline, colors.hairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.Space2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(
                Modifier
                    .size(Dimens.PillDot)
                    .clip(CircleShape)
                    .background(dot),
            )
            Spacer(Modifier.width(Dimens.Space1))
            Text(text.uppercase(), style = ForesightTheme.type.microLabel)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewPills() = ForesightTheme {
    Row(Modifier.padding(Dimens.Space2)) {
        StatusPill("Gradable", PillTone.Good)
        Spacer(Modifier.width(Dimens.Space1))
        StatusPill("Experimental", PillTone.Accent)
    }
}
