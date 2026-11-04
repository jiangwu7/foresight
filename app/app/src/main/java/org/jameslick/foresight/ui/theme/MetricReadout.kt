package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * A measurement: small-caps label above a monospaced number with an optional unit.
 * "QUALITY  0.92", "CAPTURE  1.3 s", "MODEL  v2026.11". Spec §3.10.3.
 */
@Composable
fun MetricReadout(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    compact: Boolean = false,
) {
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    Column(modifier) {
        Text(label.uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
        Spacer(Modifier.width(Dimens.Space1))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = if (compact) type.numeralSmall else type.numeral, color = colors.onBackground)
            if (unit != null) {
                Spacer(Modifier.width(Dimens.Space1))
                Text(unit, style = type.numeralSmall, color = colors.onBackgroundMuted)
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PreviewMetric() = ForesightTheme {
    Row(Modifier.padding(Dimens.Space2)) {
        MetricReadout(label = "Quality", value = "0.92")
        Spacer(Modifier.width(Dimens.Space4))
        MetricReadout(label = "Capture", value = "1.3", unit = "s")
    }
}
