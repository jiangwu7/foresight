package org.jameslick.foresight.ui

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.jameslick.foresight.R
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.GlowRing
import org.jameslick.foresight.ui.theme.HeatmapOverlay
import org.jameslick.foresight.ui.theme.InstrumentCard
import org.jameslick.foresight.ui.theme.LanguageTile
import org.jameslick.foresight.ui.theme.MetricReadout
import org.jameslick.foresight.ui.theme.Outcome
import org.jameslick.foresight.ui.theme.PillTone
import org.jameslick.foresight.ui.theme.ResultCard
import org.jameslick.foresight.ui.theme.RingState
import org.jameslick.foresight.ui.theme.Shapes
import org.jameslick.foresight.ui.theme.StatusPill
import kotlin.math.exp

/**
 * Developer gallery of every design-system component with live state (tap the ring or the action button to
 * cycle states). Reviewed against the approved mockups on a 6.1" phone in a dark room (spec §3.10).
 * Every string comes from resources; nothing medical is hard-coded here.
 */
@Composable
fun DesignGalleryScreen() {
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    var ringIndex by remember { mutableIntStateOf(0) }
    var outcomeIndex by remember { mutableIntStateOf(1) }
    var heatVisible by remember { mutableStateOf(true) }
    var language by remember { mutableIntStateOf(0) }
    val ringState = RingState.entries[ringIndex % RingState.entries.size]
    val outcome = Outcome.entries[outcomeIndex % Outcome.entries.size]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.ScreenMargin, vertical = Dimens.Space4),
    ) {
        Text(stringResource(R.string.gallery_micro_label).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
        Spacer(Modifier.height(Dimens.Space1))
        Text(stringResource(R.string.gallery_title), style = type.headline)
        Spacer(Modifier.height(Dimens.Space2))
        Row {
            StatusPill(stringResource(R.string.gallery_pill_offline), PillTone.Neutral)
            Spacer(Modifier.width(Dimens.Space1))
            StatusPill(stringResource(R.string.gallery_pill_experimental), PillTone.Accent)
        }

        Spacer(Modifier.height(Dimens.Space6))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlowRing(
                state = ringState,
                contentDescription = stringResource(R.string.gallery_ring_description),
                modifier = Modifier.clickable { ringIndex++ },
            )
        }
        Spacer(Modifier.height(Dimens.Space2))
        Text(
            stringResource(R.string.gallery_ring_hint),
            style = type.body,
            color = colors.onBackgroundMuted,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Dimens.Space6))
        InstrumentCard {
            Row {
                MetricReadout(label = stringResource(R.string.gallery_metric_quality), value = "0.92")
                Spacer(Modifier.width(Dimens.Space6))
                MetricReadout(label = stringResource(R.string.gallery_metric_time), value = "1.3", unit = "s")
            }
        }

        Spacer(Modifier.height(Dimens.Space4))
        LanguageTile(stringResource(R.string.lang_en_native), stringResource(R.string.lang_en_code), selected = language == 0, onClick = { language = 0 })
        Spacer(Modifier.height(Dimens.Space1))
        LanguageTile(stringResource(R.string.lang_es_native), stringResource(R.string.lang_es_code), selected = language == 1, onClick = { language = 1 })
        Spacer(Modifier.height(Dimens.Space1))
        LanguageTile(stringResource(R.string.lang_vi_native), stringResource(R.string.lang_vi_code), selected = language == 2, onClick = { language = 2 })

        Spacer(Modifier.height(Dimens.Space4))
        ResultCard(
            outcome = outcome,
            title = stringResource(R.string.gallery_result_title),
            body = stringResource(R.string.gallery_result_body),
            disclaimer = stringResource(R.string.medical_disclaimer),
            outcomeDescription = stringResource(R.string.gallery_outcome_description),
        )

        Spacer(Modifier.height(Dimens.Space4))
        // Heat map demo over a plain dark panel. There is no retina image here on purpose (§13.1.2).
        val demo = remember { demoHeatmap(16, 16) }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(Shapes.Large)
                .background(colors.surfaceRaised)
                .clickable { heatVisible = !heatVisible },
        ) {
            HeatmapOverlay(
                values = demo,
                rows = 16,
                cols = 16,
                visible = heatVisible,
                contentDescription = stringResource(R.string.gallery_heatmap_description),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(Dimens.Space1))
        Text(stringResource(R.string.medical_heatmap_caption), style = type.body, color = colors.onBackgroundMuted)

        Spacer(Modifier.height(Dimens.Space6))
        BigAction(stringResource(R.string.action_continue), onClick = { outcomeIndex++ })
        Spacer(Modifier.height(Dimens.Space1))
        BigAction(stringResource(R.string.action_back), onClick = { ringIndex++ }, secondary = true)
        Spacer(Modifier.height(Dimens.Space8))
    }
}

/** Two soft blobs, purely to exercise the overlay. Not model output. */
private fun demoHeatmap(rows: Int, cols: Int): FloatArray = FloatArray(rows * cols) { i ->
    val r = i / cols
    val c = i % cols
    fun blob(cr: Int, cc: Int, s: Float) = exp(-((r - cr) * (r - cr) + (c - cc) * (c - cc)) / (2f * s * s))
    (blob(5, 10, 2.2f) + 0.6f * blob(10, 5, 1.6f)).coerceIn(0f, 1f)
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10, heightDp = 1800)
@Composable
private fun PreviewGallery() = ForesightTheme { DesignGalleryScreen() }
