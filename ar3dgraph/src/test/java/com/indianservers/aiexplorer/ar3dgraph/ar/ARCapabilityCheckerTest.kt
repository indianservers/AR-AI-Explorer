package com.indianservers.aiexplorer.ar3dgraph.ar

import com.indianservers.aiexplorer.ar3dgraph.presentation.ARCapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ARCapabilityCheckerTest {
    @Test
    fun mapsSupportedMissingUpdateUnsupportedAndErrorStates() {
        val expected = mapOf(
            ARAvailabilityCode.Supported to ARCapabilityState.Supported,
            ARAvailabilityCode.NotInstalled to ARCapabilityState.ARCoreNotInstalled,
            ARAvailabilityCode.UpdateRequired to ARCapabilityState.ARCoreUpdateRequired,
            ARAvailabilityCode.Unsupported to ARCapabilityState.Unsupported,
            ARAvailabilityCode.Error to ARCapabilityState.Error,
        )
        expected.forEach { (code, state) ->
            val checker = checkerFor(code)
            assertEquals(state, checker.map(code).state)
        }
    }

    @Test
    fun transientAvailabilityRetriesThenReturnsSupported() {
        val probe = SequenceProbe(ARAvailabilityCode.Checking, ARAvailabilityCode.TimedOut, ARAvailabilityCode.Supported)
        val results = mutableListOf<ARCapabilityResult>()
        ARCapabilityChecker(probe, ARRetryScheduler { _, action -> action() }).check(results::add)
        assertEquals(3, probe.calls)
        assertEquals(listOf(ARCapabilityState.Supported), results.map { it.state })
    }

    @Test
    fun transientAvailabilityStopsAfterBoundedAttempts() {
        val probe = SequenceProbe(ARAvailabilityCode.Checking, ARAvailabilityCode.Checking, ARAvailabilityCode.Checking)
        val results = mutableListOf<ARCapabilityResult>()
        ARCapabilityChecker(probe, ARRetryScheduler { _, action -> action() }, maxTransientChecks = 3).check(results::add)
        assertEquals(3, probe.calls)
        assertEquals(ARCapabilityState.Error, results.single().state)
    }

    @Test
    fun cancellationSuppressesLateAvailabilityCallbackAndRetryLoop() {
        var callback: ((ARAvailabilityCode) -> Unit)? = null
        val scheduled = mutableListOf<() -> Unit>()
        val results = mutableListOf<ARCapabilityResult>()
        val checker = ARCapabilityChecker(
            probe = ARAvailabilityProbe { callback = it },
            scheduler = ARRetryScheduler { _, action -> scheduled += action },
        )
        checker.check(results::add)
        checker.cancel()
        callback?.invoke(ARAvailabilityCode.Checking)
        scheduled.forEach { it() }
        assertTrue(results.isEmpty())
        assertTrue(scheduled.isEmpty())
    }

    private fun checkerFor(code: ARAvailabilityCode) = ARCapabilityChecker(
        probe = ARAvailabilityProbe { it(code) },
        scheduler = ARRetryScheduler { _, action -> action() },
    )

    private class SequenceProbe(vararg values: ARAvailabilityCode) : ARAvailabilityProbe {
        private val queue = ArrayDeque(values.toList())
        var calls = 0
        override fun check(callback: (ARAvailabilityCode) -> Unit) {
            calls++
            callback(queue.removeFirstOrNull() ?: ARAvailabilityCode.Error)
        }
    }
}
