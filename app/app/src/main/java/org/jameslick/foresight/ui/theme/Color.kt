package org.jameslick.foresight.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * Color tokens (spec §3.10). This is the ONLY file in the app allowed to contain a Color literal;
 * DesignTokenLintTest fails the build if one appears elsewhere.
 *
 * Direction A "Instrument" (chosen 2026-11-03, see assets/ASSETS.md): near-black charcoal (not pure #000),
 * one luminous pale-amber accent, warm off-white text, matte surfaces with hairline rules, desaturated
 * outcome colors. Reference images: assets/ui/mockups/moodboards/A-instrument/. If the brand palette is
 * refined after the logo is finalized, change values here only; nothing outside this file should move.
 */
internal object Palette {
    // Dark (default)
    val Ink = Color(0xFF0B0D10)          // app background, "near-black"
    val Charcoal = Color(0xFF14171C)     // cards
    val Graphite = Color(0xFF1E2228)     // raised / pressed surfaces
    val Hairline = Color(0xFF2B3038)     // 1dp rules and borders
    val Amber = Color(0xFFF3B661)        // the single luminous accent = the logo mark's exact fill
    val AmberDeep = Color(0xFF9A7030)    // accent on light surfaces / dimmed accent
    val Bone = Color(0xFFF5F1E8)         // primary text on dark
    val Ash = Color(0xFFA9A49A)          // secondary text on dark (≥ 4.5:1 on Ink)
    val CaptureRed = Color(0xFF2A0608)   // capture-screen red mode backdrop (optical, §3.2)
    val CaptureRedGlow = Color(0xFF6E1414)

    // Outcome colors: desaturated, paired with shape cues (§3.4, §3.10.5)
    val OutcomeGreen = Color(0xFF6FA882)
    val OutcomeYellow = Color(0xFFD4B25A)
    val OutcomeRed = Color(0xFFC9645B)

    // Light fallback (WCAG AA for 18sp text on these surfaces)
    val LightBackground = Color(0xFFF7F4EE)
    val LightSurface = Color(0xFFFFFFFF)
    val LightRaised = Color(0xFFEFEAE0)
    val LightHairline = Color(0xFFD9D3C7)
    val LightInk = Color(0xFF14171C)
    val LightAsh = Color(0xFF5B5750)
    val LightAccent = Color(0xFF7A4E0E)
    val LightOutcomeGreen = Color(0xFF2F6B45)
    val LightOutcomeYellow = Color(0xFF7A5C00)
    val LightOutcomeRed = Color(0xFF9B3A32)
}

/** Semantic colors the UI actually asks for. Read them via `ForesightTheme.colors`. */
@Immutable
data class ForesightColors(
    val background: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val hairline: Color,
    val accent: Color,
    val accentDim: Color,
    val onBackground: Color,
    val onBackgroundMuted: Color,
    val onAccent: Color,
    val captureBackdrop: Color,
    val captureGlow: Color,
    val outcomeGreen: Color,
    val outcomeYellow: Color,
    val outcomeRed: Color,
    /** The printed referral card: cream paper with ink text, the same in both themes. */
    val paper: Color,
    val onPaper: Color,
    val isDark: Boolean,
)

internal val DarkColors = ForesightColors(
    background = Palette.Ink,
    surface = Palette.Charcoal,
    surfaceRaised = Palette.Graphite,
    hairline = Palette.Hairline,
    accent = Palette.Amber,
    accentDim = Palette.AmberDeep,
    onBackground = Palette.Bone,
    onBackgroundMuted = Palette.Ash,
    onAccent = Palette.Ink,
    captureBackdrop = Palette.CaptureRed,
    captureGlow = Palette.CaptureRedGlow,
    outcomeGreen = Palette.OutcomeGreen,
    outcomeYellow = Palette.OutcomeYellow,
    outcomeRed = Palette.OutcomeRed,
    paper = Palette.Bone,
    onPaper = Palette.Ink,
    isDark = true,
)

internal val LightColors = ForesightColors(
    background = Palette.LightBackground,
    surface = Palette.LightSurface,
    surfaceRaised = Palette.LightRaised,
    hairline = Palette.LightHairline,
    accent = Palette.LightAccent,
    accentDim = Palette.AmberDeep,
    onBackground = Palette.LightInk,
    onBackgroundMuted = Palette.LightAsh,
    onAccent = Palette.LightSurface,
    captureBackdrop = Palette.CaptureRed,      // red mode is always dark: it is an optical requirement
    captureGlow = Palette.CaptureRedGlow,
    outcomeGreen = Palette.LightOutcomeGreen,
    outcomeYellow = Palette.LightOutcomeYellow,
    outcomeRed = Palette.LightOutcomeRed,
    paper = Palette.Bone,
    onPaper = Palette.Ink,
    isDark = false,
)

internal val LocalForesightColors = staticCompositionLocalOf { DarkColors }
