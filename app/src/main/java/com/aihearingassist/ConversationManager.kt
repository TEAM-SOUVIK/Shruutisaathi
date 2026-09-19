package com.aihearingassist

import androidx.compose.runtime.mutableStateListOf

class ConversationManager {
    data class Turn(
        val speaker: String,
        val text: String,
        val needsPlaybackReview: Boolean = false
    )

    val entries = mutableStateListOf<Turn>()
    private val whitespaceRegex = Regex("\\s+")
    private val wordRegex = Regex("[\\p{L}\\p{M}\\p{N}']+")

    fun addTurn(
        speaker: String,
        text: String,
        needsPlaybackReview: Boolean = false,
        mergeWithPrevious: Boolean = true
    ): Boolean {
        val normalizedSpeaker = speaker.trim().replace(whitespaceRegex, " ")
        val normalizedText = text.trim().replace(whitespaceRegex, " ")
        if (normalizedSpeaker.isBlank() || normalizedText.isBlank()) return false
        val last = entries.lastOrNull()
        if (last != null && last.speaker.equals(normalizedSpeaker, ignoreCase = true)) {
            if (last.text.equals(normalizedText, ignoreCase = true) &&
                last.needsPlaybackReview == needsPlaybackReview
            ) {
                return false
            }
            if (mergeWithPrevious && last.needsPlaybackReview == needsPlaybackReview) {
                val mergedText = mergeIfSameSpeakerContinuation(last.text, normalizedText)
                if (mergedText == last.text) return false
                if (mergedText != null) {
                    entries[entries.lastIndex] = last.copy(text = mergedText)
                    return true
                }
            }
        }
        entries.add(Turn(normalizedSpeaker, normalizedText, needsPlaybackReview))
        return true
    }

    fun updateTurnText(index: Int, text: String): Boolean {
        if (index !in entries.indices) return false
        val normalizedText = text.trim().replace(whitespaceRegex, " ")
        if (normalizedText.isBlank()) return false
        entries[index] = entries[index].copy(text = normalizedText)
        return true
    }

    fun clear() {
        entries.clear()
    }

    fun transcript(): String = entries.joinToString("\n") { turn ->
        val reviewPrefix = if (turn.needsPlaybackReview) PLAYBACK_REVIEW_PREFIX else ""
        "$reviewPrefix${turn.speaker}: ${turn.text}"
    }

    fun asPlainText(): String = transcript()

    fun restoreFromTranscript(text: String) {
        clear()
        text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .forEach { line ->
                val needsPlaybackReview = line.startsWith(PLAYBACK_REVIEW_PREFIX)
                val content = line.removePrefix(PLAYBACK_REVIEW_PREFIX)
                val parts = content.split(": ", limit = 2)
                if (parts.size == 2) {
                    addTurn(parts[0], parts[1], needsPlaybackReview, mergeWithPrevious = false)
                } else {
                    addTurn("Saved", content, needsPlaybackReview, mergeWithPrevious = false)
                }
            }
    }

    private fun mergeIfSameSpeakerContinuation(previous: String, incoming: String): String? {
        if (incoming.isBlank()) return null
        if (isNearDuplicate(previous, incoming)) return previous
        val combinedLength = previous.length + incoming.length + 1
        if (combinedLength > MAX_MERGED_TURN_CHARS) return null
        return joinUtterances(previous, incoming)
    }

    private fun isNearDuplicate(previous: String, incoming: String): Boolean {
        val previousLower = previous.lowercase()
        val incomingLower = incoming.lowercase()
        if (previousLower.contains(incomingLower) || incomingLower.contains(previousLower)) return true
        val previousTokens = tokens(previousLower)
        val incomingTokens = tokens(incomingLower)
        if (previousTokens.size < 3 || incomingTokens.size < 3) return false
        val overlap = previousTokens.intersect(incomingTokens).size.toDouble()
        val smaller = minOf(previousTokens.size, incomingTokens.size).coerceAtLeast(1)
        return overlap / smaller >= NEAR_DUPLICATE_TOKEN_OVERLAP
    }

    private fun joinUtterances(previous: String, incoming: String): String {
        val separator = if (previous.endsWith(".") || previous.endsWith("!") ||
            previous.endsWith("?") || previous.endsWith("।")
        ) {
            " "
        } else {
            ", "
        }
        return "$previous$separator$incoming".replace(whitespaceRegex, " ").trim()
    }

    private fun tokens(text: String): Set<String> =
        wordRegex.findAll(text).map { it.value.trim('\'').lowercase() }.filter { it.length >= 2 }.toSet()

    private companion object {
        const val PLAYBACK_REVIEW_PREFIX = "[Review: reply playback overlap] "
        const val MAX_MERGED_TURN_CHARS = 900
        const val NEAR_DUPLICATE_TOKEN_OVERLAP = 0.82
    }
}
