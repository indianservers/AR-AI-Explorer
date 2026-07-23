package com.indianservers.aiexplorer.arengine.simulator

import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState
import com.indianservers.aiexplorer.arengine.contract.ArAvailability
import com.indianservers.aiexplorer.arengine.contract.ArCapabilities
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitPolicy
import com.indianservers.aiexplorer.arengine.contract.ArRuntime
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArVector2

data class FakeArRuntimeConfig(
    val capabilities: ArCapabilities = ArCapabilities(
        availability = ArAvailability.Ready,
        depthSupported = true,
        environmentalHdrSupported = true,
        instantPlacementSupported = true,
        message = "Deterministic AR runtime is ready.",
    ),
    val installationCompletesOnRequest: Boolean = true,
    val repeatLastFrame: Boolean = true,
)

/**
 * Deterministic runtime for JVM tests, simulator screens, and future ARCore recording playback.
 * It deliberately implements the same lifecycle restrictions as the live runtime contract.
 */
class FakeArRuntime(
    private val config: FakeArRuntimeConfig = FakeArRuntimeConfig(),
    frames: List<ArFrameSnapshot> = emptyList(),
    hits: List<ArHitCandidate> = emptyList(),
) : ArRuntime {
    private var depthEnabled = false
    private val scriptedFrames = frames.toMutableList()
    private var scriptedHits = hits.toList()
    private var frameIndex = 0
    private var availability = config.capabilities.availability
    private val anchorMap = linkedMapOf<String, ArAnchorHandle>()
    private var anchorCounter = 0
    private var displayGeometry: Triple<Int, Int, Int>? = null
    private var cameraTextureName: Int? = null

    override var state: ArRuntimeState = ArRuntimeState.Idle
        private set

    @Synchronized
    override fun checkAvailability(): ArRuntimeState {
        ensureOpen()
        state = when (availability) {
            ArAvailability.Checking -> ArRuntimeState.Checking
            ArAvailability.Ready -> ArRuntimeState.Ready(config.capabilities.copy(availability = ArAvailability.Ready))
            ArAvailability.InstallRequired -> ArRuntimeState.InstallRequired(reason = "Google Play Services for AR is not installed.")
            ArAvailability.UpdateRequired -> ArRuntimeState.InstallRequired(update = true, reason = "Google Play Services for AR must be updated.")
            ArAvailability.Unsupported -> ArRuntimeState.Unsupported(config.capabilities.message.ifBlank { "AR is unsupported on this device." })
            ArAvailability.Error -> ArRuntimeState.RecoverableError(config.capabilities.message.ifBlank { "AR availability could not be determined." })
        }
        return state
    }

    @Synchronized
    override fun prepare(cameraPermissionGranted: Boolean, userRequestedInstall: Boolean): ArRuntimeState {
        ensureOpen()
        if (!cameraPermissionGranted) {
            state = ArRuntimeState.PermissionRequired()
            return state
        }
        if (availability == ArAvailability.InstallRequired || availability == ArAvailability.UpdateRequired) {
            if (!userRequestedInstall) {
                state = ArRuntimeState.InstallRequired(
                    update = availability == ArAvailability.UpdateRequired,
                    reason = "Installation requires an explicit user action.",
                )
                return state
            }
            val updating = availability == ArAvailability.UpdateRequired
            state = ArRuntimeState.InstallRequested(updating)
            if (config.installationCompletesOnRequest) availability = ArAvailability.Ready
            return state
        }
        val checked = checkAvailability()
        if (checked !is ArRuntimeState.Ready) return checked
        state = ArRuntimeState.CreatingSession
        state = ArRuntimeState.Ready(config.capabilities.copy(availability = ArAvailability.Ready))
        return state
    }

    @Synchronized
    override fun resume(): Result<ArRuntimeState> = runCatching {
        ensureOpen()
        val capabilities = when (val current = state) {
            is ArRuntimeState.Ready -> current.capabilities
            is ArRuntimeState.Paused -> current.capabilities
            is ArRuntimeState.Running -> return@runCatching current
            else -> error("Runtime must be ready or paused before resume; current state is ${current::class.simpleName}.")
        }
        ArRuntimeState.Running(capabilities).also { state = it }
    }

    @Synchronized
    override fun pause(): ArRuntimeState {
        ensureOpen()
        state = when (val current = state) {
            is ArRuntimeState.Running -> ArRuntimeState.Paused(current.capabilities)
            is ArRuntimeState.Ready -> ArRuntimeState.Paused(current.capabilities)
            is ArRuntimeState.Paused -> current
            else -> current
        }
        return state
    }

    @Synchronized
    override fun setDisplayGeometry(rotation: Int, width: Int, height: Int) {
        ensureOpen()
        require(rotation in 0..3) { "Display rotation must be one of Android's four rotation constants." }
        require(width > 0 && height > 0)
        displayGeometry = Triple(rotation, width, height)
    }

    @Synchronized
    override fun setCameraTextureName(textureName: Int) {
        ensureOpen()
        require(textureName > 0)
        cameraTextureName = textureName
    }

    override fun setDepthEnabled(enabled: Boolean) {
        depthEnabled = enabled
    }

    @Synchronized
    override fun updateFrame(): Result<ArFrameSnapshot> = runCatching {
        ensureOpen()
        require(state is ArRuntimeState.Running) { "Runtime must be running before frames are updated." }
        require(scriptedFrames.isNotEmpty()) { "No deterministic frames were supplied." }
        if (frameIndex >= scriptedFrames.size && !config.repeatLastFrame) error("The deterministic frame script is exhausted.")
        val frame = scriptedFrames[frameIndex.coerceAtMost(scriptedFrames.lastIndex)]
        if (frameIndex < scriptedFrames.size) frameIndex++
        if (depthEnabled) frame else frame.copy(depth = null)
    }

    @Synchronized
    override fun hitTest(screenPoint: ArVector2): List<ArHitCandidate> {
        ensureOpen()
        if (state !is ArRuntimeState.Running) return emptyList()
        // Reading the point validates the same finite screen-space contract as the live adapter.
        screenPoint.x + screenPoint.y
        return ArHitPolicy.rank(scriptedHits)
    }

    @Synchronized
    override fun createAnchor(hitId: String, nowMillis: Long): Result<ArAnchorHandle> = runCatching {
        ensureOpen()
        require(state is ArRuntimeState.Running) { "Runtime must be running before an anchor is created." }
        require(nowMillis >= 0L)
        val hit = scriptedHits.firstOrNull { it.id == hitId } ?: error("Unknown or expired hit $hitId.")
        val id = "fake-anchor-${++anchorCounter}"
        ArAnchorHandle(id, hit.pose, ArAnchorTrackingState.Tracking, nowMillis, nowMillis).also { anchorMap[id] = it }
    }

    @Synchronized
    override fun anchors(): List<ArAnchorHandle> = anchorMap.values.toList()

    @Synchronized
    override fun detachAnchor(anchorId: String): Boolean = anchorMap.remove(anchorId) != null

    @Synchronized
    override fun close() {
        anchorMap.clear()
        scriptedHits = emptyList()
        scriptedFrames.clear()
        state = ArRuntimeState.Closed
    }

    @Synchronized
    fun enqueueFrame(frame: ArFrameSnapshot) {
        ensureOpen()
        scriptedFrames += frame
    }

    @Synchronized
    fun setHits(hits: List<ArHitCandidate>) {
        ensureOpen()
        scriptedHits = hits.toList()
    }

    @Synchronized
    fun updateAnchor(anchor: ArAnchorHandle): Boolean {
        ensureOpen()
        if (anchor.id !in anchorMap) return false
        anchorMap[anchor.id] = anchor
        return true
    }

    @Synchronized
    fun completeInstallation() {
        ensureOpen()
        availability = ArAvailability.Ready
        state = ArRuntimeState.Ready(config.capabilities.copy(availability = ArAvailability.Ready))
    }

    fun currentDisplayGeometry(): Triple<Int, Int, Int>? = displayGeometry
    fun currentCameraTextureName(): Int? = cameraTextureName

    private fun ensureOpen() {
        check(state !is ArRuntimeState.Closed) { "AR runtime is closed." }
    }
}
