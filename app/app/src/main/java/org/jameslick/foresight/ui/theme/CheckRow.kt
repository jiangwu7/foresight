package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

/** A consent checkbox row (mockup 501): whole row is the touch target, amber check, 18sp+ text. */
@Composable
fun CheckRow(text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = ForesightTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.BigActionHeight)
            .toggleable(value = checked, onValueChange = onCheckedChange, role = Role.Checkbox),
        shape = Shapes.Medium,
        color = colors.surface,
        contentColor = colors.onBackground,
        border = BorderStroke(Dimens.Hairline, colors.hairline),
    ) {
        Row(Modifier.padding(horizontal = Dimens.Space2, vertical = Dimens.Space1), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(checkedColor = colors.accent, checkmarkColor = colors.onAccent, uncheckedColor = colors.onBackgroundMuted),
            )
            Spacer(Modifier.width(Dimens.Space1))
            Text(text, style = ForesightTheme.type.body)
        }
    }
}
