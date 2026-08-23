package com.indianservers.aiexplorer.ar3dgraph.ar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ARTrackingStabilizerTest {
    @Test fun firstStateIsImmediate() {
        assertEquals(ARTrackingState.Initializing, ARTrackingStabilizer().update(ARTrackingState.Initializing, 0))
    }

    @Test fun rapidAlternationDoesNotFlash() {
        val value = ARTrackingStabilizer(400)
        value.update(ARTrackingState.Tracking, 0)
        assertNull(value.update(ARTrackingState.Paused, 10))
        assertNull(value.update(ARTrackingState.Tracking, 20))
        assertNull(value.update(ARTrackingState.Paused, 30))
    }

    @Test fun stableTransitionEmitsOnce() {
        val value = ARTrackingStabilizer(400)
        value.update(ARTrackingState.Tracking, 0)
        assertNull(value.update(ARTrackingState.Paused, 10))
        assertNull(value.update(ARTrackingState.Paused, 409))
        assertEquals(ARTrackingState.Paused, value.update(ARTrackingState.Paused, 410))
        assertNull(value.update(ARTrackingState.Paused, 900))
    }

    @Test fun resetMakesNextStateImmediate() {
        val value = ARTrackingStabilizer()
        value.update(ARTrackingState.Tracking, 0)
        value.reset()
        assertEquals(ARTrackingState.Paused, value.update(ARTrackingState.Paused, 1))
    }
}
