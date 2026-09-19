package com.aihearingassist

/**
 * Assigns neutral sequential labels to captured turns.
 *
 * This intentionally does not infer or identify speakers. It is retained as a
 * neutral fallback outside configured manual or x-vector diarization sessions.
 */
class TurnLabeler {
    private val labelPattern = Regex("^Turn\\s+(\\d+)$", RegexOption.IGNORE_CASE)

    fun nextLabel(existingLabels: Iterable<String>): String {
        val highestTurn = existingLabels
            .mapNotNull { label -> labelPattern.matchEntire(label.trim())?.groupValues?.get(1)?.toIntOrNull() }
            .maxOrNull()
            ?: 0
        return "Turn ${highestTurn + 1}"
    }
}
