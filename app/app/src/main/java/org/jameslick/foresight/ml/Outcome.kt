package org.jameslick.foresight.ml

/**
 * Result-outcome mapping (spec §3.4, §1.3): model outputs -> one of the fixed outcomes. A pure function
 * with exhaustive tests (`OutcomeTest`). No wording lives here; the UI maps each outcome to the approved
 * strings in strings_medical.xml.
 *
 * DRAFT THRESHOLDS pending clinician review (DECISIONS.md 2026-11-04):
 *   Red     grade >= 3 (severe / proliferative)            -> prompt exam, referral within 2–4 weeks
 *   Yellow  grade 1–2, or referable probability >= threshold -> closer look, referral within 3 months
 *   Green   grade 0 and referable probability < threshold   -> no signs detected in this photo
 *   Ungradable                                              -> "we couldn't get a clear enough photo", see a provider
 * Ungradable is never Green (§1.3). Overall = the more serious of the two eyes; an ungradable eye beside a
 * green eye makes the overall Ungradable, because half of a negative screen is not a negative screen.
 */
enum class DrGrade { None, Mild, Moderate, Severe, Proliferative }

enum class EyeOutcome { Green, Yellow, Red, Ungradable }

enum class OverallOutcome { Green, Yellow, Red, Ungradable }

/** Model output for one eye. `probs` are the 5 softmax values (index = DrGrade ordinal). */
data class EyeResult(
    val gradable: Boolean,
    val probs: FloatArray? = null,
    val referableProb: Float? = null,
) {
    val grade: DrGrade? get() = probs?.let { DrGrade.entries[argmax(it)] }

    companion object {
        fun argmax(a: FloatArray): Int {
            var best = 0
            for (i in a.indices) if (a[i] > a[best]) best = i
            return best
        }
    }
}

object OutcomeMapper {
    /** Referable DR = moderate or worse (spec §12). */
    const val REFERABLE_MIN_GRADE = 2
    /** Threshold on P(grade >= 2); must equal `referable_threshold` in the shipped model card. */
    const val DEFAULT_REFERABLE_THRESHOLD = 0.5f

    fun referableProb(probs: FloatArray): Float {
        var s = 0f
        for (i in REFERABLE_MIN_GRADE until probs.size) s += probs[i]
        return s
    }

    fun eyeOutcome(eye: EyeResult, threshold: Float = DEFAULT_REFERABLE_THRESHOLD): EyeOutcome {
        if (!eye.gradable || eye.probs == null) return EyeOutcome.Ungradable
        val grade = EyeResult.argmax(eye.probs)
        val referable = eye.referableProb ?: referableProb(eye.probs)
        return when {
            grade >= 3 -> EyeOutcome.Red
            grade >= 1 || referable >= threshold -> EyeOutcome.Yellow
            else -> EyeOutcome.Green
        }
    }

    fun overall(right: EyeOutcome, left: EyeOutcome): OverallOutcome {
        val eyes = listOf(right, left)
        return when {
            EyeOutcome.Red in eyes -> OverallOutcome.Red
            EyeOutcome.Yellow in eyes -> OverallOutcome.Yellow
            EyeOutcome.Ungradable in eyes -> OverallOutcome.Ungradable
            else -> OverallOutcome.Green
        }
    }

    fun overall(right: EyeResult, left: EyeResult, threshold: Float = DEFAULT_REFERABLE_THRESHOLD): OverallOutcome =
        overall(eyeOutcome(right, threshold), eyeOutcome(left, threshold))
}
