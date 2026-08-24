package com.indianservers.aiexplorer.arengine.session

import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArRuntime
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArTrackingGuidance
import com.indianservers.aiexplorer.arengine.contract.ArTrackingGuidancePolicy
import com.indianservers.aiexplorer.arengine.contract.ArVector2

data class ArLabSessionState(
    val activeLabId: String? = null,
    val runtimeState: ArRuntimeState = ArRuntimeState.Idle,
    val guidance: ArTrackingGuidance = ArTrackingGuidancePolicy.evaluate(null),
    val activeAnchor: ArAnchorHandle? = null,
    val measurementAnchors: List<ArAnchorHandle> = emptyList(),
    val lastError: String? = null,
)

class ArLabSessionController(private val runtime: ArRuntime) : AutoCloseable {
    var state = ArLabSessionState(runtimeState = runtime.state)
        private set

    fun activate(labId: String, cameraPermissionGranted: Boolean, userRequestedInstall: Boolean = false): ArLabSessionState {
        require(labId.isNotBlank())
        val prepared = runtime.prepare(cameraPermissionGranted, userRequestedInstall)
        val resumed = if (prepared is ArRuntimeState.Ready || prepared is ArRuntimeState.Paused || prepared is ArRuntimeState.Running) {
            runtime.resume().getOrElse { ArRuntimeState.RecoverableError(it.message ?: "AR session could not start", it.javaClass.name) }
        } else prepared
        state = state.copy(activeLabId = labId, runtimeState = resumed, lastError = errorMessage(resumed))
        return state
    }

    fun onFrame(frame: ArFrameSnapshot): ArLabSessionState {
        state = state.copy(runtimeState = runtime.state, guidance = ArTrackingGuidancePolicy.evaluate(frame), lastError = null)
        return state
    }

    fun setDepthNeeded(enabled: Boolean) = runtime.setDepthEnabled(enabled)

    fun hits(screenPoint: ArVector2): List<ArHitCandidate> = runtime.hitTest(screenPoint)

    fun place(hit: ArHitCandidate, nowMillis: Long): Result<ArAnchorHandle> {
        state.activeAnchor?.let { runtime.detachAnchor(it.id) }
        return runtime.createAnchor(hit.id, nowMillis).onSuccess { anchor ->
            state = state.copy(activeAnchor = anchor, lastError = null)
        }.onFailure { error ->
            state = state.copy(lastError = error.message ?: "Placement failed")
        }
    }

    fun addMeasurementPoint(screenPoint: ArVector2, nowMillis: Long): Result<ArAnchorHandle> {
        val hit = hits(screenPoint).firstOrNull()
            ?: return Result.failure(IllegalStateException("No reliable surface found at this point."))
        return runtime.createAnchor(hit.id, nowMillis).onSuccess { anchor ->
            state = state.copy(measurementAnchors = state.measurementAnchors + anchor, lastError = null)
        }.onFailure { error ->
            state = state.copy(lastError = error.message ?: "Measurement point could not be placed")
        }
    }

    fun removeLastMeasurementPoint(): Boolean {
        val anchor = state.measurementAnchors.lastOrNull() ?: return false
        val removed = runtime.detachAnchor(anchor.id)
        if (removed) state = state.copy(measurementAnchors = state.measurementAnchors.dropLast(1))
        return removed
    }

    fun clearMeasurementPoints() {
        state.measurementAnchors.forEach { runtime.detachAnchor(it.id) }
        state = state.copy(measurementAnchors = emptyList())
    }

    fun unlockAndRemoveAnchor(): Boolean {
        val anchor = state.activeAnchor ?: return false
        val removed = runtime.detachAnchor(anchor.id)
        if (removed) state = state.copy(activeAnchor = null)
        return removed
    }

    fun pause(): ArLabSessionState {
        state = state.copy(runtimeState = runtime.pause())
        return state
    }

    fun resume(): ArLabSessionState {
        val resumed = runtime.resume().getOrElse { ArRuntimeState.RecoverableError(it.message ?: "AR session could not resume", it.javaClass.name) }
        state = state.copy(runtimeState = resumed, lastError = errorMessage(resumed))
        return state
    }

    override fun close() {
        runtime.anchors().forEach { runtime.detachAnchor(it.id) }
        runtime.close()
        state = state.copy(runtimeState = ArRuntimeState.Closed, activeAnchor = null, measurementAnchors = emptyList())
    }

    private fun errorMessage(value: ArRuntimeState): String? = when (value) {
        is ArRuntimeState.RecoverableError -> value.message
        is ArRuntimeState.FatalError -> value.message
        is ArRuntimeState.Unsupported -> value.reason
        else -> null
    }
}
