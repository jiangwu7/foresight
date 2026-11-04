package org.jameslick.foresight.i18n

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
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

/**
 * A Context that resolves resources in [tag] but still *is* the Activity underneath. We wrap rather than
 * use `createConfigurationContext` directly: that returns a bare Context, and then the photo picker
 * (ActivityResultRegistry lookup), `startActivity` for the share sheet and `PrintManager.print` all fail
 * because they walk the ContextWrapper chain looking for the Activity (crash seen on the first device
 * run, 2026-11-04).
 */
fun Context.localized(tag: String): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(Languages.locale(tag))
    val localizedResources = createConfigurationContext(config).resources
    return object : ContextWrapper(this) {
        override fun getResources(): Resources = localizedResources
    }
}

@Composable
fun Localized(languageTag: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val ctx = remember(base, languageTag) { base.localized(languageTag) }
    // Captured before LocalContext changes, and re-provided explicitly so the Activity-backed owners never
    // depend on how the wrapped Context is unwrapped.
    val resultOwner = LocalActivityResultRegistryOwner.current
    val backOwner = LocalOnBackPressedDispatcherOwner.current
    CompositionLocalProvider(
        LocalContext provides ctx,
        LocalConfiguration provides ctx.resources.configuration,
        *listOfNotNull(
            resultOwner?.let { LocalActivityResultRegistryOwner provides it },
            backOwner?.let { LocalOnBackPressedDispatcherOwner provides it },
        ).toTypedArray(),
        content = content,
    )
}
