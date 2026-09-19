package com.aihearingassist

import org.junit.Assert.*
import org.junit.Test

class SummaryMemoryPolicyTest {
    private val mb = SummaryMemoryPolicy.MIB
    @Test fun fourGigabyteDevice_prefersCompactModel() {
        assertTrue(SummaryMemoryPolicy.preferCompact(4_096 * mb))
        assertFalse(SummaryMemoryPolicy.preferCompact(8_192 * mb))
    }
    @Test fun observedEmulatorBudget_rejectsLargeModel() {
        assertFalse(SummaryMemoryPolicy.canLoad(2_590 * mb, 2_750 * mb, false, 200 * mb))
    }
    @Test fun compactModel_hasRoomForCaptureAndSystem() {
        assertTrue(SummaryMemoryPolicy.canLoad(529 * mb, 2_750 * mb, false, 200 * mb))
    }
    @Test fun lowMemoryWarning_alwaysBlocksNewModelLoad() {
        assertFalse(SummaryMemoryPolicy.canLoad(529 * mb, 4_000 * mb, true, 200 * mb))
    }
    @Test fun systemThresholdAndMissingModel_areRespected() {
        assertFalse(SummaryMemoryPolicy.canLoad(529 * mb, 1_800 * mb, false, 700 * mb))
        assertFalse(SummaryMemoryPolicy.canLoad(0, 4_000 * mb, false, 200 * mb))
    }
}
