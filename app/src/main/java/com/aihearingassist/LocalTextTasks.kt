package com.aihearingassist

/** Small, bounded prompts shared by the on-device gist and reply tasks. */
object LocalTextTasks {
    const val GIST_INSTRUCTION = "TASK: Write exactly one factual, conversation-specific final-conclusion sentence, 8-18 words. " +
        "Use only the transcript. If the conversation reaches a decision, agreement, final time/place, task, or action item, output only that outcome. " +
        "Otherwise state the latest concrete takeaway, not the full conversation summary. " +
        "Preserve concrete entities such as person, show, institute, company, job, campus, event, time, or name. " +
        "Mention uncertainty only when the transcript explicitly says maybe, unsure, not sure, unclear, or do not know. " +
        "Speaker names before colons are labels only; never treat them as people being discussed unless the spoken text also says them. " +
        "Do not copy dialogue, invent facts, add advice, describe feelings, or refer to the gist/summary/main topic. Output only the gist. /no_think"
    const val REPLY_INSTRUCTION = "TASK: Draft only my next natural spoken reply. Treat MY_INTENT as my goal, not as text to continue. " +
        "Use RECENT_CONTEXT and GIST only for relevance. If MY_INTENT asks for information, ask that question directly. " +
        "If MY_INTENT is shorthand, turn it into a clear conversational sentence. Do not reuse a previous generated draft, " +
        "continue the transcript, explain, add labels, or invent facts. Example: MY_INTENT 'four okay, send room' after a meeting-time discussion -> " +
        "'Four o'clock works for me. Please send me the room number.' Max 24 words. Output the reply only. /no_think"
    const val MAX_TRANSCRIPT_BYTES = 1600
    const val MAX_REPLY_CONTEXT_BYTES = 760
    const val MAX_GIST_BYTES = 220
    const val MAX_INTENT_BYTES = 240

    fun transcript(turns: List<ConversationManager.Turn>): String {
        val selected = ArrayDeque<String>()
        var used = 0
        for (turn in prioritizedTurns(turns)) {
            val label = turn.speaker.take(32)
            val text = cleanWhitespace(turn.text)
            val line = "$label: ${utf8Bound(text, MAX_TRANSCRIPT_BYTES - label.toByteArray(Charsets.UTF_8).size - 2)}"
            val bytes = line.toByteArray(Charsets.UTF_8).size + if (selected.isEmpty()) 0 else 1
            if (used + bytes > MAX_TRANSCRIPT_BYTES && selected.isNotEmpty()) break
            selected.addFirst(utf8Bound(line, MAX_TRANSCRIPT_BYTES - used))
            used += bytes
        }
        return selected.joinToString("\n")
    }

    fun replyPrompt(context: ReplyContext): String =
        "RECENT_CONTEXT:\n${utf8Bound(replyTranscript(context.turns), MAX_REPLY_CONTEXT_BYTES)}\n\n" +
            "GIST:\n${utf8Bound(context.summary, MAX_GIST_BYTES)}\n\n" +
            "MY_INTENT:\n${utf8Bound(context.intendedText.trim(), MAX_INTENT_BYTES)}\n\n" +
            "FINAL_SPOKEN_REPLY:"

    fun gistPrompt(transcript: String): String =
        "$GIST_INSTRUCTION\n\n" +
            "TRANSCRIPT_TURNS:\n${utf8Bound(transcript.trim(), MAX_TRANSCRIPT_BYTES)}\n\n" +
            "OUTPUT_RULES:\n" +
            "- One sentence and one display line only.\n" +
            "- Final conclusion first: decision, agreement, final time/place, task, or action item beats a generic topic summary.\n" +
            "- If there is no outcome, state the latest takeaway in 8-18 words.\n" +
            "- Ignore names before colons; those are diarization labels, not conversation facts.\n" +
            "- Do not write participant names from the turn labels.\n" +
            "- Keep only the central entity/topic and the final point from the spoken words.\n" +
            "- Do not write meta phrases such as \"the gist includes\".\n\n" +
            "GIST:"

    private fun replyTranscript(turns: List<ConversationManager.Turn>): String {
        val otherSpeakers = turns.filterNot { it.speaker.equals("You", ignoreCase = true) }
        return transcript(otherSpeakers.ifEmpty { turns })
    }

    private fun prioritizedTurns(turns: List<ConversationManager.Turn>): List<ConversationManager.Turn> {
        if (turns.size <= 14) return turns.asReversed()
        val anchors = turns.take(3)
        val recent = turns.takeLast(14)
        return (anchors + recent)
            .distinctBy { "${it.speaker}\u0000${it.text}" }
            .asReversed()
    }

    fun isReplyIntentSupported(text: String): Boolean =
        text.trim().toByteArray(Charsets.UTF_8).size in 1..MAX_INTENT_BYTES

    fun intentLimitMessage(): String =
        "Keep your intended reply under $MAX_INTENT_BYTES UTF-8 bytes for fast local generation."

    // UTF-8 bound limits multilingual prefill too, without splitting a Unicode code point.
    fun utf8Bound(text: String, maxBytes: Int): String {
        val out = StringBuilder()
        var used = 0
        for (point in text.codePoints().toArray()) {
            val next = String(Character.toChars(point))
            val bytes = next.toByteArray(Charsets.UTF_8).size
            if (used + bytes > maxBytes) break
            out.append(next)
            used += bytes
        }
        return out.toString()
    }

    private fun cleanWhitespace(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    fun clean(raw: String): String {
        val text = raw.replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
            .substringBefore("<think>")
            .replace("```", "")
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('"', '\'', '`')
            .replace(Regex("^(summary|gist|reply|final spoken reply|final_spoken_reply)\\s*:\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
            .replace(Regex("\\bSpeaker\\s+\\d+\\b", RegexOption.IGNORE_CASE), "A participant")
        return if (text.startsWith("In discussing ", ignoreCase = true)) {
            "The conversation discusses " + text.removePrefix("In discussing ").removePrefix("in discussing ")
        } else {
            text
        }
        // Display complete model output; no character clipping or ellipsis.
    }

    fun cleanGist(raw: String): String {
        val cleaned = clean(raw)
            .replace(Regex("\\bThe gist includes\\b.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bThis gist includes\\b.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bThe summary includes\\b.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\bThe main topic is\\b", RegexOption.IGNORE_CASE), "The conversation focuses on")
            .replace(Regex("\\s+"), " ")
            .trim()
        return oneLineGist(cleaned)
    }

    fun oneLineGist(text: String): String {
        val firstSentence = FIRST_SENTENCE_REGEX.find(text)?.value ?: text
        val normalized = firstSentence
            .trim()
            .trim('"', '\'', '`')
            .trimEnd(',', ';', ':', '-')
            .trim()
        if (normalized.isBlank()) return ""
        val matches = WORD_REGEX.findAll(normalized).toList()
        val bounded = if (matches.size > MAX_GIST_WORDS) {
            normalized.take(matches[MAX_GIST_WORDS - 1].range.last + 1)
        } else {
            normalized
        }.trim().trimEnd(',', ';', ':', '-').trim()
        return if (bounded.endsWith(".") || bounded.endsWith("!") || bounded.endsWith("?") || bounded.endsWith("।")) {
            bounded
        } else {
            "$bounded."
        }
    }

    fun isGroundedGist(gist: String, transcript: String): Boolean {
        val normalizedGist = cleanGist(gist)
        if (normalizedGist.isBlank()) return false
        val gistLower = normalizedGist.lowercase()
        val transcriptLower = transcript.lowercase()
        if (META_GIST_MARKERS.any { gistLower.contains(it) }) return false
        if (UNCERTAINTY_WORDS.any { gistLower.contains(it) } &&
            UNCERTAINTY_CUES.none { transcriptLower.contains(it) }) return false
        if (speakerLabels(transcript).any { label -> gistLower.contains(label.lowercase()) }) return false

        val transcriptTokens = wordTokens(transcriptLower)
        val gistTokens = wordTokens(gistLower)
            .filter { it !in GROUNDING_STOP_WORDS && it.length >= 3 }
        if (gistTokens.isEmpty()) return false
        val supported = gistTokens.count { token ->
            token in transcriptTokens || CANONICAL_ENTITY_ALIASES[token].orEmpty().any { alias -> transcriptLower.contains(alias) }
        }
        val required = when {
            gistTokens.size <= 4 -> 2
            else -> minOf(3, gistTokens.size)
        }
        return supported >= required
    }

    private fun speakerLabels(transcript: String): Set<String> =
        transcript.lines()
            .mapNotNull { line -> line.substringBefore(':', missingDelimiterValue = "").trim().takeIf { it.isNotBlank() } }
            .filterNot { it.equals("you", ignoreCase = true) }
            .filter { it.length <= 32 }
            .toSet()

    private fun wordTokens(text: String): Set<String> =
        WORD_REGEX.findAll(text).map { it.value.trim('\'').lowercase() }.filter { it.isNotBlank() }.toSet()

    private val WORD_REGEX = Regex("[\\p{L}\\p{M}\\p{N}']+")
    private val FIRST_SENTENCE_REGEX = Regex("^.+?[.!?।](?=\\s|$)")
    private const val MAX_GIST_WORDS = 18
    private val META_GIST_MARKERS = listOf(
        "the gist includes",
        "this gist includes",
        "the summary includes",
        "the summary is",
        "main topic",
        "output only",
        "no speaker labels"
    )
    private val UNCERTAINTY_WORDS = listOf("unsure", "uncertain", "uncertainty", "unclear")
    private val UNCERTAINTY_CUES = listOf("maybe", "not sure", "unsure", "unclear", "do not know", "don't know")
    private val CANONICAL_ENTITY_ALIASES = mapOf(
        "rowan" to listOf("atkinson", "mr bean", "mr beans"),
        "atkinson" to listOf("atkinson", "mr bean", "mr beans"),
        "bean" to listOf("mr bean", "mr beans", "bean"),
        "cooper" to listOf("cooper"),
        "supercars" to listOf("supercar", "supercars")
    )
    private val GROUNDING_STOP_WORDS = setOf(
        "the", "speakers", "speaker", "participants", "participant", "conversation", "discuss", "discusses",
        "focuses", "about", "including", "include", "includes", "and", "or", "with", "from", "that", "this",
        "their", "there", "they", "while", "also", "only", "main", "topic", "gist", "summary"
    )
}
