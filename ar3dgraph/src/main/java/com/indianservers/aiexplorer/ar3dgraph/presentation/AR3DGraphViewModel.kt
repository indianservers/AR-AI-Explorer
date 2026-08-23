package com.indianservers.aiexplorer.ar3dgraph.presentation

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.indianservers.aiexplorer.ar3dgraph.ar.ARCapabilityResult
import com.indianservers.aiexplorer.ar3dgraph.ar.ARSessionTransition
import com.indianservers.aiexplorer.ar3dgraph.ar.ARTrackingState
import com.indianservers.aiexplorer.ar3dgraph.ar.AnchorPlacementResult
import com.indianservers.aiexplorer.ar3dgraph.integration.ARGraphAdapterResult
import com.indianservers.aiexplorer.ar3dgraph.integration.DisconnectedGraphEngineContract
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEngineAdapter
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEngineContract
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphEquationRequest
import com.indianservers.aiexplorer.ar3dgraph.integration.GraphGenerationRequest
import com.indianservers.aiexplorer.ar3dgraph.gesture.ARGraphTransformState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class AR3DGraphViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var dispatch: ((() -> Unit) -> Unit) = { block -> Handler(Looper.getMainLooper()).post(block) }

    internal constructor(
        savedStateHandle: SavedStateHandle,
        executor: ExecutorService,
        dispatch: ((() -> Unit) -> Unit),
    ) : this(savedStateHandle) {
        this.executor.shutdownNow()
        this.executor = executor
        this.dispatch = dispatch
    }

    var uiState by mutableStateOf(restore())
        private set

    private var adapter = GraphEngineAdapter(DisconnectedGraphEngineContract)
    private var generation: Future<*>? = null
    private var generationId = 0L
    private var gestureGuidanceShown = savedStateHandle[KEY_GUIDANCE_SHOWN] ?: false
    private var restoreGraphOnConnect = savedStateHandle[KEY_HAD_VALID_GRAPH] ?: false

    fun connect(contract: GraphEngineContract) {
        adapter = GraphEngineAdapter(contract)
        if (restoreGraphOnConnect && uiState.renderData == null && adapter.isConnected()) {
            restoreGraphOnConnect = false
            generate(restoring = true)
        }
    }

    fun beginCapabilityCheck() = update {
        it.copy(capability = ARCapabilityState.Checking, message = "Checking ARCore availability…")
    }

    fun onCapabilityResult(result: ARCapabilityResult) = update {
        it.copy(capability = result.state, message = result.message)
    }

    fun markCameraRequested() = update { it.copy(hasRequestedCamera = true) }

    fun onCameraPermission(state: ARCameraPermissionState) = update {
        it.copy(
            cameraPermission = state,
            message = when (state) {
                ARCameraPermissionState.Granted -> "Camera permission granted."
                ARCameraPermissionState.PermissionRequired -> "Camera permission is required only for AR 3D Graph."
                ARCameraPermissionState.PermissionDenied -> "Camera permission was denied. You can try again."
                ARCameraPermissionState.PermissionPermanentlyDenied -> "Camera permission is disabled. Open app settings to enable it."
            },
        )
    }

    fun onSessionTransition(transition: ARSessionTransition) = update {
        it.copy(
            capability = transition.capability ?: it.capability,
            session = transition.state,
            message = transition.message,
        )
    }

    fun onSceneViewSessionRunning(message: String = "ARCore + SceneView session is running.") = update {
        it.copy(
            capability = ARCapabilityState.Supported,
            session = ARSessionState.SessionRunning,
            message = message,
        )
    }

    fun onSceneViewSessionPaused() = update {
        it.copy(session = ARSessionState.SessionPaused, message = "AR session paused.")
    }

    fun onSceneViewSessionFailed(message: String) = update {
        it.copy(session = ARSessionState.SessionError, placement = ARGraphPlacementState.PlacementFailed, message = message, graphMessage = message)
    }

    fun onInstallationRequested() = update {
        it.copy(capability = ARCapabilityState.InstallationRequested, message = "Complete the ARCore installation or update, then return and retry.")
    }

    fun onEquationsChanged(value: String) = update { it.copy(equations = value) }
    fun onDomainMinimumChanged(value: String) = update { it.copy(domainMinimum = value) }
    fun onDomainMaximumChanged(value: String) = update { it.copy(domainMaximum = value) }
    fun onDensityChanged(value: String) = update { it.copy(density = value.filter(Char::isDigit).take(2)) }

    fun plot() = generate(restoring = false)

    private fun generate(restoring: Boolean) {
        val request = requestFrom(uiState).getOrElse { error ->
            update { it.copy(graphMessage = error.message ?: "Invalid graph settings.") }
            return
        }
        generation?.cancel(true)
        val id = ++generationId
        update { it.copy(placement = ARGraphPlacementState.GeneratingGraph, graphMessage = "Generating graph…") }
        generation = executor.submit {
            val result = adapter.generate(request)
            if (Thread.currentThread().isInterrupted) return@submit
            dispatch {
                if (id != generationId) return@dispatch
                when (result) {
                    is ARGraphAdapterResult.Success -> update { state ->
                        val placement = if (state.placement == ARGraphPlacementState.Placed) ARGraphPlacementState.Placed
                        else ARGraphPlacementState.GraphReadyForPlacement
                        state.copy(
                            renderData = result.data,
                            placement = if (restoring) ARGraphPlacementState.GraphReadyForPlacement else placement,
                            graphMessage = if (restoring) "Graph restored. Tap to place again."
                            else "${result.data.meshes.size} graph${if (result.data.meshes.size == 1) "" else "s"} ready. Tap anywhere to place the 3D graph.",
                        )
                    }
                    is ARGraphAdapterResult.ValidationError -> update {
                        it.copy(
                            placement = if (it.renderData == null) ARGraphPlacementState.NoGraph else it.placement,
                            graphMessage = result.message,
                        )
                    }
                    is ARGraphAdapterResult.GenerationError -> update {
                        it.copy(
                            placement = if (it.renderData == null) ARGraphPlacementState.PlacementFailed else it.placement,
                            graphMessage = result.message,
                        )
                    }
                }
            }
        }
    }

    fun onTrackingChanged(state: ARTrackingState, message: String) = update {
        val placement = when {
            state != ARTrackingState.Tracking && it.placement == ARGraphPlacementState.GraphReadyForPlacement ->
                ARGraphPlacementState.WaitingForTracking
            state == ARTrackingState.Tracking && it.placement == ARGraphPlacementState.WaitingForTracking ->
                ARGraphPlacementState.GraphReadyForPlacement
            else -> it.placement
        }
        it.copy(tracking = state, placement = placement, graphMessage = if (it.renderData == null) it.graphMessage else message)
    }

    fun onPlacementStarted() = update {
        it.copy(
            placement = if (it.placement == ARGraphPlacementState.Placed) ARGraphPlacementState.Repositioning else ARGraphPlacementState.Placing,
            graphMessage = "Placing graph…",
        )
    }

    fun onPlacementResult(result: AnchorPlacementResult) = update {
        when (result) {
            AnchorPlacementResult.Placed -> {
                val message = if (gestureGuidanceShown) "3D graph placed 1.5 metres from the camera."
                else "Drag to rotate • Pinch to resize • Tap to reposition"
                gestureGuidanceShown = true
                it.copy(placement = ARGraphPlacementState.Placed, graphMessage = message)
            }
            AnchorPlacementResult.Replaced -> it.copy(placement = ARGraphPlacementState.Placed, graphMessage = "3D graph repositioned.")
            AnchorPlacementResult.TrackingPaused -> it.copy(placement = ARGraphPlacementState.WaitingForTracking, graphMessage = "Tracking is paused. Move slowly, then tap again.")
            is AnchorPlacementResult.Failed -> it.copy(placement = ARGraphPlacementState.PlacementFailed, graphMessage = result.message)
        }
    }

    fun onRendererError(message: String) = update {
        it.copy(placement = ARGraphPlacementState.PlacementFailed, graphMessage = "Renderer error: $message")
    }

    fun prepareSessionRetry() = update {
        it.copy(session = ARSessionState.Idle, message = "Starting AR…")
    }

    fun resetPlacement() = update {
        it.copy(
            placement = if (it.renderData == null) ARGraphPlacementState.NoGraph else ARGraphPlacementState.GraphReadyForPlacement,
            graphMessage = if (it.renderData == null) "Enter an equation and tap Plot." else "Tap anywhere to place the 3D graph.",
        )
    }

    fun resetView() = update {
        it.copy(
            userTransform = ARGraphTransformState(),
            graphMessage = if (it.placement == ARGraphPlacementState.Placed) "Graph rotation and size reset." else it.graphMessage,
        )
    }

    fun onUserTransformChanged(transform: ARGraphTransformState) = update {
        it.copy(userTransform = transform)
    }

    fun clearGraph() {
        generation?.cancel(true)
        generation = null
        generationId++
        update {
            it.copy(
                renderData = null,
                userTransform = ARGraphTransformState(),
                placement = ARGraphPlacementState.Cleared,
                graphMessage = "Graph cleared. Enter an equation and tap Plot.",
            )
        }
    }

    /** Cancels screen-owned work when navigation or lifecycle disposal removes the AR destination. */
    fun onScreenExit() {
        generation?.cancel(true)
        generation = null
        generationId++
        if (uiState.placement == ARGraphPlacementState.GeneratingGraph) {
            update {
                it.copy(
                    placement = if (it.renderData == null) ARGraphPlacementState.NoGraph else ARGraphPlacementState.GraphReadyForPlacement,
                    graphMessage = if (it.renderData == null) "Graph generation cancelled." else "Tap anywhere to place the 3D graph.",
                )
            }
        } else if (uiState.renderData != null && uiState.placement in setOf(
                ARGraphPlacementState.Placed,
                ARGraphPlacementState.Placing,
                ARGraphPlacementState.Repositioning,
            )
        ) {
            update {
                it.copy(
                    placement = ARGraphPlacementState.GraphReadyForPlacement,
                    graphMessage = "AR view recreated. Tap to place again.",
                )
            }
        }
    }

    private fun requestFrom(state: AR3DGraphUiState): Result<GraphGenerationRequest> = runCatching {
        val minimum = state.domainMinimum.toDoubleOrNull() ?: error("Enter a valid domain minimum.")
        val maximum = state.domainMaximum.toDoubleOrNull() ?: error("Enter a valid domain maximum.")
        require(minimum < maximum) { "Domain minimum must be less than domain maximum." }
        val density = state.density.toIntOrNull() ?: error("Enter a valid resolution.")
        require(density in 8..56) { "Resolution must be between 8 and 56." }
        val equations = state.equations.lines().map(String::trim).filter(String::isNotBlank)
        require(equations.isNotEmpty()) { "Enter at least one equation." }
        GraphGenerationRequest(
            equations = equations.mapIndexed { index, expression ->
                GraphEquationRequest("ar-surface-$index", expression, colorIndex = index)
            },
            domainMinimum = minimum,
            domainMaximum = maximum,
            density = density,
        )
    }

    private fun update(transform: (AR3DGraphUiState) -> AR3DGraphUiState) {
        uiState = transform(uiState)
        savedStateHandle[KEY_PERMISSION] = uiState.cameraPermission.name
        savedStateHandle[KEY_REQUESTED] = uiState.hasRequestedCamera
        savedStateHandle[KEY_MESSAGE] = uiState.message
        savedStateHandle[KEY_EQUATIONS] = uiState.equations
        savedStateHandle[KEY_DOMAIN_MIN] = uiState.domainMinimum
        savedStateHandle[KEY_DOMAIN_MAX] = uiState.domainMaximum
        savedStateHandle[KEY_DENSITY] = uiState.density
        savedStateHandle[KEY_HAD_VALID_GRAPH] = uiState.renderData != null
        savedStateHandle[KEY_GUIDANCE_SHOWN] = gestureGuidanceShown
        savedStateHandle[KEY_YAW] = uiState.userTransform.yawDegrees
        savedStateHandle[KEY_PITCH] = uiState.userTransform.pitchDegrees
        savedStateHandle[KEY_SCALE] = uiState.userTransform.uniformScale
    }

    private fun restore(): AR3DGraphUiState = AR3DGraphUiState(
        cameraPermission = savedStateHandle.get<String>(KEY_PERMISSION)
            ?.let { runCatching { ARCameraPermissionState.valueOf(it) }.getOrNull() }
            ?: ARCameraPermissionState.PermissionRequired,
        hasRequestedCamera = savedStateHandle[KEY_REQUESTED] ?: false,
        message = savedStateHandle[KEY_MESSAGE] ?: "Checking ARCore availability…",
        equations = savedStateHandle[KEY_EQUATIONS] ?: "z = x^2 + y^2",
        domainMinimum = savedStateHandle[KEY_DOMAIN_MIN] ?: "-3",
        domainMaximum = savedStateHandle[KEY_DOMAIN_MAX] ?: "3",
        density = savedStateHandle[KEY_DENSITY] ?: "26",
        userTransform = ARGraphTransformState(
            yawDegrees = savedStateHandle[KEY_YAW] ?: 0f,
            pitchDegrees = savedStateHandle[KEY_PITCH] ?: 0f,
            uniformScale = savedStateHandle[KEY_SCALE] ?: 1f,
        ),
    )

    override fun onCleared() {
        onScreenExit()
        executor.shutdownNow()
        super.onCleared()
    }

    private companion object {
        const val KEY_PERMISSION = "ar3dgraph.permission"
        const val KEY_REQUESTED = "ar3dgraph.permission.requested"
        const val KEY_MESSAGE = "ar3dgraph.message"
        const val KEY_EQUATIONS = "ar3dgraph.equations"
        const val KEY_DOMAIN_MIN = "ar3dgraph.domain.minimum"
        const val KEY_DOMAIN_MAX = "ar3dgraph.domain.maximum"
        const val KEY_DENSITY = "ar3dgraph.density"
        const val KEY_HAD_VALID_GRAPH = "ar3dgraph.graph.valid"
        const val KEY_GUIDANCE_SHOWN = "ar3dgraph.guidance.shown"
        const val KEY_YAW = "ar3dgraph.transform.yaw"
        const val KEY_PITCH = "ar3dgraph.transform.pitch"
        const val KEY_SCALE = "ar3dgraph.transform.scale"
    }
}
