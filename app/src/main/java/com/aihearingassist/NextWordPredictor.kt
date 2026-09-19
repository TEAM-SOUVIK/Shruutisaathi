package com.aihearingassist

data class WordSuggestion(
    val word: String,
    val replaceStart: Int,
    val replaceEnd: Int
) {
    fun applyTo(text: String): String {
        val safeStart = replaceStart.coerceIn(0, text.length)
        val safeEnd = replaceEnd.coerceIn(safeStart, text.length)
        val before = text.substring(0, safeStart).trimEnd()
        val after = text.substring(safeEnd).trimStart()
        return buildString {
            if (before.isNotEmpty()) {
                append(before)
                append(' ')
            }
            append(word)
            append(' ')
            if (after.isNotEmpty()) append(after)
        }
    }
}

/**
 * Small deterministic next-word baseline that runs entirely on the phone.
 * It deliberately shows nothing until the user starts a response.
 */
class NextWordPredictor {
    private val tokenRegex = Regex("[\\p{L}\\p{M}\\p{N}']+")
    private val builtInTransitions = mapOf(
        "i" to listOf("am", "can", "will", "would", "need"),
        "i want" to listOf("to"),
        "want to" to listOf("ask", "book", "confirm", "meet", "say"),
        "i want to" to listOf("ask", "book", "confirm", "meet"),
        "i can" to listOf("come", "help", "join", "wait"),
        "i will" to listOf("call", "come", "confirm", "reach"),
        "please" to listOf("call", "repeat", "wait"),
        "thank" to listOf("you"),
        "what" to listOf("is", "time", "will"),
        "what time" to listOf("is", "will"),
        "could you" to listOf("please", "repeat"),
        "yes" to listOf("please"),
        "no" to listOf("thank", "that")
    )

    private val completionVocabulary = listOf(
        "agree", "appointment", "available", "book", "call", "come", "confirm",
        "help", "interview", "join", "meeting", "please", "reach", "repeat",
        "schedule", "thank", "tomorrow", "understand", "wait", "will"
    )

    fun predict(conversation: String, currentText: String): List<WordSuggestion> {
        return predict(listOf(conversation), currentText)
    }

    fun predict(turnTexts: Iterable<String>, currentText: String): List<WordSuggestion> {
        if (currentText.isBlank()) return emptyList()

        val inputTokens = tokenize(currentText)
        if (inputTokens.isEmpty()) return emptyList()

        val turnTokenSequences = turnTexts.map(::tokenize).filter { it.isNotEmpty() }
        val contextTokens = turnTokenSequences.flatten()
        val learnedTransitions = learnTransitions(turnTokenSequences)
        val phraseSuggestions = buildPhraseSuggestions(inputTokens)
        val contextSuggestions = learnedTransitions[inputTokens.last()].orEmpty()
        val trailingMatch = tokenRegex.findAll(currentText).lastOrNull()
        val hasPartialWord = trailingMatch != null && trailingMatch.range.last == currentText.lastIndex
        val partialWord = if (hasPartialWord) trailingMatch.value else ""
        val completions = if (partialWord.length >= 2) {
            (contextTokens + completionVocabulary)
                .asSequence()
                .filter { it.startsWith(partialWord, ignoreCase = true) }
                .filterNot { it.equals(partialWord, ignoreCase = true) }
                .toList()
        } else {
            emptyList()
        }

        return (phraseSuggestions + contextSuggestions + completions)
            .distinctBy { it.lowercase() }
            .filter { it.isNotBlank() }
            .take(3)
            .map { word ->
                val replacesPartial = hasPartialWord &&
                    word.startsWith(partialWord, ignoreCase = true) &&
                    !word.equals(partialWord, ignoreCase = true)
                WordSuggestion(
                    word = word,
                    replaceStart = if (replacesPartial) trailingMatch!!.range.first else currentText.length,
                    replaceEnd = if (replacesPartial) trailingMatch!!.range.last + 1 else currentText.length
                )
            }
    }

    private fun tokenize(text: String): List<String> {
        return tokenRegex
            .findAll(text)
            .map { it.value.lowercase().trim() }
            .filter { it.isNotBlank() }
            .toList()
    }

    private fun buildPhraseSuggestions(tokens: List<String>): List<String> {
        val keys = buildList {
            if (tokens.size >= 3) add(tokens.takeLast(3).joinToString(" "))
            if (tokens.size >= 2) add(tokens.takeLast(2).joinToString(" "))
            add(tokens.last())
        }
        return keys.flatMap { builtInTransitions[it].orEmpty() }
    }

    private fun learnTransitions(sequences: Iterable<List<String>>): Map<String, List<String>> {
        val counts = mutableMapOf<String, MutableMap<String, Int>>()
        sequences.forEach { tokens ->
            tokens.zipWithNext().forEach { (word, next) ->
                if (word != next) {
                    val nextCounts = counts.getOrPut(word) { mutableMapOf() }
                    nextCounts[next] = (nextCounts[next] ?: 0) + 1
                }
            }
        }
        return counts.mapValues { (_, nextCounts) ->
            nextCounts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key }
        }
    }
}
