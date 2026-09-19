package com.aihearingassist

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

class SystemSpeechRecognizer {
    private data class Callbacks(
        val onReady: () -> Unit,
        val onPartial: (String) -> Unit,
        val onResult: (SpeechRecognitionResult) -> Unit,
        val onError: (String) -> Unit
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var appContext: Context? = null
    private var language: SpeechLanguage = SpeechLanguage.ENGLISH_IN
    private var callbacks: Callbacks? = null
    private var stopped = true
    private var paused = false
    private var lastCommittedText = ""
    private var lastCommittedAtMillis = 0L
    @Volatile private var sessionGeneration = 0L

    fun start(
        context: Context,
        language: SpeechLanguage,
        onReady: () -> Unit,
        onPartial: (String) -> Unit,
        onResult: (SpeechRecognitionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val applicationContext = context.applicationContext
        if (!SpeechRecognizer.isRecognitionAvailable(applicationContext)) {
            onError("Enhanced speech capture is not available on this device. Try Private mode.")
            return
        }

        stop()
        val sessionId = sessionGeneration
        appContext = applicationContext
        this.language = language
        callbacks = Callbacks(onReady, onPartial, onResult, onError)
        stopped = false
        paused = false
        lastCommittedText = ""
        lastCommittedAtMillis = 0L

        recognizer = SpeechRecognizer.createSpeechRecognizer(applicationContext).apply {
            setRecognitionListener(listener(sessionId))
        }
        startListeningSession(sessionId)
    }

    fun pause() {
        paused = true
        recognizer?.cancel()
    }

    fun resume() {
        if (stopped) return
        paused = false
        startListeningSession(sessionGeneration)
    }

    fun stop() {
        sessionGeneration += 1
        stopped = true
        paused = false
        mainHandler.removeCallbacksAndMessages(null)
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
    }

    fun shutdown() {
        stop()
        callbacks = null
        appContext = null
    }

    private fun startListeningSession(sessionId: Long) {
        val activeRecognizer = recognizer ?: return
        if (!isSessionActive(sessionId)) return

        try {
            activeRecognizer.startListening(recognitionIntent())
        } catch (exception: RuntimeException) {
            if (isSessionActive(sessionId)) {
                callbacks?.onError?.invoke(
                    "Could not start the system speech service: ${exception.message ?: "initialization failed"}"
                )
            }
        }
    }

    private fun recognitionIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.recognitionTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.recognitionTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 30_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 600L)
        }
    }

    private fun scheduleRestart(sessionId: Long, delayMillis: Long = 220L) {
        if (!isSessionActive(sessionId)) return
        mainHandler.postDelayed(
            { if (isSessionActive(sessionId)) startListeningSession(sessionId) },
            delayMillis
        )
    }

    private fun commitBestResult(sessionId: Long, results: Bundle?) {
        if (!isSessionActive(sessionId)) return
        val text = bestText(results) ?: return
        val now = SystemClock.elapsedRealtime()
        val isImmediateDuplicate = text.equals(lastCommittedText, ignoreCase = true) &&
            now - lastCommittedAtMillis < DUPLICATE_WINDOW_MILLIS
        if (isImmediateDuplicate) return
        lastCommittedText = text
        lastCommittedAtMillis = now
        if (isSessionActive(sessionId)) {
            callbacks?.onResult?.invoke(SpeechRecognitionResult(text = text))
        }
    }

    private fun bestText(results: Bundle?): String? {
        return results
            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun errorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
            SpeechRecognizer.ERROR_CLIENT -> "Speech client error."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is missing."
            SpeechRecognizer.ERROR_NETWORK -> "Network speech recognizer error."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network speech recognizer timed out."
            SpeechRecognizer.ERROR_NO_MATCH -> "No clear speech heard."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy."
            SpeechRecognizer.ERROR_SERVER -> "Speech recognizer server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
            else -> "Speech recognizer error $error."
        }
    }

    private fun shouldRestart(error: Int): Boolean {
        return error == SpeechRecognizer.ERROR_NO_MATCH ||
            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
            error == SpeechRecognizer.ERROR_NETWORK_TIMEOUT ||
            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY
    }

    private fun listener(sessionId: Long) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            if (isSessionActive(sessionId)) {
                callbacks?.onReady?.invoke()
            }
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() = Unit

        override fun onError(error: Int) {
            if (!isSessionActive(sessionId)) return
            if (shouldRestart(error)) {
                if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    callbacks?.onPartial?.invoke("")
                }
                scheduleRestart(sessionId)
            } else {
                callbacks?.onError?.invoke("System speech service error: ${errorMessage(error)}")
            }
        }

        override fun onResults(results: Bundle?) {
            if (!isSessionActive(sessionId)) return
            commitBestResult(sessionId, results)
            scheduleRestart(sessionId)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (!isSessionActive(sessionId)) return
            bestText(partialResults)?.let { callbacks?.onPartial?.invoke(it) }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun isSessionActive(sessionId: Long): Boolean {
        return sessionId == sessionGeneration && !stopped && !paused
    }

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS = 750L
    }
}
