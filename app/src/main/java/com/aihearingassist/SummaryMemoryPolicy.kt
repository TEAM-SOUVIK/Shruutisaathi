package com.aihearingassist

/** Conservative admission check, not a guarantee of native model peak memory. */
object SummaryMemoryPolicy {
    const val MIB = 1024L * 1024L
    fun preferCompact(totalBytes: Long): Boolean = totalBytes < 6L * 1024 * MIB

    fun canLoad(modelBytes: Long, availableBytes: Long, lowMemory: Boolean, thresholdBytes: Long): Boolean {
        if (modelBytes <= 0 || lowMemory) return false
        val estimatedWorkingSet = modelBytes + modelBytes / 2 + 512 * MIB
        val systemReserve = maxOf(384 * MIB, thresholdBytes)
        return availableBytes >= estimatedWorkingSet + systemReserve
    }
}
