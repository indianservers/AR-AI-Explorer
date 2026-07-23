package com.indianservers.aiexplorer.arengine.contract

enum class ArAvailability {
    Checking,
    Ready,
    InstallRequired,
    UpdateRequired,
    Unsupported,
    Error,
}

data class ArCapabilities(
    val availability: ArAvailability,
    val depthSupported: Boolean = false,
    val environmentalHdrSupported: Boolean = false,
    val instantPlacementSupported: Boolean = false,
    val geospatialSupported: Boolean = false,
    val message: String = "",
)

sealed interface ArRuntimeState {
    data object Idle : ArRuntimeState
    data object Checking : ArRuntimeState
    data class Unsupported(val reason: String) : ArRuntimeState
    data class InstallRequired(val update: Boolean = false, val reason: String = "") : ArRuntimeState
    data class InstallRequested(val update: Boolean = false) : ArRuntimeState
    data class PermissionRequired(val permanentlyDenied: Boolean = false) : ArRuntimeState
    data object CreatingSession : ArRuntimeState
    data class Ready(val capabilities: ArCapabilities) : ArRuntimeState
    data class Running(val capabilities: ArCapabilities) : ArRuntimeState
    data class Paused(val capabilities: ArCapabilities) : ArRuntimeState
    data class RecoverableError(val message: String, val causeType: String? = null) : ArRuntimeState
    data class FatalError(val message: String, val causeType: String? = null) : ArRuntimeState
    data object Closed : ArRuntimeState
}

enum class ArTrackingState { Stopped, Paused, Tracking, Lost }

enum class ArTrackingFailure {
    None,
    Initializing,
    ExcessiveMotion,
    InsufficientFeatures,
    InsufficientLight,
    CameraUnavailable,
    Relocalizing,
    Unknown,
}

enum class ArPlaneOrientation { HorizontalUp, HorizontalDown, Vertical, Arbitrary }

data class ArPlaneSnapshot(
    val id: String,
    val centerPose: ArPose,
    val orientation: ArPlaneOrientation,
    val extentXMeters: Double,
    val extentZMeters: Double,
    val polygonMeters: List<ArVector3> = emptyList(),
    val trackingState: ArTrackingState = ArTrackingState.Tracking,
) {
    init {
        require(id.isNotBlank())
        require(extentXMeters.isFinite() && extentXMeters >= 0.0)
        require(extentZMeters.isFinite() && extentZMeters >= 0.0)
    }
}

enum class ArHitType { Plane, Depth, OrientedPoint, InstantPlacement, Simulator }

data class ArHitCandidate(
    val id: String,
    val type: ArHitType,
    val pose: ArPose,
    val distanceMeters: Double,
    val confidence: Double,
    val uncertaintyMeters: Double,
    val trackableId: String? = null,
) {
    init {
        require(id.isNotBlank())
        require(distanceMeters.isFinite() && distanceMeters >= 0.0)
        require(confidence in 0.0..1.0)
        require(uncertaintyMeters.isFinite() && uncertaintyMeters >= 0.0)
    }
}

enum class ArAnchorTrackingState { Tracking, Paused, Stopped, Relocalizing, Lost }

data class ArAnchorHandle(
    val id: String,
    val pose: ArPose,
    val trackingState: ArAnchorTrackingState,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
) {
    init {
        require(id.isNotBlank())
        require(createdAtMillis >= 0L)
        require(updatedAtMillis >= createdAtMillis)
    }
}

data class ArLightEstimate(
    val valid: Boolean = false,
    val pixelIntensity: Float = 1f,
    val mainLightDirection: ArVector3 = ArVector3(0.0, -1.0, 0.0),
    val mainLightIntensity: ArVector3 = ArVector3(1.0, 1.0, 1.0),
    val sphericalHarmonics: List<Float> = emptyList(),
) {
    init {
        require(pixelIntensity.isFinite() && pixelIntensity >= 0f)
        require(sphericalHarmonics.all(Float::isFinite))
    }
}

/** CPU-owned depth sample copy. Values are camera-axis depth in millimetres, not ray length. */
data class ArDepthSnapshot(
    val timestampNanos: Long,
    val width: Int,
    val height: Int,
    val millimeters: List<Int>,
    val confidence: List<Float> = emptyList(),
    /** Depth-image UV coordinates in bottom-left, bottom-right, top-left, top-right order. */
    val textureCoordinates: List<ArVector2> = DefaultCameraTextureCoordinates,
) {
    init {
        require(timestampNanos >= 0L)
        require(width > 0 && height > 0)
        require(millimeters.size == width * height)
        require(millimeters.all { it in 0..65_535 })
        require(confidence.isEmpty() || confidence.size == millimeters.size)
        require(confidence.all { it in 0f..1f })
        require(textureCoordinates.size == 4)
    }
}

data class ArCameraSnapshot(
    val pose: ArPose,
    val trackingState: ArTrackingState,
    val trackingFailure: ArTrackingFailure = ArTrackingFailure.None,
    val viewMatrix: ArMatrix4 = ArMatrix4.Identity,
    val projectionMatrix: ArMatrix4 = ArMatrix4.Identity,
    /**
     * Camera texture coordinates in triangle-strip order:
     * bottom-left, bottom-right, top-left, top-right.
     *
     * ARCore transforms these whenever display geometry changes, keeping platform texture
     * details outside the renderer.
     */
    val textureCoordinates: List<ArVector2> = DefaultCameraTextureCoordinates,
)

val DefaultCameraTextureCoordinates = listOf(
    ArVector2(0f, 1f),
    ArVector2(1f, 1f),
    ArVector2(0f, 0f),
    ArVector2(1f, 0f),
)

data class ArFrameSnapshot(
    val timestampNanos: Long,
    val camera: ArCameraSnapshot,
    val planes: List<ArPlaneSnapshot> = emptyList(),
    val lighting: ArLightEstimate = ArLightEstimate(),
    val depth: ArDepthSnapshot? = null,
    val displayGeometryChanged: Boolean = false,
) {
    init {
        require(timestampNanos >= 0L)
        require(depth == null || depth.timestampNanos == timestampNanos)
    }
}

/**
 * Runtime boundary implemented by live ARCore and by the deterministic simulator/fake runtime.
 * Screen coordinates passed to [hitTest] are physical pixels in the current display viewport.
 */
interface ArRuntime : AutoCloseable {
    val state: ArRuntimeState

    fun checkAvailability(): ArRuntimeState
    fun checkAvailabilityAsync(callback: (ArRuntimeState) -> Unit) {
        callback(checkAvailability())
    }
    fun prepare(cameraPermissionGranted: Boolean, userRequestedInstall: Boolean = true): ArRuntimeState
    fun resume(): Result<ArRuntimeState>
    fun pause(): ArRuntimeState
    fun setDisplayGeometry(rotation: Int, width: Int, height: Int)
    fun setCameraTextureName(textureName: Int)
    /** Enables CPU depth acquisition only while an interaction or renderer needs it. */
    fun setDepthEnabled(enabled: Boolean) = Unit
    fun updateFrame(): Result<ArFrameSnapshot>
    fun hitTest(screenPoint: ArVector2): List<ArHitCandidate>
    fun createAnchor(hitId: String, nowMillis: Long): Result<ArAnchorHandle>
    fun anchors(): List<ArAnchorHandle>
    fun detachAnchor(anchorId: String): Boolean
    override fun close()
}

object ArHitPolicy {
    private val priority = mapOf(
        ArHitType.Plane to 0,
        ArHitType.Depth to 1,
        ArHitType.OrientedPoint to 2,
        ArHitType.InstantPlacement to 3,
        ArHitType.Simulator to 4,
    )

    fun rank(candidates: Iterable<ArHitCandidate>): List<ArHitCandidate> = candidates
        .filter { it.confidence > 0.0 }
        .sortedWith(
            compareBy<ArHitCandidate>(
                { priority.getValue(it.type) },
                { -it.confidence },
                { it.uncertaintyMeters },
                { it.distanceMeters },
                { it.id },
            ),
        )
}
