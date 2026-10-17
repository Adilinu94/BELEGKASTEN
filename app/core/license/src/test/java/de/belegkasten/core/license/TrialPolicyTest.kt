package de.belegkasten.core.license

import kotlin.test.*

class TrialPolicyTest {
    @Test fun boundaries() {
        assertTrue(trialStatus(false, 0, 29 * 86400000L, 49).captureEnabled)
        assertFalse(trialStatus(false, 0, 30 * 86400000L, 0).captureEnabled)
        assertFalse(trialStatus(false, 0, 0, 50).captureEnabled)
        assertTrue(trialStatus(true, 0, 60 * 86400000L, 100).captureEnabled)
    }
    @Test fun countersNeverNegative() { assertEquals(0, trialStatus(false, 0, 100 * 86400000L, 100).daysLeft); assertEquals(0, trialStatus(false, 0, 0, Int.MAX_VALUE).receiptsLeft) }
    @Test fun clockBeforeFirstStartDoesNotGrantExtraDays() { assertEquals(30, trialStatus(false, 100, 0, 0).daysLeft) }
}
