package com.aihearingassist

enum class InSessionAction {
    EDIT_RESPONSE,
    UPDATE_CONVERSATION,
    GENERATE_REPLY,
    USE_GENERATED_REPLY,
    SAVE_TRANSCRIPT,
    SHARE_TRANSCRIPT,
    CHANGE_RECOGNITION_SETTINGS
}

/** Defines which UI work is safe without interrupting an active microphone. */
object ContinuousCapturePolicy {
    fun keepsListening(action: InSessionAction): Boolean {
        return when (action) {
            InSessionAction.EDIT_RESPONSE,
            InSessionAction.UPDATE_CONVERSATION,
            InSessionAction.GENERATE_REPLY,
            InSessionAction.USE_GENERATED_REPLY,
            InSessionAction.SAVE_TRANSCRIPT -> true

            InSessionAction.SHARE_TRANSCRIPT,
            InSessionAction.CHANGE_RECOGNITION_SETTINGS -> false
        }
    }

    fun status(message: String, captureState: String): String {
        return if (captureState == "Listening") {
            "$message Live capture continues."
        } else {
            message
        }
    }
}

/**
 * Freezes the selected manual speaker when the first partial text arrives.
 * A later speaker-button tap therefore applies to the next utterance.
 */
class SpeakerTurnTracker {
    private var pendingSpeaker: String? = null

    fun observePartial(selectedSpeaker: String) {
        if (pendingSpeaker == null) pendingSpeaker = selectedSpeaker
    }

    fun consumeSpeaker(selectedSpeaker: String): String {
        val speaker = pendingSpeaker ?: selectedSpeaker
        pendingSpeaker = null
        return speaker
    }

    fun clearPending() {
        pendingSpeaker = null
    }

    fun pendingSpeaker(): String? = pendingSpeaker
}

/**
 * Flags a normalized exact match to the app's own recent TTS reply.
 * Matching text is retained for review, never silently discarded; this is not
 * acoustic echo cancellation.
 */
class SelfSpeechEchoGuard(
    private val clockMillis: () -> Long = System::currentTimeMillis
) {
    private var expectedSpeech = ""
    private var expiresAtMillis = 0L

    @Synchronized
    fun arm(text: String) {
        expectedSpeech = normalize(text)
        val estimatedPlayback = (text.split(Regex("\\s+")).size * 650L + 2_500L)
            .coerceIn(MIN_MONITOR_MILLIS, MAX_MONITOR_MILLIS)
        expiresAtMillis = clockMillis() + estimatedPlayback
    }

    @Synchronized
    fun markPlaybackFinished() {
        if (expectedSpeech.isNotBlank()) {
            expiresAtMillis = clockMillis() + FINAL_RESULT_GRACE_MILLIS
        }
    }

    @Synchronized
    fun cancel() {
        expectedSpeech = ""
        expiresAtMillis = 0L
    }

    @Synchronized
    fun isLikelyPlaybackEcho(candidate: String): Boolean {
        if (expectedSpeech.isBlank()) return false
        if (clockMillis() > expiresAtMillis) {
            cancel()
            return false
        }
        return normalize(candidate) == expectedSpeech
    }

    @Synchronized
    fun isMonitoring(): Boolean {
        if (clockMillis() > expiresAtMillis) {
            cancel()
            return false
        }
        return expectedSpeech.isNotBlank()
    }

    private fun normalize(text: String): String {
        return TOKEN_REGEX.findAll(text.lowercase())
            .joinToString(" ") { it.value }
    }

    private companion object {
        val TOKEN_REGEX = Regex("[\\p{L}\\p{M}\\p{N}']+")
        const val MIN_MONITOR_MILLIS = 4_000L
        const val MAX_MONITOR_MILLIS = 30_000L
        const val FINAL_RESULT_GRACE_MILLIS = 1_500L
    }
}
