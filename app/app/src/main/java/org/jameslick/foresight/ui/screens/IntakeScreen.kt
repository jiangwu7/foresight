package org.jameslick.foresight.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import org.jameslick.foresight.R
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.OptionTile
import androidx.compose.foundation.clickable

/**
 * The six optional intake questions (spec §3.3), one per step, as big tiles (mockups 502/602).
 * Q1 includes "Under 18" only to refuse minors (§6). Q6 (share with clinic) defaults to No.
 */
@Composable
fun IntakeScreen(vm: ScreeningViewModel, onUnder18: () -> Unit, onDone: () -> Unit) {
    val state by vm.state.collectAsState()
    var step by rememberSaveable { mutableIntStateOf(0) }
    val total = 6
    val intake = state.intake
    val type = ForesightTheme.type
    val colors = ForesightTheme.colors

    fun next() { if (step < total - 1) step++ else onDone() }

    val headline = when (step) {
        0 -> R.string.q_age; 1 -> R.string.q_diabetes; 2 -> R.string.q_last_exam
        3 -> R.string.q_doctor; 4 -> R.string.q_contact; else -> R.string.q_share
    }
    FlowScreen(
        micro = stringResource(R.string.intake_micro, step + 1, total),
        headline = stringResource(headline),
        primaryText = stringResource(R.string.action_next),
        onPrimary = { if (step == 0 && intake.ageRange == "under18") onUnder18() else next() },
        secondary = {
            if (step != 5) Text(
                stringResource(R.string.action_skip), style = type.action, color = colors.onBackgroundMuted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { next() }.padding(Dimens.Space2),
            )
        },
    ) {
        when (step) {
            0 -> Options(
                listOf("under18" to R.string.age_under_18, "18_39" to R.string.age_18_39, "40_59" to R.string.age_40_59, "60_plus" to R.string.age_60_plus),
                intake.ageRange, grid = true,
            ) { vm.answerAge(it) }
            1 -> Options(
                listOf("yes" to R.string.answer_yes, "no" to R.string.answer_no, "not_sure" to R.string.answer_not_sure),
                intake.dxDiabetes, grid = true,
            ) { vm.answerDiabetes(it) }
            2 -> Options(
                listOf("within_1y" to R.string.exam_within_1y, "1_2y" to R.string.exam_1_2y, "over_2y" to R.string.exam_over_2y, "never" to R.string.exam_never),
                intake.lastExam,
            ) { vm.answerLastExam(it) }
            3 -> Options(
                listOf("yes" to R.string.answer_yes, "no" to R.string.answer_no),
                intake.hasDoctor?.let { if (it) "yes" else "no" }, grid = true,
            ) { vm.answerDoctor(it == "yes") }
            4 -> {
                OutlinedTextField(
                    value = intake.contactPhone, onValueChange = { vm.answerContact(it) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(), textStyle = type.bodyLarge,
                )
                Spacer(Modifier.height(Dimens.Space2))
                Text(stringResource(R.string.contact_hint), style = type.body, color = colors.onBackgroundMuted)
            }
            else -> Options(
                listOf("no" to R.string.share_no, "yes" to R.string.share_yes),
                if (intake.consentShareImages) "yes" else "no",
            ) { vm.answerShare(it == "yes") }
        }
    }
}

@Composable
private fun Options(options: List<Pair<String, Int>>, selected: String?, grid: Boolean = false, onSelect: (String) -> Unit) {
    if (grid) {
        options.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth()) {
                pair.forEachIndexed { i, (value, label) ->
                    OptionTile(stringResource(label), selected == value, { onSelect(value) }, tall = true, modifier = Modifier.weight(1f))
                    if (i == 0 && pair.size == 2) Spacer(Modifier.width(Dimens.Space1))
                }
            }
            Spacer(Modifier.height(Dimens.Space1))
        }
    } else {
        options.forEach { (value, label) ->
            OptionTile(stringResource(label), selected == value, { onSelect(value) })
            Spacer(Modifier.height(Dimens.Space1))
        }
    }
}
