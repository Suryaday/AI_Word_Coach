package com.wordcoach.app.ui

import android.app.Application
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.wordcoach.app.data.Word
import com.wordcoach.app.data.WordRepository
import com.wordcoach.app.speech.PronunciationScorer
import com.wordcoach.app.speech.SpeechRecognitionManager
import com.wordcoach.app.speech.TtsManager

/**
 * Holds all screen state and coordinates the word list, text-to-speech,
 * and the microphone-based pronunciation scoring.
 */
class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val words: List<Word> = WordRepository.getWords(app)
    private val tts = TtsManager(app)
    private val recognizer = SpeechRecognitionManager(app)

    // ---- Observable UI state -------------------------------------------------

    var index by mutableStateOf(0)
        private set

    var isSpeaking by mutableStateOf(false)
        private set

    var isListening by mutableStateOf(false)
        private set

    /** Microphone loudness, 0f..1f, used to animate the mic button. */
    var micLevel by mutableStateOf(0f)
        private set

    var ttsStatus by mutableStateOf(TtsManager.Status.INITIALIZING)
        private set

    /** Latest pronunciation result, or null if not attempted for this word. */
    var lastResult by mutableStateOf<PronunciationScorer.Result?>(null)
        private set

    /** A short hint or status message shown under the mic. */
    var statusMessage by mutableStateOf("Tap the mic and say the word.")
        private set

    val totalWords: Int get() = words.size
    val currentWord: Word get() = words[index]
    val speechAvailable: Boolean get() = recognizer.isAvailable()

    init {
        tts.setListeners(
            onSpeaking = { speaking -> onMain { isSpeaking = speaking } },
            onStatus = { status -> onMain { ttsStatus = status } }
        )
    }

    // ---- Navigation ----------------------------------------------------------

    fun nextWord() {
        index = (index + 1) % words.size
        resetForNewWord()
    }

    fun previousWord() {
        index = if (index - 1 < 0) words.size - 1 else index - 1
        resetForNewWord()
    }

    fun shuffleWord() {
        if (words.size <= 1) return
        var next = index
        while (next == index) next = words.indices.random()
        index = next
        resetForNewWord()
    }

    private fun resetForNewWord() {
        recognizer.cancel()
        tts.stop()
        isListening = false
        micLevel = 0f
        lastResult = null
        statusMessage = "Tap the mic and say the word."
    }

    // ---- Text to speech ------------------------------------------------------

    /** Replay the correct pronunciation of the current word. */
    fun replayWord() {
        tts.speak(currentWord.word)
    }

    /** Read the example sentence aloud. */
    fun speakExample() {
        if (currentWord.example.isNotBlank()) tts.speak(currentWord.example)
    }

    // ---- Microphone / scoring ------------------------------------------------

    fun startListening() {
        if (isListening) return
        lastResult = null
        isListening = true
        micLevel = 0f
        statusMessage = "Listening… say \"${currentWord.word}\""

        recognizer.startListening(object : SpeechRecognitionManager.Callback {
            override fun onReadyForSpeech() = onMain {
                statusMessage = "Listening… say \"${currentWord.word}\""
            }

            override fun onRms(rms: Float) = onMain {
                // Map the raw dB value (~ -2..10) into a 0..1 range.
                micLevel = ((rms + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onEndOfSpeech() = onMain {
                isListening = false
                micLevel = 0f
                statusMessage = "Checking your pronunciation…"
            }

            override fun onResults(hypotheses: List<String>) = onMain {
                isListening = false
                micLevel = 0f
                val result = PronunciationScorer.score(currentWord.word, hypotheses)
                lastResult = result
                statusMessage = result.message
            }

            override fun onError(friendlyMessage: String) = onMain {
                isListening = false
                micLevel = 0f
                statusMessage = friendlyMessage
            }
        })
    }

    fun stopListening() {
        recognizer.stopListening()
    }

    fun onPermissionDenied() {
        statusMessage = "Please allow microphone access to practice speaking."
    }

    // ---- Lifecycle -----------------------------------------------------------

    override fun onCleared() {
        super.onCleared()
        tts.release()
        recognizer.destroy()
    }

    private inline fun onMain(crossinline block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else mainHandler.post { block() }
    }
}
