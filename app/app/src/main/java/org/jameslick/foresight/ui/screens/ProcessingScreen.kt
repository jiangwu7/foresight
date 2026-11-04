package org.jameslick.foresight.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.delay
import org.jameslick.foresight.R
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.GlowRing
import org.jameslick.foresight.ui.theme.RingState

/** Mockup 504/604: the ring, a monospaced timer, "on this phone". Eyes were analyzed at capture; this screen computes the overall outcome. */
@Composable
fun ProcessingScreen(vm: ScreeningViewModel, onReady: () -> Unit) {
    val state by vm.state.collectAsState()
    var tenths by remember { mutableIntStateOf(0) }
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type

    LaunchedEffect(Unit) { vm.computeResult() }
    LaunchedEffect(state.referral) { if (state.referral != null) { delay(600); onReady() } }
    LaunchedEffect(Unit) { while (true) { delay(100); tenths++ } }

    Column(
        modifier = Modifier.fillMaxSize().background(colors.background).safeDrawingPadding().padding(Dimens.ScreenMargin),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlowRing(RingState.Aligning, contentDescription = stringResource(R.string.processing_title))
        Spacer(Modifier.height(Dimens.Space4))
        Text(stringResource(R.string.processing_title), style = type.headline)
        Spacer(Modifier.height(Dimens.Space1))
        Text("%d:%02d".format(tenths / 600, (tenths / 10) % 60), style = type.numeral)
        Spacer(Modifier.height(Dimens.Space3))
        Text(stringResource(R.string.processing_on_phone).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
    }
}
