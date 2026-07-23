package com.indianservers.aiexplorer.spatial

import com.indianservers.aiexplorer.arengine.contract.ArAnchorHandle
import com.indianservers.aiexplorer.arengine.contract.ArAvailability
import com.indianservers.aiexplorer.arengine.contract.ArCapabilities
import com.indianservers.aiexplorer.arengine.contract.ArFrameSnapshot
import com.indianservers.aiexplorer.arengine.contract.ArHitCandidate
import com.indianservers.aiexplorer.arengine.contract.ArHitType
import com.indianservers.aiexplorer.arengine.contract.ArPose
import com.indianservers.aiexplorer.arengine.contract.ArRuntimeState
import com.indianservers.aiexplorer.arengine.contract.ArTrackingState
import com.indianservers.aiexplorer.core.Vec3
import kotlin.math.asin
import kotlin.math.atan2

fun ArRuntimeState.toSpatialCapabilities(): ARCapabilities {
    val source = when (this) {
        is ArRuntimeState.Ready -> capabilities
        is ArRuntimeState.Running -> capabilities
        is ArRuntimeState.Paused -> capabilities
        else -> null
    }
    return ARCapabilities(
        availability = when (this) {
            ArRuntimeState.Idle, ArRuntimeState.Checking, ArRuntimeState.CreatingSession -> com.indianservers.aiexplorer.spatial.ARAvailability.Checking
            is ArRuntimeState.InstallRequired -> if (update) com.indianservers.aiexplorer.spatial.ARAvailability.UpdateRequired else com.indianservers.aiexplorer.spatial.ARAvailability.InstallRequired
            is ArRuntimeState.InstallRequested -> if (update) com.indianservers.aiexplorer.spatial.ARAvailability.UpdateRequired else com.indianservers.aiexplorer.spatial.ARAvailability.InstallRequired
            is ArRuntimeState.Unsupported -> com.indianservers.aiexplorer.spatial.ARAvailability.Unsupported
            is ArRuntimeState.RecoverableError, is ArRuntimeState.FatalError, ArRuntimeState.Closed -> com.indianservers.aiexplorer.spatial.ARAvailability.Error
            is ArRuntimeState.PermissionRequired, is ArRuntimeState.Ready, is ArRuntimeState.Running, is ArRuntimeState.Paused -> com.indianservers.aiexplorer.spatial.ARAvailability.Ready
        },
        depthSupported = source?.depthSupported == true,
        geospatialSupported = source?.geospatialSupported == true,
        message = when (this) {
            ArRuntimeState.Idle -> "ARCore is ready to be checked."
            ArRuntimeState.Checking -> "Checking this device with ARCore…"
            is ArRuntimeState.Unsupported -> reason
            is ArRuntimeState.InstallRequired -> reason.ifBlank { if (update) "Google Play Services for AR must be updated." else "Google Play Services for AR is required." }
            is ArRuntimeState.InstallRequested -> "Finish ${if (update) "updating" else "installing"} Google Play Services for AR, then return here."
            is ArRuntimeState.PermissionRequired -> "Camera permission is needed only for live AR. The simulator remains available."
            ArRuntimeState.CreatingSession -> "Creating the ARCore session…"
            is ArRuntimeState.Ready -> capabilities.summary("session configured")
            is ArRuntimeState.Running -> capabilities.summary("live AR running")
            is ArRuntimeState.Paused -> capabilities.summary("live AR paused")
            is ArRuntimeState.RecoverableError -> message
            is ArRuntimeState.FatalError -> message
            ArRuntimeState.Closed -> "The live AR session is closed; the simulator remains available."
        },
    )
}

fun ArFrameSnapshot.toSpatialFrame(): ARFrameState = ARFrameState(
    timestampNanos = timestampNanos,
    trackingQuality = camera.trackingState.toSpatialTracking(),
    cameraPose = camera.pose.toSpatialPose(),
    hitsAvailable = planes.any { it.trackingState == ArTrackingState.Tracking },
    trackedPlaneCount = planes.count { it.trackingState == ArTrackingState.Tracking },
    depthAvailable = depth != null,
    lighting = EnvironmentLighting(
        pixelIntensity = lighting.pixelIntensity,
        mainLightDirection = lighting.mainLightDirection.toVec3(),
        mainLightIntensity = lighting.mainLightIntensity.toVec3(),
        sphericalHarmonics = lighting.sphericalHarmonics,
        valid = lighting.valid,
    ),
    relocalizing = camera.trackingState == ArTrackingState.Paused,
    displayGeometryChanged = displayGeometryChanged,
    viewMatrix = camera.viewMatrix.values,
    projectionMatrix = camera.projectionMatrix.values,
)

fun ArHitCandidate.toSpatialHit(): SpatialHit = SpatialHit(
    type = when (type) {
        ArHitType.Plane -> SpatialHitType.Plane
        ArHitType.Depth -> SpatialHitType.Depth
        ArHitType.OrientedPoint -> SpatialHitType.Point
        ArHitType.InstantPlacement -> SpatialHitType.InstantPlacement
        ArHitType.Simulator -> SpatialHitType.Simulator
    },
    positionMeters = pose.positionMeters.toVec3(),
    distanceMeters = distanceMeters,
    confidence = confidence,
    uncertaintyMeters = uncertaintyMeters,
    trackableId = id,
)

fun ArHitCandidate.previewSpatialPlacement(current: SpatialScenePlacement): SpatialScenePlacement = current.copy(
    anchorId = "",
    pose = pose.toSpatialPose().copy(uniformScale = current.pose.uniformScale),
    trackingQuality = TrackingQuality.Tracking,
    estimated = true,
    measurementUncertaintyMeters = uncertaintyMeters,
)

fun ArAnchorHandle.toPersistentSpatialAnchor(lessonId: String? = null) = PersistentSpatialAnchor(
    id = id,
    pose = pose.toSpatialPose(),
    createdAt = createdAtMillis,
    updatedAt = updatedAtMillis,
    trackingState = when (trackingState) {
        com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState.Tracking -> AnchorTrackingState.Tracking
        com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState.Paused -> AnchorTrackingState.Paused
        com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState.Stopped -> AnchorTrackingState.Stopped
        com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState.Relocalizing -> AnchorTrackingState.Relocalizing
        com.indianservers.aiexplorer.arengine.contract.ArAnchorTrackingState.Lost -> AnchorTrackingState.Lost
    },
    lessonId = lessonId,
)

private fun ArCapabilities.summary(state: String): String = buildString {
    append("ARCore $state")
    if (depthSupported) append(" with Depth")
    if (environmentalHdrSupported) append(" and environmental HDR")
    append('.')
}

private fun ArTrackingState.toSpatialTracking() = when (this) {
    ArTrackingState.Tracking -> TrackingQuality.Tracking
    ArTrackingState.Paused -> TrackingQuality.Limited
    ArTrackingState.Stopped -> TrackingQuality.Stopped
    ArTrackingState.Lost -> TrackingQuality.Lost
}

fun ArPose.toSpatialPose(): SpatialPose {
    val q = orientation.normalized()
    val sinRollCosPitch = 2.0 * (q.w * q.x + q.y * q.z)
    val cosRollCosPitch = 1.0 - 2.0 * (q.x * q.x + q.y * q.y)
    val sinPitch = 2.0 * (q.w * q.y - q.z * q.x)
    val sinYawCosPitch = 2.0 * (q.w * q.z + q.x * q.y)
    val cosYawCosPitch = 1.0 - 2.0 * (q.y * q.y + q.z * q.z)
    val roll = atan2(sinRollCosPitch, cosRollCosPitch)
    val pitch = if (kotlin.math.abs(sinPitch) >= 1.0) {
        if (sinPitch < 0.0) -Math.PI / 2.0 else Math.PI / 2.0
    } else {
        asin(sinPitch)
    }
    val yaw = atan2(sinYawCosPitch, cosYawCosPitch)
    return SpatialPose(
        positionMeters = positionMeters.toVec3(),
        rotationDegrees = Vec3(Math.toDegrees(roll), Math.toDegrees(pitch), Math.toDegrees(yaw)),
    )
}

private fun com.indianservers.aiexplorer.arengine.contract.ArVector3.toVec3() = Vec3(x, y, z)
