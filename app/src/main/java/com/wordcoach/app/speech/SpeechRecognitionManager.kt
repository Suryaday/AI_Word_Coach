package com.wordcoach.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Wraps Android's [SpeechRecognizer] for pronunciation practice.
 *
 * IMPORTANT: We deliberately use the standard [SpeechRecognizer.createSpeechRecognizer]
 * (NOT createOnDeviceSpeechRecognizer) because many OEMs — including OnePlus — report
 * on-device recognition as "available" but then fail with ERROR_LANGUAGE_UNAVAILABLE
 * when actually invoked. The standard recognizer handles offline/online fallback
 * internally and is far more reliable across manufacturers.
 *
 * We also do NOT set EXTRA_PREFER_OFFLINE because on some devices (OnePlus 11,
 * certain Samsung builds) this causes an immediate rejection rather than a graceful
 * fallback. The system recognizer will use an offline model when one is available and
 * fall back to online otherwise — which is fine for our use case of scoring single
 * English words.
 */
class SpeechRecognitionManager(private val context: Context) {

    interface Callback {
        fun onReadyForSpeech() {}
        fun onRms(rms: Float) {}
        fun onEndOfSpeech() {}
        fun onResults(hypotheses: List<String>)
        fun onError(friendlyMessage: String)
    }

    private var recognizer: SpeechRecognizer? = null
    private var callback: Callback? = null

    /** Track whether we've already retried so we don't loop forever. */
    private var hasRetried: Boolean = false

    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(callback: Callback) {
        this.callback = callback
        hasRetried = false
        doStartListening()
    }

    private fun doStartListening() {
        if (!isAvailable()) {
            callback?.onError("Speech recognition is not available on this device.")
            return
        }

        // Recreate each session to avoid stale state.
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer?.setRecognitionListener(listener)

        // Use whichever English variant is available on this device.
        // Many phones (e.g. OnePlus 11 in India) only have en-GB or en-IN
        // downloaded, NOT en-US. Using "en" (generic English) lets the system
        // pick the best installed model. All English variants understand the
        // same vocabulary — only accent tolerance differs slightly.
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Do NOT set EXTRA_PREFER_OFFLINE — it causes failures on OnePlus/OxygenOS.
        }

        isListening = true
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun cancel() {
        isListening = false
        recognizer?.cancel()
    }

    fun destroy() {
        isListening = false
        recognizer?.destroy()
        recognizer = null
        callback = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            callback?.onReadyForSpeech()
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            callback?.onRms(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
            callback?.onEndOfSpeech()
        }

        override fun onError(error: Int) {
            isListening = false

            // On some devices the first attempt fails with a language/server error
            // but a second attempt succeeds (recognizer initialization race).
            if (!hasRetried && isRetryableError(error)) {
                hasRetried = true
                doStartListening()
                return
            }

            callback?.onError(messageForError(error))
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val list = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.toList()
                .orEmpty()
            if (list.isEmpty()) {
                callback?.onError("I didn't catch that. Please try again.")
            } else {
                callback?.onResults(list)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {}

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun isRetryableError(error: Int): Boolean = when (error) {
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
        SpeechRecognizer.ERROR_SERVER,
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> true
        else -> false
    }

    private fun messageForError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "There was a problem with the microphone."
        SpeechRecognizer.ERROR_NO_MATCH ->
            "I couldn't understand that. Tap the mic and try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
            "I didn't hear anything. Tap the mic and speak the word."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone permission is needed to score pronunciation."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
            "Still listening, please wait a moment."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "English speech model not found. Please open Google app → " +
                "Settings → Voice → Offline speech recognition → download English (US)."
        SpeechRecognizer.ERROR_SERVER ->
            "Speech service error. Please try again."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "No network connection, but that's fine — tap the mic and try again."
        else -> "Something went wrong (error $error). Please try again."
    }
}
