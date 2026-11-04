package org.jameslick.foresight.capture

/**
 * The auto-capture rule from spec §3.2, as a pure state machine so it can be tested without a camera:
 * preview frames arrive at ~5 fps with a gradable/not-gradable verdict; when [required] consecutive frames
 * are gradable the capture fires. One frame short of that, [Event.Settling] lets the screen say
 * "Hold still". A bad frame resets the count. After a trigger nothing fires again for [cooldownMs]
 * (the still is being taken and reviewed) — this is also what keeps the capture LED from strobing.
 */
class AutoCapture(private val required: Int = REQUIRED_FRAMES, private val cooldownMs: Long = COOLDOWN_MS) {

    enum class Event { None, Settling, Trigger }

    var consecutiveGood: Int = 0
        private set
    private var lastTriggerMs: Long = Long.MIN_VALUE / 2

    fun feed(gradable: Boolean, nowMs: Long): Event {
        if (!gradable) { consecutiveGood = 0; return Event.None }
        if (nowMs - lastTriggerMs < cooldownMs) return Event.None
        consecutiveGood++
        return when {
            consecutiveGood >= required -> { consecutiveGood = 0; lastTriggerMs = nowMs; Event.Trigger }
            consecutiveGood == required - 1 -> Event.Settling
            else -> Event.None
        }
    }

    fun reset() { consecutiveGood = 0 }

    companion object {
        const val REQUIRED_FRAMES = 3
        const val COOLDOWN_MS = 4000L
        /** Target preview analysis rate (spec §3.2: ~5 fps). */
        const val FRAME_INTERVAL_MS = 200L
    }
}
