package org.jameslick.foresight.i18n

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Session language (spec §3.8): strings, numbers, dates and TTS follow the language the participant picked,
 * not the phone's locale. We wrap the screening screens in a Context whose configuration carries that
 * locale, so every `stringResource` below resolves in it.
 */
object Languages {
    const val EN = "en"
    const val ES = "es"
    const val VI = "vi"
    val all = listOf(EN, ES, VI)

    fun locale(tag: String): Locale = when (tag) {
        ES -> Locale.forLanguageTag("es-US")
        VI -> Locale.forLanguageTag("vi-VN")
        else -> Locale.forLanguageTag("en-US")
    }
}

fun Context.localized(tag: String): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(Languages.locale(tag))
    return createConfigurationContext(config)
}

@Composable
fun Localized(languageTag: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val ctx = remember(base, languageTag) { base.localized(languageTag) }
    CompositionLocalProvider(LocalContext provides ctx, LocalConfiguration provides ctx.resources.configuration, content = content)
}
