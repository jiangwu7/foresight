package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * The one primary action per screen (spec §3.10.2): full width, 64dp tall, oversized label, accent fill.
 * `secondary = true` gives the quiet outlined variant for "Retake" / "Back" style actions.
 */
@Composable
fun BigAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false,
) {
    val colors = ForesightTheme.colors
    val container = if (secondary) colors.surface else colors.accent
    val content = if (secondary) colors.onBackground else colors.onAccent
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(Dimens.BigActionHeight)
            .then(if (!secondary && enabled) Modifier.glow(colors.accent, Dimens.GlowSoft, alpha = 0.25f) else Modifier),
        shape = Shapes.Medium,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = colors.surfaceRaised,
            disabledContentColor = colors.onBackgroundMuted,
        ),
        border = if (secondary) BorderStroke(Dimens.Hairline, colors.hairline) else null,
    ) {
        Text(text, style = ForesightTheme.type.action)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewBigAction() = ForesightTheme {
    BigAction("Continue", onClick = {}, modifier = Modifier.padding(Dimens.Space2))
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewBigActionSecondary() = ForesightTheme {
    BigAction("Retake", onClick = {}, secondary = true, modifier = Modifier.padding(Dimens.Space2))
}
