package org.jameslick.foresight.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jameslick.foresight.R
import org.jameslick.foresight.capture.AdapterLink
import org.jameslick.foresight.capture.AdapterPrompt
import org.jameslick.foresight.capture.AutoCapture
import org.jameslick.foresight.capture.CameraController
import org.jameslick.foresight.capture.CaptureSettings
import org.jameslick.foresight.i18n.Speech
import org.jameslick.foresight.ml.HeuristicAssessor
import org.jameslick.foresight.ml.QualityAssessor
import org.jameslick.foresight.ml.QualityHeuristic
import org.jameslick.foresight.ui.FlowScreen
import org.jameslick.foresight.ui.ScreeningViewModel
import org.jameslick.foresight.ui.TestHooks
import org.jameslick.foresight.ui.theme.BigAction
import org.jameslick.foresight.ui.theme.Dimens
import org.jameslick.foresight.ui.theme.ForesightTheme
import org.jameslick.foresight.ui.theme.GlowRing
import org.jameslick.foresight.ui.theme.InstrumentCard
import org.jameslick.foresight.ui.theme.PillTone
import org.jameslick.foresight.ui.theme.RingState
import org.jameslick.foresight.ui.theme.StatusPill

/** Opacity of the red scrim over the preview in dim-red alignment mode (spec §3.2). */
private const val RED_SCRIM_ALPHA = 0.55f
/** Preview quality score at or above which the traffic light shows "almost" rather than "adjust". */
private const val FAIR_SCORE = 0.35f

/**
 * Phase 2 capture (spec §3.2): full-screen live preview behind the alignment ring, traffic-light quality
 * from the [QualityAssessor] on ~5 fps preview frames, dim-red alignment mode, auto-capture after three
 * good frames with a manual shutter as fallback, adapter prompts (Mode A), haptic + spoken cues, then the
 * same gradability review as the import flow. With [TestHooks.frameProvider] set, frames come from the
 * test instead of the camera (spec §8 auto-capture test).
 */
@Composable
fun LiveCaptureScreen(vm: ScreeningViewModel, eye: String, onNext: () -> Unit) {
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = ForesightTheme.colors
    val type = ForesightTheme.type
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val settings = remember { CaptureSettings.load(context) }
    val frameHook = TestHooks.frameProvider

    val analysis = if (eye == "R") state.right else state.left
    val attempts = if (eye == "R") state.rightAttempts else state.leftAttempts
    val busy = state.analyzing == eye
    val gradable = analysis?.quality?.gradable == true
    val exhausted = analysis != null && !gradable && !vm.canRetake(eye)
    val eyeLabel = stringResource(if (eye == "R") R.string.eye_right else R.string.eye_left)
    val micro = eyeLabel + " · " + stepLabel(if (eye == "R") 4 else 5)
    val cueLook = stringResource(R.string.capture_look_green_dot)
    val cueHold = stringResource(R.string.capture_hold_still)
    val cueGotIt = stringResource(R.string.capture_got_it)

    val speech = remember { Speech(context) }
    val adapter = remember { AdapterLink.create(settings.adapterMode) }
    val assessor: QualityAssessor = remember { HeuristicAssessor }
    val auto = remember { AutoCapture() }
    var liveQuality by remember { mutableStateOf<QualityHeuristic.Result?>(null) }
    var capturing by remember { mutableStateOf(false) }
    var showReview by remember { mutableStateOf(analysis != null) }
    var granted by remember { mutableStateOf(frameHook != null || ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val prompt by adapter.prompt.collectAsState()
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }

    suspend fun captureNow(controller: CameraController?) {
        if (capturing || busy) return
        capturing = true
        try {
            val bmp: Bitmap? = if (frameHook != null) frameHook() else {
                val c = controller ?: return
                c.applyCapture()
                // Mode A: prompt the screener, then watch the stream for the flash and keep that frame;
                // if no flash is seen in the window, take an ordinary still (it will read as too dark → retake).
                val prompt = scope.launch { adapter.captureFlash() }
                val caught = c.catchFlash()
                prompt.join()
                val b = caught ?: c.takeStill()
                c.applyAlignment()
                b
            }
            if (bmp != null) {
                vm.analyzeEye(eye, bmp, source = if (frameHook != null) "test_fixture" else "camera", focusCal = settings.focusMm)
            }
        } finally {
            capturing = false
            auto.reset()
        }
    }

    // One place handles every preview frame, from the camera or from the test hook.
    var controller by remember { mutableStateOf<CameraController?>(null) }
    fun onFrame(px: IntArray, w: Int, h: Int) {
        if (showReview || capturing || busy) return
        val q = assessor.assess(px, w, h)
        val ev = auto.feed(q.gradable, SystemClock.elapsedRealtime())
        scope.launch {
            liveQuality = q
            when (ev) {
                AutoCapture.Event.Settling -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); speech.speak(state.language, cueHold) }
                AutoCapture.Event.Trigger -> { haptics.performHapticFeedback(HapticFeedbackType.LongPress); captureNow(controller) }
                AutoCapture.Event.None -> Unit
            }
        }
    }

    DisposableEffect(Unit) {
        adapter.alignOn()
        speech.speak(state.language, cueLook)
        onDispose { adapter.off(); adapter.close(); speech.close(); controller?.close() }
    }
    LaunchedEffect(Unit) { if (!granted) permission.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(granted, frameHook) {
        if (granted && frameHook == null && controller == null) {
            val c = CameraController(context, lifecycleOwner, settings) { px, w, h -> onFrame(px, w, h) }
            controller = c
            runCatching { c.bind(previewView) }
        }
    }
    // Test hook: loop the provided bitmap as preview frames at the normal analysis rate.
    LaunchedEffect(frameHook) {
        if (frameHook == null) return@LaunchedEffect
        while (isActive) {
            val b = frameHook()
            if (b != null && !showReview && !capturing) {
                val w = b.width; val h = b.height
                val px = IntArray(w * h).also { b.getPixels(it, 0, w, 0, 0, w, h) }
                onFrame(px, w, h)
            }
            delay(AutoCapture.FRAME_INTERVAL_MS)
        }
    }
    LaunchedEffect(analysis?.capture?.id) {
        if (analysis != null) {
            showReview = true
            if (analysis.quality.gradable) speech.speak(state.language, cueGotIt)
        }
    }

    val backdrop = if (settings.redMode) colors.captureBackdrop else colors.background

    when {
        showReview && analysis != null -> FlowScreen(
            micro = micro,
            headline = stringResource(R.string.capture_live_title),
            primaryText = if (gradable || exhausted) stringResource(R.string.action_continue) else stringResource(R.string.action_retake),
            primaryEnabled = !busy,
            onPrimary = { if (gradable || exhausted) onNext() else { showReview = false; liveQuality = null } },
            secondary = { if (!gradable && !exhausted) BigAction(stringResource(R.string.action_continue), onClick = onNext, secondary = true) },
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { GlowRing(state = RingState.Locked, contentDescription = eyeLabel) }
            Spacer(Modifier.height(Dimens.Space2))
            EyeReviewBlock(analysis, attempts, busy)
        }
        !granted -> FlowScreen(
            micro = micro,
            headline = stringResource(R.string.capture_live_title),
            primaryText = stringResource(R.string.action_allow_camera),
            onPrimary = { permission.launch(Manifest.permission.CAMERA) },
        ) {
            Text(stringResource(R.string.capture_permission_body), style = type.bodyLarge)
        }
        else -> Box(Modifier.fillMaxSize().background(backdrop)) {
            if (frameHook == null) {
                AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                if (settings.redMode) Box(Modifier.fillMaxSize().background(colors.captureBackdrop.copy(alpha = RED_SCRIM_ALPHA)))
            }
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = Dimens.ScreenMargin)) {
                Spacer(Modifier.height(Dimens.Space4))
                Text(micro.uppercase(), style = type.microLabel, color = colors.onBackgroundMuted)
                Spacer(Modifier.height(Dimens.Space2))
                Text(stringResource(R.string.capture_live_title), style = type.headline)
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val ringState = when {
                        capturing || busy -> RingState.Locked
                        liveQuality?.gradable == true -> RingState.Aligning
                        else -> RingState.Idle
                    }
                    GlowRing(state = ringState, contentDescription = eyeLabel, size = Dimens.RingSize)
                    // The fixation dot: the screener centres the pupil on it; the participant is told to look at it.
                    val dot = colors.outcomeGreen
                    Canvas(Modifier.size(Dimens.PillDot)) { drawCircle(color = dot) }
                }
                Spacer(Modifier.weight(1f))
                val q = liveQuality
                val (pillText, tone) = when {
                    capturing || busy -> stringResource(R.string.capture_capturing) to PillTone.Accent
                    q == null -> stringResource(R.string.quality_poor) to PillTone.Neutral
                    q.gradable -> stringResource(R.string.quality_good) to PillTone.Good
                    q.score >= FAIR_SCORE -> stringResource(R.string.quality_fair) to PillTone.Caution
                    else -> stringResource(R.string.quality_poor) to PillTone.Alert
                }
                StatusPill(pillText, tone)
                Spacer(Modifier.height(Dimens.Space2))
                val promptText = when (prompt) {
                    AdapterPrompt.TurnOnRed -> stringResource(R.string.capture_turn_on_red)
                    AdapterPrompt.PressCapture -> stringResource(R.string.capture_press_capture)
                    AdapterPrompt.TurnOffRed -> stringResource(R.string.capture_turn_off_red)
                    null -> null
                }
                InstrumentCard {
                    Text(promptText ?: if (q != null && !q.gradable) stringResource(problemTitle(q.problem)) else stringResource(R.string.capture_live_hint),
                         style = type.bodyLarge)
                    if (promptText == null && q != null && !q.gradable) {
                        Spacer(Modifier.height(Dimens.Space1))
                        Text(stringResource(problemTip(q.problem)), style = type.body, color = colors.onBackgroundMuted)
                    }
                }
                Spacer(Modifier.height(Dimens.Space2))
                BigAction(stringResource(R.string.capture_take_photo), onClick = { scope.launch { captureNow(controller) } }, enabled = !capturing && !busy)
                Spacer(Modifier.height(Dimens.Space1))
                Text(stringResource(R.string.capture_auto_on).uppercase(), style = type.microLabel, color = colors.onBackgroundMuted,
                     textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(Dimens.Space3))
            }
        }
    }
}
