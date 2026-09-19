package com.aihearingassist

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.sqrt

class AudioCaptureController {
    private var recorder: AudioRecord? = null
    private var isRecording = false
    private var isPaused = false

    fun start(): Boolean {
        if (isRecording) return true
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer <= 0) return false

        val bufferSize = maxOf(minBuffer, 4096)
        val audioRecord = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (_: SecurityException) {
            return false
        } catch (_: IllegalArgumentException) {
            return false
        }

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            return false
        }

        try {
            audioRecord.startRecording()
        } catch (_: IllegalStateException) {
            audioRecord.release()
            return false
        } catch (_: SecurityException) {
            audioRecord.release()
            return false
        }

        recorder = audioRecord
        isRecording = true
        isPaused = false
        return true
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun stop() {
        isRecording = false
        isPaused = false
        try {
            recorder?.stop()
        } catch (_: IllegalStateException) {
            // Recorder may already be stopped by the platform.
        }
        recorder?.release()
        recorder = null
    }

    fun isActive(): Boolean = isRecording && !isPaused

    fun getActivityLevel(): Double {
        val buffer = ShortArray(160)
        val record = recorder ?: return 0.0
        if (!isActive()) return 0.0
        val read = record.read(buffer, 0, buffer.size)
        if (read <= 0) return 0.0
        var squareSum = 0.0
        for (index in 0 until read) {
            val sample = buffer[index].toDouble()
            squareSum += sample * sample
        }
        val rms = sqrt(squareSum / read)
        return rms / 32768.0
    }
}
