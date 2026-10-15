package com.example.util

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import java.util.Locale

class VoiceNoteRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var speechRecognizer: SpeechRecognizer? = null

    val isRecording = MutableStateFlow(false)
    val recordingDurationSec = MutableStateFlow(0)
    val liveTranscript = MutableStateFlow("")

    private var recordStartTime: Long = 0L

    fun startRecording(onSpeechRecognized: ((String) -> Unit)? = null): Boolean {
        if (isRecording.value) return false
        return try {
            val audioDir = File(context.cacheDir, "audio")
            if (!audioDir.exists()) audioDir.mkdirs()

            val file = File(audioDir, "voice_note_${System.currentTimeMillis()}.m4a")
            currentAudioFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)

            try {
                recorder.prepare()
                recorder.start()
            } catch (e: Exception) {
                recorder.release()
                throw e
            }
            mediaRecorder = recorder
            recordStartTime = System.currentTimeMillis()
            recordingDurationSec.value = 0
            isRecording.value = true
            liveTranscript.value = ""

            // Simultaneously run speech recognizer for instant transcription
            startTranscription(onSpeechRecognized)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            currentAudioFile?.delete()
            currentAudioFile = null
            isRecording.value = false
            false
        }
    }

    private fun startTranscription(onSpeechRecognized: ((String) -> Unit)?) {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {}
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {}
                        override fun onError(error: Int) {}
                        override fun onResults(results: Bundle?) {
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                liveTranscript.value = text
                                onSpeechRecognized?.invoke(text)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val text = matches?.firstOrNull() ?: ""
                            if (text.isNotBlank()) {
                                liveTranscript.value = text
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                speechRecognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Seconds since recording started; the ViewModel polls this to drive the on-screen timer. */
    fun elapsedSeconds(): Int =
        if (isRecording.value) ((System.currentTimeMillis() - recordStartTime) / 1000).toInt() else 0

    data class VoiceNoteResult(
        val file: File,
        val durationSec: Int,
        val transcript: String
    )

    /** Stops and always releases the recorder (a failed stop() must not leak the mic). */
    private fun releaseRecorder(): Boolean {
        val recorder = mediaRecorder ?: return false
        mediaRecorder = null
        return try {
            recorder.stop()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            recorder.release()
        }
    }

    fun stopRecording(): VoiceNoteResult? {
        val file = currentAudioFile ?: return null
        val duration = ((System.currentTimeMillis() - recordStartTime) / 1000).toInt().coerceAtLeast(1)

        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val stoppedCleanly = releaseRecorder()
        currentAudioFile = null

        isRecording.value = false
        val finalTranscript = liveTranscript.value

        // MediaRecorder.stop() throws if nothing was captured (e.g. a sub-second tap); the file
        // is unusable then, so don't hand back an empty attachment.
        if (!stoppedCleanly) {
            file.delete()
            return null
        }

        return VoiceNoteResult(
            file = file,
            durationSec = duration,
            transcript = finalTranscript
        )
    }

    fun cancelRecording() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        releaseRecorder()

        currentAudioFile?.delete()
        currentAudioFile = null
        isRecording.value = false
    }
}
