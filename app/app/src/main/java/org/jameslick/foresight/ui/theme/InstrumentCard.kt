package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * The basic dark panel: charcoal surface, 1dp hairline border, large radius, wide inner padding.
 * Optional `glowColor` tints the lower edge (used for outcomes). Spec §3.10.1, §3.10.3.
 */
@Composable
fun InstrumentCard(
    modifier: Modifier = Modifier,
    glowColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ForesightTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.Large,
        color = colors.surface,
        contentColor = colors.onBackground,
        border = BorderStroke(Dimens.Hairline, colors.hairline),
    ) {
        Column(
            modifier = (if (glowColor != null) Modifier.edgeGlow(glowColor) else Modifier)
                .padding(Dimens.Space3),
            content = content,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewInstrumentCard() = ForesightTheme {
    InstrumentCard(modifier = Modifier.padding(Dimens.Space2)) {
        Text("Instrument card", style = ForesightTheme.type.title)
        Text("Body text at 18sp on charcoal.", style = ForesightTheme.type.body, color = ForesightTheme.colors.onBackgroundMuted)
    }
}
