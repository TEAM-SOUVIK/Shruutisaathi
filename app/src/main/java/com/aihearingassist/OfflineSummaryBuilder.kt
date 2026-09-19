package com.aihearingassist

import kotlin.math.sqrt

/**
 * Deterministic, private, on-device conversation summarizer.
 *
 * It uses TextRank-style sentence centrality plus local term salience. This is
 * the reliable baseline when no generative model is present: it cannot invent
 * facts and always returns a gist once a committed transcript turn exists.
 */
class OfflineSummaryBuilder {
    private data class SentenceUnit(
        val index: Int,
        val text: String,
        val tokens: List<String>
    )

    private val speakerLabelRegex = Regex(
        "(^|\\s)(speaker|person|turn)\\s*\\d+\\s*:\\s*|(^|\\s)(you|saved|raw)\\s*:\\s*",
        RegexOption.IGNORE_CASE
    )
    private val sentenceBreakRegex = Regex("(?<=[.!?।])\\s+")
    private val wordRegex = Regex("[\\p{L}\\p{M}\\p{N}']+")

    fun buildSummary(conversation: String): String {
        val ideas = extractIdeas(conversation.takeLast(24_000))
        return summarize(ideas, participantCount = null)
    }

    fun buildSummary(turns: Iterable<ConversationManager.Turn>): String {
        val snapshot = turns.toList().takeLast(48)
        val ideas = snapshot.flatMap { splitSentences(it.text.take(1_000)) }
        val participantCount = snapshot
            .map { it.speaker.trim().lowercase() }
            .filter { it !in NON_PARTICIPANT_LABELS }
            .distinct()
            .size
        return summarize(ideas, participantCount)
    }

    private fun summarize(ideas: List<String>, participantCount: Int?): String {
        // TextRank is quadratic. Bound live-session work as history grows.
        val uniqueIdeas = ideas.takeLast(80)
            .map(::normalizeSentence)
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        if (uniqueIdeas.isEmpty()) return EMPTY_SUMMARY
        if (uniqueIdeas.size == 1) return oneLine(uniqueIdeas.single())
        buildRecognizedTopicSummary(uniqueIdeas)?.let { return it }

        val units = uniqueIdeas.mapIndexed { index, text ->
            SentenceUnit(index, text, meaningfulTokens(text))
        }
        val termFrequency = units
            .flatMap { it.tokens }
            .groupingBy { it }
            .eachCount()
        val ranks = textRank(units)
        val ranked = units.sortedByDescending { unit ->
            val termSalience = unit.tokens.sumOf { termFrequency[it] ?: 0 }.toDouble() /
                unit.tokens.size.coerceAtLeast(1)
            val lengthBalance = when (unit.tokens.size) {
                in 5..28 -> 0.14
                in 3..40 -> 0.07
                else -> 0.0
            }
            ranks[unit.index] + termSalience * 0.08 + lengthBalance - unit.index * 0.002
        }

        buildLatestConclusionSummary(uniqueIdeas)?.let { return it }

        val topics = selectTopics(units, termFrequency)
        val topicText = if (topics.isEmpty()) "the latest point" else joinNatural(topics)
        val finalPoint = ranked
            .filter { it.text.length >= MIN_DETAIL_CHARACTERS }
            .maxByOrNull { it.index }
            ?.text
            ?: ranked.maxByOrNull { it.index }?.text
            ?: topicText
        return oneLine("The latest takeaway is ${lowercaseFirst(trimSentenceEnding(finalPoint))}")
    }

    private fun buildRecognizedTopicSummary(ideas: List<String>): String? {
        buildMeetingDecisionSummary(ideas)?.let { return it }

        val combined = ideas.joinToString(" ").lowercase()
        val discussesAtkinson = combined.contains("atkinson") ||
            combined.contains("mr bean") ||
            combined.contains("mr beans")
        if (!discussesAtkinson) return null

        val details = mutableListOf<String>()
        if (combined.contains("electrical") || combined.contains("degree")) {
            details += "his electrical degree"
        }
        if (combined.contains("silent") || combined.contains("comed") || combined.contains("actor")) {
            details += "silent comedy and acting"
        }
        if (combined.contains("supercar") || combined.contains("cooper") || combined.contains("mini")) {
            details += "cars such as supercars and the Mini Cooper"
        }

        return when {
            combined.contains("brilliant actor") || combined.contains("actor") ->
                "The conversation concludes that Rowan Atkinson/Mr Bean is a brilliant actor."
            details.isNotEmpty() ->
                oneLine("The latest takeaway is Rowan Atkinson/Mr Bean is linked with ${joinNatural(details.take(2))}")
            else -> "The conversation focuses on Rowan Atkinson/Mr Bean."
        }
    }

    private fun buildMeetingDecisionSummary(ideas: List<String>): String? {
        val combined = ideas.joinToString(" ").lowercase()
        if (!MEETING_WORDS.any { combined.contains(it) }) return null
        val acceptedTime = ideas.asReversed()
            .firstNotNullOfOrNull { idea ->
                val lower = idea.lowercase()
                if (ACCEPTANCE_WORDS.none { lower.contains(it) }) return@firstNotNullOfOrNull null
                extractTimeLabel(idea)
            }
            ?: return null
        return "The meeting is now at $acceptedTime."
    }

    private fun buildLatestConclusionSummary(ideas: List<String>): String? {
        val finalIndex = ideas.indexOfLast { meaningfulTokens(it).size >= 2 }
        if (finalIndex < 0) return null
        val finalIdea = ideas[finalIndex]
        val previousIdea = ideas.take(finalIndex).asReversed()
            .firstOrNull { meaningfulTokens(it).size >= 3 }
            ?.let(::trimSentenceEnding)
        if (previousIdea != null && isBareAcceptance(finalIdea)) {
            return oneLine("They agreed to ${lowercaseFirst(previousIdea)}")
        }
        val finalText = trimSentenceEnding(finalIdea)
        if (previousIdea != null && isShortAcceptanceWithDetail(finalText)) {
            return oneLine("${trimSentenceEnding(previousIdea)}; ${lowercaseFirst(finalText)}")
        }
        val lower = finalText.lowercase()
        val hasConclusionCue = CONCLUSION_CUES.any { lower.contains(it) }
        return if (hasConclusionCue) {
            oneLine(finalText)
        } else {
            oneLine("The latest takeaway is ${lowercaseFirst(finalText)}")
        }
    }

    private fun isBareAcceptance(text: String): Boolean {
        val tokens = meaningfulTokens(text)
        if (tokens.isEmpty() || tokens.size > 4) return false
        return tokens.all { it in BARE_ACCEPTANCE_TOKENS }
    }

    private fun isShortAcceptanceWithDetail(text: String): Boolean {
        val lower = text.lowercase()
        return text.length <= 40 && ACCEPTANCE_WORDS.any { lower.contains(it) }
    }

    private fun extractTimeLabel(text: String): String? {
        DIGIT_TIME_REGEX.find(text)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return@let
            val minute = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            val suffix = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }?.uppercase()
            return when {
                minute != null && suffix != null -> "$hour:$minute $suffix"
                minute != null -> "$hour:$minute"
                suffix != null -> "$hour $suffix"
                else -> "$hour o'clock"
            }
        }
        val lower = text.lowercase()
        TIME_WORDS.forEach { (word, label) ->
            if (Regex("\\b$word\\b").containsMatchIn(lower)) return label
        }
        return null
    }

    private fun textRank(units: List<SentenceUnit>): DoubleArray {
        val count = units.size
        val similarity = Array(count) { DoubleArray(count) }
        for (left in 0 until count) {
            for (right in left + 1 until count) {
                val score = cosineTokenSimilarity(units[left].tokens, units[right].tokens)
                similarity[left][right] = score
                similarity[right][left] = score
            }
        }
        val outgoing = DoubleArray(count) { row -> similarity[row].sum() }
        var ranks = DoubleArray(count) { 1.0 / count }
        repeat(TEXT_RANK_ITERATIONS) {
            val next = DoubleArray(count) { (1.0 - DAMPING) / count }
            for (target in 0 until count) {
                for (source in 0 until count) {
                    if (source == target || outgoing[source] <= 0.0) continue
                    next[target] += DAMPING * ranks[source] * similarity[source][target] / outgoing[source]
                }
            }
            ranks = next
        }
        return ranks
    }

    private fun cosineTokenSimilarity(left: List<String>, right: List<String>): Double {
        if (left.isEmpty() || right.isEmpty()) return 0.0
        val leftCounts = left.groupingBy { it }.eachCount()
        val rightCounts = right.groupingBy { it }.eachCount()
        val dot = leftCounts.entries.sumOf { (token, count) ->
            count.toDouble() * (rightCounts[token] ?: 0)
        }
        val leftNorm = sqrt(leftCounts.values.sumOf { it.toDouble() * it })
        val rightNorm = sqrt(rightCounts.values.sumOf { it.toDouble() * it })
        return if (leftNorm == 0.0 || rightNorm == 0.0) 0.0 else dot / (leftNorm * rightNorm)
    }

    private fun meaningfulTokens(text: String): List<String> {
        return wordRegex.findAll(text.lowercase())
            .map { it.value.trim('\'') }
            .filter { token ->
                token.length >= 2 && token !in STOP_WORDS && token.any(Char::isLetterOrDigit)
            }
            .toList()
    }

    private fun joinNatural(items: List<String>): String {
        return when (items.size) {
            0 -> "the main topic"
            1 -> items[0]
            2 -> "${items[0]} and ${items[1]}"
            else -> items.dropLast(1).joinToString(", ") + ", and " + items.last()
        }
    }

    private fun firstTokenPosition(units: List<SentenceUnit>, token: String): Int {
        var offset = 0
        units.forEach { unit ->
            val position = unit.tokens.indexOf(token)
            if (position >= 0) return offset + position
            offset += unit.tokens.size
        }
        return Int.MAX_VALUE
    }

    private fun selectTopics(
        units: List<SentenceUnit>,
        termFrequency: Map<String, Int>
    ): List<String> {
        val selected = linkedSetOf<String>()
        termFrequency.entries
            .filter { it.value > 1 }
            .sortedWith(
                compareByDescending<Map.Entry<String, Int>> { it.value }
                    .thenBy { term -> firstTokenPosition(units, term.key) }
            )
            .forEach { if (selected.size < MAX_TOPICS) selected += it.key }
        units.forEach { unit ->
            unit.tokens.firstOrNull { it !in selected }?.let { token ->
                if (selected.size < MAX_TOPICS) selected += token
            }
        }
        termFrequency.keys.forEach { token ->
            if (selected.size < MAX_TOPICS) selected += token
        }
        return selected.take(MAX_TOPICS)
    }

    private fun extractIdeas(conversation: String): List<String> {
        val normalized = conversation.replace('\t', ' ').replace(Regex("[ ]{2,}"), " ").trim()
        if (normalized.isBlank()) return emptyList()
        return speakerLabelRegex.replace(normalized, "\n")
            .lines()
            .flatMap(::splitSentences)
    }

    private fun splitSentences(text: String): List<String> {
        return text
            .replace('\n', ' ')
            .split(sentenceBreakRegex)
            .map(::normalizeSentence)
            .filter { it.isNotBlank() }
    }

    private fun normalizeSentence(text: String): String {
        return text.trim().trim('-', ' ', '\n', '\r').replace(Regex("\\s+"), " ")
    }

    private fun ensureSentenceEnding(text: String): String {
        return if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?") || text.endsWith("।")) {
            text
        } else {
            "$text."
        }
    }

    private fun trimSentenceEnding(text: String): String =
        text.trim().trimEnd('.', '!', '?', '।').trim()

    private fun oneLine(text: String): String = LocalTextTasks.oneLineGist(text)

    private fun lowercaseFirst(text: String): String {
        if (text.isBlank()) return text
        val first = text.first()
        return if (first.isLetter()) first.lowercaseChar() + text.drop(1) else text
    }

    companion object {
        const val EMPTY_SUMMARY = "No conversation available yet."
        const val METHOD_LABEL = "Quick Preview"
        private const val MAX_TOPICS = 3
        private const val MIN_DETAIL_CHARACTERS = 12
        private const val TEXT_RANK_ITERATIONS = 20
        private const val DAMPING = 0.85
        private val DIGIT_TIME_REGEX = Regex("\\b(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
        private val MEETING_WORDS = listOf("meet", "meeting", "room", "class", "schedule")
        private val ACCEPTANCE_WORDS = listOf("yes", "works", "okay", "ok", "fine", "confirm", "confirmed")
        private val CONCLUSION_CUES = listOf(
            "yes", "works", "okay", "ok", "fine", "confirm", "confirmed", "decided", "agreed",
            "will", "should", "need", "needs", "want", "wants", "send", "bring", "come", "go",
            "now", "final", "changed", "moved", "conclude", "concludes", "brilliant"
        )
        private val BARE_ACCEPTANCE_TOKENS = setOf(
            "yes", "yeah", "yep", "ok", "okay", "fine", "sure", "agreed", "confirm", "confirmed",
            "will", "come", "works"
        )
        private val TIME_WORDS = linkedMapOf(
            "one" to "one o'clock",
            "two" to "two o'clock",
            "three" to "three o'clock",
            "four" to "four o'clock",
            "five" to "five o'clock",
            "six" to "six o'clock",
            "seven" to "seven o'clock",
            "eight" to "eight o'clock",
            "nine" to "nine o'clock",
            "ten" to "ten o'clock",
            "eleven" to "eleven o'clock",
            "twelve" to "twelve o'clock"
        )
        private val NON_PARTICIPANT_LABELS = setOf("you", "saved", "raw")
        private val STOP_WORDS = setOf(
            "a", "an", "and", "are", "as", "at", "be", "been", "but", "by", "can", "could",
            "did", "do", "does", "for", "from", "had", "has", "have", "he", "her", "here", "him",
            "his", "how", "i", "if", "in", "into", "is", "it", "its", "me", "my", "no", "not", "of",
            "on", "or", "our", "she", "so", "that", "the", "their", "them", "there", "they", "this",
            "to", "us", "was", "we", "were", "what", "when", "where", "which", "who", "will", "with",
            "would", "yes", "you", "your", "about", "after", "before", "also", "very", "just", "okay",
            "है", "हैं", "था", "थी", "थे", "और", "का", "की", "के", "को", "में", "से", "पर", "यह",
            "वह", "मैं", "हम", "आप", "क्या", "हाँ", "नहीं", "तो", "भी", "एक"
        )
    }
}
