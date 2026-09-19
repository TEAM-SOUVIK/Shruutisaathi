import com.aihearingassist.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureLogicTest {
    @Test
    fun responseAndSummaryActions_keepContinuousCaptureActive() {
        val safeActions = listOf(
            InSessionAction.EDIT_RESPONSE,
            InSessionAction.UPDATE_CONVERSATION,
            InSessionAction.GENERATE_REPLY,
            InSessionAction.USE_GENERATED_REPLY,
            InSessionAction.SAVE_TRANSCRIPT
        )

        assertTrue(safeActions.all(ContinuousCapturePolicy::keepsListening))
        assertTrue(!ContinuousCapturePolicy.keepsListening(InSessionAction.SHARE_TRANSCRIPT))
        assertTrue(!ContinuousCapturePolicy.keepsListening(InSessionAction.CHANGE_RECOGNITION_SETTINGS))
    }

    @Test
    fun speakerSwitchDuringPartial_appliesOnlyToNextUtterance() {
        val tracker = SpeakerTurnTracker()
        tracker.observePartial("Speaker 1")

        assertEquals("Speaker 1", tracker.consumeSpeaker("Speaker 2"))
        assertEquals("Speaker 2", tracker.consumeSpeaker("Speaker 2"))
    }

    @Test
    fun manualSpeakerSelection_staysFixedUntilUserChangesIt() {
        val tracker = SpeakerTurnTracker()

        assertEquals("Speaker 1", tracker.consumeSpeaker("Speaker 1"))
        assertEquals("Speaker 1", tracker.consumeSpeaker("Speaker 1"))
        assertEquals("Speaker 2", tracker.consumeSpeaker("Speaker 2"))
    }

    @Test
    fun exactOwnTtsEcho_isFlaggedButUnrelatedSpeechIsKept() {
        var now = 1_000L
        val guard = SelfSpeechEchoGuard { now }
        guard.arm("Yes, I can attend tomorrow.")

        assertTrue(guard.isLikelyPlaybackEcho("yes i can attend tomorrow"))
        assertTrue(!guard.isLikelyPlaybackEcho("The meeting moved to Friday"))
        assertTrue(!guard.isLikelyPlaybackEcho("Yes I can attend tomorrow and bring the report"))
    }

    @Test
    fun ownTtsWordsAfterReviewWindow_areNotFlagged() {
        var now = 5_000L
        val guard = SelfSpeechEchoGuard { now }
        guard.arm("Please repeat that")
        guard.markPlaybackFinished()
        now += 1_501L

        assertTrue(!guard.isLikelyPlaybackEcho("Please repeat that"))
    }

    @Test
    fun playbackReviewFlag_survivesTranscriptPersistence() {
        val manager = ConversationManager()
        manager.addTurn("Speaker 1", "Yes I can attend", needsPlaybackReview = true)

        val restored = ConversationManager().apply { restoreFromTranscript(manager.transcript()) }

        assertTrue(restored.entries.single().needsPlaybackReview)
        assertTrue(restored.transcript().contains("Review: reply playback overlap"))
    }

    @Test
    fun ruleBasedPredictor_offersKnownPhraseSuggestions() {
        val conversation = "I want to book a train ticket. I want to travel tomorrow."
        val predictor = NextWordPredictor()
        val suggestions = predictor.predict(conversation, "I want to")
        assertTrue(suggestions.any { it.word == "travel" || it.word == "book" })
    }

    @Test
    fun predictor_returnsNoSuggestionsForBlankDraft() {
        val suggestions = NextWordPredictor().predict("I want to travel", "   ")

        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun predictor_replacesPartialWordInsteadOfDuplicatingIt() {
        val suggestion = NextWordPredictor()
            .predict(emptyList(), "Please inter")
            .first { it.word == "interview" }

        assertEquals("Please interview ", suggestion.applyTo("Please inter"))
    }

    @Test
    fun predictor_appendsNextWordWithSingleSpacing() {
        val suggestion = NextWordPredictor()
            .predict(emptyList(), "I want")
            .first { it.word == "to" }

        assertEquals("I want to ", suggestion.applyTo("I want"))
    }

    @Test
    fun predictor_preservesHindiCombiningMarksWhenLearningTurns() {
        val suggestions = NextWordPredictor().predict(
            turnTexts = listOf("मुझे टिकट चाहिए"),
            currentText = "मुझे "
        )

        assertTrue(suggestions.any { it.word == "टिकट" })
    }

    @Test
    fun replyGenerator_doesNotStrengthenShortIntent() {
        val generator = OfflineReplyGenerator()
        val question = ConversationManager.Turn("Speaker 1", "Can you attend tomorrow?")

        assertEquals(
            "Yes.",
            generator.generate(ReplyContext(listOf(question), "Meeting tomorrow.", "yes"))
        )
        assertEquals(
            "Okay.",
            generator.generate(ReplyContext(listOf(question), "Meeting tomorrow.", "okay"))
        )
    }

    @Test
    fun replyGenerator_usesLatestQuestionOnlyForSafeRepeatWording() {
        val result = OfflineReplyGenerator().generate(
            ReplyContext(
                turns = listOf(ConversationManager.Turn("Speaker 2", "What time should we meet?")),
                summary = "They are arranging a meeting.",
                intendedText = "repeat"
            )
        )

        assertEquals("Could you please repeat the question?", result)
    }

    @Test
    fun meetingDecisionFlow_preservesTranscriptGistAndDraftsReply() {
        val turns = listOf(
            ConversationManager.Turn("Speaker One", "Can we meet at three?"),
            ConversationManager.Turn("Speaker Two", "I have a class then. Can we make it four?"),
            ConversationManager.Turn("Speaker One", "Yes, four works.")
        )

        val gist = OfflineSummaryBuilder().buildSummary(turns)
        val reply = OfflineReplyGenerator().generate(
            ReplyContext(
                turns = turns,
                summary = gist,
                intendedText = "four okay, send room"
            )
        )

        assertEquals("The meeting is now at four o'clock.", gist)
        assertEquals("Four o'clock works for me. Please send me the room number.", reply)
    }

    @Test
    fun replyGeneration_doesNotMutateTranscript() {
        val manager = ConversationManager().apply {
            addTurn("Speaker 1", "Can you attend tomorrow?")
        }
        val before = manager.transcript()

        OfflineReplyGenerator().generate(
            ReplyContext(manager.entries.toList(), "Meeting tomorrow.", "yes")
        )

        assertEquals(before, manager.transcript())
    }

    @Test
    fun summaryBuilder_extractsCoreIntentFromConversation() {
        val conversation = "Person 1: Would you like to go to the restaurant tonight? Person 2: Yes. What time? Person 1: Let's go at 8 PM. Person 2: Okay."
        val summary = OfflineSummaryBuilder().buildSummary(conversation)

        assertTrue(summary.contains("restaurant") || summary.contains("8 PM"))
    }

    @Test
    fun summaryBuilder_extractsHindiIntentFromConversation() {
        val conversation = "Speaker 1: क्या ट्रेन टिकट बुक हो गया? Speaker 2: हाँ टिकट बुक है।"
        val summary = OfflineSummaryBuilder().buildSummary(conversation)

        assertTrue(summary.contains("ट्रेन") || summary.contains("टिकट"))
    }

    @Test
    fun summaryBuilder_splitsSpeakerLabelsOnSameLine() {
        val conversation = "Speaker 1: Meet the doctor at 7 PM Speaker 2: Okay I will come"
        val summary = OfflineSummaryBuilder().buildSummary(conversation)

        assertTrue(summary.contains("doctor"))
        assertTrue(summary.contains("agreed") || summary.contains("7 PM"))
    }

    @Test
    fun summaryBuilder_stripsSequentialTurnLabels() {
        val conversation = "Turn 1: Meet the doctor at 7 PM Turn 2: Okay I will come"
        val summary = OfflineSummaryBuilder().buildSummary(conversation)

        assertTrue(summary.contains("doctor"))
        assertTrue(!summary.contains("Turn 1:"))
        assertTrue(!summary.contains("Turn 2:"))
    }

    @Test
    fun summaryBuilder_reportsEmptyConversation() {
        val summary = OfflineSummaryBuilder().buildSummary("   ")

        assertEquals("No conversation available yet.", summary)
    }

    @Test
    fun summaryBuilder_returnsAtMostTwoExtractedIdeas() {
        val summary = OfflineSummaryBuilder().buildSummary(
            "Speaker 1: We need a job. Speaker 2: The interview is tomorrow. " +
                "Speaker 3: Call me at 8 PM. Speaker 1: The office address changed."
        )

        val sentenceCount = Regex("[.!?।]").findAll(summary).count()
        assertTrue(sentenceCount <= 1)
        assertTrue(summary.contains("office address") || summary.contains("changed"))
    }

    @Test
    fun structuredSummary_doesNotTreatCustomSpeakerNameAsContent() {
        val turns = listOf(
            ConversationManager.Turn("Riya", "Finding a job is difficult."),
            ConversationManager.Turn("Arun", "The interview is tomorrow.")
        )

        val summary = OfflineSummaryBuilder().buildSummary(turns)

        assertTrue(!summary.contains("Riya"))
        assertTrue(!summary.contains("Arun"))
        assertTrue(summary.contains("job") || summary.contains("interview"))
    }

    @Test
    fun structuredSummary_isActualGistNotParticipantCounting() {
        val turns = listOf(
            ConversationManager.Turn("Speaker 1", "The institute invited a consulting company to campus."),
            ConversationManager.Turn("Speaker 2", "Students are discussing job opportunities and interview timing."),
            ConversationManager.Turn("Speaker 3", "They want the company name before going to college.")
        )

        val summary = OfflineSummaryBuilder().buildSummary(turns)

        assertTrue(summary.contains("institute") || summary.contains("company") || summary.contains("job"))
        assertTrue(!summary.startsWith("3 participants"))
    }

    @Test
    fun summaryBuilder_handlesRowanAtkinsonConversationSpecifically() {
        val turns = listOf(
            ConversationManager.Turn("Amit", "do you know one Atkinson has an electrical degree"),
            ConversationManager.Turn("Rahul", "wow he please Mr Bean so well it's hard to believe"),
            ConversationManager.Turn("Priya", "true his silence comedies pure genius"),
            ConversationManager.Turn("Amit", "he is also love supercars in real life"),
            ConversationManager.Turn("Rahul", "that's funny since Mr Beans only drive many time Cooper"),
            ConversationManager.Turn("Priya", "he is truly a brilliant actor")
        )

        val summary = OfflineSummaryBuilder().buildSummary(turns)

        assertTrue(summary.contains("Atkinson") || summary.contains("Bean"))
        assertTrue(summary.contains("brilliant actor") || summary.contains("actor"))
        assertTrue(Regex("[.!?।]").findAll(summary).count() <= 1)
        assertTrue(!summary.contains("Amit"))
        assertTrue(!summary.contains("Rahul"))
        assertTrue(!summary.contains("Priya"))
        assertTrue(!summary.contains("gist includes", ignoreCase = true))
    }

    @Test
    fun conversationManager_preservesTranscriptAndSupportsReset() {
        val manager = ConversationManager()
        manager.addTurn("Speaker 1", "Hello there")
        manager.addTurn("Speaker 2", "Hi")
        assertEquals(2, manager.entries.size)
        manager.clear()
        assertTrue(manager.entries.isEmpty())
    }

    @Test
    fun conversationManager_restoresSavedTranscript() {
        val manager = ConversationManager()
        manager.restoreFromTranscript("Speaker 1: Hello\nYou: I can join")

        assertEquals(2, manager.entries.size)
        assertEquals("Speaker 1", manager.entries.first().speaker)
        assertEquals("I can join", manager.entries.last().text)
    }

    @Test
    fun conversationManager_updatesCaptionWithoutAddingDuplicateTurn() {
        val manager = ConversationManager()
        manager.addTurn("Speaker 1", "Incorrect caption")

        assertTrue(manager.updateTurnText(0, "Corrected caption"))
        assertEquals(1, manager.entries.size)
        assertEquals("Corrected caption", manager.entries.single().text)
    }

    @Test
    fun sessionSetupValidator_requiresPermissionAndUniqueSpeakerNames() {
        assertTrue(
            SessionSetupValidator.validate(
                privacyAccepted = true,
                microphoneGranted = false,
                mode = SpeakerSetupMode.PREDEFINED,
                speakerNames = listOf("Speaker 1", "Speaker 2")
            )!!.contains("Microphone")
        )
        assertTrue(
            SessionSetupValidator.validate(
                privacyAccepted = true,
                microphoneGranted = true,
                mode = SpeakerSetupMode.PREDEFINED,
                speakerNames = listOf("Alex", "alex")
            )!!.contains("different")
        )
        assertTrue(
            SessionSetupValidator.validate(
                privacyAccepted = true,
                microphoneGranted = true,
                mode = SpeakerSetupMode.PREDEFINED,
                speakerNames = listOf("Alex: Team", "Riya")
            )!!.contains("colon")
        )
        assertEquals(
            null,
            SessionSetupValidator.validate(
                privacyAccepted = true,
                microphoneGranted = true,
                mode = SpeakerSetupMode.PREDEFINED,
                speakerNames = listOf("Speaker 1", "Speaker 2")
            )
        )
    }

    @Test
    fun sessionSetupValidator_requiresAtLeastTwoDynamicVoiceClusters() {
        val oneClusterError = SessionSetupValidator.validate(
            privacyAccepted = true,
            microphoneGranted = true,
            mode = SpeakerSetupMode.DYNAMIC,
            speakerNames = listOf("Speaker 1")
        )

        assertTrue(oneClusterError!!.contains("at least 2"))

        val error = SessionSetupValidator.validate(
            privacyAccepted = true,
            microphoneGranted = true,
            mode = SpeakerSetupMode.DYNAMIC,
            speakerNames = listOf("Speaker 1", "Speaker 2")
        )

        assertEquals(null, error)
    }

    @Test
    fun onlineDiarizer_keepsSimilarVoiceAndCreatesDistinctSpeaker() {
        val diarizer = OnlineSpeakerDiarizer(listOf("Speaker 1", "Speaker 2"))

        val first = diarizer.assign(doubleArrayOf(1.0, 0.0, 0.0), 240, "Speaker 1")
        val sameVoice = diarizer.assign(doubleArrayOf(0.98, 0.08, 0.0), 220, "Speaker 1")
        val otherVoice = diarizer.assign(doubleArrayOf(0.0, 1.0, 0.0), 260, "Speaker 1")

        assertEquals("Speaker 1", first.label)
        assertEquals("Speaker 1", sameVoice.label)
        assertEquals("Speaker 2", otherVoice.label)
        assertEquals(2, diarizer.discoveredSpeakerCount())
    }

    @Test
    fun onlineDiarizer_shortSegmentWithoutEmbeddingUsesLastVoice() {
        val diarizer = OnlineSpeakerDiarizer(listOf("Riya", "Arun"))
        diarizer.assign(doubleArrayOf(0.0, 1.0), 200, "Riya")

        val shortSegment = diarizer.assign(null, 0, "Speaker 1")

        assertEquals("Riya", shortSegment.label)
        assertTrue(!shortSegment.usedVoiceEmbedding)
    }

    @Test
    fun sessionSetupValidator_rejectsReservedNamesAndNormalizesSpacing() {
        val error = SessionSetupValidator.validate(
            privacyAccepted = true,
            microphoneGranted = true,
            mode = SpeakerSetupMode.PREDEFINED,
            speakerNames = listOf("You", "Riya")
        )

        assertTrue(error!!.contains("Choose names"))
        assertEquals("Speaker One", SessionSetupValidator.normalizeDisplayName("  Speaker   One  "))
    }

    @Test
    fun turnLabeler_assignsNeutralSequentialLabels() {
        val labeler = TurnLabeler()

        assertEquals("Turn 1", labeler.nextLabel(emptyList()))
        assertEquals("Turn 2", labeler.nextLabel(listOf("Turn 1")))
        assertEquals("Turn 4", labeler.nextLabel(listOf("Turn 1", "You", "Saved", "Turn 3")))
    }

    @Test
    fun conversationManager_flattensMultilineTurnsForSafeRestore() {
        val manager = ConversationManager()
        manager.addTurn("You", "First line\nSecond line")

        assertEquals("You: First line Second line", manager.transcript())

        val restored = ConversationManager()
        restored.restoreFromTranscript(manager.transcript())
        assertEquals(1, restored.entries.size)
        assertEquals("You", restored.entries.single().speaker)
    }

    @Test
    fun defaultSpeechEngine_isOfflineVosk() {
        assertEquals(SpeechEngine.OFFLINE_VOSK, SpeechEngine.DEFAULT)
    }

    @Test
    fun indianEnglish_usesEightKhzModelRate() {
        assertEquals(8_000f, SpeechLanguage.ENGLISH_IN.sampleRateHz, 0f)
    }

    @Test
    fun usEnglish_usesSixteenKhzModelRate() {
        assertEquals(16_000f, SpeechLanguage.ENGLISH_US.sampleRateHz, 0f)
    }

    @Test
    fun offlineLanguages_excludeSystemOnlyAccents() {
        val languages = SpeechLanguage.availableEntries(SpeechEngine.OFFLINE_VOSK)

        assertTrue(!languages.contains(SpeechLanguage.ENGLISH_UK))
        assertTrue(!languages.contains(SpeechLanguage.ENGLISH_AU))
    }

    @Test
    fun bhojpuriUsesBundledHindiModel() {
        assertEquals(SpeechLanguage.HINDI.assetFolder, SpeechLanguage.BHOJPURI.assetFolder)
        assertEquals(SpeechLanguage.HINDI.storageFolder, SpeechLanguage.BHOJPURI.storageFolder)
    }

    @Test
    fun hinglishUsesHindiRecognizerWithRomanOutput() {
        assertEquals(SpeechLanguage.HINDI.assetFolder, SpeechLanguage.HINGLISH.assetFolder)
        assertTrue(SpeechLanguage.HINGLISH.hinglishOutput)

        val converted = SpeechTextNormalizer.normalize("नमस्ते टिकट बुक है", SpeechLanguage.HINGLISH)
        assertTrue(converted.contains("namaste"))
        assertTrue(converted.contains("tikat"))
        assertTrue(converted.contains("buk"))
    }

    @Test
    fun indianEnglishUsesIndianEnglishModel() {
        assertEquals("model-en-in", SpeechLanguage.ENGLISH_IN.assetFolder)
        assertEquals("model-en-in", SpeechLanguage.ENGLISH_IN.storageFolder)
        assertEquals("en-IN", SpeechLanguage.ENGLISH_IN.recognitionTag)
    }

    @Test
    fun accentModesCoverBundledOfflineModels() {
        val assetFolders = SpeechLanguage.entries.map { it.assetFolder }.toSet()

        assertTrue(assetFolders.contains("model-en-us"))
        assertTrue(assetFolders.contains("model-en-in"))
        assertTrue(assetFolders.contains("model-hi"))
        assertEquals(7, SpeechLanguage.entries.size)
    }

    @Test
    fun systemServiceModeOffersExtraEnglishAccents() {
        val languages = SpeechLanguage.availableEntries(SpeechEngine.SYSTEM_SERVICE)

        assertTrue(languages.contains(SpeechLanguage.ENGLISH_UK))
        assertTrue(languages.contains(SpeechLanguage.ENGLISH_AU))
        assertTrue(languages.contains(SpeechLanguage.HINGLISH))
    }
}
