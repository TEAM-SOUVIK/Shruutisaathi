package com.aihearingassist

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import org.json.JSONObject
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.SpeakerModel
import org.vosk.android.RecognitionListener
import org.vosk.android.StorageService
import java.io.IOException

enum class SpeechLanguage(
    val label: String,
    val shortLabel: String,
    val recognitionTag: String,
    val assetFolder: String,
    val storageFolder: String,
    val modelNote: String,
    val hinglishOutput: Boolean = false,
    val sampleRateHz: Float = 16_000f
) {
    ENGLISH_IN(
        label = "English (India)",
        shortLabel = "India",
        recognitionTag = "en-IN",
        assetFolder = "model-en-in",
        storageFolder = "model-en-in",
        modelNote = "Indian English offline model",
        sampleRateHz = 8_000f
    ),
    HINGLISH(
        label = "Hinglish",
        shortLabel = "Hinglish",
        recognitionTag = "hi-IN",
        assetFolder = "model-hi",
        storageFolder = "model-hi",
        modelNote = "Catches Hindi speech and converts it to English letters",
        hinglishOutput = true
    ),
    HINDI(
        label = "Hindi",
        shortLabel = "Hindi",
        recognitionTag = "hi-IN",
        assetFolder = "model-hi",
        storageFolder = "model-hi",
        modelNote = "Hindi speech in Devanagari text"
    ),
    BHOJPURI(
        label = "Bhojpuri",
        shortLabel = "Bhojpuri",
        recognitionTag = "hi-IN",
        assetFolder = "model-hi",
        storageFolder = "model-hi",
        modelNote = "Uses Hindi recognition as the closest lightweight option"
    ),
    ENGLISH_US(
        label = "English (US)",
        shortLabel = "US",
        recognitionTag = "en-US",
        assetFolder = "model-en-us",
        storageFolder = "model-en-us",
        modelNote = "US English offline model"
    ),
    ENGLISH_UK(
        label = "English (UK)",
        shortLabel = "UK",
        recognitionTag = "en-GB",
        assetFolder = "",
        storageFolder = "",
        modelNote = "Available through the device system speech service"
    ),
    ENGLISH_AU(
        label = "English (Australia)",
        shortLabel = "AU",
        recognitionTag = "en-AU",
        assetFolder = "",
        storageFolder = "",
        modelNote = "Available through the device system speech service"
    );

    val isBundled: Boolean
        get() = assetFolder.isNotBlank() && assetFolder in bundledAssetFolders

    companion object {
        private val bundledAssetFolders: Set<String> by lazy {
            BuildConfig.BUNDLED_SPEECH_MODELS
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .toSet()
        }

        fun availableEntries(engine: SpeechEngine): List<SpeechLanguage> {
            if (engine == SpeechEngine.SYSTEM_SERVICE) return entries
            return offlineEntries()
        }

        fun offlineEntries(): List<SpeechLanguage> {
            return entries.filter { it.isBundled }.ifEmpty { listOf(ENGLISH_US) }
        }

        fun defaultLanguage(engine: SpeechEngine): SpeechLanguage {
            if (engine == SpeechEngine.SYSTEM_SERVICE) return ENGLISH_IN
            return listOf(ENGLISH_IN, ENGLISH_US, HINGLISH, HINDI, BHOJPURI)
                .firstOrNull { it.isBundled }
                ?: offlineEntries().first()
        }
    }
}

enum class SpeechEngine(
    val label: String,
    val shortLabel: String,
    val detail: String
) {
    SYSTEM_SERVICE(
        label = "Enhanced",
        shortLabel = "Enhanced",
        detail = "Uses your phone's enhanced speech service when available"
    ),
    OFFLINE_VOSK(
        label = "Private",
        shortLabel = "Private",
        detail = "Keeps live caption capture on this device"
    );

    companion object {
        val DEFAULT: SpeechEngine = OFFLINE_VOSK
    }
}

class OfflineSpeechRecognizer {
    private var model: Model? = null
    private var modelAssetFolder: String? = null
    private var speakerModel: SpeakerModel? = null
    private var speechService: BufferedVoskCapture? = null
    var onAudioLevel: (Float) -> Unit = {}
    private var isLoading = false
    private var isLoadingSpeakerModel = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingStart: PendingStart? = null
    private var lastCommittedText = ""
    private var lastCommittedAtMillis = 0L
    @Volatile private var sessionGeneration = 0L
    @Volatile private var isPaused = false

    private data class PendingStart(
        val generation: Long,
        val context: Context,
        val language: SpeechLanguage,
        val enableSpeakerEmbeddings: Boolean,
        val onReady: () -> Unit,
        val onPartial: (String) -> Unit,
        val onResult: (SpeechRecognitionResult) -> Unit,
        val onError: (String) -> Unit
    )

    fun start(
        context: Context,
        language: SpeechLanguage,
        enableSpeakerEmbeddings: Boolean = false,
        onReady: () -> Unit,
        onPartial: (String) -> Unit,
        onResult: (SpeechRecognitionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!language.isBundled) {
            onError("${language.label} is available only in Enhanced mode on this device.")
            return
        }

        val loadedModel = model
        if (loadedModel == null || modelAssetFolder != language.assetFolder) {
            stop()
            pendingStart = PendingStart(
                generation = sessionGeneration,
                context = context.applicationContext,
                language = language,
                enableSpeakerEmbeddings = enableSpeakerEmbeddings,
                onReady = onReady,
                onPartial = onPartial,
                onResult = onResult,
                onError = onError
            )
            loadPendingModel()
            return
        }

        if (enableSpeakerEmbeddings && speakerModel == null) {
            stop()
            pendingStart = PendingStart(
                generation = sessionGeneration,
                context = context.applicationContext,
                language = language,
                enableSpeakerEmbeddings = true,
                onReady = onReady,
                onPartial = onPartial,
                onResult = onResult,
                onError = onError
            )
            loadPendingSpeakerModel()
            return
        }

        startListening(context.applicationContext, language, enableSpeakerEmbeddings, onReady, onPartial, onResult, onError)
    }

    fun pause() {
        isPaused = true
        speechService?.setPause(true)
    }

    fun resume() {
        isPaused = false
        speechService?.setPause(false)
    }

    fun stop() {
        sessionGeneration += 1
        isPaused = false
        pendingStart = null
        mainHandler.removeCallbacksAndMessages(null)
        stopSpeechService()
    }

    private fun stopSpeechService() {
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
    }

    fun shutdown() {
        stop()
        model?.close()
        model = null
        modelAssetFolder = null
        speakerModel?.close()
        speakerModel = null
    }

    private fun loadPendingModel() {
        if (isLoading) {
            return
        }

        val start = pendingStart ?: return
        val language = start.language
        isLoading = true
        LibVosk.setLogLevel(LogLevel.INFO)
        StorageService.unpack(
            start.context,
            language.assetFolder,
            language.storageFolder,
            { loaded ->
                val pending = pendingStart
                isLoading = false
                if (pending == null) {
                    loaded.close()
                    return@unpack
                }
                if (pending.generation != sessionGeneration) {
                    loaded.close()
                    return@unpack
                }
                if (pending.language.assetFolder != language.assetFolder) {
                    loaded.close()
                    loadPendingModel()
                    return@unpack
                }

                model?.close()
                model = loaded
                modelAssetFolder = language.assetFolder
                if (pending.enableSpeakerEmbeddings && speakerModel == null) {
                    loadPendingSpeakerModel()
                } else {
                    pendingStart = null
                    post {
                        if (pending.generation == sessionGeneration) {
                            startListening(
                                pending.context,
                                pending.language,
                                pending.enableSpeakerEmbeddings,
                                pending.onReady,
                                pending.onPartial,
                                pending.onResult,
                                pending.onError
                            )
                        }
                    }
                }
            },
            { exception ->
                val pending = pendingStart
                isLoading = false
                if (pending == null) {
                    return@unpack
                }
                if (pending.generation != sessionGeneration) {
                    return@unpack
                }
                if (pending.language.assetFolder != language.assetFolder) {
                    loadPendingModel()
                    return@unpack
                }

                pendingStart = null
                post {
                    if (pending.generation == sessionGeneration) {
                        pending.onError("${language.label} speech model failed to load: ${exception.message}")
                    }
                }
            }
        )
    }

    private fun loadPendingSpeakerModel() {
        if (isLoadingSpeakerModel) return
        val start = pendingStart ?: return
        isLoadingSpeakerModel = true
        Thread({
            try {
                val modelPath = StorageService.sync(
                    start.context,
                    SPEAKER_MODEL_ASSET_FOLDER,
                    SPEAKER_MODEL_STORAGE_FOLDER
                )
                val loadedSpeakerModel = SpeakerModel(modelPath)
                val pending = pendingStart
                isLoadingSpeakerModel = false
                if (pending == null || pending.generation != sessionGeneration) {
                    loadedSpeakerModel.close()
                    return@Thread
                }
                speakerModel?.close()
                speakerModel = loadedSpeakerModel
                pendingStart = null
                postForGeneration(pending.generation) {
                    startListening(
                        pending.context,
                        pending.language,
                        pending.enableSpeakerEmbeddings,
                        pending.onReady,
                        pending.onPartial,
                        pending.onResult,
                        pending.onError
                    )
                }
            } catch (exception: IOException) {
                finishSpeakerModelFailure(start, exception.message)
            } catch (exception: RuntimeException) {
                finishSpeakerModelFailure(start, exception.message)
            }
        }, "ShrutiSaathi-speaker-model").start()
    }

    private fun finishSpeakerModelFailure(start: PendingStart, detail: String?) {
        isLoadingSpeakerModel = false
        val pending = pendingStart
        if (pending == null || pending.generation != sessionGeneration) return
        pendingStart = null
        postForGeneration(start.generation) {
            start.onError(
                "Participant labeling could not start: ${detail ?: "setup failed"}. " +
                    "Restart the app or reinstall ShrutiSaathi."
            )
        }
    }

    private fun startListening(
        context: Context,
        language: SpeechLanguage,
        enableSpeakerEmbeddings: Boolean,
        onReady: () -> Unit,
        onPartial: (String) -> Unit,
        onResult: (SpeechRecognitionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val loadedModel = model ?: run {
            onError("Private capture is not ready yet.")
            return
        }

        stop()
        val sessionId = sessionGeneration
        isPaused = false
        lastCommittedText = ""
        lastCommittedAtMillis = 0L
        try {
            val recognizer = if (enableSpeakerEmbeddings) {
                val loadedSpeakerModel = speakerModel ?: run {
                    onError("Participant labeling is not ready yet.")
                    return
                }
                Recognizer(loadedModel, language.sampleRateHz, loadedSpeakerModel)
            } else {
                Recognizer(loadedModel, language.sampleRateHz)
            }
            recognizer.setWords(true)
            speechService = BufferedVoskCapture(context, recognizer, language.sampleRateHz) { level ->
                if (isSessionActive(sessionId)) onAudioLevel(level)
            }
            speechService?.startListening(
                object : RecognitionListener {
                    override fun onPartialResult(hypothesis: String?) {
                        if (!isSessionActive(sessionId)) return
                        extractText(hypothesis)?.let { partial ->
                            postForSession(sessionId) { onPartial(partial) }
                        }
                    }

                    override fun onResult(hypothesis: String?) {
                        commitResult(sessionId, hypothesis, onResult)
                    }

                    override fun onFinalResult(hypothesis: String?) {
                        commitResult(sessionId, hypothesis, onResult)
                    }

                    override fun onError(exception: Exception?) {
                        postForSession(sessionId) {
                            onError("Private capture error: ${exception?.message ?: "unknown error"}")
                        }
                    }

                    override fun onTimeout() {
                        postForSession(sessionId) { onError("Listening timed out. Press Start again.") }
                    }
                }
            )
            postForSession(sessionId, onReady)
        } catch (exception: IOException) {
            stopSpeechService()
            postForSession(sessionId) { onError("Could not start private capture: ${exception.message}") }
        } catch (exception: RuntimeException) {
            stopSpeechService()
            postForSession(sessionId) {
                onError("Could not start private capture: ${exception.message ?: "setup failed"}")
            }
        }
    }

    private fun commitResult(
        sessionId: Long,
        hypothesis: String?,
        onResult: (SpeechRecognitionResult) -> Unit
    ) {
        if (!isSessionActive(sessionId)) return
        val result = extractResult(hypothesis) ?: return
        val now = SystemClock.elapsedRealtime()
        val isImmediateDuplicate = result.text.equals(lastCommittedText, ignoreCase = true) &&
            now - lastCommittedAtMillis < DUPLICATE_WINDOW_MILLIS
        if (isImmediateDuplicate) return
        lastCommittedText = result.text
        lastCommittedAtMillis = now
        postForSession(sessionId) { onResult(result) }
    }

    private fun extractResult(json: String?): SpeechRecognitionResult? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val parsed = JSONObject(json)
            val text = parsed.optString("text").trim().takeIf { it.isNotBlank() } ?: return@runCatching null
            val embeddingJson = parsed.optJSONArray("spk")
            val embedding = embeddingJson?.let { values ->
                DoubleArray(values.length()) { index -> values.optDouble(index, Double.NaN) }
                    .takeIf { vector -> vector.isNotEmpty() && vector.all(Double::isFinite) }
            }
            SpeechRecognitionResult(
                text = text,
                speakerEmbedding = embedding,
                speakerFrames = parsed.optInt("spk_frames", 0),
                wordConfidence = parsed.optJSONArray("result")?.let { words ->
                    (0 until words.length()).mapNotNull { i ->
                        words.optJSONObject(i)?.optDouble("conf", Double.NaN)?.takeIf { it.isFinite() }
                    }.takeIf { it.isNotEmpty() }?.average()
                }
            )
        }.getOrNull()
    }

    private fun extractText(json: String?): String? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val parsed = JSONObject(json)
            val text = parsed.optString("text").ifBlank { parsed.optString("partial") }
            text.trim().takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun post(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun isSessionActive(sessionId: Long): Boolean {
        return sessionId == sessionGeneration && !isPaused
    }

    private fun postForSession(sessionId: Long, action: () -> Unit) {
        post {
            if (isSessionActive(sessionId)) {
                action()
            }
        }
    }

    private fun postForGeneration(generation: Long, action: () -> Unit) {
        post {
            if (generation == sessionGeneration) action()
        }
    }

    private companion object {
        const val DUPLICATE_WINDOW_MILLIS = 750L
        const val SPEAKER_MODEL_ASSET_FOLDER = "model-spk"
        const val SPEAKER_MODEL_STORAGE_FOLDER = "model-spk"
    }
}
