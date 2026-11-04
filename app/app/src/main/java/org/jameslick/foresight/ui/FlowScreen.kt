package org.jameslick.foresight.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme

/**
 * The shape of every screening screen (approved mockups, spec §3.10.2): micro-label, one headline,
 * content, and one primary action pinned to the bottom. Secondary actions sit just above it.
 */
@Composable
fun FlowScreen(
    headline: String,
    modifier: Modifier = Modifier,
    micro: String? = null,
    primaryText: String? = null,
    primaryEnabled: Boolean = true,
    onPrimary: () -> Unit = {},
    secondary: (@Composable ColumnScope.() -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .padding(horizontal = Dimens.ScreenMargin),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
        ) {
            Spacer(Modifier.height(Dimens.Space4))
            if (micro != null) {
                Text(micro.uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
                Spacer(Modifier.height(Dimens.Space2))
            }
            Text(headline, style = type.headline)
            Spacer(Modifier.height(Dimens.Space3))
            content()
            Spacer(Modifier.height(Dimens.Space3))
        }
        if (secondary != null) {
            secondary()
            Spacer(Modifier.height(Dimens.Space1))
        }
        if (primaryText != null) {
            BigAction(primaryText, onClick = onPrimary, enabled = primaryEnabled)
        }
        Spacer(Modifier.height(Dimens.Space3))
    }
}
