package org.jameslick.foresight.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import org.jameslick.foresight.R
import org.jameslick.foresight.i18n.Speech
import org.jameslick.foresight.ml.QualityHeuristic
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.TestHooks
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.GlowRing
import org.jameslick.foresight.ui.theme.InstrumentCard
import org.jameslick.foresight.ui.theme.MetricReadout
import org.jameslick.foresight.ui.theme.PillTone
import org.jameslick.foresight.ui.theme.RingState
import org.jameslick.foresight.ui.theme.StatusPill

/**
 * Phase 1 capture: the photo is imported (system photo picker, or the test hook), then checked for
 * gradability and analyzed (spec §3.2 "gallery import", §10 Phase 1). The live camera, red mode and
 * auto-capture replace the import button in Phase 2; the rest of this screen stays.
 */
@Composable
fun CaptureScreen(vm: ScreeningViewModel, eye: String, onNext: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val speech = remember { Speech(context) }
    DisposableEffect(Unit) { onDispose { speech.close() } }
    val analysis = if (eye == "R") state.right else state.left
    val attempts = if (eye == "R") state.rightAttempts else state.leftAttempts
    val busy = state.analyzing == eye
    val type = ForesightTheme.type
    val colors = ForesightTheme.colors
    val gotIt = stringResource(R.string.capture_got_it)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let { loadBitmap(context, it) }?.let { vm.analyzeEye(eye, it) }
    }
    fun choose() {
        val hook = TestHooks.bitmapProvider
        if (hook != null) hook(eye)?.let { vm.analyzeEye(eye, it, source = "test_fixture") }
        else picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    LaunchedEffect(analysis?.capture?.id) { if (analysis?.quality?.gradable == true) speech.speak(state.language, gotIt) }

    val eyeLabel = stringResource(if (eye == "R") R.string.eye_right else R.string.eye_left)
    val gradable = analysis?.quality?.gradable == true
    val exhausted = analysis != null && !gradable && !vm.canRetake(eye)

    FlowScreen(
        micro = eyeLabel + " · " + stringResource(R.string.step_of, if (eye == "R") 4 else 5, FLOW_STEPS),
        headline = stringResource(R.string.capture_import_title),
        primaryText = when {
            analysis == null -> stringResource(R.string.action_choose_photo)
            gradable || exhausted -> stringResource(R.string.action_continue)
            else -> stringResource(R.string.action_retake)
        },
        primaryEnabled = !busy,
        onPrimary = { if (analysis == null || (!gradable && !exhausted)) choose() else onNext() },
        secondary = {
            if (analysis != null && !gradable && !exhausted) {
                BigAction(stringResource(R.string.action_continue), onClick = onNext, secondary = true)
            }
        },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            GlowRing(
                state = when { busy -> RingState.Aligning; gradable -> RingState.Locked; else -> RingState.Idle },
                contentDescription = eyeLabel,
            )
        }
        Spacer(Modifier.height(Dimens.Space2))
        if (analysis == null && !busy) Text(stringResource(R.string.capture_import_hint), style = type.body, color = colors.onBackgroundMuted)
        else EyeReviewBlock(analysis, attempts, busy)
        state.error?.let { Spacer(Modifier.height(Dimens.Space2)); Text(it, style = type.body, color = colors.outcomeRed) }
    }
}

/** The gradability verdict for one eye photo: shared by the import flow and the live camera flow. */
@Composable
fun EyeReviewBlock(analysis: org.jameslick.foresight.data.ScreeningRepository.EyeAnalysis?, attempts: Int, busy: Boolean) {
    val type = ForesightTheme.type
    val colors = ForesightTheme.colors
    when {
        busy || analysis == null -> Text(stringResource(R.string.capture_hold_still), style = type.bodyLarge)
        analysis.quality.gradable -> {
            StatusPill(stringResource(R.string.capture_got_it), PillTone.Good)
            Spacer(Modifier.height(Dimens.Space2))
            MetricReadout(stringResource(R.string.capture_quality), "%.2f".format(analysis.quality.score), compact = true)
        }
        else -> {
            val problem = analysis.quality.problem
            InstrumentCard {
                Text(stringResource(problemTitle(problem)), style = type.title)
                Spacer(Modifier.height(Dimens.Space1))
                Text(stringResource(problemTip(problem)), style = type.body, color = colors.onBackgroundMuted)
                Spacer(Modifier.height(Dimens.Space2))
                Text(stringResource(R.string.capture_attempt, attempts, ScreeningViewModel.MAX_ATTEMPTS).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
            }
        }
    }
}

fun problemTitle(p: QualityHeuristic.Problem?): Int = when (p) {
    QualityHeuristic.Problem.Dark -> R.string.problem_dark
    QualityHeuristic.Problem.Blurry -> R.string.problem_blurry
    QualityHeuristic.Problem.OffCenter -> R.string.problem_offcenter
    QualityHeuristic.Problem.NotFundus -> R.string.problem_not_fundus
    QualityHeuristic.Problem.Reflection, null -> R.string.problem_reflection
}

fun problemTip(p: QualityHeuristic.Problem?): Int = when (p) {
    QualityHeuristic.Problem.Dark -> R.string.problem_dark_tip
    QualityHeuristic.Problem.Blurry -> R.string.problem_blurry_tip
    QualityHeuristic.Problem.OffCenter -> R.string.problem_offcenter_tip
    QualityHeuristic.Problem.NotFundus -> R.string.problem_not_fundus_tip
    QualityHeuristic.Problem.Reflection, null -> R.string.problem_reflection_tip
}

@Suppress("DEPRECATION")
private fun loadBitmap(context: android.content.Context, uri: Uri): Bitmap? = try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { d, _, _ -> d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE; d.isMutableRequired = false }
    } else MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
} catch (e: Exception) { null }
