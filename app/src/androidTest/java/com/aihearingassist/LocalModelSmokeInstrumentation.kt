package com.aihearingassist

import android.app.Instrumentation
import android.app.Activity
import android.Manifest
import android.os.Build
import android.os.Bundle
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Explicit device smoke test. Requires the model already downloaded through setup.
 * Run: adb shell am instrument -w com.aihearingassist.test/com.aihearingassist.LocalModelSmokeInstrumentation
 * Does not change the user's transcript, preferences, or models.
 */
class LocalModelSmokeInstrumentation : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        val builder = LocalGenerativeSummaryBuilder(targetContext, OfflineSummaryBuilder())
        try {
            runBlocking {
                val turns = listOf(
                    ConversationManager.Turn("Speaker 1", "Have you watched the new Netflix series?"),
                    ConversationManager.Turn("Speaker 2", "Yes, I enjoyed the story and acting."),
                    ConversationManager.Turn("Speaker 3", "I plan to watch it this weekend.")
                )
                val gist = builder.buildSummary(turns)
                check(gist.usedGenerativeModel) { gist.detail }
                check(gist.text.isNotBlank()) { "Empty gist" }
                result.putString("gist", gist.text)
                result.putString("gist_timing", gist.detail)
                val reply = builder.generateReply(ReplyContext(turns, gist.text, "I will watch it this weekend too"))
                check(reply.usedGenerativeModel && reply.text.isNotBlank()) { reply.detail }
                result.putString("reply", reply.text)
                result.putString("reply_timing", reply.detail)
                val warm = builder.buildSummary(turns + ConversationManager.Turn("Speaker 2", "Let's discuss it on Monday."))
                check(warm.usedGenerativeModel) { warm.detail }
                result.putString("warm_gist", warm.text)
                result.putString("warm_timing", warm.detail)
                val jobs = builder.buildSummary(listOf(
                    ConversationManager.Turn("Speaker 1", "Finding a job through LinkedIn is difficult now."),
                    ConversationManager.Turn("Speaker 2", "I sometimes get company calls and interviews but no offers.")
                ))
                check(jobs.usedGenerativeModel) { jobs.detail }
                result.putString("jobs_gist", jobs.text)
                val hindi = builder.buildSummary(listOf(
                    ConversationManager.Turn("Speaker 1", "आज हम लोग संस्थान की फीस और एडमिशन प्रक्रिया पर बात कर रहे हैं।"),
                    ConversationManager.Turn("Speaker 2", "हॉस्टल सुविधा अच्छी है लेकिन स्कॉलरशिप की जानकारी चाहिए।")
                ))
                check(hindi.usedGenerativeModel && hindi.text.isNotBlank()) { hindi.detail }
                result.putString("hindi_gist", hindi.text)
                val longTurns = (1..12).map {
                    ConversationManager.Turn("Speaker ${it % 3 + 1}",
                        "We are discussing admission, fees, placement support, hostel facilities, scholarship deadlines, and document verification for the institute.")
                }
                repeat(3) { index ->
                    val repeated = builder.buildSummary(longTurns + ConversationManager.Turn("Speaker 1", "Short final decision number $index."))
                    check(repeated.usedGenerativeModel && repeated.text.isNotBlank()) { repeated.detail }
                    result.putString("repeat_${index}_gist", repeated.text)
                    result.putString("repeat_${index}_timing", repeated.detail)
                }
                smokeCaptureLifecycle(result)
            }
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            result.putString("failure", "${error.javaClass.simpleName}: ${error.message}")
            finish(Activity.RESULT_CANCELED, result)
        } finally { builder.close() }
    }

    private fun smokeCaptureLifecycle(result: Bundle) {
        if (Build.VERSION.SDK_INT >= 23) {
            uiAutomation.grantRuntimePermission(targetContext.packageName, Manifest.permission.RECORD_AUDIO)
        }
        val recognizer = OfflineSpeechRecognizer()
        val ready = CountDownLatch(1)
        val stopped = CountDownLatch(1)
        val error = AtomicReference<String?>()
        runOnMainSync {
            recognizer.onAudioLevel = { }
            recognizer.start(
                targetContext,
                SpeechLanguage.ENGLISH_IN,
                enableSpeakerEmbeddings = false,
                onReady = { ready.countDown() },
                onPartial = { },
                onResult = { },
                onError = {
                    error.set(it)
                    ready.countDown()
                    stopped.countDown()
                }
            )
        }
        check(ready.await(20, TimeUnit.SECONDS)) { "Offline capture did not become ready" }
        error.get()?.let { fail -> error("Offline capture failed: $fail") }
        Thread.sleep(1_000)
        runOnMainSync { recognizer.pause() }
        Thread.sleep(300)
        runOnMainSync { recognizer.resume() }
        Thread.sleep(700)
        runOnMainSync {
            recognizer.stop()
            recognizer.shutdown()
            stopped.countDown()
        }
        check(stopped.await(5, TimeUnit.SECONDS)) { "Offline capture did not stop" }
        result.putString("capture_smoke", "Offline Vosk capture started, paused, resumed, and stopped.")
    }
}
