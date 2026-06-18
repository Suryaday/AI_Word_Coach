package com.wordcoach.app.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import java.util.Locale

/**
 * Wraps Android's [SpeechRecognizer] and prefers fully on-device recognition
 * so pronunciation practice works without an internet connection.
 *
 * On Android 13+ it uses the dedicated on-device recognizer when available.
 * On older versions it asks the system recognizer to prefer offline mode.
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

    var isListening: Boolean = false
        private set

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(callback: Callback) {
        this.callback = callback

        if (!isAvailable()) {
            callback.onError("Speech recognition is not available on this device.")
            return
        }

        // Recreate each session to avoid stale state.
        recognizer?.destroy()
        recognizer = createRecognizer()
        recognizer?.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Ask the engine to stay offline where supported.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
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

    private fun createRecognizer(): SpeechRecognizer {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else {
            SpeechRecognizer.createSpeechRecognizer(context)
        }
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
            "Offline English speech is not installed on this device yet."
        else -> "Something went wrong. Please try again."
    }
}
