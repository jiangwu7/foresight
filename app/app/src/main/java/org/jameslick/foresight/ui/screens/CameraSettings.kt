package org.jameslick.foresight.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import org.jameslick.foresight.R
import org.jameslick.foresight.capture.AdapterMode
import org.jameslick.foresight.capture.CameraController
import org.jameslick.foresight.capture.CaptureSettings
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.CheckRow
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.MetricReadout
import org.jameslick.foresight.ui.theme.OptionTile

/**
 * Settings → Camera (spec §3.1 "device check, LED test", §3.2 "calibrated per phone model in Settings"):
 * focus distance and the two exposure programs, dim-red mode, adapter mode, the import switch for test
 * images, and a device check that reads the camera's capabilities without opening it.
 */
@Composable
fun CameraSettingsSection() {
    val context = LocalContext.current
    val type = ForesightTheme.type
    val colors = ForesightTheme.colors
    var s by remember { mutableStateOf(CaptureSettings.load(context)) }
    var focus by remember { mutableStateOf(s.focusMm.toString()) }
    var alignIso by remember { mutableStateOf(s.alignIso.toString()) }
    var alignShutter by remember { mutableStateOf(s.alignShutterMs.toString()) }
    var captureIso by remember { mutableStateOf(s.captureIso.toString()) }
    var captureShutter by remember { mutableStateOf(s.captureShutterMs.toString()) }
    val caps = remember { CameraController.probe(context) }

    @Composable
    fun NumberField(label: Int, value: String, onChange: (String) -> Unit) {
        OutlinedTextField(value = value, onValueChange = onChange, singleLine = true, label = { Text(stringResource(label)) },
                          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), textStyle = type.body)
        Spacer(Modifier.height(Dimens.Space1))
    }

    Column {
        NumberField(R.string.settings_focus_mm, focus) { focus = it }
        NumberField(R.string.settings_align_iso, alignIso) { alignIso = it }
        NumberField(R.string.settings_align_shutter, alignShutter) { alignShutter = it }
        NumberField(R.string.settings_capture_iso, captureIso) { captureIso = it }
        NumberField(R.string.settings_capture_shutter, captureShutter) { captureShutter = it }
        CheckRow(stringResource(R.string.settings_red_mode), s.redMode, onCheckedChange = { s = s.copy(redMode = it) })
        Spacer(Modifier.height(Dimens.Space1))
        CheckRow(stringResource(R.string.settings_import_mode), s.importMode, onCheckedChange = { s = s.copy(importMode = it) })
        Spacer(Modifier.height(Dimens.Space1))
        CheckRow(stringResource(R.string.settings_ask_intake), s.askIntake, onCheckedChange = { s = s.copy(askIntake = it) })
        Spacer(Modifier.height(Dimens.Space2))
        Text(stringResource(R.string.settings_adapter_mode).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
        Spacer(Modifier.height(Dimens.Space1))
        OptionTile(stringResource(R.string.adapter_mode_manual), s.adapterMode == AdapterMode.MANUAL, { s = s.copy(adapterMode = AdapterMode.MANUAL) })
        Spacer(Modifier.height(Dimens.Space1))
        OptionTile(stringResource(R.string.adapter_mode_ble), s.adapterMode == AdapterMode.BLE, { s = s.copy(adapterMode = AdapterMode.BLE) })
        Spacer(Modifier.height(Dimens.Space2))
        BigAction(stringResource(R.string.action_save), secondary = true, onClick = {
            val saved = s.copy(
                focusMm = focus.toFloatOrNull() ?: s.focusMm,
                alignIso = alignIso.toIntOrNull() ?: s.alignIso,
                alignShutterMs = alignShutter.toFloatOrNull() ?: s.alignShutterMs,
                captureIso = captureIso.toIntOrNull() ?: s.captureIso,
                captureShutterMs = captureShutter.toFloatOrNull() ?: s.captureShutterMs,
            )
            CaptureSettings.save(context, saved); s = saved
        })
        Spacer(Modifier.height(Dimens.Space3))

        Text(stringResource(R.string.settings_device_check).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
        Spacer(Modifier.height(Dimens.Space1))
        val yes = stringResource(R.string.device_ok); val no = stringResource(R.string.device_missing); val unknown = stringResource(R.string.device_unknown)
        fun flag(v: Boolean?) = when (v) { true -> yes; false -> no; null -> unknown }
        val minFocus = caps?.minFocusDiopters?.takeIf { it > 0f }?.let { "%.0f".format(1000f / it) } ?: unknown
        MetricReadout(stringResource(R.string.device_min_focus_mm), minFocus, compact = true)
        Spacer(Modifier.height(Dimens.Space1))
        Row {
            MetricReadout(stringResource(R.string.device_manual_focus), flag(caps?.manualFocus), compact = true)
            Spacer(Modifier.width(Dimens.Space3))
            MetricReadout(stringResource(R.string.device_manual_exposure), flag(caps?.manualExposure), compact = true)
            Spacer(Modifier.width(Dimens.Space3))
            MetricReadout(stringResource(R.string.device_awb_lock), flag(caps?.awbLock), compact = true)
        }
        Spacer(Modifier.height(Dimens.Space3))
        Text(stringResource(R.string.settings_led_test).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
        Spacer(Modifier.height(Dimens.Space1))
        Text(stringResource(R.string.settings_led_test_body), style = type.body, color = colors.onBackgroundMuted)
    }
}
