package org.jameslick.foresight.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jameslick.foresight.R

/*
 * Typography tokens (spec §3.10, §3.9). Be Vietnam Pro has full Vietnamese diacritics; IBM Plex Mono is the
 * monospaced numeral face for measurements and timers. Body text is never below 18sp. This is the only file
 * allowed to contain .sp literals.
 */
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_light, FontWeight.Light),
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
)

val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
)

@Immutable
data class ForesightTypography(
    /** Oversized numbers and the one-word states ("Ready"). */
    val display: TextStyle,
    /** Screen headline, ≥ 32sp. One per screen. */
    val headline: TextStyle,
    /** Card titles. */
    val title: TextStyle,
    /** Body copy, ≥ 18sp (accessibility floor, §3.9). */
    val body: TextStyle,
    /** Larger body for result wording read at arm's length. */
    val bodyLarge: TextStyle,
    /** Button labels. */
    val action: TextStyle,
    /** Small-caps micro-labels above readouts. Not used for sentences. */
    val microLabel: TextStyle,
    /** Monospaced numerals for measurements, timers, versions. */
    val numeral: TextStyle,
    val numeralSmall: TextStyle,
)

internal val DefaultTypography = ForesightTypography(
    display = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Light, fontSize = 56.sp, lineHeight = 64.sp, letterSpacing = (-0.02).em),
    headline = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.01).em),
    title = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium, fontSize = 24.sp, lineHeight = 32.sp),
    body = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Normal, fontSize = 22.sp, lineHeight = 32.sp),
    action = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 24.sp, letterSpacing = 0.01.em),
    microLabel = TextStyle(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 16.sp, letterSpacing = 0.12.em),
    numeral = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 44.sp),
    numeralSmall = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, fontSize = 18.sp, lineHeight = 24.sp),
)

internal val LocalForesightTypography = staticCompositionLocalOf { DefaultTypography }

/** Material 3 mapping so any Material component we do use inherits the same type. */
internal fun ForesightTypography.toMaterial(): Typography = Typography(
    displayLarge = display,
    headlineLarge = headline,
    headlineMedium = headline,
    titleLarge = title,
    titleMedium = title,
    bodyLarge = bodyLarge,
    bodyMedium = body,
    bodySmall = body,
    labelLarge = action,
    labelMedium = microLabel,
    labelSmall = microLabel,
)
