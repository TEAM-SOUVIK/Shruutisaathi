package com.aihearingassist

enum class SpeakerSetupMode {
    PREDEFINED,
    DYNAMIC
}

data class SessionSetup(
    val speakerMode: SpeakerSetupMode,
    val speakerNames: List<String>
)

object SessionSetupValidator {
    const val MIN_SPEAKERS = 1
    const val MIN_DYNAMIC_SPEAKERS = 2
    const val MAX_SPEAKERS = 8
    const val DYNAMIC_SPEAKER_AVAILABLE = true
    const val MAX_NAME_LENGTH = 32
    private val whitespaceRegex = Regex("\\s+")
    private val turnLabelRegex = Regex("turn\\s+\\d+", RegexOption.IGNORE_CASE)
    private val reservedNames = setOf("you", "saved", "raw")

    fun defaultSpeakerNames(count: Int): List<String> {
        val safeCount = count.coerceIn(MIN_SPEAKERS, MAX_SPEAKERS)
        return (1..safeCount).map { "Speaker $it" }
    }

    fun resizeSpeakerNames(existing: List<String>, count: Int): List<String> {
        val safeCount = count.coerceIn(MIN_SPEAKERS, MAX_SPEAKERS)
        return (0 until safeCount).map { index ->
            existing.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "Speaker ${index + 1}"
        }
    }

    fun validate(
        privacyAccepted: Boolean,
        microphoneGranted: Boolean,
        mode: SpeakerSetupMode,
        speakerNames: List<String>
    ): String? {
        if (!privacyAccepted) {
            return "Please accept microphone use and private on-device transcript storage."
        }
        if (!microphoneGranted) {
            return "Microphone permission is missing. Tap Allow microphone and approve the Android prompt."
        }
        if (speakerNames.size !in MIN_SPEAKERS..MAX_SPEAKERS) {
            return "Choose between $MIN_SPEAKERS and $MAX_SPEAKERS speakers."
        }
        if (mode == SpeakerSetupMode.DYNAMIC && speakerNames.size < MIN_DYNAMIC_SPEAKERS) {
            return "Automatic speaker separation needs at least $MIN_DYNAMIC_SPEAKERS voice clusters."
        }
        if (speakerNames.any { it.isBlank() }) {
            return "Enter a name for every speaker or keep the default names."
        }
        if (speakerNames.any { ':' in it }) {
            return "Speaker names cannot contain a colon."
        }
        if (speakerNames.any { normalizeName(it).length > MAX_NAME_LENGTH }) {
            return "Speaker names must be $MAX_NAME_LENGTH characters or fewer."
        }
        val normalized = speakerNames.map(::normalizeName)
        if (normalized.any { it in reservedNames || turnLabelRegex.matches(it) }) {
            return "Choose names other than You, Saved, Raw, or Turn followed by a number."
        }
        if (normalized.distinct().size != normalized.size) {
            return "Each speaker needs a different name."
        }
        return null
    }

    fun normalizeDisplayName(name: String): String = name.trim().replace(whitespaceRegex, " ")

    private fun normalizeName(name: String): String = normalizeDisplayName(name).lowercase()
}

enum class RecognizerReadiness {
    SELECTED,
    LOADING,
    READY,
    ERROR
}
