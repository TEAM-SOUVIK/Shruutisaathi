package com.aihearingassist

data class ReplyContext(
    val turns: List<ConversationManager.Turn>,
    val summary: String,
    val intendedText: String
)

/**
 * Conservative on-device reply baseline.
 *
 * This is intentionally not presented as an LLM: the user's intended words
 * remain the factual source, while recent turns only help with safe phrasing.
 */
class OfflineReplyGenerator {
    private val whitespaceRegex = Regex("\\s+")

    fun generate(context: ReplyContext): String {
        val intent = context.intendedText.trim().replace(whitespaceRegex, " ")
        if (intent.isBlank()) return ""

        val lowerIntent = intent.lowercase().trimEnd('.', '!', '?')
        buildScheduleRoomReply(lowerIntent, context)?.let { return it }
        val latestOtherTurn = context.turns.lastOrNull {
            !it.speaker.equals("You", ignoreCase = true)
        }?.text.orEmpty()
        val hasQuestionContext = latestOtherTurn.trimEnd().endsWith("?") ||
            (latestOtherTurn.isBlank() && context.summary.trimEnd().endsWith("?"))

        val reply = when (lowerIntent) {
            "yes", "yeah", "yep" -> "Yes."
            "no", "nope" -> "No."
            "ok", "okay" -> "Okay."
            "thanks", "thank you" -> "Thank you."
            "repeat", "please repeat" -> if (hasQuestionContext) {
                "Could you please repeat the question?"
            } else {
                "Could you please repeat that?"
            }
            else -> finishSentence(capitalizeFirst(intent))
        }

        return reply.take(MAX_REPLY_LENGTH)
    }

    private fun buildScheduleRoomReply(lowerIntent: String, context: ReplyContext): String? {
        if (!lowerIntent.contains("room")) return null
        if (listOf("send", "share", "tell", "give").none { lowerIntent.contains(it) }) return null
        val time = extractTimePhrase(lowerIntent)
            ?: extractTimePhrase(context.summary.lowercase())
            ?: context.turns.asReversed().firstNotNullOfOrNull { extractTimePhrase(it.text.lowercase()) }
            ?: return null
        return "${capitalizeFirst(time)} works for me. Please send me the room number."
    }

    private fun extractTimePhrase(text: String): String? {
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
        TIME_WORDS.forEach { (word, label) ->
            if (Regex("\\b$word\\b").containsMatchIn(text)) return label
        }
        return null
    }

    private fun capitalizeFirst(text: String): String {
        if (text.isEmpty()) return text
        return text.replaceFirstChar { first ->
            if (first.isLowerCase()) first.titlecase() else first.toString()
        }
    }

    private fun finishSentence(text: String): String {
        return if (text.endsWith(".") || text.endsWith("!") || text.endsWith("?")) text else "$text."
    }

    companion object {
        const val MAX_REPLY_LENGTH = 280
        private val DIGIT_TIME_REGEX = Regex("\\b(1[0-2]|0?[1-9])(?::([0-5][0-9]))?\\s*(am|pm)?\\b", RegexOption.IGNORE_CASE)
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
    }
}
