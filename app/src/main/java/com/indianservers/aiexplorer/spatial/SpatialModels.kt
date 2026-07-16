package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.cos
import kotlin.math.sin

enum class ARAvailability { Checking, Ready, InstallRequired, UpdateRequired, Unsupported, Error }
enum class TrackingQuality { Stopped, Limited, Tracking, Lost }
enum class ARScaleMode { OneToOne, FitToSpace }

data class SpatialPose(
    val positionMeters: Vec3 = Vec3(0.0, 0.0, -1.2),
    val rotationDegrees: Vec3 = Vec3(0.0, 0.0, 0.0),
    val uniformScale: Double = 1.0,
)

data class SpatialScenePlacement(
    val anchorId: String = "",
    val pose: SpatialPose = SpatialPose(),
    val scaleMode: ARScaleMode = ARScaleMode.FitToSpace,
    val metersPerMathUnit: Double = 0.1,
    val trackingQuality: TrackingQuality = TrackingQuality.Stopped,
    val estimated: Boolean = true,
    val depthOcclusionEnabled: Boolean = false,
    val placedAt: Long? = null,
) {
    val isPlaced: Boolean get() = anchorId.isNotBlank()
    val visibleScale: String get() {
        val meters = metersPerMathUnit * pose.uniformScale
        return if (meters >= 1.0) "1 unit = ${"%.2f".format(java.util.Locale.US, meters)} m"
        else "1 unit = ${"%.0f".format(java.util.Locale.US, meters * 100)} cm"
    }
}

data class ARCapabilities(
    val availability: ARAvailability,
    val depthSupported: Boolean = false,
    val geospatialSupported: Boolean = false,
    val message: String = "Checking ARCore support…",
)

data class TrackingGuidance(val title: String, val instruction: String, val safeToPlace: Boolean)

object SpatialSafety {
    fun guidance(quality: TrackingQuality, lowLight: Boolean = false, movingTooFast: Boolean = false): TrackingGuidance = when {
        movingTooFast -> TrackingGuidance("Slow down", "Stop walking and move the phone slowly. Stay aware of your surroundings.", false)
        lowLight -> TrackingGuidance("More light needed", "Move to a well-lit area or continue in the non-AR spatial simulator.", false)
        quality == TrackingQuality.Tracking -> TrackingGuidance("Surface ready", "Tap Place while stationary; walk around only when the path is clear.", true)
        quality == TrackingQuality.Limited -> TrackingGuidance("Scan slowly", "Point at a textured floor or table and move the phone gently side to side.", false)
        quality == TrackingQuality.Lost -> TrackingGuidance("Tracking lost", "Hold still and return to the last well-tracked area. Math state is unchanged.", false)
        else -> TrackingGuidance("Spatial preview", "Use the seated simulator or enable ARCore when available.", false)
    }
}

object SpatialPlacementEngine {
    fun place(current: SpatialScenePlacement, hitPositionMeters: Vec3, now: Long): SpatialScenePlacement = current.copy(
        anchorId = "local-anchor-$now",
        pose = current.pose.copy(positionMeters = hitPositionMeters),
        trackingQuality = TrackingQuality.Tracking,
        estimated = true,
        placedAt = now,
    )

    fun move(current: SpatialScenePlacement, deltaMeters: Vec3): SpatialScenePlacement = current.copy(
        pose = current.pose.copy(positionMeters = current.pose.positionMeters + deltaMeters),
    )

    fun rotate(current: SpatialScenePlacement, deltaDegrees: Vec3): SpatialScenePlacement = current.copy(
        pose = current.pose.copy(rotationDegrees = current.pose.rotationDegrees + deltaDegrees),
    )

    fun scale(current: SpatialScenePlacement, factor: Double): SpatialScenePlacement = current.copy(
        pose = current.pose.copy(uniformScale = (current.pose.uniformScale * factor).coerceIn(0.1, 8.0)),
        scaleMode = ARScaleMode.FitToSpace,
    )

    fun setScaleMode(current: SpatialScenePlacement, mode: ARScaleMode): SpatialScenePlacement = current.copy(
        scaleMode = mode,
        pose = if (mode == ARScaleMode.OneToOne) current.pose.copy(uniformScale = 1.0) else current.pose,
    )
}

object MathSpaceTransform {
    fun mathToAnchorMeters(point: Vec3, placement: SpatialScenePlacement): Vec3 {
        val scale = placement.metersPerMathUnit * placement.pose.uniformScale
        return placement.pose.positionMeters + Quaternion.fromEulerDegrees(placement.pose.rotationDegrees).rotate(point * scale)
    }

    fun anchorMetersToMath(point: Vec3, placement: SpatialScenePlacement): Vec3 {
        val scale = placement.metersPerMathUnit * placement.pose.uniformScale
        require(scale > 0.0)
        return Quaternion.fromEulerDegrees(placement.pose.rotationDegrees).conjugate().rotate(point - placement.pose.positionMeters) * (1.0 / scale)
    }
}

private data class Quaternion(val w: Double, val x: Double, val y: Double, val z: Double) {
    fun conjugate() = Quaternion(w, -x, -y, -z)
    operator fun times(other: Quaternion) = Quaternion(
        w * other.w - x * other.x - y * other.y - z * other.z,
        w * other.x + x * other.w + y * other.z - z * other.y,
        w * other.y - x * other.z + y * other.w + z * other.x,
        w * other.z + x * other.y - y * other.x + z * other.w,
    )
    fun rotate(point: Vec3): Vec3 {
        val result = this * Quaternion(0.0, point.x, point.y, point.z) * conjugate()
        return Vec3(result.x, result.y, result.z)
    }

    companion object {
        fun fromEulerDegrees(rotation: Vec3): Quaternion {
            val hx = Math.toRadians(rotation.x) / 2; val hy = Math.toRadians(rotation.y) / 2; val hz = Math.toRadians(rotation.z) / 2
            val qx = Quaternion(cos(hx), sin(hx), 0.0, 0.0)
            val qy = Quaternion(cos(hy), 0.0, sin(hy), 0.0)
            val qz = Quaternion(cos(hz), 0.0, 0.0, sin(hz))
            return qz * qy * qx
        }
    }
}
