package com.indianservers.aiexplorer.ar3dgraph.ar

import com.indianservers.aiexplorer.ar3dgraph.presentation.ARSessionState
import com.indianservers.aiexplorer.ar3dgraph.presentation.ARCapabilityState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ARSessionLifecycleCoordinatorTest {
    @Test
    fun enterPauseResumeAndCloseFollowSafeTransitions() {
        val controller = FakeController()
        val lifecycle = ARSessionLifecycleCoordinator(controller)
        assertEquals(ARSessionState.SessionRunning, lifecycle.enter().state)
        assertEquals(ARSessionState.SessionPaused, lifecycle.pause().state)
        assertEquals(ARSessionState.SessionRunning, lifecycle.resume().state)
        lifecycle.close()
        assertEquals(ARSessionState.Closed, controller.state)
        assertTrue(controller.closed)
    }

    @Test
    fun repeatedEntryAndExitCreatesNoRetainedSession() {
        repeat(20) {
            val controller = FakeController()
            ARSessionLifecycleCoordinator(controller).run {
                assertEquals(ARSessionState.SessionRunning, enter().state)
                close()
            }
            assertTrue(controller.closed)
        }
    }

    @Test
    fun installationRequestStopsBeforeSessionCreationAndResume() {
        val controller = FakeController().apply { installCapability = ARCapabilityState.InstallationRequested }
        val result = ARSessionLifecycleCoordinator(controller).enter()
        assertEquals(ARCapabilityState.InstallationRequested, result.capability)
        assertEquals(0, controller.createCalls)
        assertEquals(0, controller.resumeCalls)
    }

    @Test
    fun resumeOutsidePausedStateDoesNotCreateDuplicateRendererSessionLoop() {
        val controller = FakeController()
        val lifecycle = ARSessionLifecycleCoordinator(controller)
        lifecycle.enter()
        assertEquals(1, controller.resumeCalls)
        assertEquals(ARSessionState.SessionRunning, lifecycle.resume().state)
        assertEquals(1, controller.resumeCalls)
        lifecycle.pause()
        lifecycle.resume()
        assertEquals(2, controller.resumeCalls)
    }

    @Test
    fun rapidDuplicatePauseResumeAndCloseRemainIdempotentAtCoordinatorBoundary() {
        val controller = FakeController()
        val lifecycle = ARSessionLifecycleCoordinator(controller)
        lifecycle.enter()
        repeat(20) {
            lifecycle.pause()
            lifecycle.resume()
        }
        lifecycle.close()
        lifecycle.close()
        assertTrue(controller.closed)
        assertEquals(ARSessionState.Closed, controller.state)
    }

    private class FakeController : ARSessionController {
        override var state = ARSessionState.Idle
        var closed = false
        var installCapability: ARCapabilityState? = null
        var createCalls = 0
        var resumeCalls = 0
        override fun requestInstall(userRequested: Boolean) = ARSessionTransition(state, "installed", installCapability)
        override fun create() = ARSessionTransition(ARSessionState.Ready, "ready").also { createCalls++; state = it.state }
        override fun resume() = ARSessionTransition(ARSessionState.SessionRunning, "running").also { resumeCalls++; state = it.state }
        override fun pause() = ARSessionTransition(ARSessionState.SessionPaused, "paused").also { state = it.state }
        override fun close() { closed = true; state = ARSessionState.Closed }
    }
}
