package com.indianservers.aiexplorer.ar3dgraph.presentation

import androidx.lifecycle.SavedStateHandle
import com.indianservers.aiexplorer.ar3dgraph.ar.ARCapabilityResult
import com.indianservers.aiexplorer.ar3dgraph.ar.ARTrackingState
import com.indianservers.aiexplorer.ar3dgraph.ar.AnchorPlacementResult
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineColor
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineGraphResult
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineMeshSnapshot
import com.indianservers.aiexplorer.ar3dgraph.integration.EngineVector3
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphTransformState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class AR3DGraphViewModelTest {
    private class DirectExecutor : AbstractExecutorService() {
        private var shutdown = false
        override fun execute(command: Runnable) = command.run()
        override fun shutdown() { shutdown = true }
        override fun shutdownNow(): MutableList<Runnable> { shutdown = true; return mutableListOf() }
        override fun isShutdown() = shutdown
        override fun isTerminated() = shutdown
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = shutdown
    }

    private class QueuedExecutor : AbstractExecutorService() {
        var queued: Runnable? = null
        private var shutdown = false
        override fun execute(command: Runnable) { queued = command }
        override fun shutdown() { shutdown = true }
        override fun shutdownNow(): MutableList<Runnable> { shutdown = true; return mutableListOf() }
        override fun isShutdown() = shutdown
        override fun isTerminated() = shutdown
        override fun awaitTermination(timeout: Long, unit: TimeUnit) = shutdown
    }

    private fun model() = AR3DGraphViewModel(SavedStateHandle(), DirectExecutor()) { it() }

    @Test
    fun restoresPermissionRequestAndPermanentDenial() {
        val handle = SavedStateHandle()
        AR3DGraphViewModel(handle).apply {
            markCameraRequested()
            onCameraPermission(ARCameraPermissionState.PermissionPermanentlyDenied)
        }
        val restored = AR3DGraphViewModel(handle).uiState
        assertTrue(restored.hasRequestedCamera)
        assertEquals(ARCameraPermissionState.PermissionPermanentlyDenied, restored.cameraPermission)
    }

    @Test
    fun processRecreationRestoresGraphInputsAndSettings() {
        val handle = SavedStateHandle()
        AR3DGraphViewModel(handle).apply {
            onEquationsChanged("z=x+2*y\nz=x^2-y")
            onDomainMinimumChanged("-7")
            onDomainMaximumChanged("9")
            onDensityChanged("40")
        }

        val restored = AR3DGraphViewModel(handle).uiState
        assertEquals("z=x+2*y\nz=x^2-y", restored.equations)
        assertEquals("-7", restored.domainMinimum)
        assertEquals("9", restored.domainMaximum)
        assertEquals("40", restored.density)
    }

    @Test
    fun processRecreationRegeneratesValidGraphRestoresTransformAndRequiresFreshPlacement() {
        val handle = SavedStateHandle()
        val original = AR3DGraphViewModel(handle, DirectExecutor()) { it() }
        original.connect { request -> EngineGraphResult.Success(request, listOf(mesh())) }
        original.plot()
        original.onUserTransformChanged(ARGraphTransformState(35f, -20f, 1.7f))
        original.onPlacementResult(AnchorPlacementResult.Placed)

        val restored = AR3DGraphViewModel(handle, DirectExecutor()) { it() }
        assertEquals(ARGraphTransformState(35f, -20f, 1.7f), restored.uiState.userTransform)
        assertEquals(null, restored.uiState.renderData)
        restored.connect { request -> EngineGraphResult.Success(request, listOf(mesh())) }
        assertTrue(restored.uiState.renderData != null)
        assertEquals(ARGraphPlacementState.GraphReadyForPlacement, restored.uiState.placement)
        assertEquals("Graph restored. Tap to place again.", restored.uiState.graphMessage)
        restored.onPlacementResult(AnchorPlacementResult.Placed)
        assertEquals("3D graph placed 1.5 metres from the camera.", restored.uiState.graphMessage)
    }

    @Test
    fun unsupportedCapabilityProducesControlledState() {
        val model = AR3DGraphViewModel(SavedStateHandle())
        model.onCapabilityResult(ARCapabilityResult(ARCapabilityState.Unsupported, "unsupported"))
        assertEquals(ARCapabilityState.Unsupported, model.uiState.capability)
        assertEquals("unsupported", model.uiState.message)
    }

    @Test
    fun equationPlotReadyPlacedResetAndClearStatesAreExplicit() {
        val model = model()
        model.connect { request -> EngineGraphResult.Success(request, listOf(mesh())) }
        model.onEquationsChanged("z=x+2*y")
        model.plot()
        assertEquals(ARGraphPlacementState.GraphReadyForPlacement, model.uiState.placement)
        assertEquals(1, model.uiState.renderData?.meshes?.size)

        model.onTrackingChanged(ARTrackingState.Tracking, "Tracking active.")
        model.onPlacementStarted()
        assertEquals(ARGraphPlacementState.Placing, model.uiState.placement)
        model.onPlacementResult(AnchorPlacementResult.Placed)
        assertEquals(ARGraphPlacementState.Placed, model.uiState.placement)
        assertEquals("Drag to rotate • Pinch to resize • Tap to reposition", model.uiState.graphMessage)
        model.resetView()
        assertEquals("Graph rotation and size reset.", model.uiState.graphMessage)
        model.onPlacementResult(AnchorPlacementResult.Placed)
        assertEquals("3D graph placed 1.5 metres from the camera.", model.uiState.graphMessage)

        model.onUserTransformChanged(ARGraphTransformState(20f, 10f, 2f))

        model.resetPlacement()
        assertEquals(ARGraphPlacementState.GraphReadyForPlacement, model.uiState.placement)
        assertTrue(model.uiState.renderData != null)
        assertEquals(ARGraphTransformState(20f, 10f, 2f), model.uiState.userTransform)
        model.clearGraph()
        assertEquals(ARGraphPlacementState.Cleared, model.uiState.placement)
        assertEquals(null, model.uiState.renderData)
        assertEquals(ARGraphTransformState(), model.uiState.userTransform)
    }

    @Test
    fun invalidUpdatePreservesLastValidGraph() {
        val model = model()
        model.connect { request ->
            if (request.equations.single().expression == "bad") EngineGraphResult.ValidationError("invalid")
            else EngineGraphResult.Success(request, listOf(mesh()))
        }
        model.plot()
        val valid = model.uiState.renderData
        model.onEquationsChanged("bad")
        model.plot()
        assertEquals(valid, model.uiState.renderData)
        assertEquals("invalid", model.uiState.graphMessage)
    }

    @Test
    fun rapidConsecutivePlotsDeliverLatestRequest() {
        val expressions = mutableListOf<String>()
        val model = model()
        model.connect { request ->
            expressions += request.equations.single().expression
            EngineGraphResult.Success(request, listOf(mesh().copy(canonicalEquation = request.equations.single().expression)))
        }
        model.onEquationsChanged("z=x")
        model.plot()
        model.onEquationsChanged("z=y")
        model.plot()
        assertEquals(listOf("z=x", "z=y"), expressions)
        assertEquals("z=y", model.uiState.renderData?.meshes?.single()?.canonicalEquation)
    }

    @Test
    fun trackingPauseDoesNotDiscardGeneratedGraph() {
        val model = model()
        model.connect { request -> EngineGraphResult.Success(request, listOf(mesh())) }
        model.plot()
        model.onTrackingChanged(ARTrackingState.Paused, "paused")
        assertEquals(ARGraphPlacementState.WaitingForTracking, model.uiState.placement)
        assertTrue(model.uiState.renderData != null)
        model.onTrackingChanged(ARTrackingState.Tracking, "active")
        assertEquals(ARGraphPlacementState.GraphReadyForPlacement, model.uiState.placement)
    }

    @Test
    fun clearAndScreenExitCancelQueuedGenerationWithoutPublishingStaleData() {
        listOf<(AR3DGraphViewModel) -> Unit>(AR3DGraphViewModel::clearGraph, AR3DGraphViewModel::onScreenExit).forEach { cancel ->
            val executor = QueuedExecutor()
            val model = AR3DGraphViewModel(SavedStateHandle(), executor) { it() }
            model.connect { request -> EngineGraphResult.Success(request, listOf(mesh())) }
            model.plot()
            assertEquals(ARGraphPlacementState.GeneratingGraph, model.uiState.placement)
            val queued = requireNotNull(executor.queued)
            val future = queued as Future<*>

            cancel(model)

            assertTrue(future.isCancelled)
            assertEquals(null, model.uiState.renderData)
            assertTrue(model.uiState.placement == ARGraphPlacementState.Cleared || model.uiState.placement == ARGraphPlacementState.NoGraph)
            queued.run()
            assertEquals(null, model.uiState.renderData)
        }
    }

    @Test
    fun trackingAndPlacementFramesNeverRegenerateGeometry() {
        var generations = 0
        val model = model()
        model.connect { request ->
            generations++
            EngineGraphResult.Success(request, listOf(mesh()))
        }
        model.plot()
        repeat(100) {
            model.onTrackingChanged(ARTrackingState.Tracking, "active")
            model.onPlacementStarted()
            model.onPlacementResult(if (it == 0) AnchorPlacementResult.Placed else AnchorPlacementResult.Replaced)
        }
        assertEquals(1, generations)
    }

    @Test
    fun screenExitInvalidatesNativeAnchorButPreservesSerializableGraphState() {
        val model = model()
        model.connect { request -> EngineGraphResult.Success(request, listOf(mesh())) }
        model.plot()
        model.onUserTransformChanged(ARGraphTransformState(12f, 8f, 1.4f))
        model.onPlacementResult(AnchorPlacementResult.Placed)
        model.onScreenExit()
        assertEquals(ARGraphPlacementState.GraphReadyForPlacement, model.uiState.placement)
        assertEquals("AR view recreated. Tap to place again.", model.uiState.graphMessage)
        assertTrue(model.uiState.renderData != null)
        assertEquals(ARGraphTransformState(12f, 8f, 1.4f), model.uiState.userTransform)
    }

    @Test
    fun injectedEngineExceptionTerminatesLoadingAndSuccessfulRetryRecovers() {
        var fail = true
        val model = model()
        model.connect { request ->
            if (fail) error("injected engine failure")
            EngineGraphResult.Success(request, listOf(mesh()))
        }
        model.plot()
        assertEquals(ARGraphPlacementState.PlacementFailed, model.uiState.placement)
        assertTrue("injected engine failure" in model.uiState.graphMessage)
        assertEquals(null, model.uiState.renderData)

        fail = false
        model.plot()
        assertEquals(ARGraphPlacementState.GraphReadyForPlacement, model.uiState.placement)
        assertTrue(model.uiState.renderData != null)
    }

    private fun mesh() = EngineMeshSnapshot(
        "one", "z=x^2+y^2",
        listOf(
            EngineVector3(0.0, 0.0, 0.0), EngineVector3(0.0, 1.0, 1.0),
            EngineVector3(1.0, 0.0, 1.0), EngineVector3(1.0, 1.0, 2.0),
        ),
        2, 2, emptyList(),
        listOf(EngineColor(0f, 1f, 1f)), EngineColor(0f, 1f, 1f), 1f,
    )
}
