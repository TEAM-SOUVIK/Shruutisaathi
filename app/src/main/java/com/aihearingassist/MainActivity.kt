package com.aihearingassist

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.ActivityCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private val Cyan = Color(0xFF67E8F9)
private val CyanDeep = Color(0xFF0F7F8D)
private val Lavender = Color(0xFF9B8AFB)
private val Mint = Color(0xFF2ECDBB)
private val Warm = Color(0xFFF7B267)
private val Danger = Color(0xFFEF5A73)

private data class AppPalette(
    val isDark: Boolean,
    val background: Color,
    val backgroundGradient: List<Color>,
    val panel: Color,
    val panelSoft: Color,
    val panelSunken: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val field: Color,
    val visualGradient: List<Color>,
    val captionGradient: List<Color>
)

private val DarkPalette = AppPalette(
    isDark = true,
    background = Color(0xFF17162A),
    backgroundGradient = listOf(Color(0xFF121426), Color(0xFF201A3C), Color(0xFF2B2348), Color(0xFF17162A)),
    panel = Color(0xFF211E34),
    panelSoft = Color(0xFF2C2942),
    panelSunken = Color(0xFF151629),
    textPrimary = Color(0xFFF6F3FF),
    textSecondary = Color(0xFFD6D0E8),
    textMuted = Color(0xFFA9A1BD),
    border = Color(0xFFC4B5FD).copy(alpha = 0.16f),
    field = Color(0xFF18182C),
    visualGradient = listOf(Color(0xFF18182D), Color(0xFF312652), Color(0xFF211E38)),
    captionGradient = listOf(Color(0xFF19182C), Color(0xFF28223E))
)

private val LightPalette = AppPalette(
    isDark = false,
    background = Color(0xFFE1ECF3),
    backgroundGradient = listOf(Color(0xFFEDF6F8), Color(0xFFD8E9F1), Color(0xFFC9DDF0), Color(0xFFD8D4F0)),
    panel = Color(0xFFEAF3F7),
    panelSoft = Color(0xFFD7E8F0),
    panelSunken = Color(0xFFDCEAF3),
    textPrimary = Color(0xFF102033),
    textSecondary = Color(0xFF43576C),
    textMuted = Color(0xFF62768A),
    border = Color(0xFFAFC8D8),
    field = Color(0xFFE8F0F5),
    visualGradient = listOf(Color(0xFFD7ECF4), Color(0xFFE4EEF8), Color(0xFFDCD8F2)),
    captionGradient = listOf(Color(0xFFEAF3F7), Color(0xFFDCEAF2))
)

private val LocalAppPalette = staticCompositionLocalOf { DarkPalette }

class MainActivity : ComponentActivity() {
    private lateinit var offlineSpeechRecognizer: OfflineSpeechRecognizer
    private lateinit var systemSpeechRecognizer: SystemSpeechRecognizer
    private lateinit var ttsHelper: TextToSpeechHelper
    private lateinit var generativeSummaryBuilder: LocalGenerativeSummaryBuilder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val conversationManager = ConversationManager()
        val summaryBuilder = OfflineSummaryBuilder()
        generativeSummaryBuilder = LocalGenerativeSummaryBuilder.shared(this, summaryBuilder)
        val predictor = NextWordPredictor()
        offlineSpeechRecognizer = OfflineSpeechRecognizer()
        systemSpeechRecognizer = SystemSpeechRecognizer()
        val localConversationStore = LocalConversationStore(this)
        ttsHelper = TextToSpeechHelper(this)

        conversationManager.restoreFromTranscript(localConversationStore.loadTranscript())

        setContent {
            AIHearingAssistApp(
                conversationManager = conversationManager,
                generativeSummaryBuilder = generativeSummaryBuilder,
                predictor = predictor,
                offlineSpeechRecognizer = offlineSpeechRecognizer,
                systemSpeechRecognizer = systemSpeechRecognizer,
                localConversationStore = localConversationStore,
                ttsHelper = ttsHelper
            )
        }
    }

    override fun onDestroy() {
        offlineSpeechRecognizer.shutdown()
        systemSpeechRecognizer.shutdown()
        ttsHelper.shutdown()
        generativeSummaryBuilder.releaseMemory()
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW &&
            ::generativeSummaryBuilder.isInitialized) {
            generativeSummaryBuilder.releaseMemory()
        }
    }
}

@Composable
fun AIHearingAssistApp(
    conversationManager: ConversationManager,
    generativeSummaryBuilder: LocalGenerativeSummaryBuilder,
    predictor: NextWordPredictor,
    offlineSpeechRecognizer: OfflineSpeechRecognizer,
    systemSpeechRecognizer: SystemSpeechRecognizer,
    localConversationStore: LocalConversationStore,
    ttsHelper: TextToSpeechHelper
) {
    val context = LocalContext.current
    val entries = conversationManager.entries
    val systemDarkTheme = isSystemInDarkTheme()
    val turnLabeler = remember { TurnLabeler() }
    val speakerTurnTracker = remember { SpeakerTurnTracker() }
    val speakerDiarizer = remember { OnlineSpeakerDiarizer() }
    val selfSpeechEchoGuard = remember { SelfSpeechEchoGuard() }
    val appScope = rememberCoroutineScope()
    var useDarkTheme by rememberSaveable { mutableStateOf(systemDarkTheme) }
    var privacyAccepted by rememberSaveable { mutableStateOf(false) }
    var speakerMode by rememberSaveable { mutableStateOf(SpeakerSetupMode.PREDEFINED) }
    var speakerNames by rememberSaveable {
        mutableStateOf(ArrayList(SessionSetupValidator.defaultSpeakerNames(2)))
    }
    var setupComplete by rememberSaveable { mutableStateOf(false) }
    var showBrandIntro by rememberSaveable { mutableStateOf(true) }
    var setupError by rememberSaveable { mutableStateOf<String?>(null) }
    var startNewConversation by rememberSaveable { mutableStateOf(entries.isNotEmpty()) }
    var activeSpeakerIndex by rememberSaveable { mutableStateOf(0) }
    var llmModelStatus by remember { mutableStateOf(generativeSummaryBuilder.modelDownloadStatus()) }
    var isDownloadingLlmModel by remember { mutableStateOf(false) }
    SideEffect {
        val window = (context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = if (useDarkTheme) {
                android.graphics.Color.rgb(18, 20, 38)
            } else {
                android.graphics.Color.rgb(237, 246, 248)
            }
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowInsetsControllerCompat(window, window.decorView).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }
    val importModelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isDownloadingLlmModel = true
            llmModelStatus = ModelDownloadStatus(ModelDownloadState.DOWNLOADING, "Installing Private AI Pack...")
            appScope.launch {
                try { llmModelStatus = generativeSummaryBuilder.importCompactModel(uri) }
                finally { isDownloadingLlmModel = false }
            }
        }
    }
    val sessionSetup = if (setupComplete) {
        SessionSetup(speakerMode = speakerMode, speakerNames = speakerNames.toList())
    } else {
        null
    }
    var heardText by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var currentText by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var generatedReply by rememberSaveable { mutableStateOf<String?>(null) }
    var generatedReplyBaseTurnCount by rememberSaveable { mutableStateOf<Int?>(null) }
    var generatedReplyStale by rememberSaveable { mutableStateOf(false) }
    var lastReplyIntent by rememberSaveable { mutableStateOf<String?>(null) }
    var lastAcceptedGeneratedReply by rememberSaveable { mutableStateOf<String?>(null) }
    var isGeneratingReply by remember { mutableStateOf(false) }
    var isReplyPlaying by remember { mutableStateOf(false) }
    var conversationRevision by remember { mutableLongStateOf(0L) }
    var replyGenerationToken by remember { mutableLongStateOf(0L) }
    var replyPlaybackToken by remember { mutableLongStateOf(0L) }
    var resumeCaptureOnForeground by rememberSaveable { mutableStateOf(false) }
    var liveCaption by remember { mutableStateOf("Speak after pressing Start. Captions appear here.") }
    val summaryCoordinator = remember(generativeSummaryBuilder) {
        LiveSummaryCoordinator(
            scope = appScope,
            initial = generativeSummaryBuilder.initialSummary(emptyList()),
            preview = { turns ->
                withContext(Dispatchers.Default) { generativeSummaryBuilder.initialSummary(turns) }
            },
            generate = { turns, partial -> generativeSummaryBuilder.buildSummary(turns, partial) },
            cancelGeneration = { generativeSummaryBuilder.cancelActive() }
        )
    }
    val summaryState by summaryCoordinator.state.collectAsStateWithLifecycle()
    val summary = summaryState.result.text
    val summaryMethodLabel = summaryState.result.methodLabel
    val summaryDetail = summaryState.result.detail + if (
        summaryState.coveredTurns > 0 && summaryState.coveredTurns < summaryState.latestTurns
    ) " New speech is waiting for the next update." else ""
    val isUpdatingSummary = summaryState.isUpdating
    DisposableEffect(summaryCoordinator) {
        onDispose { summaryCoordinator.close() }
    }
    // Do not load a multi-GB native model just because the setup page is visible.
    LaunchedEffect(setupComplete) {
        if (setupComplete && entries.isNotEmpty()) summaryCoordinator.request(entries.toList())
    }
    var suggestions by remember { mutableStateOf(emptyList<WordSuggestion>()) }
    var captureState by remember { mutableStateOf("Ready") }
    var selectedEngine by remember { mutableStateOf(SpeechEngine.DEFAULT) }
    var modelReadiness by remember { mutableStateOf(RecognizerReadiness.SELECTED) }
    var speakerAnalysisStatus by remember { mutableStateOf("Manual speaker labels") }
    var statusMessage by remember { mutableStateOf("Private mode selected. Start when you are ready.") }
    var selectedLanguage by remember { mutableStateOf(SpeechLanguage.defaultLanguage(selectedEngine)) }
    val availableLanguages = remember(selectedEngine) { SpeechLanguage.availableEntries(selectedEngine) }
    var pendingCaptionText by remember { mutableStateOf("") }
    var lastCaptionTurnIndex by rememberSaveable {
        mutableStateOf(entries.indexOfLast { !it.speaker.equals("You", ignoreCase = true) }.takeIf { it >= 0 })
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasRequestedAudioPermission by rememberSaveable { mutableStateOf(false) }
    var microphonePermanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var activityLevel by remember { mutableDoubleStateOf(0.0) }

    fun nextTurnLabel(): String {
        val setup = sessionSetup
        return when (setup?.speakerMode) {
            SpeakerSetupMode.PREDEFINED ->
                setup.speakerNames.getOrNull(activeSpeakerIndex) ?: "Speaker ${activeSpeakerIndex + 1}"
            SpeakerSetupMode.DYNAMIC ->
                setup.speakerNames.firstOrNull() ?: "Speaker 1"
            null -> turnLabeler.nextLabel(entries.map { it.speaker })
        }
    }

    fun isCaptureActive(): Boolean = captureState == "Listening" || captureState == "Paused" || captureState == "Loading"

    fun stopActiveRecognizer() {
        offlineSpeechRecognizer.stop()
        systemSpeechRecognizer.stop()
    }

    fun pauseActiveRecognizer() {
        if (selectedEngine == SpeechEngine.SYSTEM_SERVICE) {
            systemSpeechRecognizer.pause()
        } else {
            offlineSpeechRecognizer.pause()
        }
    }

    fun resumeActiveRecognizer() {
        if (selectedEngine == SpeechEngine.SYSTEM_SERVICE) {
            systemSpeechRecognizer.resume()
        } else {
            offlineSpeechRecognizer.resume()
        }
    }

    fun refreshSuggestions(value: TextFieldValue = currentText) {
        if (!value.selection.collapsed || value.selection.end != value.text.length) {
            suggestions = emptyList()
            return
        }
        suggestions = predictor.predict(
            turnTexts = entries.map { it.text },
            currentText = value.text
        )
    }

    fun clearReplyWork() {
        if (isGeneratingReply) {
            generativeSummaryBuilder.cancelActive()
            summaryCoordinator.resumeAfterReply()
        }
        replyGenerationToken += 1L
        isGeneratingReply = false
        generatedReply = null
        generatedReplyBaseTurnCount = null
        generatedReplyStale = false
    }

    fun resetAcceptedReplyMemory() {
        lastReplyIntent = null
        lastAcceptedGeneratedReply = null
    }

    fun rebuildConversationState(message: String, preserveGeneratedDraft: Boolean = true) {
        conversationRevision += 1L
        val transcriptSnapshot = entries.toList()
        if (preserveGeneratedDraft && generatedReply != null) {
            generatedReplyStale = true
        } else if (!preserveGeneratedDraft) {
            clearReplyWork()
        }
        summaryCoordinator.request(transcriptSnapshot)
        localConversationStore.saveTranscript(conversationManager.asPlainText())
        refreshSuggestions()
        statusMessage = ContinuousCapturePolicy.status(message, captureState)
    }

    fun deleteTranscriptHistory(message: String) {
        conversationRevision += 1L
        clearReplyWork()
        resetAcceptedReplyMemory()
        conversationManager.clear()
        lastCaptionTurnIndex = null
        summaryCoordinator.reset(generativeSummaryBuilder.initialSummary(emptyList()))
        localConversationStore.clearTranscript()
        refreshSuggestions()
        if (captureState != "Listening") {
            liveCaption = "Speak after pressing Start. Captions appear here."
        }
        statusMessage = ContinuousCapturePolicy.status(message, captureState)
    }

    fun clearWorkingSession(message: String) {
        resumeCaptureOnForeground = false
        stopActiveRecognizer()
        ttsHelper.stop()
        replyPlaybackToken += 1L
        isReplyPlaying = false
        selfSpeechEchoGuard.cancel()
        speakerTurnTracker.clearPending()
        speakerDiarizer.reset(speakerNames)
        speakerAnalysisStatus = if (speakerMode == SpeakerSetupMode.DYNAMIC) {
            "Learning participant voices"
        } else {
            "Manual speaker labels"
        }
        conversationManager.clear()
        pendingCaptionText = ""
        lastCaptionTurnIndex = null
        summaryCoordinator.reset(generativeSummaryBuilder.initialSummary(emptyList()))
        localConversationStore.clearTranscript()
        captureState = "Ready"
        activityLevel = 0.0
        liveCaption = "Speak after pressing Start. Captions appear here."
        heardText = TextFieldValue()
        currentText = TextFieldValue()
        resetAcceptedReplyMemory()
        suggestions = emptyList()
        conversationRevision += 1L
        clearReplyWork()
        modelReadiness = RecognizerReadiness.SELECTED
        statusMessage = message
    }

    fun saveSpeechCaption(
        text: String,
        message: String,
        language: SpeechLanguage = selectedLanguage,
        speakerEmbedding: DoubleArray? = null,
        speakerFrames: Int = 0
    ): Boolean {
        val caption = SpeechTextNormalizer.normalize(text, language)
        if (caption.isBlank()) return false

        val needsPlaybackReview = selfSpeechEchoGuard.isLikelyPlaybackEcho(caption)
        val speaker = if (sessionSetup?.speakerMode == SpeakerSetupMode.DYNAMIC) {
            val assignment = speakerDiarizer.assign(
                embedding = speakerEmbedding,
                speakerFrames = speakerFrames,
                fallbackLabel = nextTurnLabel()
            )
            speakerAnalysisStatus = if (assignment.usedVoiceEmbedding) {
                if (assignment.createdNewCluster) {
                    "${assignment.label} identified as a new participant"
                } else {
                    val similarityPercent = (assignment.cosineSimilarity * 100).toInt().coerceIn(0, 100)
                    "${assignment.label} matched with $similarityPercent% voice confidence"
                }
            } else {
                "${assignment.label} assigned from a short speech segment"
            }
            assignment.label
        } else {
            speakerTurnTracker.consumeSpeaker(nextTurnLabel())
        }
        if (!conversationManager.addTurn(speaker, caption, needsPlaybackReview)) {
            pendingCaptionText = ""
            liveCaption = caption
            statusMessage = ContinuousCapturePolicy.status("Duplicate caption ignored.", captureState)
            return true
        }
        lastCaptionTurnIndex = entries.lastIndex
        pendingCaptionText = ""
        liveCaption = caption
        rebuildConversationState(
            if (needsPlaybackReview) {
                "Possible app-reply echo retained and marked for review."
            } else {
                message
            }
        )
        return true
    }

    fun changeSpeechEngine(engine: SpeechEngine) {
        if (engine == selectedEngine) return
        if (sessionSetup?.speakerMode == SpeakerSetupMode.DYNAMIC && engine != SpeechEngine.OFFLINE_VOSK) {
            statusMessage = "Auto speaker labels require Private capture mode."
            return
        }
        if (isCaptureActive()) {
            statusMessage = "Recognition mode is locked while capture is active. Stop capture before changing it."
            return
        }

        val nextLanguages = SpeechLanguage.availableEntries(engine)
        selectedEngine = engine
        clearReplyWork()
        modelReadiness = RecognizerReadiness.SELECTED
        if (selectedLanguage !in nextLanguages) {
            selectedLanguage = SpeechLanguage.defaultLanguage(engine)
        }
        statusMessage = "${engine.label} mode selected. ${engine.detail}."
    }

    fun changeSpeechLanguage(language: SpeechLanguage) {
        if (language == selectedLanguage) return
        if (isCaptureActive()) {
            statusMessage = "Language is locked while capture is active. Stop capture before changing it."
            return
        }

        selectedLanguage = language
        clearReplyWork()
        modelReadiness = RecognizerReadiness.SELECTED
        statusMessage = "${language.label} selected."
    }

    fun beginListening() {
        if (captureState != "Ready") {
            statusMessage = "Stop the current capture before starting again."
            return
        }

        val engine = selectedEngine
        val language = selectedLanguage
        resumeCaptureOnForeground = false
        captureState = "Loading"
        modelReadiness = RecognizerReadiness.LOADING
        statusMessage = if (engine == SpeechEngine.SYSTEM_SERVICE) {
            "Starting enhanced speech capture for ${language.label}."
        } else {
            "Preparing private speech capture for ${language.label}."
        }
        liveCaption = "Preparing recognizer..."
        pendingCaptionText = ""
        speakerTurnTracker.clearPending()

        val onReady = {
            captureState = "Listening"
            modelReadiness = RecognizerReadiness.READY
            activityLevel = 0.12
            statusMessage = if (engine == SpeechEngine.SYSTEM_SERVICE) {
                "Listening with enhanced speech capture in ${language.label}."
            } else {
                "Listening privately in ${language.label}. Speak near the phone."
            }
            liveCaption = "Listening..."
        }
        val onPartial: (String) -> Unit = { partial ->
            val caption = SpeechTextNormalizer.normalize(partial, language)
            if (caption.isNotBlank()) {
                speakerTurnTracker.observePartial(nextTurnLabel())
            }
            pendingCaptionText = caption
            liveCaption = caption.ifBlank { "Listening..." }
            if (engine == SpeechEngine.SYSTEM_SERVICE) activityLevel = 1.0
        }
        val onResult: (SpeechRecognitionResult) -> Unit = { result ->
            saveSpeechCaption(
                text = result.text,
                message = "Caption added and gist refreshed.",
                language = language,
                speakerEmbedding = result.speakerEmbedding,
                speakerFrames = result.speakerFrames
            )
            if (result.wordConfidence != null && result.wordConfidence < 0.65) {
                statusMessage = "Some words are uncertain. Move the microphone closer, check the selected language, and correct the caption if needed."
            }
        }
        val onError: (String) -> Unit = { error ->
            stopActiveRecognizer()
            resumeCaptureOnForeground = false
            speakerTurnTracker.clearPending()
            captureState = "Ready"
            modelReadiness = RecognizerReadiness.ERROR
            statusMessage = error
            liveCaption = "Recognizer stopped."
        }

        if (engine == SpeechEngine.SYSTEM_SERVICE) {
            systemSpeechRecognizer.start(
                context = context,
                language = language,
                onReady = onReady,
                onPartial = onPartial,
                onResult = onResult,
                onError = onError
            )
        } else {
            offlineSpeechRecognizer.onAudioLevel = { activityLevel = it.toDouble() }
            offlineSpeechRecognizer.start(
                context = context,
                language = language,
                enableSpeakerEmbeddings = sessionSetup?.speakerMode == SpeakerSetupMode.DYNAMIC,
                onReady = onReady,
                onPartial = onPartial,
                onResult = onResult,
                onError = onError
            )
        }
    }

    fun startListening() {
        if (!hasAudioPermission) {
            statusMessage = "Microphone permission is needed. Stop this session to return to setup and restore access."
            return
        }
        if (sessionSetup == null) {
            statusMessage = "Complete Session Setup before starting live captions."
            return
        }
        beginListening()
    }

    fun openAppPermissionSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        val activity = context as? Activity
        microphonePermanentlyDenied = !granted &&
            hasRequestedAudioPermission &&
            activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO)
        setupError = if (granted) {
            null
        } else if (microphonePermanentlyDenied) {
            "Microphone access is blocked. Open Android app settings, allow Microphone, then return here."
        } else {
            "Microphone permission was denied. Allow it to start live captions."
        }
        statusMessage = if (granted) {
            "Microphone permission granted. Complete speaker setup to continue."
        } else {
            "Microphone permission was denied."
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val permissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        hasAudioPermission = permissionGranted
        if (permissionGranted) {
            microphonePermanentlyDenied = false
            setupError = null
        }
        if (resumeCaptureOnForeground) {
            resumeCaptureOnForeground = false
            if (permissionGranted && sessionSetup != null && captureState == "Ready") {
                beginListening()
                statusMessage = "Returning to live capture after the app became visible."
            }
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (isCaptureActive()) {
            resumeCaptureOnForeground = captureState == "Listening" || captureState == "Loading"
            val pending = pendingCaptionText
            val language = selectedLanguage
            stopActiveRecognizer()
            saveSpeechCaption(
                pending,
                "Latest partial caption saved before the app moved to the background.",
                language
            )
            captureState = "Ready"
            pendingCaptionText = ""
            speakerTurnTracker.clearPending()
            activityLevel = 0.0
            liveCaption = "Stopped because the app is no longer visible."
            statusMessage = if (resumeCaptureOnForeground) {
                "Capture suspended while the app is hidden; it will restart when you return."
            } else {
                "Microphone stopped because the app is no longer visible."
            }
        }
        replyPlaybackToken += 1L
        isReplyPlaying = false
        selfSpeechEchoGuard.cancel()
        ttsHelper.stop()
    }

    LaunchedEffect(activityLevel, captureState) {
        if (captureState == "Listening" && activityLevel > 0.12) {
            delay(250)
            activityLevel = 0.12
        } else if (captureState != "Listening") {
            activityLevel = 0.0
        }
    }

    val palette = if (useDarkTheme) DarkPalette else LightPalette
    val colors = if (useDarkTheme) darkColorScheme(
        primary = Cyan,
        secondary = Lavender,
        tertiary = Warm,
        background = palette.background,
        surface = palette.panel,
        surfaceVariant = palette.panelSoft,
        onSurface = palette.textPrimary
    ) else lightColorScheme(
        primary = CyanDeep,
        secondary = Lavender,
        tertiary = Warm,
        background = palette.background,
        surface = palette.panel,
        surfaceVariant = palette.panelSoft,
        onSurface = palette.textPrimary
    )
    val setupValidationError = SessionSetupValidator.validate(
        privacyAccepted = privacyAccepted,
        microphoneGranted = hasAudioPermission,
        mode = speakerMode,
        speakerNames = speakerNames
    )
    val llmSetupError = when {
        llmModelStatus.state == ModelDownloadState.INSTALLED -> null
        llmModelStatus.state == ModelDownloadState.NEEDS_SMALLER_MODEL -> llmModelStatus.detail
        isDownloadingLlmModel || llmModelStatus.state == ModelDownloadState.DOWNLOADING ->
            "Wait for the Private AI Pack installation to finish before starting."
        llmModelStatus.state == ModelDownloadState.FAILED ->
            "Install the Private AI Pack before starting. It powers conversation gist and reply drafting."
        else ->
            "Install the Private AI Pack before starting."
    }
    val setupBlockingError = setupValidationError ?: llmSetupError

    MaterialTheme(colorScheme = colors) {
        CompositionLocalProvider(LocalAppPalette provides palette) {
            Surface(modifier = Modifier.fillMaxSize(), color = palette.background) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(palette.backgroundGradient))
                ) {
                    val configuredSession = sessionSetup
                    if (showBrandIntro) {
                        BrandIntroSplash(onFinished = { showBrandIntro = false })
                    } else if (configuredSession == null) {
                        SessionSetupScreen(
                            useDarkTheme = useDarkTheme,
                            onThemeChange = { useDarkTheme = it },
                            privacyAccepted = privacyAccepted,
                            onPrivacyAcceptedChange = {
                                privacyAccepted = it
                                setupError = null
                            },
                            microphoneGranted = hasAudioPermission,
                            onRequestMicrophone = {
                                setupError = null
                                hasRequestedAudioPermission = true
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            microphonePermanentlyDenied = microphonePermanentlyDenied,
                            onOpenAppSettings = { openAppPermissionSettings() },
                            speakerMode = speakerMode,
                            onSpeakerModeChange = {
                                speakerMode = it
                                if (it == SpeakerSetupMode.DYNAMIC &&
                                    speakerNames.size < SessionSetupValidator.MIN_DYNAMIC_SPEAKERS
                                ) {
                                    speakerNames = ArrayList(
                                        SessionSetupValidator.resizeSpeakerNames(
                                            speakerNames,
                                            SessionSetupValidator.MIN_DYNAMIC_SPEAKERS
                                        )
                                    )
                                }
                                setupError = null
                            },
                            speakerNames = speakerNames,
                            onSpeakerCountChange = { count ->
                                speakerNames = ArrayList(SessionSetupValidator.resizeSpeakerNames(speakerNames, count))
                                setupError = null
                            },
                            onSpeakerNameChange = { index, value ->
                                speakerNames = ArrayList(speakerNames).also { names -> names[index] = value }
                                setupError = null
                            },
                            hasSavedTranscript = entries.isNotEmpty(),
                            startNewConversation = startNewConversation,
                            onStartNewConversationChange = { startNewConversation = it },
                            llmModelStatus = llmModelStatus,
                            isDownloadingLlmModel = isDownloadingLlmModel,
                            onImportLlmModel = { importModelLauncher.launch(arrayOf("*/*")) },
                            onDownloadLlmModel = {
                                if (!isDownloadingLlmModel) {
                                    isDownloadingLlmModel = true
                                    appScope.launch {
                                        try {
                                            val finalStatus = withContext(Dispatchers.IO) {
                                                generativeSummaryBuilder.downloadModel() { progress ->
                                                    withContext(Dispatchers.Main) {
                                                        llmModelStatus = progress
                                                    }
                                                }
                                            }
                                            llmModelStatus = finalStatus
                                        } finally {
                                            isDownloadingLlmModel = false
                                        }
                                    }
                                }
                            },
                            errorMessage = setupError ?: setupBlockingError,
                            canStart = setupBlockingError == null,
                            onStartSession = {
                                val error = setupBlockingError
                                setupError = error
                                if (error == null) {
                                    if (startNewConversation && entries.isNotEmpty()) {
                                        clearWorkingSession("Previous transcript cleared for the new session.")
                                    }
                                    speakerNames = ArrayList(speakerNames.map(SessionSetupValidator::normalizeDisplayName))
                                    speakerDiarizer.reset(speakerNames)
                                    speakerAnalysisStatus = if (speakerMode == SpeakerSetupMode.DYNAMIC) {
                                        selectedEngine = SpeechEngine.OFFLINE_VOSK
                                        "Auto speaker labels ready"
                                    } else {
                                        "Manual speaker labels"
                                    }
                                    setupComplete = true
                                    activeSpeakerIndex = 0
                                    startNewConversation = false
                                    statusMessage = "Session ready. Press Start for live captions."
                                }
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .imePadding()
                        ) {
                            Box(Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Header(
                                    useDarkTheme = useDarkTheme,
                                    onThemeChange = { useDarkTheme = it }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                                    .padding(start = 10.dp, end = 10.dp, bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                            SessionReadinessStrip(
                                hasAudioPermission = hasAudioPermission,
                                selectedEngine = selectedEngine,
                                modelReadiness = modelReadiness,
                                setup = configuredSession,
                                captureState = captureState,
                                speakerAnalysisStatus = speakerAnalysisStatus
                            )

                            LiveCaptionPanel(
                            statusMessage = statusMessage,
                            liveCaption = liveCaption,
                            activityLevel = activityLevel,
                            isSpeaking = captureState == "Listening",
                            selectedEngine = selectedEngine,
                            selectedLanguage = selectedLanguage,
                            availableLanguages = availableLanguages,
                            onEngineChange = { changeSpeechEngine(it) },
                            onLanguageSelect = { changeSpeechLanguage(it) },
                            heardText = heardText,
                            onHeardTextChange = { heardText = it },
                            onSaveCorrection = {
                                val corrected = heardText.text.trim()
                                val turnIndex = lastCaptionTurnIndex
                                if (turnIndex == null) {
                                    statusMessage = "Capture a caption before saving a correction."
                                } else if (conversationManager.updateTurnText(turnIndex, corrected)) {
                                    liveCaption = entries[turnIndex].text
                                    heardText = TextFieldValue()
                                    rebuildConversationState("Latest caption corrected and summary refreshed.")
                                }
                            },
                            canSaveCorrection = captureState == "Ready" && lastCaptionTurnIndex != null,
                            speakerNames = configuredSession.speakerNames,
                            speakerMode = configuredSession.speakerMode,
                            speakerAnalysisStatus = speakerAnalysisStatus,
                            activeSpeakerIndex = activeSpeakerIndex,
                            onSpeakerSelect = { activeSpeakerIndex = it },
                            canChangeSpeaker = configuredSession.speakerMode == SpeakerSetupMode.PREDEFINED,
                            captureActiveForSpeaker = isCaptureActive(),
                            recognitionSettingsEnabled = captureState == "Ready",
                            onStart = {
                                startListening()
                            },
                            onPause = {
                                pauseActiveRecognizer()
                                captureState = "Paused"
                                liveCaption = pendingCaptionText.ifBlank { "Paused" }
                                statusMessage = "Paused by you. Nearby speech is not captured until Resume."
                            },
                            onResume = {
                                resumeActiveRecognizer()
                                captureState = "Listening"
                                liveCaption = "Listening..."
                                statusMessage = "Listening with ${selectedEngine.label} in ${selectedLanguage.label}."
                            },
                            onStop = {
                                val pending = pendingCaptionText
                                val language = selectedLanguage
                                resumeCaptureOnForeground = false
                                stopActiveRecognizer()
                                val saved = saveSpeechCaption(
                                    pending,
                                    "Latest caption saved and summary refreshed.",
                                    language
                                )
                                captureState = "Ready"
                                speakerTurnTracker.clearPending()
                                if (!saved) liveCaption = "Stopped. Press Start for live captions."
                                statusMessage = if (saved) {
                                    "Microphone stopped and latest caption saved."
                                } else {
                                    "Microphone stopped."
                                }
                            },
                            canStart = captureState == "Ready" && hasAudioPermission,
                            canPause = captureState == "Listening",
                            canResume = captureState == "Paused",
                            canStop = captureState == "Listening" || captureState == "Paused" || captureState == "Loading"
                        )

                            SummaryPanel(
                                summary = summary,
                                methodLabel = summaryMethodLabel,
                                detail = summaryDetail,
                                isUpdating = isUpdatingSummary
                            )

                        ResponsePanel(
                            text = currentText,
                            suggestions = suggestions,
                            generatedReply = generatedReply,
                            generatedReplyBaseTurnCount = generatedReplyBaseTurnCount,
                            generatedReplyStale = generatedReplyStale,
                            isGeneratingReply = isGeneratingReply,
                            continuousCaptureActive = captureState == "Listening",
                            onTextChange = {
                                currentText = it
                                if (lastAcceptedGeneratedReply?.trim() != it.text.trim()) {
                                    resetAcceptedReplyMemory()
                                }
                                clearReplyWork()
                                refreshSuggestions(it)
                            },
                            onSuggestion = { suggestion ->
                                val updated = suggestion.applyTo(currentText.text)
                                currentText = currentText.copy(
                                    text = updated,
                                    selection = TextRange(updated.length)
                                )
                                resetAcceptedReplyMemory()
                                clearReplyWork()
                                refreshSuggestions(currentText)
                            },
                            onUpdateConversation = {
                                val response = currentText.text.trim()
                                if (response.isNotBlank()) {
                                    val added = conversationManager.addTurn("You", response)
                                    currentText = TextFieldValue()
                                    suggestions = emptyList()
                                    resetAcceptedReplyMemory()
                                    if (added) {
                                        rebuildConversationState(
                                            "Your response was added and the conversation summary was updated.",
                                            preserveGeneratedDraft = false
                                        )
                                    } else {
                                        clearReplyWork()
                                        refreshSuggestions()
                                        statusMessage = ContinuousCapturePolicy.status(
                                            "That response is already in the transcript.",
                                            captureState
                                        )
                                    }
                                }
                            },
                            onGenerateReply = {
                                val typedText = currentText.text.trim()
                                val intendedText = if (
                                    lastAcceptedGeneratedReply?.trim() == typedText &&
                                    !lastReplyIntent.isNullOrBlank()
                                ) {
                                    lastReplyIntent!!.trim()
                                } else {
                                    typedText
                                }
                                if (intendedText.isNotBlank() && !LocalTextTasks.isReplyIntentSupported(intendedText)) {
                                    clearReplyWork()
                                    statusMessage = ContinuousCapturePolicy.status(
                                        LocalTextTasks.intentLimitMessage(),
                                        captureState
                                    )
                                } else if (intendedText.isNotBlank()) {
                                    val requestToken = replyGenerationToken + 1L
                                    val baseTurnCount = entries.size
                                    val snapshot = ReplyContext(
                                        turns = entries.toList(),
                                        summary = summary,
                                        intendedText = intendedText
                                    )
                                    summaryCoordinator.pauseForReply()
                                    replyGenerationToken = requestToken
                                    isGeneratingReply = true
                                    generatedReply = null
                                    generatedReplyBaseTurnCount = null
                                    generatedReplyStale = false
                                    lastReplyIntent = intendedText
                                    statusMessage = ContinuousCapturePolicy.status(
                                        if (typedText != intendedText) {
                                            "Regenerating from your original intent, not from the previous draft."
                                        } else {
                                            "Generating a local reply from your intent and transcript snapshot."
                                        },
                                        captureState
                                    )
                                    appScope.launch {
                                        try {
                                            val result = generativeSummaryBuilder.generateReply(snapshot)
                                            if (requestToken == replyGenerationToken) {
                                                val unchangedPrefix = entries.take(baseTurnCount) == snapshot.turns
                                                generatedReply = result.text.takeIf { it.isNotBlank() && unchangedPrefix }
                                                generatedReplyBaseTurnCount = baseTurnCount
                                                generatedReplyStale = false
                                                statusMessage = ContinuousCapturePolicy.status(
                                                    if (unchangedPrefix) result.detail else "Conversation was corrected; generate a new reply.",
                                                    captureState
                                                )
                                            }
                                        } finally {
                                            if (requestToken == replyGenerationToken) {
                                                isGeneratingReply = false
                                                summaryCoordinator.resumeAfterReply()
                                            }
                                        }
                                    }
                                }
                            },
                            onUseGeneratedReply = { draft ->
                                val intentUsed = lastReplyIntent ?: currentText.text.trim()
                                currentText = TextFieldValue(
                                    text = draft,
                                    selection = TextRange(draft.length)
                                )
                                clearReplyWork()
                                lastReplyIntent = intentUsed
                                lastAcceptedGeneratedReply = draft
                                refreshSuggestions(currentText)
                                    statusMessage = ContinuousCapturePolicy.status("Draft moved to Your Response.", captureState)
                            },
                            onSpeak = {
                                val spoken = currentText.text.trim()
                                if (spoken.isNotBlank()) {
                                    val added = conversationManager.addTurn("You", spoken)
                                    if (!added) {
                                        currentText = TextFieldValue()
                                        resetAcceptedReplyMemory()
                                        clearReplyWork()
                                        refreshSuggestions()
                                        statusMessage = ContinuousCapturePolicy.status(
                                            "That response is already in the transcript.",
                                            captureState
                                        )
                                        return@ResponsePanel
                                    }
                                    val playbackToken = replyPlaybackToken + 1L
                                    replyPlaybackToken = playbackToken
                                    selfSpeechEchoGuard.arm(spoken)
                                    val queued = ttsHelper.speak(
                                        text = spoken,
                                        language = selectedLanguage,
                                        onStarted = {
                                            if (playbackToken == replyPlaybackToken) {
                                                isReplyPlaying = true
                                                statusMessage = ContinuousCapturePolicy.status(
                                                    "Reply playback started; matching captured text will be retained and marked for review.",
                                                    captureState
                                                )
                                            }
                                        },
                                        onCompleted = { successful ->
                                            if (playbackToken == replyPlaybackToken) {
                                                isReplyPlaying = false
                                                selfSpeechEchoGuard.markPlaybackFinished()
                                                statusMessage = ContinuousCapturePolicy.status(
                                                    if (successful) "Reply playback finished." else "Reply playback stopped.",
                                                    captureState
                                                )
                                            }
                                        }
                                    )
                                    if (!queued) {
                                        selfSpeechEchoGuard.cancel()
                                        isReplyPlaying = false
                                    }
                                    currentText = TextFieldValue()
                                    resetAcceptedReplyMemory()
                                    rebuildConversationState(
                                        if (queued) {
                                            "Response queued for offline speech in ${selectedLanguage.label}, saved, and summary refreshed."
                                        } else {
                                            "${selectedLanguage.label} offline TTS voice is not installed. Response saved."
                                        },
                                        preserveGeneratedDraft = false
                                    )
                                }
                            }
                        )

                        TranscriptPanel(
                            modifier = Modifier.fillMaxWidth(),
                            entries = entries.toList(),
                            shareEnabled = captureState == "Ready",
                            onSave = {
                                localConversationStore.saveTranscript(conversationManager.asPlainText())
                                summaryCoordinator.request(entries.toList())
                                statusMessage = ContinuousCapturePolicy.status(
                                    "Transcript snapshot saved on this device.",
                                    captureState
                                )
                            },
                            onShare = {
                                val transcript = conversationManager.asPlainText()
                                if (transcript.isBlank()) {
                                    statusMessage = "Nothing to share yet."
                                } else {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "ShrutiSaathi Transcript")
                                        putExtra(Intent.EXTRA_TEXT, transcript)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share transcript"))
                                }
                            },
                            onDelete = {
                                heardText = TextFieldValue()
                                deleteTranscriptHistory("Committed transcript history deleted; any in-progress utterance is preserved.")
                            }
                        )
                            }

                            SessionActionsPanel(
                                onResetSession = {
                                    clearWorkingSession("New empty session ready. Press Start for live captions.")
                                    activeSpeakerIndex = 0
                                },
                                onStopSession = {
                                    clearWorkingSession("Current session stopped and cleared.")
                                    setupComplete = false
                                    privacyAccepted = false
                                    speakerMode = SpeakerSetupMode.PREDEFINED
                                    speakerNames = ArrayList(SessionSetupValidator.defaultSpeakerNames(2))
                                    activeSpeakerIndex = 0
                                    setupError = null
                                    startNewConversation = false
                                    selectedEngine = SpeechEngine.DEFAULT
                                    selectedLanguage = SpeechLanguage.defaultLanguage(SpeechEngine.DEFAULT)
                                    modelReadiness = RecognizerReadiness.SELECTED
                                    statusMessage = "Session stopped. Complete setup to begin again."
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandIntroSplash(onFinished: () -> Unit) {
    var circleStarted by remember { mutableStateOf(false) }
    var wordStarted by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        circleStarted = true
        delay(620)
        wordStarted = true
        delay(2_150)
        onFinished()
    }
    val circleReveal by animateFloatAsState(
        targetValue = if (circleStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 900),
        label = "brand-circle-reveal"
    )
    val wordReveal by animateFloatAsState(
        targetValue = if (wordStarted) 1f else 0f,
        animationSpec = tween(durationMillis = 1_050),
        label = "brand-word-reveal"
    )
    val transition = rememberInfiniteTransition(label = "brand-motion")
    val breathe by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(1_050), RepeatMode.Reverse),
        label = "brand-breathe"
    )
    val highlight by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Restart),
        label = "brand-highlight"
    )
    val shimmer by transition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(1_900), RepeatMode.Restart),
        label = "brand-shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFF3FEFF),
                        Color(0xFFEAF8FF),
                        Color(0xFFFFF6E5),
                        Color(0xFFF1EEFF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Cyan.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.26f, size.height * 0.22f),
                    radius = size.minDimension * 0.78f
                ),
                radius = size.minDimension * 0.78f,
                center = Offset(size.width * 0.26f, size.height * 0.22f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Warm.copy(alpha = 0.20f), Color.Transparent),
                    center = Offset(size.width * 0.78f, size.height * 0.78f),
                    radius = size.minDimension * 0.62f
                ),
                radius = size.minDimension * 0.62f,
                center = Offset(size.width * 0.78f, size.height * 0.78f)
            )
            repeat(9) { index ->
                val x = size.width * (0.12f + index * 0.10f)
                val y = size.height * (0.18f + ((index * 37) % 61) / 100f)
                drawCircle(
                    color = CyanDeep.copy(alpha = 0.06f + (index % 3) * 0.018f),
                    radius = 2.5f + (index % 4) * 1.5f,
                    center = Offset(x, y)
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(184.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val inset = 16f
                    val arcSize = size.width - inset * 2
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color.White.copy(alpha = 0.82f), Cyan.copy(alpha = 0.12f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.46f
                        ),
                        radius = size.minDimension * 0.46f,
                        center = center
                    )
                    drawCircle(
                        color = CyanDeep.copy(alpha = 0.13f),
                        radius = size.minDimension * 0.43f,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )
                    drawArc(
                        color = CyanDeep.copy(alpha = 0.20f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = CyanDeep.copy(alpha = 0.92f),
                        startAngle = -92f + circleReveal * 42f,
                        sweepAngle = 312f * circleReveal,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(arcSize, arcSize),
                        style = Stroke(width = 9f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Cyan.copy(alpha = 0.92f),
                        startAngle = 136f,
                        sweepAngle = 118f * circleReveal,
                        useCenter = false,
                        topLeft = Offset(inset + 11f, inset + 11f),
                        size = androidx.compose.ui.geometry.Size(arcSize - 22f, arcSize - 22f),
                        style = Stroke(width = 6f, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Warm.copy(alpha = 0.96f),
                        startAngle = -36f + highlight * 360f,
                        sweepAngle = 28f,
                        useCenter = false,
                        topLeft = Offset(inset + 3f, inset + 3f),
                        size = androidx.compose.ui.geometry.Size(arcSize - 6f, arcSize - 6f),
                        style = Stroke(width = 4f, cap = StrokeCap.Round)
                    )
                }
                AppLogoMark(
                    modifier = Modifier.graphicsLayer {
                        alpha = circleReveal
                        scaleX = (0.78f + circleReveal * 0.22f) * breathe
                        scaleY = (0.78f + circleReveal * 0.22f) * breathe
                    },
                    size = 118.dp,
                    imageSize = 92.dp,
                    shimmerPosition = shimmer
                )
            }
            Box(
                modifier = Modifier.width(320.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .width(320.dp * wordReveal.coerceIn(0.01f, 1f))
                        .height(58.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "ShrutiSaathi",
                        color = Color(0xFF13253A),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        modifier = Modifier.width(320.dp)
                    )
                }
            }
            Text(
                text = "Hear less. Understand more. Reply with confidence.",
                color = Color(0xFF476174),
                fontStyle = FontStyle.Italic,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.graphicsLayer { alpha = wordReveal }
            )
            LinearProgressIndicator(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .width(148.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(50))
                    .graphicsLayer { alpha = wordReveal },
                color = CyanDeep,
                trackColor = CyanDeep.copy(alpha = 0.12f)
            )
        }
    }
}

@Composable
private fun AppLogoMark(
    modifier: Modifier = Modifier,
    size: Dp,
    imageSize: Dp,
    shimmerPosition: Float? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White,
                        Color(0xFFF2FEFF),
                        Color(0xFFE3F6FF)
                    )
                )
            )
            .border(1.dp, CyanDeep.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Warm.copy(alpha = 0.13f),
                radius = this.size.minDimension * 0.40f,
                center = Offset(this.size.width * 0.35f, this.size.height * 0.30f)
            )
            drawCircle(
                color = Cyan.copy(alpha = 0.14f),
                radius = this.size.minDimension * 0.44f,
                center = Offset(this.size.width * 0.68f, this.size.height * 0.70f)
            )
        }
        if (shimmerPosition != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { translationX = size.toPx() * (shimmerPosition - 0.55f) }
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.36f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "ShrutiSaathi logo",
            modifier = Modifier.size(imageSize),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun Header(
    useDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val palette = LocalAppPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.20f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            AppLogoMark(size = 34.dp, imageSize = 28.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "ShrutiSaathi v1.0",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Pro conversation workspace",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        ThemeToggle(useDarkTheme = useDarkTheme, onThemeChange = onThemeChange)
        }
    }
}

@Composable
private fun ThemeToggle(useDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val palette = LocalAppPalette.current
    val thumbScale by animateFloatAsState(
        targetValue = if (useDarkTheme) 0.9f else 1f,
        animationSpec = tween(durationMillis = 220),
        label = "theme-toggle-thumb"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (useDarkTheme) 5.dp else 59.dp,
        animationSpec = tween(durationMillis = 240),
        label = "theme-toggle-slide"
    )
    Box(
        modifier = Modifier
            .width(86.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(50))
            .clickable { onThemeChange(!useDarkTheme) }
            .background(
                Brush.horizontalGradient(
                    if (useDarkTheme) {
                        listOf(Color(0xFF2A2348), Color(0xFF151729))
                    } else {
                        listOf(Color(0xFFE8F8FA), Color(0xFFFFEDC7))
                    }
                )
            )
            .border(1.dp, palette.border.copy(alpha = 0.92f), RoundedCornerShape(50)),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (useDarkTheme) "Dark" else "Light",
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = if (useDarkTheme) 32.dp else 10.dp,
                    end = if (useDarkTheme) 10.dp else 32.dp
                ),
            color = palette.textSecondary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (useDarkTheme) TextAlign.End else TextAlign.Start,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = thumbOffset)
                .size(22.dp)
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color.White.copy(alpha = 0.70f),
                            if (useDarkTheme) Lavender else Warm
                        )
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.74f), CircleShape)
        )
    }
}

@Composable
private fun StatusBadge(text: String, color: Color) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = if (palette.isDark) 0.14f else 0.16f))
            .border(1.dp, color.copy(alpha = 0.22f), RoundedCornerShape(50))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(
            text,
            color = palette.textPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SessionSetupScreen(
    useDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    privacyAccepted: Boolean,
    onPrivacyAcceptedChange: (Boolean) -> Unit,
    microphoneGranted: Boolean,
    onRequestMicrophone: () -> Unit,
    microphonePermanentlyDenied: Boolean,
    onOpenAppSettings: () -> Unit,
    speakerMode: SpeakerSetupMode,
    onSpeakerModeChange: (SpeakerSetupMode) -> Unit,
    speakerNames: List<String>,
    onSpeakerCountChange: (Int) -> Unit,
    onSpeakerNameChange: (Int, String) -> Unit,
    hasSavedTranscript: Boolean,
    startNewConversation: Boolean,
    onStartNewConversationChange: (Boolean) -> Unit,
    llmModelStatus: ModelDownloadStatus,
    isDownloadingLlmModel: Boolean,
    onImportLlmModel: () -> Unit,
    onDownloadLlmModel: () -> Unit,
    errorMessage: String?,
    canStart: Boolean,
    onStartSession: () -> Unit
) {
    val palette = LocalAppPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding()
    ) {
        SetupHeroHeader(
            useDarkTheme = useDarkTheme,
            onThemeChange = onThemeChange
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SetupStepHeader(
                title = "Set up your workspace",
                detail = "Prepare Private AI, microphone access, and participant labels."
            )

            ProAccessCard()

            LlmModelSetupCard(
                status = llmModelStatus,
                isDownloading = isDownloadingLlmModel,
                onDownload = onDownloadLlmModel,
                onImport = onImportLlmModel
            )

            Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = palette.panel),
            border = BorderStroke(1.dp, Mint.copy(alpha = 0.24f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Privacy & access", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.Top) {
                    Checkbox(checked = privacyAccepted, onCheckedChange = onPrivacyAcceptedChange)
                    Text(
                        "I agree to microphone access and private on-device transcript storage.",
                        modifier = Modifier.padding(top = 9.dp),
                        color = palette.textPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                PermissionStatusRow(
                    title = "Microphone",
                    detail = if (microphoneGranted) "Ready for live caption capture" else "Required for live captions",
                    ready = microphoneGranted
                )
                if (!microphoneGranted && !microphonePermanentlyDenied) {
                    Button(
                        onClick = onRequestMicrophone,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanDeep, contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Allow microphone")
                    }
                }
                if (!microphoneGranted && microphonePermanentlyDenied) {
                    Button(
                        onClick = onOpenAppSettings,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Warm, contentColor = Color(0xFF1B1308))
                    ) {
                        Text("Open Android app settings")
                    }
                }
                PermissionStatusRow(
                    title = "Private local storage",
                    detail = "Ready for session transcripts and AI Pack storage",
                    ready = true
                )
            }
        }

        if (hasSavedTranscript) {
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = palette.panel),
                border = BorderStroke(1.dp, Warm.copy(alpha = 0.28f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = startNewConversation,
                        onCheckedChange = onStartNewConversationChange
                    )
                    Column(Modifier.weight(1f)) {
                        Text("Start a new conversation", color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (startNewConversation) {
                                "The saved transcript will be deleted when this session starts."
                            } else {
                                "The saved transcript will remain and new turns will be added to it."
                            },
                            color = palette.textMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = palette.panel),
            border = BorderStroke(1.dp, Lavender.copy(alpha = 0.24f))
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Participants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Choose named speaker labels or let ShrutiSaathi separate voices during the session.",
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeakerSetupModeButton(
                        text = "Predefined",
                        selected = speakerMode == SpeakerSetupMode.PREDEFINED,
                        onClick = { onSpeakerModeChange(SpeakerSetupMode.PREDEFINED) },
                        modifier = Modifier.weight(1f)
                    )
                    SpeakerSetupModeButton(
                        text = "Automatic AI",
                        selected = speakerMode == SpeakerSetupMode.DYNAMIC,
                        onClick = { onSpeakerModeChange(SpeakerSetupMode.DYNAMIC) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (speakerMode == SpeakerSetupMode.DYNAMIC) "Maximum voice clusters" else "Number of speakers",
                            modifier = Modifier.weight(1f),
                            color = palette.textPrimary
                        )
                        OutlinedButton(
                            onClick = { onSpeakerCountChange(speakerNames.size - 1) },
                            enabled = speakerNames.size > SessionSetupValidator.MIN_SPEAKERS,
                            modifier = Modifier.height(38.dp)
                        ) { Text("-") }
                        Text(
                            speakerNames.size.toString(),
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontWeight = FontWeight.Bold,
                            color = Cyan
                        )
                        OutlinedButton(
                            onClick = { onSpeakerCountChange(speakerNames.size + 1) },
                            enabled = speakerNames.size < SessionSetupValidator.MAX_SPEAKERS,
                            modifier = Modifier.height(38.dp)
                        ) { Text("+") }
                }
                speakerNames.forEachIndexed { index, name ->
                        OutlinedTextField(
                            value = name,
                            onValueChange = { onSpeakerNameChange(index, it) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = {
                                Text(
                                    if (speakerMode == SpeakerSetupMode.DYNAMIC) {
                                        "Participant ${index + 1} label"
                                    } else {
                                        "Speaker ${index + 1} name"
                                    }
                                )
                            },
                            colors = appTextFieldColors()
                        )
                }
                if (speakerMode == SpeakerSetupMode.DYNAMIC) {
                    Text(
                        "ShrutiSaathi maps each newly heard voice to the next participant label. " +
                            "It supports conversation flow, not identity verification.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Mint.copy(alpha = 0.12f))
                            .border(1.dp, Mint.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        color = Mint
                    )
                }
            }
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Danger.copy(alpha = 0.12f))
                    .border(1.dp, Danger.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                color = Danger,
                fontWeight = FontWeight.SemiBold
            )
        }
        }
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            color = palette.background,
            shadowElevation = 8.dp
        ) {
            GlowingStartButton(
                canStart = canStart,
                onClick = onStartSession,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
private fun SetupHeroHeader(
    useDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        CyanDeep.copy(alpha = if (palette.isDark) 0.38f else 0.18f),
                        palette.background.copy(alpha = 0.98f)
                    )
                )
            )
            .padding(start = 14.dp, end = 14.dp, top = 9.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppLogoMark(size = 42.dp, imageSize = 34.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        "ShrutiSaathi v1.0",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "Hear less. Understand more.",
                        color = Warm,
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ThemeToggle(useDarkTheme = useDarkTheme, onThemeChange = onThemeChange)
        }
        Text(
            "Premium conversation support with live captions, speaker-aware transcript, concise gist, and reply assistance.",
            color = palette.textSecondary,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun SetupStepHeader(title: String, detail: String) {
    val palette = LocalAppPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = palette.textPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = palette.textMuted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProAccessCard() {
    val palette = LocalAppPalette.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panelSoft.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, Warm.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Warm.copy(alpha = 0.18f))
                    .border(1.dp, Warm.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text("PRO", color = Warm, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("ShrutiSaathi Pro Workspace", color = palette.textPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Private captions, concise gist, and reply assistance.", color = palette.textMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun LlmModelSetupCard(
    status: ModelDownloadStatus,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onImport: () -> Unit
) {
    val palette = LocalAppPalette.current
    val needsSmaller = status.state == ModelDownloadState.NEEDS_SMALLER_MODEL
    val installed = status.state == ModelDownloadState.INSTALLED
    val failed = status.state == ModelDownloadState.FAILED
    val downloading = isDownloading || status.state == ModelDownloadState.DOWNLOADING
    val titleColor = when {
        installed -> Mint
        failed -> Danger
        downloading -> Lavender
        else -> Warm
    }
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = BorderStroke(1.dp, titleColor.copy(alpha = 0.30f))
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Private AI Pack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            installed -> "Ready"
                            downloading -> "Installing"
                            failed -> "Installation failed"
                            needsSmaller -> "Memory-safe setup required"
                            else -> "Required for Pro features"
                        },
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Box(Modifier.size(12.dp).clip(CircleShape).background(titleColor))
            }
            Text(status.detail, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (downloading) {
                val progress = status.progressFraction
                if (progress != null) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Lavender,
                        trackColor = palette.panelSunken
                    )
                    Text(
                        "${formatModelBytes(status.downloadedBytes)} / ${formatModelBytes(status.totalBytes ?: 0L)}",
                        color = palette.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Lavender,
                        trackColor = palette.panelSunken
                    )
                    Text(
                        formatModelBytes(status.downloadedBytes),
                        color = palette.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (installed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Mint.copy(alpha = 0.12f))
                        .border(1.dp, Mint.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Mint, modifier = Modifier.size(18.dp))
                    Text("AI Pack Ready", color = Mint, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Button(
                    onClick = onDownload,
                    enabled = !downloading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (failed) Danger else Lavender,
                        contentColor = Color.White,
                        disabledContainerColor = palette.panelSoft,
                        disabledContentColor = palette.textMuted
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        when {
                            needsSmaller -> "Install AI Pack"
                            failed -> "Retry AI Pack"
                            else -> "Install AI Pack"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Text(
                "One-time setup. Gist and reply drafting run privately on this device.",
                color = palette.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun formatModelBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format(Locale.US, "%.2f GB", mb / 1024.0)
    } else {
        String.format(Locale.US, "%.0f MB", mb)
    }
}

@Composable
private fun PermissionStatusRow(title: String, detail: String, ready: Boolean) {
    val palette = LocalAppPalette.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(if (ready) Mint else Warm))
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
            Text(
                detail,
                color = palette.textMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SpeakerSetupModeButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(42.dp),
        border = BorderStroke(1.dp, if (selected) Lavender else palette.border.copy(alpha = if (enabled) 1f else 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Lavender.copy(alpha = 0.14f) else palette.panelSunken,
            contentColor = if (selected) Lavender else palette.textSecondary,
            disabledContainerColor = palette.panelSunken.copy(alpha = 0.55f),
            disabledContentColor = palette.textMuted
        )
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun GlowingStartButton(
    canStart: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val transition = rememberInfiniteTransition(label = "setup-start-glow")
    val glow by transition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "setup-start-glow-alpha"
    )
    Box(
        modifier = Modifier
            .then(modifier)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (canStart) Mint.copy(alpha = glow) else Color.Transparent)
            .padding(2.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canStart) Mint else palette.panelSoft,
                contentColor = if (canStart) Color(0xFF07111F) else palette.textMuted
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Start ShrutiSaathi", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SessionReadinessStrip(
    hasAudioPermission: Boolean,
    selectedEngine: SpeechEngine,
    modelReadiness: RecognizerReadiness,
    setup: SessionSetup,
    captureState: String,
    speakerAnalysisStatus: String
) {
    val palette = LocalAppPalette.current
    val voiceState = when {
        !hasAudioPermission -> "Permission needed"
        captureState == "Listening" -> "Capturing"
        captureState == "Paused" -> "Paused"
        captureState == "Loading" -> "Preparing"
        else -> "Ready"
    }
    val modelState = when (modelReadiness) {
        RecognizerReadiness.SELECTED -> "Ready"
        RecognizerReadiness.LOADING -> "Loading"
        RecognizerReadiness.READY -> "Ready"
        RecognizerReadiness.ERROR -> "Error"
    }
    val modelColor = when (modelReadiness) {
        RecognizerReadiness.READY -> Mint
        RecognizerReadiness.ERROR -> Danger
        RecognizerReadiness.LOADING -> Lavender
        RecognizerReadiness.SELECTED -> Mint
    }
    val sessionState = when {
        !hasAudioPermission -> "Blocked"
        modelReadiness == RecognizerReadiness.ERROR -> "Check AI"
        captureState == "Listening" -> "Live"
        captureState == "Paused" -> "Paused"
        captureState == "Loading" -> "Starting"
        else -> "Ready"
    }
    val sessionColor = when {
        !hasAudioPermission || modelReadiness == RecognizerReadiness.ERROR -> Danger
        captureState == "Listening" -> Mint
        captureState == "Loading" -> Lavender
        else -> palette.textSecondary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, palette.border)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ReadinessLight(
                title = "Voice",
                state = voiceState,
                color = if (hasAudioPermission) Mint else Danger,
                modifier = Modifier.weight(1f)
            )
            ReadinessLight(
                title = "AI",
                state = modelState,
                color = modelColor,
                modifier = Modifier.weight(1f)
            )
            ReadinessLight(
                title = "Speakers",
                state = if (setup.speakerMode == SpeakerSetupMode.DYNAMIC) "Auto" else "Manual",
                color = if (setup.speakerMode == SpeakerSetupMode.DYNAMIC) Mint else Lavender,
                detail = if (setup.speakerMode == SpeakerSetupMode.DYNAMIC) {
                    "AI"
                } else {
                    "${setup.speakerNames.size}"
                },
                modifier = Modifier.weight(1f)
            )
            ReadinessLight(
                title = "Session",
                state = sessionState,
                color = sessionColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ReadinessLight(
    title: String,
    state: String,
    color: Color,
    detail: String? = null,
    modifier: Modifier = Modifier
) {
    val palette = LocalAppPalette.current
    val info = if (detail.isNullOrBlank()) state else "$state · $detail"
    Column(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(palette.panelSunken.copy(alpha = 0.36f))
            .border(1.dp, color.copy(alpha = 0.18f), RoundedCornerShape(11.dp))
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(4.dp))
            Text(
                title,
                color = palette.textPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            info,
            color = color,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CaptureContinuityPanel(
    captureState: String,
    selectedEngine: SpeechEngine,
    isReplyPlaying: Boolean
) {
    val palette = LocalAppPalette.current
    val title: String
    val detail: String
    val color: Color

    when {
        captureState == "Listening" && isReplyPlaying -> {
            title = "Reply playback active"
            detail = "Possible playback echo is retained and marked for review. Earphones reduce overlap with nearby speech."
            color = Warm
        }
        captureState == "Listening" && selectedEngine == SpeechEngine.OFFLINE_VOSK -> {
            title = "Live capture is active"
            detail = "Typing, gist updates, reply drafting, scrolling, and saving keep the microphone running."
            color = Mint
        }
        captureState == "Listening" -> {
            title = "Enhanced capture is active"
            detail = "Your phone's speech service handles capture. Private mode is preferred for consistent sessions."
            color = Warm
        }
        captureState == "Paused" -> {
            title = "Live capture paused"
            detail = "Nearby speech is not captured until you press Resume."
            color = Danger
        }
        captureState == "Loading" -> {
            title = "Preparing live capture"
            detail = "Wait until the microphone shows Listening."
            color = Lavender
        }
        else -> {
            title = "Ready when you are"
            detail = "Press Start to capture while ShrutiSaathi stays open."
            color = palette.textSecondary
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.padding(top = 5.dp).size(10.dp).clip(CircleShape).background(color))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = color, fontWeight = FontWeight.Bold)
                Text(detail, color = palette.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LiveCaptionPanel(
    statusMessage: String,
    liveCaption: String,
    activityLevel: Double,
    isSpeaking: Boolean,
    selectedEngine: SpeechEngine,
    selectedLanguage: SpeechLanguage,
    availableLanguages: List<SpeechLanguage>,
    onEngineChange: (SpeechEngine) -> Unit,
    onLanguageSelect: (SpeechLanguage) -> Unit,
    heardText: TextFieldValue,
    onHeardTextChange: (TextFieldValue) -> Unit,
    onSaveCorrection: () -> Unit,
    canSaveCorrection: Boolean,
    speakerNames: List<String>,
    speakerMode: SpeakerSetupMode,
    speakerAnalysisStatus: String,
    activeSpeakerIndex: Int,
    onSpeakerSelect: (Int) -> Unit,
    canChangeSpeaker: Boolean,
    captureActiveForSpeaker: Boolean,
    recognitionSettingsEnabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    canStart: Boolean,
    canPause: Boolean,
    canResume: Boolean,
    canStop: Boolean
) {
    val palette = LocalAppPalette.current
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(9.dp))
                            .background(Cyan.copy(alpha = 0.13f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Cyan, modifier = Modifier.size(17.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "Live Caption Console",
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            selectedLanguage.label,
                            color = palette.textMuted,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    StatusBadge(
                        text = when {
                            canPause -> "Live"
                            canResume -> "Paused"
                            canStop -> "Starting"
                            else -> "Ready"
                        },
                        color = if (canPause) Mint else palette.textMuted
                    )
                    SpeakingWobble(isSpeaking = isSpeaking, activityLevel = activityLevel)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier.weight(0.86f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CaptionEngineVisual(
                        isSpeaking = isSpeaking,
                        selectedLanguage = selectedLanguage,
                        compact = true
                    )
                    LinearProgressIndicator(
                        progress = { activityLevel.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Cyan,
                        trackColor = palette.panelSoft
                    )
                }

                Column(
                    modifier = Modifier.weight(1.45f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        "Live Caption",
                        color = palette.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 96.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.verticalGradient(palette.captionGradient))
                            .border(1.dp, Cyan.copy(alpha = 0.24f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = liveCaption,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textPrimary,
                            maxLines = 5,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            CurrentSpeakerSelector(
                speakerNames = speakerNames,
                speakerMode = speakerMode,
                speakerAnalysisStatus = speakerAnalysisStatus,
                activeSpeakerIndex = activeSpeakerIndex,
                onSpeakerSelect = onSpeakerSelect,
                enabled = canChangeSpeaker,
                captureActive = captureActiveForSpeaker,
                compact = true
            )

            SpeechOptionsPicker(
                selectedEngine = selectedEngine,
                selectedLanguage = selectedLanguage,
                availableLanguages = availableLanguages,
                onEngineChange = onEngineChange,
                onLanguageSelect = onLanguageSelect,
                enabled = recognitionSettingsEnabled,
                compact = true
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = heardText,
                    onValueChange = onHeardTextChange,
                    enabled = canSaveCorrection,
                    modifier = Modifier.weight(1f),
                    minLines = 1,
                    maxLines = 2,
                    label = { Text("Correct latest caption") },
                    colors = appTextFieldColors()
                )
                Button(
                    onClick = onSaveCorrection,
                    enabled = canSaveCorrection && heardText.text.isNotBlank(),
                    modifier = Modifier.height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apply", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        text = "Start",
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        color = Mint,
                        onClick = onStart,
                        enabled = canStart,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "Pause",
                        icon = { Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        color = Warm,
                        enabled = canPause,
                        onClick = onPause,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ActionButton(
                        text = "Resume",
                        icon = { Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        color = Lavender,
                        enabled = canResume,
                        onClick = onResume,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButton(
                        text = "Stop",
                        icon = { Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        color = Danger,
                        enabled = canStop,
                        onClick = onStop,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!canSaveCorrection) {
                Text(
                    "Stop capture after at least one caption to make a correction.",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    "Edit the latest caption, then apply the correction.",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CurrentSpeakerSelector(
    speakerNames: List<String>,
    speakerMode: SpeakerSetupMode,
    speakerAnalysisStatus: String,
    activeSpeakerIndex: Int,
    onSpeakerSelect: (Int) -> Unit,
    enabled: Boolean,
    captureActive: Boolean,
    compact: Boolean = false
) {
    val palette = LocalAppPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.panelSunken.copy(alpha = 0.40f))
            .border(1.dp, Lavender.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
            .padding(if (compact) 7.dp else 9.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
    ) {
        Text(
            if (speakerMode == SpeakerSetupMode.DYNAMIC) "Auto speakers" else "Speakers",
            color = palette.textPrimary,
            fontWeight = FontWeight.SemiBold,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall
        )
        if (compact) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                speakerNames.forEachIndexed { index, name ->
                    val selected = speakerMode == SpeakerSetupMode.PREDEFINED && index == activeSpeakerIndex
                    OutlinedButton(
                        onClick = { onSpeakerSelect(index) },
                        enabled = enabled,
                        modifier = Modifier
                            .height(34.dp)
                            .defaultMinSize(minWidth = 104.dp),
                        border = BorderStroke(1.dp, if (selected) Lavender.copy(alpha = 0.72f) else palette.border),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) Lavender.copy(alpha = 0.15f) else palette.field.copy(alpha = 0.74f),
                            contentColor = if (selected) Lavender else palette.textSecondary
                        ),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                        }
                        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            if (speakerMode == SpeakerSetupMode.PREDEFINED) {
                Text(
                    "Manual selection stays fixed until you tap another speaker.",
                    color = palette.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                speakerNames.forEachIndexed { index, name ->
                    val selected = speakerMode == SpeakerSetupMode.PREDEFINED && index == activeSpeakerIndex
                    OutlinedButton(
                        onClick = { onSpeakerSelect(index) },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        border = BorderStroke(1.dp, if (selected) Lavender else palette.border),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) Lavender.copy(alpha = 0.16f) else palette.field,
                            contentColor = if (selected) Lavender else palette.textSecondary
                        )
                    ) {
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(3.dp))
                        }
                        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (!compact) {
            Text(
                when {
                    speakerMode == SpeakerSetupMode.DYNAMIC ->
                        "$speakerAnalysisStatus. Distinct voices map to these names in first-heard order. Longer turns improve accuracy."
                    !enabled -> "Speaker selection is unavailable."
                    captureActive -> "Manual mode: the selected speaker stays active until you tap another name."
                    else -> "Manual mode: tap the speaker name before recording that person's turn."
                },
                color = palette.textMuted,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CaptionEngineVisual(
    isSpeaking: Boolean,
    selectedLanguage: SpeechLanguage,
    compact: Boolean = false
) {
    val palette = LocalAppPalette.current
    val transition = rememberInfiniteTransition(label = "engine-glow")
    val pulse by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "engine-pulse"
    )
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350),
            repeatMode = RepeatMode.Restart
        ),
        label = "engine-wave"
    )
    val voiceEnergy = if (isSpeaking) activityBoost(pulse) else 0f
    val scale = if (isSpeaking) 0.96f + voiceEnergy * 0.10f else 1f
    val tileShape = RoundedCornerShape(if (compact) 16.dp else 18.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 76.dp else 114.dp)
            .clip(tileShape)
            .background(
                Brush.verticalGradient(
                    if (palette.isDark) {
                        listOf(Color(0xFF201E35), Color(0xFF17182B))
                    } else {
                        listOf(Color(0xFFE9F7FB), Color(0xFFDEEAF8))
                    }
                )
            )
            .border(1.dp, Cyan.copy(alpha = if (isSpeaking) 0.30f else 0.14f), tileShape)
            .padding(if (compact) 7.dp else 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isSpeaking) {
            repeat(2) { index ->
                val progress = (wave + index * 0.50f) % 1f
                val baseSize = if (compact) 42 else 74
                val expansion = if (compact) 28 else 60
                val ringSize = (baseSize + progress * (expansion + voiceEnergy * 14)).dp
                val ringAlpha = ((1f - progress) * (0.18f + voiceEnergy * 0.24f))
                    .coerceIn(0.04f, 0.36f)
                Box(
                    modifier = Modifier
                        .size(ringSize)
                        .graphicsLayer { alpha = ringAlpha }
                        .clip(CircleShape)
                        .border(width = 2.dp, color = Cyan, shape = CircleShape)
                )
            }
        } else {
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    color = Cyan.copy(alpha = 0.055f),
                    radius = size.minDimension * 0.34f,
                    center = center
                )
                drawCircle(
                    color = Lavender.copy(alpha = 0.05f),
                    radius = size.minDimension * 0.45f,
                    center = center,
                    style = Stroke(width = 2f)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(if (compact) 44.dp else 68.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(RoundedCornerShape(if (compact) 15.dp else 20.dp))
                .background(
                    Brush.verticalGradient(
                        if (isSpeaking) {
                            listOf(Cyan.copy(alpha = 0.96f), Lavender.copy(alpha = 0.86f), Color(0xFF344064))
                        } else {
                            listOf(Color(0xFFE8FEFF), Color(0xFFD8E0FF), Color(0xFF8E85E8))
                        }
                    )
                )
                .border(1.dp, Color.White.copy(alpha = 0.62f), RoundedCornerShape(if (compact) 15.dp else 20.dp))
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(if (compact) 28.dp else 42.dp)
                    .clip(RoundedCornerShape(if (compact) 11.dp else 14.dp))
                    .background(
                        if (palette.isDark) {
                            palette.panelSunken.copy(alpha = 0.86f)
                        } else {
                            Color.White.copy(alpha = 0.74f)
                        }
                    )
                    .border(1.dp, Cyan.copy(alpha = 0.38f), RoundedCornerShape(if (compact) 11.dp else 14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = if (palette.isDark) palette.textPrimary else Color(0xFF18304A),
                    modifier = Modifier.size(if (compact) 17.dp else 24.dp)
                )
            }
        }
        Text(
            text = if (compact) selectedLanguage.shortLabel else selectedLanguage.label,
            modifier = Modifier
                .align(if (compact) Alignment.BottomCenter else Alignment.BottomEnd)
                .clip(RoundedCornerShape(50))
                .background(palette.panel.copy(alpha = if (palette.isDark) 0.90f else 0.82f))
                .border(1.dp, Cyan.copy(alpha = 0.20f), RoundedCornerShape(50))
                .padding(horizontal = if (compact) 7.dp else 9.dp, vertical = if (compact) 2.dp else 4.dp),
            color = palette.textPrimary,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun activityBoost(pulse: Float): Float {
    return ((pulse - 0.82f) / (1.08f - 0.82f)).coerceIn(0f, 1f)
}

@Composable
private fun SpeechOptionsPicker(
    selectedEngine: SpeechEngine,
    selectedLanguage: SpeechLanguage,
    availableLanguages: List<SpeechLanguage>,
    onEngineChange: (SpeechEngine) -> Unit,
    onLanguageSelect: (SpeechLanguage) -> Unit,
    enabled: Boolean,
    compact: Boolean = false
) {
    val palette = LocalAppPalette.current
    var languageMenuOpen by remember { mutableStateOf(false) }

    LaunchedEffect(enabled) {
        if (!enabled) languageMenuOpen = false
    }

    if (compact) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.panelSunken.copy(alpha = 0.42f))
                .border(1.dp, Cyan.copy(alpha = 0.16f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1.08f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "Recognition",
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpeechModeButton(
                        engine = SpeechEngine.SYSTEM_SERVICE,
                        selectedEngine = selectedEngine,
                        onEngineChange = onEngineChange,
                        enabled = enabled,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                    SpeechModeButton(
                        engine = SpeechEngine.OFFLINE_VOSK,
                        selectedEngine = selectedEngine,
                        onEngineChange = onEngineChange,
                        enabled = enabled,
                        compact = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(0.92f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    "Language",
                    fontWeight = FontWeight.SemiBold,
                    color = palette.textPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { languageMenuOpen = true },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Cyan.copy(alpha = 0.38f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                        contentPadding = ButtonDefaults.TextButtonContentPadding
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = Cyan, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = selectedLanguage.shortLabel,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                    }
                    DropdownMenu(
                        expanded = languageMenuOpen,
                        onDismissRequest = { languageMenuOpen = false },
                        modifier = Modifier
                            .background(palette.panel)
                            .border(1.dp, Cyan.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                    ) {
                        availableLanguages.forEach { language ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(language.label, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                language.modelNote,
                                                color = palette.textMuted,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (language == selectedLanguage) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = Mint)
                                        }
                                    }
                                },
                                onClick = {
                                    languageMenuOpen = false
                                    onLanguageSelect(language)
                                }
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panelSunken)
            .border(1.dp, Cyan.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
            .padding(if (compact) 7.dp else 12.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 8.dp)
    ) {
        Text(
            "Recognition mode",
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SpeechModeButton(
                engine = SpeechEngine.SYSTEM_SERVICE,
                selectedEngine = selectedEngine,
                onEngineChange = onEngineChange,
                enabled = enabled,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
            SpeechModeButton(
                engine = SpeechEngine.OFFLINE_VOSK,
                selectedEngine = selectedEngine,
                onEngineChange = onEngineChange,
                enabled = enabled,
                compact = compact,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            "Language",
            fontWeight = FontWeight.SemiBold,
            color = palette.textPrimary,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { languageMenuOpen = true },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                contentPadding = if (compact) ButtonDefaults.TextButtonContentPadding else ButtonDefaults.ButtonWithIconContentPadding
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = Cyan, modifier = Modifier.size(if (compact) 15.dp else 24.dp))
                Spacer(Modifier.width(if (compact) 4.dp else 8.dp))
                Text(
                    text = if (compact) selectedLanguage.shortLabel else selectedLanguage.label,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Cyan)
            }
            DropdownMenu(
                expanded = languageMenuOpen,
                onDismissRequest = { languageMenuOpen = false },
                modifier = Modifier
                    .background(palette.panel)
                    .border(1.dp, Cyan.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            ) {
                availableLanguages.forEach { language ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(language.label, color = palette.textPrimary, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        language.modelNote,
                                        color = palette.textMuted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (language == selectedLanguage) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Mint)
                                }
                            }
                        },
                        onClick = {
                            languageMenuOpen = false
                            onLanguageSelect(language)
                        }
                    )
                }
            }
        }
        if (!compact) {
            Text(
                text = if (enabled) {
                    "${selectedEngine.detail}. ${selectedLanguage.modelNote}."
                } else {
                    "Recognition settings are locked during live capture to prevent an audio gap."
                },
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }
    }
}

@Composable
private fun SpeechModeButton(
    engine: SpeechEngine,
    selectedEngine: SpeechEngine,
    onEngineChange: (SpeechEngine) -> Unit,
    enabled: Boolean,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val selected = engine == selectedEngine
    val palette = LocalAppPalette.current
    val color = if (selected) Cyan else palette.border

    OutlinedButton(
        onClick = { onEngineChange(engine) },
        enabled = enabled,
        modifier = modifier.height(if (compact) 34.dp else 48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = if (selected) 0.8f else 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) Cyan.copy(alpha = 0.14f) else palette.panelSunken,
            contentColor = if (selected) Cyan else palette.textSecondary
        )
    ) {
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(if (compact) 14.dp else 18.dp))
            Spacer(Modifier.width(if (compact) 1.dp else 4.dp))
        }
        Text(
            text = if (compact) {
                when (engine) {
                    SpeechEngine.SYSTEM_SERVICE -> "Phone"
                    SpeechEngine.OFFLINE_VOSK -> "Local"
                }
            } else {
                engine.label
            },
            style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun appTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = LocalAppPalette.current.textPrimary,
    unfocusedTextColor = LocalAppPalette.current.textPrimary,
    focusedContainerColor = LocalAppPalette.current.field,
    unfocusedContainerColor = LocalAppPalette.current.field,
    focusedBorderColor = Cyan,
    unfocusedBorderColor = LocalAppPalette.current.border,
    focusedLabelColor = Cyan,
    unfocusedLabelColor = LocalAppPalette.current.textSecondary,
    cursorColor = Cyan
)

@Composable
private fun SpeakingWobble(isSpeaking: Boolean, activityLevel: Double) {
    val transition = rememberInfiniteTransition(label = "speaking-wave")
    val wobble by transition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 280),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wobble"
    )
    val scale = if (isSpeaking) 1f + activityLevel.toFloat().coerceIn(0.05f, 0.35f) else 1f
    val color = if (isSpeaking) Color(0xFF16A34A) else MaterialTheme.colorScheme.surfaceVariant

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(7.dp)
                    .height((18 + index * 5).dp)
                    .graphicsLayer {
                        scaleY = if (isSpeaking) scale + (index * 0.05f) else 0.55f
                        translationY = if (isSpeaking) wobble * (if (index % 2 == 0) 1 else -1) else 0f
                    }
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    icon: @Composable () -> Unit,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(if (compact) 38.dp else 52.dp)
            .defaultMinSize(minWidth = if (compact) 48.dp else 76.dp),
        shape = RoundedCornerShape(if (compact) 10.dp else 8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color(0xFF07111F),
            disabledContainerColor = color.copy(alpha = 0.22f),
            disabledContentColor = Color.White.copy(alpha = 0.42f)
        ),
        contentPadding = if (compact) ButtonDefaults.TextButtonContentPadding else ButtonDefaults.ButtonWithIconContentPadding
    ) {
        icon()
        Spacer(Modifier.width(if (compact) 3.dp else 5.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ResponsePanel(
    text: TextFieldValue,
    suggestions: List<WordSuggestion>,
    generatedReply: String?,
    generatedReplyBaseTurnCount: Int?,
    generatedReplyStale: Boolean,
    isGeneratingReply: Boolean,
    continuousCaptureActive: Boolean,
    onTextChange: (TextFieldValue) -> Unit,
    onSuggestion: (WordSuggestion) -> Unit,
    onUpdateConversation: () -> Unit,
    onGenerateReply: () -> Unit,
    onUseGeneratedReply: (String) -> Unit,
    onSpeak: () -> Unit
) {
    val palette = LocalAppPalette.current
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = BorderStroke(1.dp, Lavender.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Your Response", fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                label = { Text("Type intended words or a complete response") },
                supportingText = {
                    Text(
                        if (continuousCaptureActive) {
                            "Live capture continues while you prepare a reply."
                        } else {
                            "Write a short intent, then generate a polished reply."
                        }
                    )
                },
                colors = appTextFieldColors()
            )
            if (continuousCaptureActive) {
                Text(
                    "Live capture remains active while you write or generate a reply.",
                    color = Mint,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (suggestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Next-word suggestions",
                        color = palette.textMuted,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            OutlinedButton(
                                onClick = { onSuggestion(suggestion) },
                                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.4f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Text(suggestion.word)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onUpdateConversation,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                enabled = text.text.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Mint,
                    contentColor = Color(0xFF07111F)
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Update Conversation & Summary")
            }
            OutlinedButton(
                onClick = onGenerateReply,
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                enabled = text.text.isNotBlank() && !isGeneratingReply,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Warm.copy(alpha = 0.7f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Warm)
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    when {
                        isGeneratingReply -> "Generating Reply..."
                        generatedReplyStale -> "Refresh Generated Reply"
                        else -> "Generate Reply"
                    }
                )
            }
            if (generatedReply != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = palette.panelSunken),
                    border = BorderStroke(1.dp, Warm.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Generated Reply", fontWeight = FontWeight.Bold, color = Warm)
                        if (generatedReplyBaseTurnCount != null) {
                            Text(
                                "Prepared from the latest conversation context",
                                color = palette.textMuted,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        if (generatedReplyStale) {
                            Text(
                                "New conversation was captured after this draft. Refresh it before use.",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Danger.copy(alpha = 0.12f))
                                    .padding(8.dp),
                                color = Danger,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        SelectionContainer {
                            Text(generatedReply, color = palette.textPrimary, style = MaterialTheme.typography.bodyLarge)
                        }
                        Text(
                            "Private AI draft. Review before speaking or saving.",
                            color = palette.textMuted,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = onGenerateReply,
                                enabled = !isGeneratingReply,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Cyan.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Cyan)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Try Again", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            OutlinedButton(
                                onClick = { onUseGeneratedReply(generatedReply) },
                                enabled = !generatedReplyStale,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Warm.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Warm)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                            Text("Use Draft", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onSpeak,
                modifier = Modifier.fillMaxWidth(),
                enabled = text.text.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Lavender, contentColor = Color(0xFF0E1020))
            ) {
                Icon(Icons.Default.RecordVoiceOver, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Speak and Add to Transcript")
            }
        }
    }
}

@Composable
private fun SummaryPanel(
    summary: String,
    methodLabel: String,
    detail: String,
    isUpdating: Boolean
) {
    val palette = LocalAppPalette.current
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = BorderStroke(1.dp, Warm.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Conversation Gist", fontWeight = FontWeight.Bold, color = palette.textPrimary)
                    Text(
                        if (isUpdating) "Updating privately…" else methodLabel,
                        color = palette.textMuted,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isUpdating) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Mint))
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.panelSunken)
                    .border(1.dp, Warm.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 11.dp, vertical = 9.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TranscriptPanel(
    modifier: Modifier = Modifier,
    entries: List<ConversationManager.Turn>,
    shareEnabled: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalAppPalette.current
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        ConfirmationDialog(
            title = "Delete transcript?",
            message = "This removes committed transcript history. Speaker setup, typed response, live capture, and any in-progress utterance are kept.",
            confirmLabel = "Delete Transcript",
            confirmColor = Danger,
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Complete Transcript", fontWeight = FontWeight.SemiBold, color = palette.textPrimary)
            Text(
                if (entries.isEmpty()) "No saved turns" else "${entries.size} conversation ${if (entries.size == 1) "turn" else "turns"}",
                color = palette.textMuted,
                style = MaterialTheme.typography.labelMedium
            )
            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.panelSunken)
                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                        .padding(14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        "Transcript appears here as the conversation grows.",
                        color = palette.textMuted
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    entries.forEachIndexed { index, turn ->
                        TranscriptRow(turn = turn, index = index)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TranscriptActionButton(
                    text = "Save",
                    color = Color(0xFF0F9F8F),
                    enabled = entries.isNotEmpty(),
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.Save, contentDescription = null) }
                )
                TranscriptActionButton(
                    text = "Share",
                    color = Color(0xFF4F7CEB),
                    enabled = entries.isNotEmpty() && shareEnabled,
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
                TranscriptActionButton(
                    text = "Delete",
                    color = Danger,
                    enabled = entries.isNotEmpty(),
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f),
                    icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
            if (entries.isNotEmpty() && !shareEnabled) {
                Text(
                    "Share is available after capture stops because opening another Android screen would suspend the foreground microphone.",
                    color = Warm,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun TranscriptActionButton(
    text: String,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 50.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = color.copy(alpha = 0.18f),
            disabledContentColor = Color.White.copy(alpha = 0.35f)
        ),
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding
    ) {
        icon()
        Spacer(Modifier.width(5.dp))
        Text(text, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SessionActionsPanel(
    onResetSession: () -> Unit,
    onStopSession: () -> Unit
) {
    val palette = LocalAppPalette.current
    var pendingAction by rememberSaveable { mutableStateOf<String?>(null) }

    when (pendingAction) {
        "reset" -> ConfirmationDialog(
            title = "Reset this session?",
            message = "This clears the transcript, summary, response draft, generated reply, and local history. Your speaker setup stays ready.",
            confirmLabel = "Reset Session",
            confirmColor = Warm,
            onConfirm = {
                pendingAction = null
                onResetSession()
            },
            onDismiss = { pendingAction = null }
        )
        "stop" -> ConfirmationDialog(
            title = "Stop current session?",
            message = "This stops microphone and speech output, deletes all current session data, and returns to Session Setup.",
            confirmLabel = "Stop & Return",
            confirmColor = Danger,
            onConfirm = {
                pendingAction = null
                onStopSession()
            },
            onDismiss = { pendingAction = null }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = palette.panel.copy(alpha = 0.90f)),
        border = BorderStroke(1.dp, palette.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { pendingAction = "reset" },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Warm.copy(alpha = 0.62f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Warm)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Reset", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Button(
                onClick = { pendingAction = "stop" },
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Danger,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text("Stop", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmColor,
                    contentColor = Color.White
                )
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TranscriptRow(turn: ConversationManager.Turn, index: Int) {
    val palette = LocalAppPalette.current
    val isUser = turn.speaker.equals("You", ignoreCase = true)
    val accent = when {
        turn.needsPlaybackReview -> Warm
        isUser -> Lavender
        else -> Cyan
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.panelSunken)
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                color = accent,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Spacer(Modifier.width(10.dp))
        SelectionContainer {
            Column {
                Text(turn.speaker, color = accent, fontWeight = FontWeight.SemiBold)
                if (turn.needsPlaybackReview) {
                    Text(
                        "Review: captured during app reply playback",
                        color = Warm,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(turn.text, style = MaterialTheme.typography.bodyLarge, color = palette.textPrimary)
            }
        }
    }
}
