package org.jameslick.foresight.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import org.jameslick.foresight.R
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.GlowRing
import org.jameslick.foresight.ui.theme.InstrumentCard
import org.jameslick.foresight.ui.theme.RingState

/** Mockup 511: logo over a faint ring, the current event card, "New event", quiet Settings link. */
@Composable
fun HomeScreen(vm: ScreeningViewModel, onStart: () -> Unit, onNewEvent: () -> Unit, onSettings: () -> Unit) {
    val state by vm.state.collectAsState()
    val events by vm.events.collectAsState(initial = emptyList())
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    val current = state.event ?: events.firstOrNull()

    FlowScreen(
        headline = "",
        primaryText = if (current != null) stringResource(R.string.action_continue) else stringResource(R.string.action_new_event),
        onPrimary = { if (current != null) { vm.selectEvent(current); onStart() } else onNewEvent() },
        secondary = {
            Text(
                stringResource(R.string.action_settings),
                style = type.action, color = colors.onBackgroundMuted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onSettings).padding(Dimens.Space2),
            )
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlowRing(RingState.Idle, contentDescription = stringResource(R.string.app_name), size = Dimens.RingSize)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.foresight_mark), contentDescription = null, modifier = Modifier.size(Dimens.RingSizeSmall))
                Text(stringResource(R.string.app_name), style = type.display)
            }
        }
        Spacer(Modifier.height(Dimens.Space4))
        if (current != null) {
            Text(stringResource(R.string.home_resume_event).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
            Spacer(Modifier.height(Dimens.Space1))
            InstrumentCard(modifier = Modifier.clickable { vm.selectEvent(current); onStart() }) {
                Text(current.name, style = type.title)
                Text(current.locationLabel, style = type.body, color = colors.onBackgroundMuted)
            }
            Spacer(Modifier.height(Dimens.Space2))
            Text(stringResource(R.string.action_new_event), style = type.action, color = colors.accent,
                 modifier = Modifier.clickable(onClick = onNewEvent).padding(Dimens.Space1))
        } else {
            Text(stringResource(R.string.home_no_events), style = type.body, color = colors.onBackgroundMuted)
        }
    }
}

@Composable
fun NewEventScreen(vm: ScreeningViewModel, onCreated: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }
    FlowScreen(
        headline = stringResource(R.string.new_event_title),
        primaryText = stringResource(R.string.action_create_event),
        primaryEnabled = name.isNotBlank(),
        onPrimary = { vm.createEvent(name, location); onCreated() },
    ) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.new_event_name)) },
                          singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = ForesightTheme.type.body)
        Spacer(Modifier.height(Dimens.Space2))
        OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text(stringResource(R.string.new_event_location)) },
                          singleLine = true, modifier = Modifier.fillMaxWidth(), textStyle = ForesightTheme.type.body)
    }
}

