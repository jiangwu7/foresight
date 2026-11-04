package org.jameslick.foresight.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.jameslick.foresight.R
import org.jameslick.foresight.data.ScreeningRepository
import org.jameslick.foresight.i18n.Speech
import org.jameslick.foresight.ml.EyeOutcome
import org.jameslick.foresight.ml.OverallOutcome
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.HeatmapOverlay
import org.jameslick.foresight.ui.theme.InstrumentCard
import org.jameslick.foresight.ui.theme.Outcome
import org.jameslick.foresight.ui.theme.OutcomeGlyph
import org.jameslick.foresight.ui.theme.ResultCard
import org.jameslick.foresight.ui.theme.Shapes

/** Approved wording per outcome (strings_medical.xml). Ungradable is never a negative result (§1.3). */
data class OutcomeText(val title: Int, val body: Int, val card: Int, val description: Int)

fun outcomeText(o: OverallOutcome): OutcomeText = when (o) {
    OverallOutcome.Green -> OutcomeText(R.string.medical_green_title, R.string.medical_green_body, R.string.medical_card_green, R.string.outcome_green_description)
    OverallOutcome.Yellow -> OutcomeText(R.string.medical_yellow_title, R.string.medical_yellow_body, R.string.medical_card_yellow, R.string.outcome_yellow_description)
    OverallOutcome.Red -> OutcomeText(R.string.medical_red_title, R.string.medical_red_body, R.string.medical_card_red, R.string.outcome_red_description)
    OverallOutcome.Ungradable -> OutcomeText(R.string.medical_ungradable_title, R.string.medical_ungradable_body, R.string.medical_card_ungradable, R.string.outcome_ungradable_description)
}

/** Mockups 505/506/607/508: result card, per-eye cards, heat-map toggle, delete-my-record, read aloud. */
@Composable
fun ResultsScreen(vm: ScreeningViewModel, onCard: () -> Unit, onDeleted: () -> Unit) {
    val state by vm.state.collectAsState()
    val overall = state.overall ?: OverallOutcome.Ungradable
    val text = outcomeText(overall)
    val context = LocalContext.current
    val speech = remember { Speech(context) }
    DisposableEffect(Unit) { onDispose { speech.close() } }
    var heat by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    val title = stringResource(text.title); val body = stringResource(text.body); val disclaimer = stringResource(R.string.medical_disclaimer)

    FlowScreen(
        micro = stringResource(R.string.result_micro),
        headline = "",
        primaryText = stringResource(if (overall == OverallOutcome.Green) R.string.action_print_card else R.string.action_print_referral_card),
        onPrimary = onCard,
        secondary = { BigAction(stringResource(R.string.action_read_aloud), onClick = { speech.speak(state.language, "$title $body $disclaimer") }, secondary = true) },
    ) {
        ResultCard(
            outcome = when (overall) { OverallOutcome.Green -> Outcome.Green; OverallOutcome.Yellow -> Outcome.Yellow; else -> Outcome.Red },
            title = title, body = body, disclaimer = disclaimer, outcomeDescription = stringResource(text.description),
        )
        if (overall == OverallOutcome.Red) {
            Spacer(Modifier.height(Dimens.Space2))
            InstrumentCard {
                Text(stringResource(R.string.next_step).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
                Spacer(Modifier.height(Dimens.Space1))
                Text(stringResource(R.string.next_step_card), style = type.body)
                Spacer(Modifier.height(Dimens.Space1))
                Text(stringResource(R.string.clinic_placeholder_name), style = type.body, color = colors.onBackgroundMuted)
            }
        }
        Spacer(Modifier.height(Dimens.Space2))
        Row(Modifier.fillMaxWidth()) {
            EyeCard(stringResource(R.string.eye_right), state.right, state.rightOutcome, Modifier.weight(1f))
            Spacer(Modifier.width(Dimens.Space1))
            EyeCard(stringResource(R.string.eye_left), state.left, state.leftOutcome, Modifier.weight(1f))
        }
        val heatSource = listOf(state.right, state.left).firstOrNull { it?.output != null && it.outcome != EyeOutcome.Green } ?: state.right
        if (heatSource?.output != null) {
            Spacer(Modifier.height(Dimens.Space2))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.show_heatmap), style = type.body, modifier = Modifier.weight(1f))
                Switch(checked = heat, onCheckedChange = { heat = it }, colors = SwitchDefaults.colors(checkedTrackColor = colors.accent, checkedThumbColor = colors.onAccent))
            }
            if (heat) {
                val bmp: Bitmap? = remember(heatSource.capture.id) { vm.repo.loadImage(heatSource.capture.imagePathEncrypted) }
                val out = heatSource.output
                Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(Shapes.Large).background(colors.surfaceRaised)) {
                    bmp?.let { Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize()) }
                    HeatmapOverlay(out.heatmap, out.heatRows, out.heatCols, visible = true,
                                   contentDescription = stringResource(R.string.medical_heatmap_caption), modifier = Modifier.fillMaxSize())
                }
                Spacer(Modifier.height(Dimens.Space1))
                Text(stringResource(R.string.medical_heatmap_caption), style = type.body, color = colors.onBackgroundMuted)
            }
        }
        Spacer(Modifier.height(Dimens.Space3))
        if (!confirmDelete) {
            Text(stringResource(R.string.action_delete_my_record), style = type.action, color = colors.onBackgroundMuted, textAlign = TextAlign.Center,
                 modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(Dimens.Space1))
        } else {
            InstrumentCard(glowColor = colors.outcomeRed) {
                Text(stringResource(R.string.delete_confirm_title), style = type.title)
                Spacer(Modifier.height(Dimens.Space1))
                Text(stringResource(R.string.delete_confirm_body), style = type.body, color = colors.onBackgroundMuted)
                Spacer(Modifier.height(Dimens.Space2))
                BigAction(stringResource(R.string.action_delete_my_record), onClick = { vm.deleteMyRecord(); onDeleted() })
                Spacer(Modifier.height(Dimens.Space1))
                BigAction(stringResource(R.string.action_cancel), onClick = { confirmDelete = false }, secondary = true)
            }
        }
    }
}

@Composable
private fun EyeCard(label: String, analysis: ScreeningRepository.EyeAnalysis?, outcome: EyeOutcome, modifier: Modifier = Modifier) {
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    val (word, glyph, tint) = when (outcome) {
        EyeOutcome.Green -> Triple(R.string.eye_clear, Outcome.Green, colors.outcomeGreen)
        EyeOutcome.Yellow -> Triple(R.string.eye_signs_seen, Outcome.Yellow, colors.outcomeYellow)
        EyeOutcome.Red -> Triple(R.string.eye_signs_seen, Outcome.Red, colors.outcomeRed)
        EyeOutcome.Ungradable -> Triple(R.string.eye_not_clear, null, colors.onBackgroundMuted)
    }
    InstrumentCard(modifier = modifier) {
        Text(label.uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
        Spacer(Modifier.height(Dimens.Space1))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (glyph != null) { OutcomeGlyph(glyph, tint); Spacer(Modifier.width(Dimens.Space1)) }
            Text(stringResource(word), style = type.body)
        }
        analysis?.output?.let { out ->
            Spacer(Modifier.height(Dimens.Space1))
            Text(stringResource(gradText(out.grade)), style = type.body, color = colors.onBackgroundMuted)
        }
    }
}

fun gradText(grade: Int): Int = when (grade) {
    0 -> R.string.medical_grade_none; 1 -> R.string.medical_grade_mild; 2 -> R.string.medical_grade_moderate
    3 -> R.string.medical_grade_severe; else -> R.string.medical_grade_proliferative
}

