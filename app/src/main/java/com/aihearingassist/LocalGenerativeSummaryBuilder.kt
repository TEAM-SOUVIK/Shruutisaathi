package com.aihearingassist

import android.content.Context
import android.app.ActivityManager
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SummaryResult(
    val text: String,
    val methodLabel: String,
    val detail: String,
    val usedGenerativeModel: Boolean
)

data class ModelDownloadStatus(
    val state: ModelDownloadState,
    val detail: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null
) {
    val progressFraction: Float?
        get() {
            val total = totalBytes ?: return null
            if (total <= 0L) return null
            return (downloadedBytes.toDouble() / total.toDouble()).coerceIn(0.0, 1.0).toFloat()
        }
}

enum class ModelDownloadState {
    NOT_INSTALLED,
    DOWNLOADING,
    INSTALLED,
    NEEDS_SMALLER_MODEL,
    FAILED
}

class LocalGenerativeSummaryBuilder(
    context: Context,
    private val fallback: OfflineSummaryBuilder
) {
    private val appContext = context.applicationContext
    private val modelFile: File
        get() = File(modelDirectory(), COMPACT_MODEL_FILE_NAME)
    private val engineLock = Mutex()
    private val inferenceDispatcher = Executors.newSingleThreadExecutor { task ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "ShrutiSaathi-summary")
    }.asCoroutineDispatcher()
    private val cleanupScope = CoroutineScope(SupervisorJob() + inferenceDispatcher)
    private val generationEpoch = AtomicLong()
    @Volatile private var closed = false
    @Volatile
    private var bridge: LiteRtLmSummaryBridge? = null
    private var lastSummaryKey: String? = null
    private var lastSummaryResult: SummaryResult? = null
    private var retryAfterNanos = 0L

    private fun memoryInfo(): ActivityManager.MemoryInfo = ActivityManager.MemoryInfo().also {
        appContext.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
    }

    private fun memoryAllows(file: File): Boolean {
        val info = memoryInfo()
        return SummaryMemoryPolicy.canLoad(file.length(), info.availMem, info.lowMemory, info.threshold)
    }

    fun modelDownloadStatus(): ModelDownloadStatus {
        val selected = modelFile
        if (selected.isFile && bridge == null && !memoryAllows(selected)) {
            return ModelDownloadStatus(ModelDownloadState.NEEDS_SMALLER_MODEL,
                "Not enough free memory to start Private AI. Close other apps, then reopen ShrutiSaathi.")
        }
        return if (modelFile.isFile) {
            ModelDownloadStatus(
                state = ModelDownloadState.INSTALLED,
                detail = "Private AI Pack is ready.",
                downloadedBytes = modelFile.length(),
                totalBytes = modelFile.length()
            )
        } else {
            val partial = File(modelDirectory(), "${selected.name}.download")
            if (partial.isFile && partial.length() > 0L) {
                ModelDownloadStatus(
                    state = ModelDownloadState.NOT_INSTALLED,
                    detail = "AI Pack download is ready to resume.",
                    downloadedBytes = partial.length()
                )
            } else {
                ModelDownloadStatus(
                    state = ModelDownloadState.NOT_INSTALLED,
                    detail = "Install the Private AI Pack to enable conversation gist and reply drafting."
                )
            }
        }
    }

    fun initialSummary(turns: Iterable<ConversationManager.Turn>): SummaryResult {
        return SummaryResult(
            text = fallback.buildSummary(turns),
            methodLabel = fallbackMethodLabel(),
            detail = if (turns.any()) "Quick preview. Private AI is preparing the conversation gist."
                else "The gist will appear as the conversation begins.",
            usedGenerativeModel = false
        )
    }

    suspend fun buildSummary(
        turns: Iterable<ConversationManager.Turn>,
        onPartial: (SummaryResult) -> Unit = {}
    ): SummaryResult {
        val requestEpoch = generationEpoch.get()
        return withContext(inferenceDispatcher) {
            engineLock.withLock {
                check(!closed) { "Summary engine is closed" }
                if (requestEpoch != generationEpoch.get()) {
                    initialSummary(turns).copy(detail = "Summary cancelled.")
                } else {
                    buildSummaryLocked(turns, onPartial, requestEpoch)
                }
            }
        }
    }

    private fun buildSummaryLocked(
        turns: Iterable<ConversationManager.Turn>,
        onPartial: (SummaryResult) -> Unit,
        requestEpoch: Long
    ): SummaryResult {
        val snapshot = turns.toList()
        val fallbackText = fallback.buildSummary(snapshot)
        if (snapshot.isEmpty()) {
            return SummaryResult(fallbackText, fallbackMethodLabel(), fallbackDetail(), false)
        }

        val transcript = compactTranscriptForModel(snapshot)
        val summaryKey = "$requestEpoch:$transcript"
        if (summaryKey == lastSummaryKey) {
            lastSummaryResult?.takeIf { it.usedGenerativeModel }?.let { return it }
        }

        if (System.nanoTime() < retryAfterNanos) {
            return SummaryResult(fallbackText, fallbackMethodLabel(),
                "Private AI is recovering. Quick preview shown; retrying after a short cooldown.", false)
        }
        val installResult = ensureModelAvailable()
        val file = installResult.file
        if (!file.isFile) {
            return SummaryResult(
                text = fallbackText,
                methodLabel = fallbackMethodLabel(),
                detail = installResult.detail,
                usedGenerativeModel = false
            ).also {
                clearSummaryCache()
            }
        }

        val started = System.nanoTime()
        return runCatching {
            if (bridge == null && !memoryAllows(file)) {
                return SummaryResult(fallbackText, fallbackMethodLabel(),
                    "Not enough free memory for Private AI. Close other apps and try again.", false)
            }
            val activeBridge = bridge ?: createBridge(file).also { bridge = it }
            if (requestEpoch != generationEpoch.get() || closed) throw CancellationException()
            val generated = activeBridge.summarize(LocalTextTasks.GIST_INSTRUCTION, promptFor(transcript)) { raw ->
                val partial = cleanGeneratedSummary(raw)
                if (partial.isNotBlank() &&
                    LocalTextTasks.isGroundedGist(partial, transcript) &&
                    requestEpoch == generationEpoch.get() && !closed) {
                    onPartial(SummaryResult(partial, GENERATIVE_METHOD_LABEL,
                        "Writing gist privately. Live captions continue.", true))
                }
            }
            val cleaned = cleanGeneratedSummary(generated)
            if (cleaned.isBlank()) {
                SummaryResult(
                    text = fallbackText,
                    methodLabel = fallbackMethodLabel(),
                    detail = "Private AI returned an empty gist; quick preview used.",
                    usedGenerativeModel = false
                )
            } else if (!LocalTextTasks.isGroundedGist(cleaned, transcript)) {
                SummaryResult(
                    text = fallbackText,
                    methodLabel = fallbackMethodLabel(),
                    detail = "Private AI gist was not grounded in the transcript; quick preview used.",
                    usedGenerativeModel = false
                )
            } else {
                SummaryResult(
                    text = cleaned,
                    methodLabel = GENERATIVE_METHOD_LABEL,
                    detail = "Generated privately in ${String.format(Locale.US, "%.1f", (System.nanoTime() - started) / 1_000_000_000.0)} s · recent conversation.",
                    usedGenerativeModel = true
                )
            }
        }.getOrElse { error ->
            if (error is CancellationException) {
                // Reset/timeout cancels only this request; keep the loaded engine for reuse.
                return SummaryResult(fallbackText, fallbackMethodLabel(), "Summary cancelled.", false)
            }
            closeEngine()
            retryAfterNanos = System.nanoTime() + 30_000_000_000L
            SummaryResult(
                text = fallbackText,
                methodLabel = fallbackMethodLabel(),
                detail = "Private AI summary failed: ${error.message ?: error.javaClass.simpleName}; quick preview used.",
                usedGenerativeModel = false
            )
        }.also {
            if (it.usedGenerativeModel && requestEpoch == generationEpoch.get()) {
                lastSummaryKey = summaryKey
                lastSummaryResult = it
            } else {
                clearSummaryCache()
            }
        }
    }

    suspend fun downloadModel(
        onProgress: suspend (ModelDownloadStatus) -> Unit = {}
    ): ModelDownloadStatus = modelDownloadLock.withLock {
        if (modelFile.isFile) return@withLock modelDownloadStatus().also { onProgress(it) }
        downloadModelFromNetwork(modelFile, COMPACT_MODEL_DOWNLOAD_URL, onProgress)
    }

    suspend fun generateReply(context: ReplyContext, onPartial: (String) -> Unit = {}): SummaryResult {
        val requestEpoch = generationEpoch.get()
        return withContext(inferenceDispatcher) {
            engineLock.withLock {
                val started = System.nanoTime()
                try {
                    check(!closed && requestEpoch == generationEpoch.get()) { "Reply cancelled" }
                    check(modelFile.isFile) { "Install the Private AI Pack from setup first" }
                    check(bridge != null || memoryAllows(modelFile)) { "Not enough free memory; close other apps" }
                    val active = bridge ?: createBridge(modelFile).also { bridge = it }
                    val text = active.summarize(LocalTextTasks.REPLY_INSTRUCTION,
                        LocalTextTasks.REPLY_INSTRUCTION + "\n" + LocalTextTasks.replyPrompt(context)) { raw ->
                        if (requestEpoch == generationEpoch.get()) onPartial(LocalTextTasks.clean(raw))
                    }
                    check(requestEpoch == generationEpoch.get()) { "Reply cancelled" }
                    val cleaned = LocalTextTasks.clean(text)
                    check(cleaned.isNotBlank()) { "Model returned an empty reply; please retry" }
                    SummaryResult(cleaned, GENERATIVE_METHOD_LABEL,
                        "Private reply draft · ${String.format(Locale.US, "%.1f", (System.nanoTime() - started) / 1e9)} s. Review before using.", true)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    SummaryResult("", "Reply unavailable", "Could not generate reply: ${error.message}. Your intended words are unchanged.", false)
                } catch (error: LinkageError) {
                    closeEngine()
                    SummaryResult("", "Reply unavailable", "This device could not load the local engine.", false)
                }
            }
        }
    }

    suspend fun prepareModel() = withContext(inferenceDispatcher) {
        engineLock.withLock {
            if (!closed && modelFile.isFile && memoryAllows(modelFile) && System.nanoTime() >= retryAfterNanos) {
                try {
                    (bridge ?: createBridge(modelFile).also { bridge = it }).prepare()
                } catch (error: Exception) {
                    closeEngine()
                    retryAfterNanos = System.nanoTime() + 30_000_000_000L
                } catch (error: LinkageError) {
                    closeEngine()
                    retryAfterNanos = System.nanoTime() + 30_000_000_000L
                }
            }
        }
    }

    fun cancelActive() {
        generationEpoch.incrementAndGet()
        runCatching { bridge?.cancel() }
    }

    fun releaseMemory() {
        cancelActive()
        cleanupScope.launch { engineLock.withLock { closeEngine() } }
    }

    suspend fun importCompactModel(uri: Uri): ModelDownloadStatus = withContext(inferenceDispatcher) {
        engineLock.withLock {
            closeEngine()
            val directory = modelDirectory().apply { mkdirs() }
            val temporary = File(directory, "$COMPACT_MODEL_FILE_NAME.import")
            try {
                appContext.contentResolver.openInputStream(uri).use { input ->
                    checkNotNull(input) { "Cannot open selected model" }
                    FileOutputStream(temporary).use { output ->
                        val buffer = ByteArray(256 * 1024)
                        var size = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            size += count
                            check(size <= EXPECTED_MODEL_BYTES) { "Select the correct 347 MB AI Pack file." }
                            output.write(buffer, 0, count)
                        }
                        check(size >= MIN_VALID_MODEL_BYTES) { "Selected file is too small to be the model." }
                        output.fd.sync()
                    }
                }
                verifyDownload(temporary)
                check(temporary.renameTo(File(directory, COMPACT_MODEL_FILE_NAME))) { "Could not install model; check storage." }
                retryAfterNanos = 0L
                modelDownloadStatus()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                ModelDownloadStatus(ModelDownloadState.NEEDS_SMALLER_MODEL,
                    "AI Pack import failed: ${error.message}. Select the verified 347 MB AI Pack file.")
            } finally {
                temporary.delete()
            }
        }
    }

    fun close() {
        if (closed) return
        closed = true
        cancelActive()
        // Native teardown must never block Activity.onDestroy or race an inference.
        cleanupScope.launch {
            try { engineLock.withLock { closeEngine() } }
            finally {
                inferenceDispatcher.close()
                cleanupScope.cancel()
            }
        }
    }

    private fun createBridge(file: File): LiteRtLmSummaryBridge {
        return LiteRtLmSummaryBridge(
            file.absolutePath,
            File(appContext.cacheDir, "litertlm").absolutePath,
            MAX_CONTEXT_TOKENS,
            MAX_OUTPUT_TOKENS
        )
    }

    private fun promptFor(transcript: String): String {
        // Static-template LiteRT exports may not serialize a system role; include the
        // task in the user message too, so the instruction is never silently dropped.
        return LocalTextTasks.gistPrompt(transcript.takeLast(MAX_TRANSCRIPT_CHARS))
    }

    private fun compactTranscriptForModel(snapshot: List<ConversationManager.Turn>): String = LocalTextTasks.transcript(snapshot)
    private fun cleanGeneratedSummary(raw: String): String = LocalTextTasks.cleanGist(raw)

    private fun fallbackMethodLabel(): String = OfflineSummaryBuilder.METHOD_LABEL

    private fun fallbackDetail(): String = "Quick preview is active until the Private AI Pack is installed."

    private fun ensureModelAvailable(): ModelInstallResult {
        val destination = modelFile
        if (destination.isFile) {
            return ModelInstallResult(
                file = destination,
                source = ModelSource.APP_STORAGE,
                detail = "Private AI Pack is ready."
            )
        }

        return ModelInstallResult(
            file = destination,
            source = ModelSource.MISSING,
            detail = "Install the Private AI Pack on the first page before starting."
        )
    }

    private suspend fun downloadModelFromNetwork(
        destination: File,
        downloadUrl: String,
        onProgress: suspend (ModelDownloadStatus) -> Unit
    ): ModelDownloadStatus {
        return runCatching {
            modelDirectory().mkdirs()
            val tmp = File(modelDirectory(), "${destination.name}.download")

            val existingBytes = if (tmp.isFile) tmp.length() else 0L
            val connection = openDownloadConnection(downloadUrl, existingBytes)

            try {
                val code = connection.responseCode
                if (code == 401 || code == 403) {
                    return ModelDownloadStatus(ModelDownloadState.FAILED,
                        "Secure AI Pack download was refused (HTTP $code). Retry later.")
                }
                if (code == 416) {
                    val completeSize = connection.getHeaderField("Content-Range")
                        ?.substringAfterLast('/')?.toLongOrNull()
                    if (completeSize != null && completeSize >= MIN_VALID_MODEL_BYTES && existingBytes == completeSize) {
                        // A previous download finished, but the process stopped before promotion.
                        verifyDownload(tmp)
                        check(tmp.renameTo(destination)) { "Could not install the completed download; check storage." }
                        return modelDownloadStatus()
                    }
                }
                val append = code == HttpURLConnection.HTTP_PARTIAL && existingBytes > 0L
                if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                    throw IllegalStateException("HTTP $code")
                }
                if (code == HttpURLConnection.HTTP_PARTIAL) {
                    val start = connection.getHeaderField("Content-Range")
                        ?.substringAfter("bytes ")?.substringBefore('-')?.toLongOrNull()
                    check(start == if (append) existingBytes else 0L) { "AI Pack resume failed. Tap Install AI Pack again." }
                }
                check(connection.contentType?.contains("text/html", ignoreCase = true) != true) {
                    "AI Pack source returned an unexpected response"
                }

                val remoteBytes = connection.contentLengthLong.takeIf { it > 0L } ?: -1L
                val rangeTotal = if (code == HttpURLConnection.HTTP_PARTIAL) {
                    connection.getHeaderField("Content-Range")?.substringAfterLast('/')?.toLongOrNull()
                } else null
                val expectedTotal = rangeTotal ?: if (append && remoteBytes > 0L) existingBytes + remoteBytes else remoteBytes
                check(expectedTotal <= 0L || expectedTotal == EXPECTED_MODEL_BYTES) {
                    "AI Pack size did not match the verified release."
                }
                onProgress(
                    ModelDownloadStatus(
                        state = ModelDownloadState.DOWNLOADING,
                        detail = if (append) {
                            "Resuming AI Pack download..."
                        } else {
                            "Downloading AI Pack..."
                        },
                        downloadedBytes = if (append) existingBytes else 0L,
                        totalBytes = expectedTotal.takeIf { it > 0L }
                    )
                )
                if (expectedTotal > 0L &&
                    modelDirectory().usableSpace < expectedTotal - (if (append) existingBytes else 0L) + MIN_FREE_SPACE_AFTER_DOWNLOAD_BYTES
                ) {
                    throw IllegalStateException("not enough free storage for the ${expectedTotal / BYTES_PER_MB} MB AI Pack")
                }

                if (!append && tmp.exists()) {
                    tmp.delete()
                }

                connection.inputStream.use { input ->
                    FileOutputStream(tmp, append).use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
                        var downloaded = if (append) existingBytes else 0L
                        var lastProgressNanos = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            currentCoroutineContext().ensureActive()
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            downloaded += read
                            check(downloaded <= EXPECTED_MODEL_BYTES) { "AI Pack download exceeded the verified size." }
                            val now = System.nanoTime()
                            if (now - lastProgressNanos >= 250_000_000L || downloaded == expectedTotal) {
                                lastProgressNanos = now
                                onProgress(
                                    ModelDownloadStatus(
                                        state = ModelDownloadState.DOWNLOADING,
                                        detail = if (append) "Resuming AI Pack download..." else "Downloading AI Pack...",
                                        downloadedBytes = downloaded,
                                        totalBytes = expectedTotal.takeIf { it > 0L }
                                    )
                                )
                            }
                        }
                        output.fd.sync()
                    }
                }

                if (tmp.length() < MIN_VALID_MODEL_BYTES) {
                    throw IllegalStateException("AI Pack download was incomplete")
                }
                check(expectedTotal <= 0L || tmp.length() == expectedTotal) {
                    "Incomplete AI Pack download. Tap Install AI Pack to resume."
                }

                onProgress(ModelDownloadStatus(ModelDownloadState.DOWNLOADING, "Verifying AI Pack…", tmp.length(), tmp.length()))
                verifyDownload(tmp)
                check(tmp.renameTo(destination)) { "Could not install AI Pack. Download was preserved; check storage and retry." }

                modelDownloadStatus()
            } finally {
                connection.disconnect()
            }
        }.getOrElse { error ->
            if (error is CancellationException) throw error
            ModelDownloadStatus(
                state = ModelDownloadState.FAILED,
                detail = "AI Pack download failed: ${error.message ?: error.javaClass.simpleName}. Check internet/storage and retry."
            )
        }
    }

    private fun verifyDownload(file: File) {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(256 * 1024).use { input ->
            val buffer = ByteArray(256 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (file.length() != EXPECTED_MODEL_BYTES || actual != MODEL_SHA256) {
            file.delete() // Only the untrusted temporary download/import, never an installed model.
            error("AI Pack verification failed. Retry to install a clean copy.")
        }
    }

    private fun openDownloadConnection(url: String, existingBytes: Long): HttpURLConnection {
        var next = URL(url)
        repeat(8) {
            check(next.protocol == "https") { "AI Pack setup requires a secure connection" }
            val connection = (next.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = DOWNLOAD_CONNECT_TIMEOUT_MS
                readTimeout = DOWNLOAD_READ_TIMEOUT_MS
                setRequestProperty("User-Agent", "ShrutiSaathi/1.0")
                setRequestProperty("Accept-Encoding", "identity")
                if (existingBytes > 0L) setRequestProperty("Range", "bytes=$existingBytes-")
            }
            try {
                if (connection.responseCode !in listOf(301, 302, 303, 307, 308)) return connection
                val location = checkNotNull(connection.getHeaderField("Location")) { "AI Pack source did not provide the next download step" }
                next = URL(next, location)
            } catch (error: Exception) {
                connection.disconnect()
                throw error
            }
            connection.disconnect()
        }
        error("AI Pack source redirected too many times")
    }

    private fun modelDirectory(): File {
        val external = appContext.getExternalFilesDir(null)
        return File(external ?: appContext.filesDir, MODEL_DIR_NAME)
    }

    private fun closeEngine() {
        val previous = bridge
        bridge = null
        clearSummaryCache()
        runCatching { previous?.close() }
    }

    private fun clearSummaryCache() {
        lastSummaryKey = null
        lastSummaryResult = null
    }

    private data class ModelInstallResult(
        val file: File,
        val source: ModelSource,
        val detail: String
    ) {
        fun detailForSuccess(): String {
            return when (source) {
                ModelSource.BUNDLED_ASSET -> "Private AI Pack installed and ready."
                ModelSource.APP_STORAGE -> "Generated privately with the installed AI Pack."
                ModelSource.DOWNLOADED -> "AI Pack installed and ready."
                ModelSource.MISSING -> detail
            }
        }
    }

    private enum class ModelSource {
        BUNDLED_ASSET,
        APP_STORAGE,
        DOWNLOADED,
        MISSING
    }

    companion object {
        // All Activity instances share one engine owner. Recreation cannot load two multi-GB engines.
        @Volatile private var shared: LocalGenerativeSummaryBuilder? = null
        fun shared(context: Context, fallback: OfflineSummaryBuilder): LocalGenerativeSummaryBuilder =
            shared ?: synchronized(this) {
                shared ?: LocalGenerativeSummaryBuilder(context, fallback).also { shared = it }
            }
        const val COMPACT_MODEL_FILE_NAME = "shrutisaathi-qwen3-06b-int4.litertlm"
        const val COMPACT_MODEL_PAGE = "https://huggingface.co/litert-community/Qwen3-0.6B-int4"
        private const val COMPACT_MODEL_DOWNLOAD_URL =
            "$COMPACT_MODEL_PAGE/resolve/6aa2daf8aba4aa456797fb8040b36a3948bcfda7/qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm?download=true"
        private val modelDownloadLock = Mutex()
        const val MODEL_FILE_NAME = "shrutisaathi-summary.litertlm"
        const val MODEL_DIR_NAME = "models"
        const val GENERATIVE_METHOD_LABEL = "Private AI"
        private const val EXPECTED_MODEL_BYTES = 347251840L
        private const val MODEL_SHA256 = "2df6821ec12702dafd33915e7a1a1adc7c4b053f3672fd9555dfaf3a114c4139"
        private const val MAX_CONTEXT_TOKENS = 768
        private const val MAX_OUTPUT_TOKENS = 64
        private const val MAX_TRANSCRIPT_CHARS = LocalTextTasks.MAX_TRANSCRIPT_BYTES
        private const val DOWNLOAD_CONNECT_TIMEOUT_MS = 30_000
        private const val DOWNLOAD_READ_TIMEOUT_MS = 60_000
        private const val DOWNLOAD_BUFFER_BYTES = 4 * 1024 * 1024
        private const val BYTES_PER_MB = 1024L * 1024L
        private const val MIN_VALID_MODEL_BYTES = 100L * BYTES_PER_MB
        private const val MIN_FREE_SPACE_AFTER_DOWNLOAD_BYTES = 512L * BYTES_PER_MB
    }
}
