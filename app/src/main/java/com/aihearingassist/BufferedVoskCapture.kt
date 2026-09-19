package com.aihearingassist

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.content.ContextCompat
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.sqrt

/** The audio thread only records; Vosk decoding runs on a separate worker.
 * A bounded ten-second queue absorbs short CPU stalls without unbounded memory growth.
 * Overload and microphone failures are reported, never silently ignored.
 * This session owns and closes its native Recognizer.
 */
class BufferedVoskCapture(
    private val context: Context,
    private val recognizer: Recognizer,
    sampleRate: Float,
    private val onLevel: (Float) -> Unit
) {
    private data class Frame(val samples: ShortArray, val epoch: Long)
    private val rate = sampleRate.toInt()
    private val frameSamples = rate / 10
    private val queue = ArrayBlockingQueue<Frame>(100)
    private val active = AtomicBoolean(false)
    private val recognizerClosed = AtomicBoolean(false)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var paused = false
    @Volatile private var epoch = 0L
    private var recorder: AudioRecord? = null
    private var producer: Thread? = null
    private var consumer: Thread? = null
    private var listener: RecognitionListener? = null

    fun startListening(listener: RecognitionListener): Boolean {
        check(consumer == null) { "Capture session cannot be reused" }
        this.listener = listener
        val minimum = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        check(minimum > 0) { "Device does not support this speech model's sample rate ($rate Hz)" }
        check(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            "Microphone permission is required for private capture"
        }
        val audio = AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, rate,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum * 2, rate * 2))
        recorder = audio
        try {
            check(audio.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable; close other recording apps" }
            audio.startRecording()
            check(audio.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Microphone could not start" }
        } catch (error: Exception) {
            audio.release()
            recorder = null
            closeRecognizer()
            throw error
        }
        active.set(true)
        producer = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            try {
                while (active.get()) {
                    val frameEpoch = epoch
                    val samples = ShortArray(frameSamples)
                    val count = audio.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                    if (!active.get()) break
                    check(count > 0) { "Microphone read failed ($count); restart capture" }
                    if (paused || frameEpoch != epoch) continue
                    val pcm = if (count == samples.size) samples else samples.copyOf(count)
                    val rms = sqrt(pcm.sumOf { it.toDouble() * it } / pcm.size) / 32768.0
                    main.post { if (active.get() && !paused) onLevel((rms * 8).toFloat().coerceIn(0f, 1f)) }
                    check(queue.offer(Frame(pcm, frameEpoch))) {
                        "Audio processing fell more than 10 seconds behind. Close other apps and restart capture; a recording gap occurred."
                    }
                }
            } catch (error: Exception) { fail(error) }
            finally { runCatching { audio.stop() }; audio.release() }
        }, "ShrutiSaathi-microphone")
        consumer = Thread({
            var decodedEpoch = epoch
            try {
                while (active.get()) {
                    val frame = queue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    if (paused || frame.epoch != epoch) continue
                    if (decodedEpoch != frame.epoch) { recognizer.reset(); decodedEpoch = frame.epoch }
                    val final = recognizer.acceptWaveForm(frame.samples, frame.samples.size)
                    val json = if (final) recognizer.result else recognizer.partialResult
                    main.post {
                        if (active.get() && !paused && frame.epoch == epoch) {
                            if (final) listener.onResult(json) else listener.onPartialResult(json)
                        }
                    }
                }
            } catch (error: Exception) { fail(error) }
            finally {
                active.set(false)
                runCatching { audio.stop() }
                producer?.join()
                closeRecognizer()
            }
        }, "ShrutiSaathi-speech-decoder")
        producer!!.start()
        consumer!!.start()
        return true
    }

    private fun fail(error: Exception) {
        if (active.getAndSet(false)) main.post { listener?.onError(error) }
    }

    fun setPause(value: Boolean) {
        paused = value
        epoch++
        queue.clear()
    }

    fun stop() {
        active.set(false)
        queue.clear()
        runCatching { recorder?.stop() } // Unblock read; never join native decoding on the UI thread.
    }

    private fun closeRecognizer() {
        if (recognizerClosed.compareAndSet(false, true)) recognizer.close()
    }

    fun shutdown() {
        stop()
        if (consumer == null) closeRecognizer()
    }
}
