package com.example.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING
}

class VoiceManager(private val context: Context) {
    companion object {
        /** Long answers are cut off when read aloud; the full text is always on screen. */
        const val MAX_SPOKEN_CHARS = 2500
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState = _voiceState.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText = _spokenText.asStateFlow()

    private val _volumeLevel = MutableStateFlow(0f)
    val volumeLevel = _volumeLevel.asStateFlow()

    // Queued speech: utterance IDs still playing, and a callback for when the queue drains.
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingUtterances = mutableSetOf<String>()
    private var utteranceCounter = 0
    private var onQueueDrained: (() -> Unit)? = null
    private var queueClosed = false

    val canSpeak: Boolean get() = isTtsInitialized

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.getDefault()
                isTtsInitialized = true
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _voiceState.value = VoiceState.SPEAKING
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { utteranceFinished(utteranceId) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { utteranceFinished(utteranceId) }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        mainHandler.post { utteranceFinished(utteranceId) }
                    }
                })
            }
        }
    }

    fun startListening(
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition is not available on this device.")
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _voiceState.value = VoiceState.LISTENING
                    _spokenText.value = ""
                }

                override fun onBeginningOfSpeech() {
                    _voiceState.value = VoiceState.LISTENING
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _volumeLevel.value = (rmsdB.coerceIn(0f, 10f) / 10f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _voiceState.value = VoiceState.PROCESSING
                }

                override fun onError(error: Int) {
                    _voiceState.value = VoiceState.IDLE
                    _volumeLevel.value = 0f
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't catch that. Tap the mic to try again."
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is needed for voice chat."
                        SpeechRecognizer.ERROR_NETWORK -> "Network error during speech recognition."
                        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                        else -> "Speech recognition error ($error)"
                    }
                    onError(msg)
                }

                override fun onResults(results: Bundle?) {
                    _voiceState.value = VoiceState.IDLE
                    _volumeLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val result = matches?.firstOrNull() ?: ""
                    _spokenText.value = result
                    if (result.isNotBlank()) {
                        onFinalResult(result)
                    } else {
                        onError("I didn't catch that. Tap the mic to try again.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull() ?: ""
                    if (partial.isNotBlank()) {
                        _spokenText.value = partial
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.IDLE
            onError("Failed to start speech recognizer: ${e.message}")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _voiceState.value = VoiceState.IDLE
        _volumeLevel.value = 0f
    }

    /** Speaks [text] right away (replacing anything queued). Markdown is cleaned first. */
    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!isTtsInitialized) {
            onDone()
            return
        }
        stopListening()
        stopSpeaking()
        enqueueSpeech(SpeechText.clean(text).take(MAX_SPOKEN_CHARS))
        finishSpeechQueue(onDone)
    }

    /** Adds already-cleaned text to the speech queue (used for sentence-by-sentence replies). */
    fun enqueueSpeech(text: String) {
        if (!isTtsInitialized || text.isBlank()) return
        queueClosed = false
        val id = "kora_${utteranceCounter++}"
        pendingUtterances += id
        _voiceState.value = VoiceState.SPEAKING
        val result = textToSpeech?.speak(text, TextToSpeech.QUEUE_ADD, null, id)
        if (result != TextToSpeech.SUCCESS) utteranceFinished(id)
    }

    /** No more text is coming for this reply; [onDone] runs once everything queued has been spoken. */
    fun finishSpeechQueue(onDone: () -> Unit) {
        queueClosed = true
        if (pendingUtterances.isEmpty()) {
            _voiceState.value = VoiceState.IDLE
            mainHandler.post(onDone)
        } else {
            onQueueDrained = onDone
        }
    }

    private fun utteranceFinished(id: String?) {
        if (id != null) pendingUtterances -= id
        if (pendingUtterances.isEmpty()) {
            if (_voiceState.value == VoiceState.SPEAKING) _voiceState.value = VoiceState.IDLE
            if (queueClosed) {
                val callback = onQueueDrained
                onQueueDrained = null
                callback?.invoke()
            }
        }
    }

    fun stopSpeaking() {
        pendingUtterances.clear()
        onQueueDrained = null
        queueClosed = false
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
        if (_voiceState.value == VoiceState.SPEAKING) _voiceState.value = VoiceState.IDLE
    }

    fun release() {
        stopSpeaking()
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        try {
            textToSpeech?.shutdown()
        } catch (_: Exception) {}
    }
}
