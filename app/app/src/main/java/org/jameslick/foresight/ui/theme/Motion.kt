package org.jameslick.foresight.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode

/*
 * Motion tokens (spec §3.10.4). Every animation communicates state; nothing bounces or decorates.
 * Durations in milliseconds. Motion references from Higgsfield (§13.3a step 3) will tune these in Phase 1.
 */
object Motion {
    const val Fast = 150
    const val Medium = 300
    const val Slow = 600
    /** One breath of the idle alignment ring (in + out). */
    const val Breathe = 1800
    /** The result card unfolding. */
    const val Unfold = 700

    val Ease: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)      // emphasized decelerate
    val EaseInOut: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    /** Settles without overshoot: a lens snapping into focus, not a toy. */
    val Snap: SpringSpec<Float> = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium)
    val Gentle: SpringSpec<Float> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
}

/**
 * True when the system asks for reduced motion (animator duration scale = 0, i.e. "Remove animations"
 * in Accessibility settings). Components use it to replace loops with static states (§3.10.4).
 */
@Composable
fun rememberReduceMotion(): Boolean {
    if (LocalInspectionMode.current) return false
    val context = LocalContext.current
    return remember(context) {
        val scale = Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        scale == 0f
    }
}
