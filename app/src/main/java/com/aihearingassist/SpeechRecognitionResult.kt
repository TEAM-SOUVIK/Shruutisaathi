package com.aihearingassist

/** One finalized ASR segment, optionally enriched with a Vosk x-vector. */
data class SpeechRecognitionResult(
    val text: String,
    val speakerEmbedding: DoubleArray? = null,
    val speakerFrames: Int = 0,
    val wordConfidence: Double? = null
)
