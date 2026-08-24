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

class ArLabSessionControllerTest {
    @Test
    fun controllerOwnsLifecycleMeasurementAnchorsAndCleanup() {
        val hit = ArHitCandidate("surface", ArHitType.Plane, ArPose(ArVector3(0.0, 0.0, -1.0)), 1.0, .9, .02)
        val runtime = FakeArRuntime(frames = listOf(frame()), hits = listOf(hit))
        val controller = ArLabSessionController(runtime)
        assertTrue(controller.activate("ar-coordinate-plane", true).runtimeState is ArRuntimeState.Running)
        controller.onFrame(runtime.updateFrame().getOrThrow())
        assertEquals(ArStudentTrackingStatus.SearchingForSurface, controller.state.guidance.status)
        controller.addMeasurementPoint(ArVector2(100f, 100f), 10L).getOrThrow()
        assertEquals(1, controller.state.measurementAnchors.size)
        assertTrue(controller.removeLastMeasurementPoint())
        controller.close()
        assertEquals(ArRuntimeState.Closed, runtime.state)
    }

    private fun frame() = ArFrameSnapshot(
        1L,
        ArCameraSnapshot(ArPose(), ArTrackingState.Tracking),
    )
}
