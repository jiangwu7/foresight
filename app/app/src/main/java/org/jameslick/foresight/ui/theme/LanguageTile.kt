package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview

/**
 * One language choice on the first screen (spec §3.1 step 1): a tall tile with the language's own name in
 * large type, its code as a micro-label, and the accent border when selected. The flag/illustration slot
 * comes with the Phase 1 illustration set; the tile is fully usable without it.
 */
@Composable
fun LanguageTile(
    nativeName: String,
    code: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.LanguageTileHeight)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = Shapes.Large,
        color = if (selected) colors.surfaceRaised else colors.surface,
        contentColor = colors.onBackground,
        border = BorderStroke(if (selected) Dimens.RingStroke else Dimens.Hairline, if (selected) colors.accent else colors.hairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.Space3),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(code.uppercase(), style = type.numeral, color = if (selected) colors.accent else colors.onBackgroundMuted)
            Spacer(Modifier.width(Dimens.Space3))
            Column {
                Text(nativeName, style = type.title)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewLanguageTiles() = ForesightTheme {
    Column(Modifier.padding(Dimens.Space2)) {
        LanguageTile("English", "en", selected = true, onClick = {})
        Spacer(Modifier.height(Dimens.Space1))
        LanguageTile("Español", "es", selected = false, onClick = {})
        Spacer(Modifier.height(Dimens.Space1))
        LanguageTile("Tiếng Việt", "vi", selected = false, onClick = {})
    }
}
