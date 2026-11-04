package org.jameslick.foresight.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The Foresight design system entry point (spec §3.10 Implementation).
 *
 * Dark is the default and the real screening theme. Light is a fallback that passes WCAG AA; it is only
 * used when the screener explicitly chooses it in Settings (bright outdoor events). We never follow the
 * phone's own dark/light setting automatically because the capture flow must be dark for optical reasons.
 *
 * Material 3 is wired underneath so that any Material component we use picks up our tokens instead of the
 * default purple/teal (an anti-pattern, §3.10).
 */
@Composable
fun ForesightTheme(
    dark: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colors = if (dark) DarkColors else LightColors
    val typography = DefaultTypography
    CompositionLocalProvider(
        LocalForesightColors provides colors,
        LocalForesightTypography provides typography,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = typography.toMaterial(),
            content = content,
        )
    }
}

/** Token accessors: `ForesightTheme.colors.accent`, `ForesightTheme.type.headline`. */
object ForesightTheme {
    val colors: ForesightColors
        @Composable @ReadOnlyComposable get() = LocalForesightColors.current
    val type: ForesightTypography
        @Composable @ReadOnlyComposable get() = LocalForesightTypography.current
}

/** Convenience for previews that want to follow the host. */
@Composable
fun systemPrefersDark(): Boolean = isSystemInDarkTheme()

private fun ForesightColors.toMaterial(): ColorScheme = if (isDark) {
    darkColorScheme(
        primary = accent, onPrimary = onAccent,
        secondary = accentDim, onSecondary = onBackground,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onBackground,
        surfaceVariant = surfaceRaised, onSurfaceVariant = onBackgroundMuted,
        outline = hairline, outlineVariant = hairline,
        error = outcomeRed, onError = onAccent,
    )
} else {
    lightColorScheme(
        primary = accent, onPrimary = onAccent,
        secondary = accentDim, onSecondary = onBackground,
        background = background, onBackground = onBackground,
        surface = surface, onSurface = onBackground,
        surfaceVariant = surfaceRaised, onSurfaceVariant = onBackgroundMuted,
        outline = hairline, outlineVariant = hairline,
        error = outcomeRed, onError = onAccent,
    )
}
