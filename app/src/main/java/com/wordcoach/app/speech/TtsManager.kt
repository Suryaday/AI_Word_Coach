package com.wordcoach.app.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Wraps Android's built-in [TextToSpeech] engine.
 *
 * Once the device's English voice data is installed, this speaks fully
 * offline, so the learner can replay the correct pronunciation as many
 * times as she likes without any internet connection.
 */
class TtsManager(context: Context) {

    enum class Status { INITIALIZING, READY, LANGUAGE_MISSING, FAILED }

    private var tts: TextToSpeech? = null

    /** Current engine status; observed by the UI to show helpful hints. */
    var status: Status = Status.INITIALIZING
        private set

    /** True while audio is actively playing. */
    var isSpeaking: Boolean = false
        private set

    private var onSpeakingChanged: ((Boolean) -> Unit)? = null
    private var onStatusChanged: ((Status) -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { result ->
            if (result == TextToSpeech.SUCCESS) {
                applyLanguage()
            } else {
                updateStatus(Status.FAILED)
            }
        }
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = updateSpeaking(true)
            override fun onDone(utteranceId: String?) = updateSpeaking(false)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = updateSpeaking(false)
            override fun onError(utteranceId: String?, errorCode: Int) = updateSpeaking(false)
        })
    }

    private fun applyLanguage() {
        val engine = tts ?: return
        // A slightly slower rate makes it easier for a learner to follow.
        engine.setSpeechRate(0.85f)
        engine.setPitch(1.0f)
        val result = engine.setLanguage(Locale.US)
        updateStatus(
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                Status.LANGUAGE_MISSING
            } else {
                Status.READY
            }
        )
    }

    /** Speak [text] aloud, cancelling anything already playing. */
    fun speak(text: String) {
        val engine = tts ?: return
        if (status != Status.READY) return
        engine.stop()
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "wc-${text.hashCode()}")
    }

    /** Stop any ongoing speech. */
    fun stop() {
        tts?.stop()
        updateSpeaking(false)
    }

    fun setListeners(
        onSpeaking: (Boolean) -> Unit,
        onStatus: (Status) -> Unit
    ) {
        onSpeakingChanged = onSpeaking
        onStatusChanged = onStatus
        // Push current values immediately.
        onStatus(status)
        onSpeaking(isSpeaking)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        onSpeakingChanged = null
        onStatusChanged = null
    }

    private fun updateSpeaking(value: Boolean) {
        isSpeaking = value
        onSpeakingChanged?.invoke(value)
    }

    private fun updateStatus(value: Status) {
        status = value
        onStatusChanged?.invoke(value)
    }
}
