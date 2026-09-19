package com.aihearingassist

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class TextToSpeechHelper(context: Context) : TextToSpeech.OnInitListener {
    private data class PlaybackCallbacks(
        val onStarted: () -> Unit,
        val onCompleted: (Boolean) -> Unit
    )

    private val tts = TextToSpeech(context.applicationContext, this)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val utteranceCounter = AtomicLong(0L)
    private val playbackCallbacks = ConcurrentHashMap<String, PlaybackCallbacks>()
    private var ready = false

    init {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    val callbacks = utteranceId?.let(playbackCallbacks::get) ?: return
                    mainHandler.post { callbacks.onStarted() }
                }

                override fun onDone(utteranceId: String?) {
                    complete(utteranceId, successful = true)
                }

                @Deprecated("Deprecated by Android; required for older engines")
                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?) {
                    complete(utteranceId, successful = false)
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    complete(utteranceId, successful = false)
                }

                @Suppress("OVERRIDE_DEPRECATION")
                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    complete(utteranceId, successful = false)
                }
            }
        )
    }

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    fun speak(
        text: String,
        language: SpeechLanguage = SpeechLanguage.ENGLISH_US,
        onStarted: () -> Unit = {},
        onCompleted: (Boolean) -> Unit = {}
    ): Boolean {
        if (!ready) return false
        val locale = when (language) {
            SpeechLanguage.ENGLISH_US -> Locale.US
            SpeechLanguage.ENGLISH_IN -> Locale("en", "IN")
            SpeechLanguage.ENGLISH_UK -> Locale.UK
            SpeechLanguage.ENGLISH_AU -> Locale("en", "AU")
            SpeechLanguage.HINGLISH,
            SpeechLanguage.HINDI,
            SpeechLanguage.BHOJPURI -> Locale("hi", "IN")
        }

        val offlineVoice = tts.voices
            .orEmpty()
            .asSequence()
            .filter { voice ->
                !voice.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in voice.features.orEmpty() &&
                    voice.locale.language.equals(locale.language, ignoreCase = true)
            }
            .sortedWith(
                compareByDescending<Voice> { if (it.locale == locale) 1 else 0 }
                    .thenByDescending { it.quality }
                    .thenBy { it.latency }
            )
            .firstOrNull()
            ?: return false

        if (tts.setVoice(offlineVoice) == TextToSpeech.ERROR) return false
        val utteranceId = "ShrutiSaathiReply-${utteranceCounter.incrementAndGet()}"
        playbackCallbacks[utteranceId] = PlaybackCallbacks(onStarted, onCompleted)
        val queued = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) == TextToSpeech.SUCCESS
        if (!queued) playbackCallbacks.remove(utteranceId)
        return queued
    }

    fun stop() {
        playbackCallbacks.clear()
        tts.stop()
    }

    fun shutdown() {
        stop()
        tts.shutdown()
    }

    private fun complete(utteranceId: String?, successful: Boolean) {
        val callbacks = utteranceId?.let(playbackCallbacks::remove) ?: return
        mainHandler.post { callbacks.onCompleted(successful) }
    }
}
