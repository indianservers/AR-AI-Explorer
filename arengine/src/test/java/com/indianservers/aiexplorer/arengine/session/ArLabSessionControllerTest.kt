package com.indianservers.aiexplorer.arengine.session

import com.indianservers.aiexplorer.arengine.contract.ArCameraSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitType
import com.indianservers.aiexplorer.arengine.contract.ArPose
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArStudentTrackingStatus
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArVector2
import com.indianservers.aiexplorer.arengine.contract.ArVector3
import com.indianservers.aiexplorer.arengine.simulator.FakeArRuntime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import com.indianservers.aiexplorer.arengine.contract.ArPlaneSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArPlaneOrientation

class ArLabSessionControllerTest {
    @Test
    fun controllerOwnsLifecycleMeasurementAnchorsAndCleanup() {
        val hit = ArHitCandidate("surface", ArHitType.Plane, ArPose(ArVector3(0.0, 0.0, -1.0)), 1.0, .9, .02, trackableId = "floor")
        val runtime = FakeArRuntime(frames = listOf(frame()), hits = listOf(hit))
        val controller = ArLabSessionController(runtime)
        assertTrue(controller.activate("ar-coordinate-plane", true).runtimeState is ArRuntimeState.Running)
        controller.onFrame(runtime.updateFrame().getOrThrow())
        assertEquals(ArStudentTrackingStatus.SearchingForSurface, controller.state.guidance.status)
        val tracked = frame().copy(timestampNanos = 1_000_000_000L, planes = listOf(ArPlaneSnapshot("floor", ArPose(), ArPlaneOrientation.HorizontalUp, 2.0, 2.0)))
        controller.onFrame(tracked)
        assertTrue(controller.hits(ArVector2(100f, 100f)).isEmpty())
        controller.onFrame(tracked.copy(timestampNanos = 1_300_000_000L))
        controller.addMeasurementPoint(ArVector2(100f, 100f), 10L).getOrThrow()
        assertEquals(1, controller.state.measurementAnchors.size)
        assertTrue(controller.removeLastMeasurementPoint())
        controller.close()
        assertEquals(ArRuntimeState.Closed, runtime.state)
    }

    @Test
    fun failedReplacementPreservesExistingAnchor() {
        val hit = ArHitCandidate("surface", ArHitType.Plane, ArPose(), 1.0, .9, .02, trackableId = "floor")
        val runtime = FakeArRuntime(hits = listOf(hit))
        val controller = ArLabSessionController(runtime)
        controller.activate("graph3d", true)
        val tracked = frame().copy(timestampNanos = 1_000_000_000L, planes = listOf(ArPlaneSnapshot("floor", ArPose(), ArPlaneOrientation.HorizontalUp, 2.0, 2.0)))
        controller.onFrame(tracked)
        controller.onFrame(tracked.copy(timestampNanos = 1_300_000_000L))
        val anchor = controller.place(hit, 10L).getOrThrow()
        assertTrue(controller.place(hit.copy(id = "expired"), 20L).isFailure)
        assertEquals(anchor, controller.state.activeAnchor)
        assertEquals(1, runtime.anchors().size)
        controller.pause()
        assertTrue(controller.hits(ArVector2(0f, 0f)).isEmpty())
        controller.close()
    }

    private fun frame() = ArFrameSnapshot(
        1L,
        ArCameraSnapshot(ArPose(), ArTrackingState.Tracking),
    )
}
