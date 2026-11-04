package org.jameslick.foresight.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.jameslick.foresight.R
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.InstrumentCard
import org.jameslick.foresight.ui.theme.MetricReadout
import org.jameslick.foresight.ui.theme.PillTone
import org.jameslick.foresight.ui.theme.StatusPill

/** Mockup 510 + "About models" (spec §4.4): event, model card, exports, retention purge, design gallery. */
@Composable
fun SettingsScreen(vm: ScreeningViewModel, onBack: () -> Unit, onGallery: () -> Unit) {
    val state by vm.state.collectAsState()
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    var password by rememberSaveable { mutableStateOf("") }
    val card = vm.repo.modelCard

    FlowScreen(
        headline = stringResource(R.string.settings_title),
        primaryText = stringResource(R.string.action_back),
        onPrimary = onBack,
    ) {
        StatusPill(stringResource(R.string.offline), PillTone.Neutral)
        Spacer(Modifier.height(Dimens.Space3))

        Section(stringResource(R.string.settings_event)) {
            Text(state.event?.name ?: "—", style = type.title)
            state.event?.let { Text(it.locationLabel, style = type.body, color = colors.onBackgroundMuted) }
        }

        Section(stringResource(R.string.settings_models)) {
            Text(stringResource(R.string.settings_model_line, card.name, card.version), style = type.numeralSmall)
            Spacer(Modifier.height(Dimens.Space2))
            Row {
                MetricReadout(stringResource(R.string.settings_metric_sensitivity), pct(card.metrics["referable_sensitivity"]), compact = true)
                Spacer(Modifier.width(Dimens.Space3))
                MetricReadout(stringResource(R.string.settings_metric_specificity), pct(card.metrics["referable_specificity"]), compact = true)
                Spacer(Modifier.width(Dimens.Space3))
                MetricReadout(stringResource(R.string.settings_metric_kappa), fmt(card.metrics["kappa_quadratic"]), compact = true)
            }
            Spacer(Modifier.height(Dimens.Space2))
            Text(stringResource(R.string.settings_intended_use).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
            Text(card.intendedUse, style = type.body)
            Spacer(Modifier.height(Dimens.Space2))
            Text(stringResource(R.string.settings_limitations).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
            card.limitations.forEach { Text("· $it", style = type.body, color = colors.onBackgroundMuted) }
            Spacer(Modifier.height(Dimens.Space2))
            Text(card.disclaimer, style = type.body, color = colors.onBackgroundMuted)
        }

        Section(stringResource(R.string.settings_camera)) { CameraSettingsSection() }

        Section(stringResource(R.string.settings_export)) {
            BigAction(stringResource(R.string.action_export_deid), onClick = { vm.exportDeid() }, secondary = true, enabled = state.event != null)
            Spacer(Modifier.height(Dimens.Space2))
            OutlinedTextField(value = password, onValueChange = { password = it }, singleLine = true,
                              label = { Text(stringResource(R.string.settings_clinic_password)) },
                              visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), textStyle = type.body)
            Spacer(Modifier.height(Dimens.Space1))
            BigAction(stringResource(R.string.action_export_clinic), onClick = { vm.exportClinic(password) }, secondary = true,
                      enabled = state.event != null && password.length >= 8)
            state.lastExport?.let {
                Spacer(Modifier.height(Dimens.Space1))
                Text(stringResource(R.string.settings_export_done, it.name), style = type.body, color = colors.onBackgroundMuted)
            }
            state.error?.let { Text(it, style = type.body, color = colors.outcomeRed) }
            Spacer(Modifier.height(Dimens.Space2))
            BigAction(stringResource(R.string.settings_purge), onClick = { vm.purgeExpired() }, secondary = true)
        }

        Text(stringResource(R.string.settings_design_gallery), style = type.action, color = colors.accent,
             modifier = Modifier.clickable(onClick = onGallery).padding(Dimens.Space1))
    }
}

@Composable
private fun Section(label: String, content: @Composable () -> Unit) {
    val colors = ForesightTheme.colors
    Text(label.uppercase(), style = ForesightTheme.type.microLabel, color = colors.onBackgroundMuted)
    Spacer(Modifier.height(Dimens.Space1))
    InstrumentCard { Column { content() } }
    Spacer(Modifier.height(Dimens.Space2))
    HorizontalDivider(thickness = Dimens.Hairline, color = colors.hairline)
    Spacer(Modifier.height(Dimens.Space2))
}

private fun pct(v: Float?): String = if (v == null || v.isNaN()) "—" else "%.0f%%".format(v * 100)
private fun fmt(v: Float?): String = if (v == null || v.isNaN()) "—" else "%.2f".format(v)
