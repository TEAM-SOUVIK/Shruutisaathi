package com.aihearingassist

import org.junit.Assert.*
import org.junit.Test

class LocalTextTasksTest {
    @Test fun gistRequestsOneSentence() {
        assertTrue(LocalTextTasks.GIST_INSTRUCTION.contains("conversation-specific final-conclusion sentence"))
        assertTrue(LocalTextTasks.GIST_INSTRUCTION.contains("18 words"))
        assertTrue(LocalTextTasks.GIST_INSTRUCTION.contains("final-conclusion"))
        assertTrue(LocalTextTasks.GIST_INSTRUCTION.contains("output only that outcome"))
        assertTrue(LocalTextTasks.GIST_INSTRUCTION.contains("latest concrete takeaway"))
        assertTrue(LocalTextTasks.GIST_INSTRUCTION.contains("Do not copy dialogue"))
        assertTrue(LocalTextTasks.GIST_INSTRUCTION.contains("Mention uncertainty only"))
    }

    @Test fun gistPromptIsTranscriptSpecificAndRejectsMetaOutput() {
        val transcript = listOf(
            ConversationManager.Turn("Amit", "do you know one Atkinson has an electrical degree"),
            ConversationManager.Turn("Rahul", "wow he please Mr Bean so well it's hard to believe"),
            ConversationManager.Turn("Priya", "true his silence comedies pure genius"),
            ConversationManager.Turn("Amit", "he is also love supercars in real life"),
            ConversationManager.Turn("Rahul", "that's funny since Mr Beans only drive many time Cooper"),
            ConversationManager.Turn("Priya", "he is truly a brilliant actor")
        )
        val compact = LocalTextTasks.transcript(transcript)
        val prompt = LocalTextTasks.gistPrompt(compact)

        assertTrue(prompt.contains("TRANSCRIPT_TURNS"))
        assertTrue(prompt.contains("conversation-specific"))
        assertTrue(prompt.contains("Final conclusion first"))
        assertTrue(prompt.contains("Do not write participant names"))
        assertTrue(prompt.contains("Mr Bean"))

        val bad = "Amit and Rahul are unsure about their knowledge of Atkinson's electrical degree, while Priya and Amit share their admiration for Rahul's love for supercars. The gist includes uncertainty and the main topic."
        val good = "The speakers discuss Rowan Atkinson/Mr. Bean, including his electrical degree, silent comedy, supercars, the Mini Cooper, and acting."

        assertTrue(!LocalTextTasks.isGroundedGist(bad, compact))
        assertTrue(LocalTextTasks.isGroundedGist(good, compact))
    }

    @Test fun gistCleanupKeepsOnlyOneDisplayLine() {
        val verbose = "The meeting is now at four o'clock. Speaker 2 also asked about the room and other details."
        val cleaned = LocalTextTasks.cleanGist(verbose)

        assertEquals("The meeting is now at four o'clock.", cleaned)
        assertTrue(cleaned.lines().size == 1)
        assertTrue(Regex("[.!?।]").findAll(cleaned).count() == 1)
    }
    @Test fun replyIncludesIntentGistAndConversation() {
        val prompt = LocalTextTasks.replyPrompt(ReplyContext(
            listOf(ConversationManager.Turn("Speaker 1", "Did you like the series?")),
            "Two people discussed a Netflix series.", "yes enjoyed it"
        ))
        assertTrue(prompt.contains("yes enjoyed it"))
        assertTrue(prompt.contains("Two people discussed"))
        assertTrue(prompt.contains("Did you like"))
        assertTrue(prompt.contains("MY_INTENT"))
        assertTrue(prompt.contains("RECENT_CONTEXT"))
        assertTrue(prompt.contains("FINAL_SPOKEN_REPLY"))
        assertTrue(LocalTextTasks.REPLY_INSTRUCTION.contains("Treat MY_INTENT as my goal"))
        assertTrue(LocalTextTasks.REPLY_INSTRUCTION.contains("Do not reuse a previous generated draft"))
    }
    @Test fun replyPromptIgnoresPreviousUserRepliesWhenOtherSpeakersExist() {
        val prompt = LocalTextTasks.replyPrompt(ReplyContext(
            listOf(
                ConversationManager.Turn("Speaker 1", "Can you join the institute meeting tomorrow?"),
                ConversationManager.Turn("You", "Sure, I will join."),
                ConversationManager.Turn("Speaker 2", "Please confirm the time.")
            ),
            "People discussed an institute meeting.",
            "ask for the exact time politely"
        ))
        assertTrue(prompt.contains("confirm the time"))
        assertTrue(prompt.contains("ask for the exact time"))
        assertFalse(prompt.contains("Sure, I will join"))
    }
    @Test fun promptRemainsBoundedForLongConversation() {
        val turns = (1..1000).map { ConversationManager.Turn("Speaker $it", "A".repeat(5000)) }
        assertTrue(LocalTextTasks.transcript(turns).toByteArray(Charsets.UTF_8).size <= LocalTextTasks.MAX_TRANSCRIPT_BYTES)
        assertTrue(
            LocalTextTasks.replyPrompt(ReplyContext(turns, "B".repeat(5000), "C".repeat(5000)))
                .toByteArray(Charsets.UTF_8).size < 1300
        )
    }
    @Test fun transcriptKeepsRecentCompleteTurnsWhenPossible() {
        val turns = listOf(
            ConversationManager.Turn("Speaker 1", "old topic ".repeat(80)),
            ConversationManager.Turn("Speaker 2", "new topic about institute admission"),
            ConversationManager.Turn("Speaker 3", "fees and hostel discussion")
        )
        val transcript = LocalTextTasks.transcript(turns)
        assertTrue(transcript.contains("old topic"))
        assertTrue(transcript.contains("new topic"))
        assertTrue(transcript.contains("hostel discussion"))
    }
    @Test fun transcriptKeepsConversationAnchorAndRecentTurns() {
        val turns = listOf(
            ConversationManager.Turn("Speaker 1", "the institute invited a consulting company to campus"),
            ConversationManager.Turn("Speaker 2", "students are discussing job opportunities"),
        ) + (1..20).map {
            ConversationManager.Turn("Speaker ${(it % 3) + 1}", "later detail number $it about timing and company visit")
        }
        val transcript = LocalTextTasks.transcript(turns)
        assertTrue(transcript.contains("institute invited"))
        assertTrue(transcript.contains("job opportunities"))
        assertTrue(transcript.contains("later detail number 20"))
        assertFalse(transcript.contains("later detail number 3"))
    }
    @Test fun replyIntentHasExplicitLimit() {
        assertTrue(LocalTextTasks.isReplyIntentSupported("yes, I agree"))
        assertFalse(LocalTextTasks.isReplyIntentSupported(""))
        assertFalse(LocalTextTasks.isReplyIntentSupported("ह".repeat(300)))
    }
    @Test fun outputExpandsWithoutEllipsisOrCharacterTruncation() {
        val text = "This is a complete sentence that remains visible. ".repeat(12).trim()
        assertEquals(text, LocalTextTasks.clean(text))
    }
    @Test fun reasoningIsNotShown() {
        assertEquals("Two people discussed a series.", LocalTextTasks.clean("<think>hidden\nreasoning</think>Two people discussed a series."))
        assertEquals("", LocalTextTasks.clean("<think>unfinished reasoning"))
    }
    @Test fun speakerLabelsAreRemovedFromModelText() {
        assertEquals("A participant is asking about scholarships.", LocalTextTasks.clean("Speaker 2 is asking about scholarships."))
        assertEquals("The conversation discusses admission and fees.", LocalTextTasks.clean("In discussing admission and fees."))
        assertEquals("What is the company name?", LocalTextTasks.clean("FINAL_SPOKEN_REPLY: What is the company name?"))
        assertEquals("The speakers are unsure about the visit.", LocalTextTasks.clean("Gist: The speakers are unsure about the visit."))
    }
}
