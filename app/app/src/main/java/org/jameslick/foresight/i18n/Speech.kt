package org.jameslick.foresight.i18n

import android.content.Context
import android.speech.tts.TextToSpeech
import java.io.Closeable

/**
 * Spoken cues and read-aloud (spec §3.2, §3.4). Android TTS is the development placeholder; Phase 4 swaps in
 * student recordings looked up by string key (§13.8) and keeps TTS as the fallback for missing clips.
 */
class Speech(context: Context) : Closeable {
    private var ready = false
    private val pending = ArrayDeque<Pair<String, String>>()
    private val tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) while (pending.isNotEmpty()) pending.removeFirst().let { (tag, text) -> speakNow(tag, text) }
    }

    fun speak(languageTag: String, text: String) {
        if (!ready) { pending.addLast(languageTag to text); return }
        speakNow(languageTag, text)
    }

    private fun speakNow(languageTag: String, text: String) {
        tts.language = Languages.locale(languageTag)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "foresight-${text.hashCode()}")
    }

    fun stop() = tts.stop()

    override fun close() { tts.stop(); tts.shutdown() }
}
