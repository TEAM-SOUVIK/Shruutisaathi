package com.aihearingassist

import org.junit.Assert.*
import org.junit.Test

class ConversationManagerTest {
    @Test fun duplicateAdjacentTurnsAreIgnored() {
        val manager = ConversationManager()
        assertTrue(manager.addTurn("You", "What is the company name?"))
        assertFalse(manager.addTurn(" you ", "  What is the company name?  "))
        assertEquals(1, manager.entries.size)
    }

    @Test fun sameTextFromDifferentSpeakerIsKept() {
        val manager = ConversationManager()
        assertTrue(manager.addTurn("Speaker 1", "Yes"))
        assertTrue(manager.addTurn("Speaker 2", "Yes"))
        assertEquals(2, manager.entries.size)
    }

    @Test fun consecutiveSameSpeakerFragmentsMergeIntoOneReadableTurn() {
        val manager = ConversationManager()

        assertTrue(manager.addTurn("Speaker 1", "yeah I know but there is something like"))
        assertTrue(manager.addTurn("Speaker 1", "and maybe they come or not"))

        assertEquals(1, manager.entries.size)
        assertTrue(manager.entries.single().text.contains("something like"))
        assertTrue(manager.entries.single().text.contains("maybe they come"))
    }

    @Test fun restoredTranscriptDoesNotMergeSavedAdjacentSpeakerLines() {
        val manager = ConversationManager()

        manager.restoreFromTranscript("Speaker 1: First saved line\nSpeaker 1: Second saved line")

        assertEquals(2, manager.entries.size)
    }
}
