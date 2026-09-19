package com.aihearingassist

import kotlin.math.sqrt

data class SpeakerAssignment(
    val label: String,
    val clusterIndex: Int?,
    val cosineSimilarity: Double,
    val usedVoiceEmbedding: Boolean,
    val createdNewCluster: Boolean
)

/**
 * Incremental on-device clustering for Vosk speaker x-vectors.
 *
 * This is speaker diarization (who spoke when), not biometric identity
 * verification. Configured names are assigned to newly discovered voice
 * clusters in first-heard order.
 */
class OnlineSpeakerDiarizer(
    labels: List<String> = emptyList(),
    private val newSpeakerDistanceThreshold: Double = DEFAULT_NEW_SPEAKER_DISTANCE
) {
    private data class Cluster(
        var centroid: DoubleArray,
        var accumulatedWeight: Double
    )

    private val clusters = mutableListOf<Cluster>()
    private var speakerLabels = sanitizeLabels(labels)
    private var lastClusterIndex: Int? = null

    @Synchronized
    fun reset(labels: List<String> = speakerLabels) {
        speakerLabels = sanitizeLabels(labels)
        clusters.clear()
        lastClusterIndex = null
    }

    @Synchronized
    fun assign(
        embedding: DoubleArray?,
        speakerFrames: Int,
        fallbackLabel: String
    ): SpeakerAssignment {
        val vector = embedding?.normalizedOrNull()
        if (vector == null) {
            val fallbackIndex = lastClusterIndex
            return SpeakerAssignment(
                label = fallbackIndex?.let(::labelFor) ?: fallbackLabel,
                clusterIndex = fallbackIndex,
                cosineSimilarity = 0.0,
                usedVoiceEmbedding = false,
                createdNewCluster = false
            )
        }

        if (clusters.isEmpty()) {
            clusters += Cluster(vector, weightFor(speakerFrames))
            lastClusterIndex = 0
            return SpeakerAssignment(labelFor(0), 0, 1.0, true, true)
        }

        val distances = clusters.map { cosineDistance(vector, it.centroid) }
        val bestIndex = distances.indices.minBy { distances[it] }
        val bestDistance = distances[bestIndex]
        val mayCreateCluster = clusters.size < maximumSpeakerCount()
        val createNew = mayCreateCluster && bestDistance > newSpeakerDistanceThreshold
        val assignedIndex = if (createNew) {
            clusters += Cluster(vector, weightFor(speakerFrames))
            clusters.lastIndex
        } else {
            updateCentroid(clusters[bestIndex], vector, speakerFrames, bestDistance)
            bestIndex
        }

        lastClusterIndex = assignedIndex
        return SpeakerAssignment(
            label = labelFor(assignedIndex),
            clusterIndex = assignedIndex,
            cosineSimilarity = (1.0 - bestDistance).coerceIn(0.0, 1.0),
            usedVoiceEmbedding = true,
            createdNewCluster = createNew
        )
    }

    @Synchronized
    fun discoveredSpeakerCount(): Int = clusters.size

    private fun updateCentroid(
        cluster: Cluster,
        vector: DoubleArray,
        speakerFrames: Int,
        distance: Double
    ) {
        // Avoid contaminating a cluster with a borderline forced assignment.
        if (distance > MAX_CENTROID_UPDATE_DISTANCE) return
        val observationWeight = weightFor(speakerFrames)
        val totalWeight = cluster.accumulatedWeight + observationWeight
        val merged = DoubleArray(vector.size) { index ->
            (cluster.centroid[index] * cluster.accumulatedWeight + vector[index] * observationWeight) /
                totalWeight
        }
        cluster.centroid = merged.normalizedOrNull() ?: cluster.centroid
        cluster.accumulatedWeight = totalWeight.coerceAtMost(MAX_ACCUMULATED_WEIGHT)
    }

    private fun labelFor(index: Int): String {
        return speakerLabels.getOrNull(index) ?: "Speaker ${index + 1}"
    }

    private fun maximumSpeakerCount(): Int {
        return speakerLabels.size.takeIf { it > 0 } ?: SessionSetupValidator.MAX_SPEAKERS
    }

    private fun weightFor(frames: Int): Double {
        return frames.coerceIn(MIN_WEIGHT_FRAMES, MAX_WEIGHT_FRAMES).toDouble()
    }

    private fun DoubleArray.normalizedOrNull(): DoubleArray? {
        if (isEmpty() || any { !it.isFinite() }) return null
        val norm = sqrt(sumOf { it * it })
        if (!norm.isFinite() || norm < MIN_VECTOR_NORM) return null
        return DoubleArray(size) { this[it] / norm }
    }

    private fun cosineDistance(left: DoubleArray, right: DoubleArray): Double {
        if (left.size != right.size || left.isEmpty()) return 1.0
        return (1.0 - left.indices.sumOf { left[it] * right[it] }).coerceIn(0.0, 2.0)
    }

    private fun sanitizeLabels(labels: List<String>): List<String> {
        return labels.mapIndexed { index, label ->
            label.trim().takeIf { it.isNotBlank() } ?: "Speaker ${index + 1}"
        }
    }

    private companion object {
        // Research parameter: expose in evaluation tooling before publication.
        const val DEFAULT_NEW_SPEAKER_DISTANCE = 0.48
        const val MAX_CENTROID_UPDATE_DISTANCE = 0.62
        const val MIN_VECTOR_NORM = 1e-9
        const val MIN_WEIGHT_FRAMES = 50
        const val MAX_WEIGHT_FRAMES = 400
        const val MAX_ACCUMULATED_WEIGHT = 2_000.0
    }
}
