package com.aihearingassist

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class LiveSummaryCoordinatorTest {
    @Test fun replyPriorityDefersNewSummariesAndKeepsLatestSpeech() = runBlocking {
        withTimeout(5_000) {
            val started = Channel<Int>(Channel.UNLIMITED)
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { turns, _ ->
                started.send(turns.size)
                generated(turns.size)
            }, {}, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.pauseForReply()
                coordinator.request(turns(1))
                coordinator.request(turns(10))
                kotlinx.coroutines.delay(30)
                assertTrue(started.tryReceive().isFailure)
                assertFalse(coordinator.state.value.isUpdating)
                coordinator.resumeAfterReply()
                assertEquals(10, started.receive())
                coordinator.state.first { !it.isUpdating }
                assertEquals(10, coordinator.state.value.coveredTurns)
            } finally { coordinator.close() }
        }
    }

    @Test fun resetDuringReplyDoesNotRestoreOldConversation() = runBlocking {
        withTimeout(5_000) {
            val started = Channel<Int>(Channel.UNLIMITED)
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { turns, _ ->
                started.send(turns.size)
                generated(turns.size)
            }, {}, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.pauseForReply()
                coordinator.request(turns(10))
                coordinator.reset(empty)
                coordinator.resumeAfterReply()
                kotlinx.coroutines.delay(30)
                assertTrue(started.tryReceive().isFailure)
                assertEquals(empty, coordinator.state.value.result)
            } finally { coordinator.close() }
        }
    }
    private val empty = SummaryResult("Empty", "TextRank", "", false)
    private fun turns(count: Int) = (1..count).map { ConversationManager.Turn("Speaker 1", "Turn $it") }
    private fun generated(count: Int) = SummaryResult("Gist $count", "Gemma", "", true)

    @Test fun continuousSpeech_keepsOnlyNewestPendingSnapshot_andPublishesCompletedGist() = runBlocking {
        withTimeout(5_000) {
            val started = Channel<Int>(Channel.UNLIMITED)
            val finish = Channel<Unit>(Channel.UNLIMITED)
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { snapshot, _ ->
                started.send(snapshot.size)
                finish.receive()
                generated(snapshot.size)
            }, {}, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.request(turns(1))
                assertEquals(1, started.receive())
                (2..20).forEach { coordinator.request(turns(it)) }
                finish.send(Unit)
                assertEquals(20, started.receive())
                assertEquals("Gist 1", coordinator.state.value.result.text)
                assertEquals(1, coordinator.state.value.coveredTurns)
                finish.send(Unit)
                val done = coordinator.state.first { it.coveredTurns == 20 && !it.isUpdating }
                assertEquals("Gist 20", done.result.text)
                assertTrue(started.tryReceive().isFailure)
            } finally { coordinator.close() }
        }
    }

    @Test fun reset_discardsOldResultAndLateStream_evenWhenNativeWorkFinishesLater() = runBlocking {
        withTimeout(5_000) {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val completed = CompletableDeferred<Unit>()
            var cancels = 0
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { _, partial ->
                started.complete(Unit)
                release.await()
                partial(generated(9))
                completed.complete(Unit)
                generated(9)
            }, { cancels++ }, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.request(turns(9))
                started.await()
                coordinator.reset(empty)
                release.complete(Unit)
                completed.await()
                assertEquals(empty, coordinator.state.value.result)
                assertFalse(coordinator.state.value.isUpdating)
                assertEquals(1, cancels)
            } finally { coordinator.close() }
        }
    }

    @Test fun correction_invalidatesOldSnapshot() = runBlocking {
        withTimeout(5_000) {
            val started = Channel<String>(Channel.UNLIMITED)
            val finish = Channel<Unit>(Channel.UNLIMITED)
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { snapshot, partial ->
                started.send(snapshot.first().text)
                finish.receive()
                val result = generated(1).copy(text = snapshot.first().text)
                partial(result)
                result
            }, {}, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.request(turns(1))
                started.receive()
                coordinator.request(listOf(ConversationManager.Turn("Speaker 1", "Corrected")))
                finish.send(Unit)
                assertEquals("Corrected", started.receive())
                assertNotEquals("Turn 1", coordinator.state.value.result.text)
                finish.send(Unit)
                assertEquals("Corrected", coordinator.state.first { !it.isUpdating }.result.text)
            } finally { coordinator.close() }
        }
    }

    @Test fun duplicateSave_doesNotQueueAnotherInference() = runBlocking {
        withTimeout(5_000) {
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var calls = 0
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { _, _ ->
                calls++
                started.complete(Unit)
                release.await()
                generated(1)
            }, {}, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.request(turns(1))
                started.await()
                repeat(10) { coordinator.request(turns(1)) }
                release.complete(Unit)
                coordinator.state.first { !it.isUpdating }
                coordinator.request(turns(1))
                assertEquals(1, calls)
                assertFalse(coordinator.state.value.isUpdating)
            } finally { coordinator.close() }
        }
    }

    @Test fun timeout_exitsUpdatingAndIgnoresLateSuccess() = runBlocking {
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            val cancelled = CompletableDeferred<Unit>()
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { _, _ ->
                release.await()
                generated(1)
            }, { cancelled.complete(Unit) }, batchDelayMs = 0, cooldownMs = 0, timeoutMs = 20)
            try {
                coordinator.request(turns(1))
                cancelled.await()
                release.complete(Unit)
                assertFalse(coordinator.state.value.isUpdating)
                assertFalse(coordinator.state.value.result.usedGenerativeModel)
                assertTrue(coordinator.state.value.result.detail.contains("too long"))
            } finally { coordinator.close() }
        }
    }

    @Test fun inferenceFailure_exitsUpdatingWithExplicitFallback() = runBlocking {
        withTimeout(5_000) {
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { _, _ ->
                error("Unsupported device")
            }, {}, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.request(turns(1))
                val failed = coordinator.state.first { !it.isUpdating }
                assertFalse(failed.result.usedGenerativeModel)
                assertTrue(failed.result.detail.contains("Unsupported device"))
            } finally { coordinator.close() }
        }
    }

    @Test fun streaming_displaysTextBeforeGenerationCompletes() = runBlocking {
        withTimeout(5_000) {
            val release = CompletableDeferred<Unit>()
            val coordinator = LiveSummaryCoordinator(this, empty, { empty }, { _, partial ->
                partial(generated(1).copy(text = "First words"))
                release.await()
                generated(1)
            }, {}, batchDelayMs = 0, cooldownMs = 0)
            try {
                coordinator.request(turns(1))
                val streaming = coordinator.state.first { it.result.text == "First words" }
                assertTrue(streaming.isUpdating)
                release.complete(Unit)
                assertEquals("Gist 1", coordinator.state.first { !it.isUpdating }.result.text)
            } finally { coordinator.close() }
        }
    }
}
