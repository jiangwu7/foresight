package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

/**
 * One tappable answer (intake mockups 502/602): 64dp+ tall, hairline border, amber border when selected,
 * centered 22sp text. Big enough for a 70-year-old at arm's length (spec §3.9).
 */
@Composable
fun OptionTile(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
) {
    val colors = ForesightTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = if (tall) Dimens.LanguageTileHeight else Dimens.BigActionHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = Shapes.Medium,
        color = if (selected) colors.surfaceRaised else colors.surface,
        contentColor = colors.onBackground,
        border = BorderStroke(if (selected) Dimens.RingStroke else Dimens.Hairline, if (selected) colors.accent else colors.hairline),
    ) {
        Box(Modifier.padding(horizontal = Dimens.Space2, vertical = Dimens.Space2), contentAlignment = Alignment.Center) {
            Text(text, style = ForesightTheme.type.bodyLarge, textAlign = TextAlign.Center)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewOptionTile() = ForesightTheme {
    OptionTile("Within 1 year", selected = true, onClick = {}, modifier = Modifier.padding(Dimens.Space2))
}
