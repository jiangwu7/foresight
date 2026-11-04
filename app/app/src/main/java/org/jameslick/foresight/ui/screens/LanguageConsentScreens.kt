package org.jameslick.foresight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.style.TextAlign
import org.jameslick.foresight.capture.CaptureSettings
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jameslick.foresight.R
import org.jameslick.foresight.i18n.Languages
import org.jameslick.foresight.i18n.Speech
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.CheckRow
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.LanguageTile

/** Steps with the optional intake in the flow (language, consent, intake, R, L, processing, results, card). */
const val FLOW_STEPS = 8

/**
 * "Step n of N" for the participant-facing screens. Steps are numbered as if intake were present; when the
 * intake questions are switched off (the default since the 2026-11-04 user tests) everything after consent
 * moves up one and the total drops to 7.
 */
@Composable
fun stepLabel(stepWithIntake: Int): String {
    val ask = CaptureSettings.load(LocalContext.current).askIntake
    val step = if (ask || stepWithIntake <= 2) stepWithIntake else stepWithIntake - 1
    return stringResource(R.string.step_of, step, if (ask) FLOW_STEPS else FLOW_STEPS - 1)
}

/** Mockup 600: three tall tiles with the team's display codes EN / ESP / VN. */
@Composable
fun LanguageScreen(vm: ScreeningViewModel, onNext: () -> Unit) {
    val state by vm.state.collectAsState()
    FlowScreen(
        micro = stepLabel(1),
        headline = stringResource(R.string.language_title),
        primaryText = stringResource(R.string.action_continue),
        onPrimary = onNext,
    ) {
        val tiles = listOf(
            Triple(Languages.EN, stringResource(R.string.lang_en_native), stringResource(R.string.lang_en_code)),
            Triple(Languages.ES, stringResource(R.string.lang_es_native), stringResource(R.string.lang_es_code)),
            Triple(Languages.VI, stringResource(R.string.lang_vi_native), stringResource(R.string.lang_vi_code)),
        )
        tiles.forEach { (tag, native, code) ->
            LanguageTile(nativeName = native, code = code, selected = state.language == tag, onClick = { vm.setLanguage(tag) })
            Spacer(Modifier.height(Dimens.Space1))
        }
    }
}

/** Mockup 501: the two required sentences, two checkboxes, read-aloud, "I agree". */
@Composable
fun ConsentScreen(vm: ScreeningViewModel, onAgree: () -> Unit, onUnder18: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val speech = remember { Speech(context) }
    DisposableEffect(Unit) { onDispose { speech.close() } }
    var understood by rememberSaveable { mutableStateOf(false) }
    var photos by rememberSaveable { mutableStateOf(false) }
    var adult by rememberSaveable { mutableStateOf(false) }
    val p1 = stringResource(R.string.consent_fingerprint)
    val p2 = stringResource(R.string.consent_stays)
    val c1 = stringResource(R.string.consent_check_screening)
    val c2 = stringResource(R.string.consent_check_photo)
    val c3 = stringResource(R.string.consent_check_adult)
    val type = ForesightTheme.type
    val colors = ForesightTheme.colors

    FlowScreen(
        micro = stepLabel(2),
        headline = stringResource(R.string.consent_title),
        primaryText = stringResource(R.string.action_i_agree),
        primaryEnabled = understood && photos && adult,
        onPrimary = onAgree,
        secondary = { BigAction(stringResource(R.string.action_read_aloud), onClick = { speech.speak(state.language, "$p1 $p2 $c1. $c2. $c3.") }, secondary = true) },
    ) {
        Text(p1, style = type.bodyLarge)
        Spacer(Modifier.height(Dimens.Space2))
        Text(p2, style = type.body)
        Spacer(Modifier.height(Dimens.Space3))
        CheckRow(c1, understood, { understood = it })
        Spacer(Modifier.height(Dimens.Space1))
        CheckRow(c2, photos, { photos = it })
        Spacer(Modifier.height(Dimens.Space1))
        // Spec §6: minors are not screened. The age gate moved here from the intake questions (2026-11-04).
        CheckRow(c3, adult, { adult = it })
        Spacer(Modifier.height(Dimens.Space2))
        Text(stringResource(R.string.consent_under18_link), style = type.action, color = colors.onBackgroundMuted, textAlign = TextAlign.Center,
             modifier = Modifier.fillMaxWidth().clickable(onClick = onUnder18).padding(Dimens.Space1))
    }
}

/** Spec §6: minors are not screened; the session ends here. */
@Composable
fun Under18Screen(onEnd: () -> Unit) {
    FlowScreen(
        headline = stringResource(R.string.under18_title),
        primaryText = stringResource(R.string.action_end_session),
        onPrimary = onEnd,
    ) {
        Text(stringResource(R.string.under18_body), style = ForesightTheme.type.bodyLarge)
    }
}
