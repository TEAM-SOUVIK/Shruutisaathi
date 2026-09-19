package com.aihearingassist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LiveSummaryState(
    val result: SummaryResult,
    val isUpdating: Boolean = false,
    val coveredTurns: Int = 0,
    val latestTurns: Int = 0
)

/** Main-thread owner. One running inference and one replaceable pending snapshot.
 * Appended speech does not invalidate a useful result; edits and reset do.
 */
class LiveSummaryCoordinator(
    private val scope: CoroutineScope,
    initial: SummaryResult,
    private val preview: suspend (List<ConversationManager.Turn>) -> SummaryResult,
    private val generate: suspend (List<ConversationManager.Turn>, (SummaryResult) -> Unit) -> SummaryResult,
    private val cancelGeneration: () -> Unit,
    private val batchDelayMs: Long = 600L,
    private val cooldownMs: Long = 1_500L,
    private val timeoutMs: Long = 45_000L
) {
    private data class Request(val turns: List<ConversationManager.Turn>, val epoch: Long)
    private val pending = Channel<Request>(Channel.CONFLATED)
    private val mutableState = MutableStateFlow(LiveSummaryState(initial))
    val state = mutableState.asStateFlow()
    private var epoch = 0L
    private var latest = emptyList<ConversationManager.Turn>()
    private val replyHasPriority = MutableStateFlow(false)

    private fun applicable(request: Request): Boolean =
        request.epoch == epoch && latest.size >= request.turns.size &&
            latest.take(request.turns.size) == request.turns

    private val worker = scope.launch {
        for (first in pending) {
            replyHasPriority.first { !it }
            // Fixed batching window: continuous speech cannot restart this delay forever.
            delay(batchDelayMs)
            replyHasPriority.first { !it }
            var request = first
            while (true) request = pending.tryReceive().getOrNull() ?: break
            if (!applicable(request)) continue
            var finished = false
            var expired = false
            val quick = preview(request.turns)
            if (!applicable(request)) continue
            if (!mutableState.value.result.usedGenerativeModel) {
                mutableState.value = mutableState.value.copy(result = quick)
            }
            mutableState.value = mutableState.value.copy(isUpdating = true)
            val watchdog = scope.launch {
                delay(timeoutMs)
                if (!finished && applicable(request)) {
                    expired = true
                    mutableState.value = mutableState.value.copy(
                        result = quick.copy(detail = "Private AI took too long. Showing a quick preview; try again after the device cools down."),
                        isUpdating = false,
                        coveredTurns = request.turns.size
                    )
                    cancelGeneration()
                }
            }
            try {
                val result = generate(request.turns) { partial ->
                    // Native callbacks can arrive on a background thread or after cancellation.
                    scope.launch {
                        if (!finished && !expired && applicable(request)) {
                            mutableState.value = mutableState.value.copy(result = partial, isUpdating = true)
                        }
                    }
                }
                if (!expired && applicable(request)) {
                    mutableState.value = mutableState.value.copy(
                        result = result, isUpdating = false, coveredTurns = request.turns.size
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!expired && applicable(request)) {
                    mutableState.value = mutableState.value.copy(
                        result = quick.copy(detail = "Summary unavailable: ${error.message ?: "Private AI error"}. Quick preview shown."),
                        isUpdating = false, coveredTurns = request.turns.size
                    )
                }
            } finally {
                finished = true
                watchdog.cancel()
            }
            // Let caption processing/UI get CPU time between generations.
            delay(cooldownMs)
        }
    }

    fun request(turns: List<ConversationManager.Turn>) {
        if (turns.isEmpty()) return
        if (turns == latest && (mutableState.value.isUpdating || mutableState.value.result.usedGenerativeModel)) return
        if (latest.isNotEmpty() && turns.take(latest.size) != latest) {
            mutableState.value = mutableState.value.copy(
                result = mutableState.value.result.copy(
                    methodLabel = "Previous gist", usedGenerativeModel = false,
                    detail = "Caption changed. Updating the gist from the corrected transcript."
                ), coveredTurns = 0
            )
        }
        latest = turns.toList()
        mutableState.value = mutableState.value.copy(latestTurns = latest.size, isUpdating = !replyHasPriority.value)
        pending.trySend(Request(latest, epoch))
    }

    fun reset(initial: SummaryResult) {
        epoch++
        latest = emptyList()
        while (pending.tryReceive().isSuccess) { /* Discard previous session requests. */ }
        mutableState.value = LiveSummaryState(initial)
        cancelGeneration()
    }

    fun pauseForReply() {
        replyHasPriority.value = true
        epoch++ // Invalidate the summary watchdog before it can cancel a reply.
        while (pending.tryReceive().isSuccess) { }
        mutableState.value = mutableState.value.copy(isUpdating = false)
        cancelGeneration()
    }

    fun resumeAfterReply() {
        replyHasPriority.value = false
        if (latest.isNotEmpty()) {
            mutableState.value = mutableState.value.copy(isUpdating = true)
            pending.trySend(Request(latest, epoch))
        }
    }

    fun close() {
        epoch++
        pending.close()
        worker.cancel()
        cancelGeneration()
    }
}
