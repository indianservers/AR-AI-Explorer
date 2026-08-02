package com.indianservers.aiexplorer.gamifymaths

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RescueEngineerGameTest {
    @Test
    fun exactConnectedTwelveMetreTrussPasses() {
        val result = assessRescueBridge(
            targetSpan = 12,
            beams = listOf(4, 4, 4),
            joints = 2,
            minimumSegments = 3,
        )

        assertTrue(result.exactSpan)
        assertTrue(result.connected)
        assertTrue(result.strong)
        assertTrue(result.success)
    }

    @Test
    fun correctSpanWithoutEnoughJointsFails() {
        val result = assessRescueBridge(
            targetSpan = 12,
            beams = listOf(4, 4, 4),
            joints = 1,
            minimumSegments = 3,
        )

        assertTrue(result.exactSpan)
        assertFalse(result.connected)
        assertFalse(result.success)
    }

    @Test
    fun connectedBridgeWithWrongSpanFails() {
        val result = assessRescueBridge(
            targetSpan = 12,
            beams = listOf(3, 3, 3),
            joints = 2,
            minimumSegments = 3,
        )

        assertFalse(result.exactSpan)
        assertTrue(result.connected)
        assertFalse(result.success)
    }
}
